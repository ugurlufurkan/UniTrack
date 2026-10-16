const { test } = require("node:test");
const assert = require("node:assert/strict");
const { createDatabase } = require("./database.cjs");
Object.assign(process.env, { DATABASE_URL: "postgresql://test:test@127.0.0.1:1/test", JWT_SECRET: "session-test-access-secret-over-32-characters", JWT_REFRESH_SECRET: "session-test-refresh-secret-over-32-characters", GOOGLE_CLIENT_ID: "test-client", NODE_ENV: "test" });

test("revoking a device invalidates access immediately, including a rotated descendant", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const { authRepository } = require("../dist/modules/auth/auth.repository");
  const { authService } = require("../dist/modules/auth/auth.service");
  const { authMiddleware } = require("../dist/shared/middleware/auth.middleware");
  const { generateAccessToken, generateRefreshToken } = require("../dist/lib/jwt");
  const authorize = async token => {
    let status = 200, called = false;
    const response = { status(code) { status = code; return this; }, json() { return this; } };
    await authMiddleware({ headers: { authorization: `Bearer ${token}` }, cookies: {} }, response, error => { if (error) throw error; called = true; });
    return { status, called };
  };
  try {
    const [user] = await db.insert(schema.users).values({ email: "session@example.invalid", name: "Session" }).returning();
    const original = generateRefreshToken({ userId: user.id });
    const sessionId = await authRepository.saveRefreshToken(user.id, original, new Date(Date.now() + 86400000));
    const access = generateAccessToken({ userId: user.id, email: user.email, sessionId });
    assert.equal((await authorize(access)).called, true);
    const rotated = await authService.refresh(original);
    assert.equal((await authorize(access)).status, 401);
    assert.equal((await authorize(rotated.accessToken)).called, true);
    const other = generateRefreshToken({ userId: user.id });
    const otherId = await authRepository.saveRefreshToken(user.id, other, new Date(Date.now() + 86400000));
    const otherAccess = generateAccessToken({ userId: user.id, email: user.email, sessionId: otherId });
    await authService.logout(original);
    assert.equal((await authorize(rotated.accessToken)).status, 401, "late refresh response cannot resurrect a logged-out session");
    assert.equal((await authorize(otherAccess)).called, true, "another device remains active");
  } finally { await client.close(); }
});
