const { test } = require("node:test");
const assert = require("node:assert/strict");
const { randomUUID } = require("node:crypto");
const path = require("node:path");
const connectionString = process.env.TEST_DATABASE_URL;
if (!connectionString || !/^unitrack_test(?:_|$)/.test(new URL(connectionString).pathname.slice(1))) {
  throw new Error("TEST_DATABASE_URL must point to a dedicated unitrack_test database");
}
Object.assign(process.env, { DATABASE_URL: connectionString, NODE_ENV: "test", JWT_SECRET: "postgres-test-access-secret-over-32-characters", JWT_REFRESH_SECRET: "postgres-test-refresh-secret-over-32-characters", GOOGLE_CLIENT_ID: "test-client" });
const { Pool } = require("pg");
const { drizzle } = require("drizzle-orm/node-postgres");
const { migrate } = require("drizzle-orm/node-postgres/migrator");
const { eq, and, isNull } = require("drizzle-orm");
const schema = require("../dist/db/schema");

test("physical PostgreSQL migrations and concurrent imports/rotations/push claims", async () => {
  const pool = new Pool({ connectionString });
  const db = drizzle(pool, { schema });
  require("../dist/db").db = db;
  const { importSemesters } = require("../dist/modules/transcript-import/transcript-import.service");
  const { authRepository } = require("../dist/modules/auth/auth.repository");
  const { authService } = require("../dist/modules/auth/auth.service");
  const { generateRefreshToken } = require("../dist/lib/jwt");
  const { runPushCycle } = require("../dist/modules/push/push.jobs");
  let user;
  try {
    await migrate(db, { migrationsFolder: path.resolve(__dirname, "../drizzle") });
    await migrate(db, { migrationsFolder: path.resolve(__dirname, "../drizzle") });
    [user] = await db.insert(schema.users).values({ email: `postgres-${randomUUID()}@example.invalid`, name: "PostgreSQL acceptance" }).returning();
    const data = [{ year: 2026, term: "Güz", courses: [{ name: "Concurrent import", credit: 3, grade: "AA" }] }];
    const imports = await Promise.all([importSemesters(user.id, data), importSemesters(user.id, data)]);
    assert.equal(imports.reduce((total, item) => total + item.importedCourses, 0), 1);
    const token = generateRefreshToken({ userId: user.id });
    await authRepository.saveRefreshToken(user.id, token, new Date(Date.now() + 86400000));
    const rotations = await Promise.all([authService.refresh(token), authService.refresh(token), authService.refresh(token)]);
    assert.equal(new Set(rotations.map(result => result.refreshToken)).size, 1);
    const session = await authRepository.findRefreshToken(rotations[0].refreshToken);
    await db.insert(schema.pushSubscriptions).values({ userId: user.id, tokenId: session.id, endpoint: `https://fcm.googleapis.com/fcm/send/${randomUUID()}`, p256dh: "test-public-key", auth: "test-auth" });
    const [event] = await db.insert(schema.events).values({ userId: user.id, title: "Concurrent reminder", type: "exam", startAt: new Date(Date.now() + 59000) }).returning();
    await db.insert(schema.eventNotifications).values({ eventId: event.id, minutesBefore: 1 });
    let deliveries = 0;
    const sender = async () => { deliveries++; await new Promise(resolve => setTimeout(resolve, 50)); };
    await Promise.all([runPushCycle(sender), runPushCycle(sender), runPushCycle(sender)]);
    assert.equal(deliveries, 1, "replicas must claim a reminder only once");
    await runPushCycle(sender);
    assert.equal(deliveries, 1, "sent jobs must remain terminal");
    const gate = await pool.connect();
    const operations = [];
    try {
      await gate.query("BEGIN");
      await gate.query("SELECT id FROM refresh_tokens WHERE id = $1 FOR UPDATE", [session.id]);
      const waitForLocks = async count => {
        const deadline = Date.now() + 5000;
        while (Date.now() < deadline) {
          const result = await pool.query("SELECT count(*)::int AS n FROM pg_stat_activity WHERE datname = current_database() AND wait_event_type = 'Lock' AND pid <> pg_backend_pid()");
          if (result.rows[0].n >= count) return;
          await new Promise(resolve => setTimeout(resolve, 20));
        }
        throw new Error(`Expected ${count} concurrent lock waiters`);
      };
      operations.push(authService.refresh(rotations[0].refreshToken));
      await waitForLocks(1);
      operations.push(authService.logoutAll(user.id));
      await waitForLocks(2);
      await gate.query("COMMIT");
      await Promise.all(operations);
      const active = await db.select().from(schema.refreshTokens).where(and(eq(schema.refreshTokens.userId, user.id), isNull(schema.refreshTokens.revokedAt)));
      assert.equal(active.length, 0, "logout-all must include a concurrently rotated descendant");
    } finally {
      await gate.query("ROLLBACK");
      gate.release();
      await Promise.allSettled(operations);
    }
  } finally {
    if (user) await db.delete(schema.users).where(eq(schema.users.id, user.id));
    await pool.end();
  }
});
