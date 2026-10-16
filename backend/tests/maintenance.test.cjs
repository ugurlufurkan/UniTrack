const { test } = require("node:test");
const assert = require("node:assert/strict");
const { createDatabase } = require("./database.cjs");
test("maintenance deletes only expired transient records and preserves active sessions and accounts", async () => {
  const { client, db, schema } = await createDatabase();
  require("../dist/db").db = db;
  const { cleanupEphemeralRecords } = require("../dist/lib/maintenance");
  try {
    const [user] = await db.insert(schema.users).values({ email: "cleanup@example.invalid", name: "Retained user" }).returning();
    await db.insert(schema.refreshTokens).values([
      { userId: user.id, token: "expired-old", expiresAt: new Date(Date.now() - 9 * 86400000) },
      { userId: user.id, token: "recent-history", expiresAt: new Date(Date.now() - 86400000) },
      { userId: user.id, token: "active", expiresAt: new Date(Date.now() + 86400000) },
    ]);
    await db.insert(schema.rateLimitBuckets).values([
      { key: "expired", hits: 10, resetAt: new Date(Date.now() - 2 * 86400000) },
      { key: "active", hits: 1, resetAt: new Date(Date.now() + 60000) },
    ]);
    await cleanupEphemeralRecords();
    assert.equal((await db.select().from(schema.users)).length, 1);
    assert.deepEqual((await db.select().from(schema.refreshTokens)).map(row => row.token).sort(), ["active", "recent-history"]);
    assert.deepEqual((await db.select().from(schema.rateLimitBuckets)).map(row => row.key), ["active"]);
  } finally { await client.close(); }
});
