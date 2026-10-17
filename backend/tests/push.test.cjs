const { test } = require("node:test");
const assert = require("node:assert/strict");
const { createECDH, randomBytes } = require("node:crypto");
const { eq } = require("drizzle-orm");
const { createDatabase } = require("./database.cjs");
Object.assign(process.env, { DATABASE_URL: "postgresql://test:test@127.0.0.1:1/test", JWT_SECRET: "push-test-access-secret-over-32-characters", JWT_REFRESH_SECRET: "push-test-refresh-secret-over-32-characters", GOOGLE_CLIENT_ID: "test-client", NODE_ENV: "test" });

test("push endpoints reject SSRF and calendar recurrence preserves local wall time", () => {
  const { subscriptionSchema, dueOccurrences } = require("../dist/modules/push/push.config");
  const pair = createECDH("prime256v1"); pair.generateKeys();
  const keys = { p256dh: pair.getPublicKey().toString("base64url"), auth: randomBytes(16).toString("base64url") };
  assert.equal(subscriptionSchema.safeParse({ endpoint: "https://fcm.googleapis.com/fcm/send/test", keys }).success, true);
  for (const endpoint of ["http://fcm.googleapis.com/test", "https://127.0.0.1/private", "https://fcm.googleapis.com.evil.com/test", "https://user:secret@fcm.googleapis.com/test"]) assert.equal(subscriptionSchema.safeParse({ endpoint, keys }).success, false);
  const original = process.env.PUSH_TIMEZONE;
  try {
    process.env.PUSH_TIMEZONE = "Europe/Istanbul";
    assert.equal(dueOccurrences(new Date("2026-01-30T22:00:00Z"), "monthly", 0, new Date("2026-02-27T22:00:00Z"), 0).length, 1, "Jan 31 local clamps to Feb 28 local");
    process.env.PUSH_TIMEZONE = "Europe/Berlin";
    assert.equal(dueOccurrences(new Date("2026-03-28T08:00:00Z"), "daily", 0, new Date("2026-03-29T07:00:00Z"), 0).length, 1, "09:00 stays local 09:00 across DST");
  } finally { if (original === undefined) delete process.env.PUSH_TIMEZONE; else process.env.PUSH_TIMEZONE = original; }
});

test("durable push jobs deduplicate replicas, follow rotations, stop on logout and remove gone subscriptions", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const { runPushCycle } = require("../dist/modules/push/push.jobs");
  const { authRepository } = require("../dist/modules/auth/auth.repository");
  const { authService } = require("../dist/modules/auth/auth.service");
  const { generateRefreshToken, verifyAccessToken } = require("../dist/lib/jwt");
  try {
    const [user] = await db.insert(schema.users).values({ email: "push@example.invalid", name: "Push test" }).returning();
    const original = generateRefreshToken({ userId: user.id });
    const tokenId = await authRepository.saveRefreshToken(user.id, original, new Date(Date.now() + 86400000));
    const [subscription] = await db.insert(schema.pushSubscriptions).values({ userId: user.id, tokenId, endpoint: "https://fcm.googleapis.com/fcm/send/test-one", p256dh: "fixture-key", auth: "fixture-auth" }).returning();
    const [event] = await db.insert(schema.events).values({ userId: user.id, title: "Exam", type: "exam", startAt: new Date(Date.now() + 59000) }).returning();
    await db.insert(schema.eventNotifications).values({ eventId: event.id, minutesBefore: 1 });
    const messages = [];
    const sender = async (_subscription, payload) => messages.push(JSON.parse(payload));
    await Promise.all([runPushCycle(sender), runPushCycle(sender)]);
    assert.equal(messages.length, 1);
    assert.equal(messages[0].owner, user.id);
    await runPushCycle(sender);
    assert.equal(messages.length, 1, "completed job must not send twice");
    const rotated = await authService.refresh(original);
    const [updated] = await db.select().from(schema.pushSubscriptions).where(eq(schema.pushSubscriptions.id, subscription.id));
    assert.equal(updated.tokenId, verifyAccessToken(rotated.accessToken).sessionId);
    await authService.logout(original);
    const [nextEvent] = await db.insert(schema.events).values({ userId: user.id, title: "Revoked exam", type: "exam", startAt: new Date(Date.now() + 59000) }).returning();
    await db.insert(schema.eventNotifications).values({ eventId: nextEvent.id, minutesBefore: 1 });
    await runPushCycle(sender);
    assert.equal(messages.length, 1, "logged-out subscription must not receive reminders");
    const nextToken = generateRefreshToken({ userId: user.id });
    const nextId = await authRepository.saveRefreshToken(user.id, nextToken, new Date(Date.now() + 86400000));
    const [gone] = await db.insert(schema.pushSubscriptions).values({ userId: user.id, tokenId: nextId, endpoint: "https://fcm.googleapis.com/fcm/send/gone", p256dh: "fixture-key", auth: "fixture-auth" }).returning();
    await runPushCycle(async () => { throw { statusCode: 410 }; });
    assert.equal((await db.select().from(schema.pushSubscriptions).where(eq(schema.pushSubscriptions.id, gone.id))).length, 0);
  } finally { await client.close(); }
});

test("a logout during one delivery stops the remaining claimed reminders", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const { runPushCycle } = require("../dist/modules/push/push.jobs");
  const { authRepository } = require("../dist/modules/auth/auth.repository");
  const { authService } = require("../dist/modules/auth/auth.service");
  const { generateRefreshToken } = require("../dist/lib/jwt");
  try {
    const [user] = await db.insert(schema.users).values({ email: "push-revoke@example.invalid", name: "Push revoke" }).returning();
    const refresh = generateRefreshToken({ userId: user.id });
    const tokenId = await authRepository.saveRefreshToken(user.id, refresh, new Date(Date.now() + 86400000));
    await db.insert(schema.pushSubscriptions).values({ userId: user.id, tokenId, endpoint: "https://fcm.googleapis.com/fcm/send/revoke", p256dh: "fixture-key", auth: "fixture-auth" });
    for (let n = 0; n < 2; n++) {
      const [event] = await db.insert(schema.events).values({ userId: user.id, title: `Exam ${n}`, type: "exam", startAt: new Date(Date.now() + 59000) }).returning();
      await db.insert(schema.eventNotifications).values({ eventId: event.id, minutesBefore: 1 });
    }
    let delivered = 0;
    await runPushCycle(async () => { delivered++; await authService.logout(refresh); });
    assert.equal(delivered, 1, "the second claimed reminder must recheck the revoked session");
  } finally { await client.close(); }
});
