const { test } = require("node:test");
const assert = require("node:assert/strict");
const { eq, isNull, and } = require("drizzle-orm");
const { createDatabase } = require("./database.cjs");
Object.assign(process.env, { DATABASE_URL: "postgresql://test:test@127.0.0.1:1/test", JWT_SECRET: "recovery-test-access-secret-over-32-characters", JWT_REFRESH_SECRET: "recovery-test-refresh-secret-over-32-characters", GOOGLE_CLIENT_ID: "test-client", NODE_ENV: "test" });

test("recovery links can be consumed only once, even by simultaneous requests", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const email = require("../dist/lib/email");
  const originals = { reset: email.sendPasswordResetEmail, verify: email.sendVerificationEmail };
  let resetToken, verifyToken;
  email.sendPasswordResetEmail = async (_email, token) => { resetToken = token; return true; };
  email.sendVerificationEmail = async (_email, token) => { verifyToken = token; return true; };
  const { authService } = require("../dist/modules/auth/auth.service");
  const { authRepository } = require("../dist/modules/auth/auth.repository");
  const { generateRefreshToken } = require("../dist/lib/jwt");
  try {
    const [user] = await db.insert(schema.users).values({ email: "recovery@example.invalid", name: "Recovery" }).returning();
    await authRepository.saveRefreshToken(user.id, generateRefreshToken({ userId: user.id }), new Date(Date.now() + 60000));
    await authService.forgotPassword(user.email);
    assert.notEqual((await authRepository.findById(user.id)).passwordResetToken, resetToken, "raw recovery credentials must not be stored");
    const results = await Promise.allSettled([authService.resetPassword(resetToken, "NewPassword123!"), authService.resetPassword(resetToken, "OtherPassword123!")]);
    assert.equal(results.filter(result => result.status === "fulfilled").length, 1);
    assert.equal((await db.select().from(schema.refreshTokens).where(and(eq(schema.refreshTokens.userId, user.id), isNull(schema.refreshTokens.revokedAt)))).length, 0);
    const [updated] = await db.select().from(schema.users).where(eq(schema.users.id, user.id));
    assert.equal(updated.passwordResetToken, null);
    assert.equal(updated.passwordResetExpiresAt, null);
    await authService.requestEmailVerification(user.id);
    assert.notEqual((await authRepository.findById(user.id)).emailVerificationToken, verifyToken);
    const verifications = await Promise.allSettled([authService.verifyEmail(verifyToken), authService.verifyEmail(verifyToken)]);
    assert.equal(verifications.filter(result => result.status === "fulfilled").length, 1);
    const [verified] = await db.select().from(schema.users).where(eq(schema.users.id, user.id));
    assert.equal(verified.isEmailVerified, true);
    assert.equal(verified.emailVerificationToken, null);
    assert.equal(verified.emailVerificationExpiresAt, null);
  } finally { email.sendPasswordResetEmail = originals.reset; email.sendVerificationEmail = originals.verify; await client.close(); }
});

test("expired recovery links cannot modify an account", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const { hashRecoveryToken } = require("../dist/lib/jwt");
  const { authService } = require("../dist/modules/auth/auth.service");
  try {
    const [user] = await db.insert(schema.users).values({ email: "expired@example.invalid", name: "Expired", passwordResetToken: hashRecoveryToken("expired-reset"), passwordResetExpiresAt: new Date(0), emailVerificationToken: hashRecoveryToken("expired-verify"), emailVerificationExpiresAt: new Date(0) }).returning();
    await assert.rejects(authService.resetPassword("expired-reset", "Password123!"));
    await assert.rejects(authService.verifyEmail("expired-verify"));
    const [unchanged] = await db.select().from(schema.users).where(eq(schema.users.id, user.id));
    assert.equal(unchanged.password, null);
    assert.equal(unchanged.isEmailVerified, false);
  } finally { await client.close(); }
});

test("changing a password invalidates outstanding reset links and stale login credentials", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const email = require("../dist/lib/email");
  const original = email.sendPasswordResetEmail;
  let resetToken;
  email.sendPasswordResetEmail = async (_email, token) => { resetToken = token; return true; };
  const { authService } = require("../dist/modules/auth/auth.service");
  const { authRepository } = require("../dist/modules/auth/auth.repository");
  const { hashPassword } = require("../dist/lib/password");
  const { generateRefreshToken } = require("../dist/lib/jwt");
  try {
    const password = await hashPassword("OriginalPassword123!");
    const [user] = await db.insert(schema.users).values({ email: "password@example.invalid", name: "Password", password }).returning();
    await authService.forgotPassword(user.email);
    await authService.changePassword(user.id, "OriginalPassword123!", "ChangedPassword123!");
    await assert.rejects(authService.resetPassword(resetToken, "StolenLinkPassword123!"));
    await assert.rejects(authRepository.saveRefreshToken(user.id, generateRefreshToken({ userId: user.id }), new Date(Date.now() + 60000), undefined, password));
  } finally { email.sendPasswordResetEmail = original; await client.close(); }
});
