const { test } = require("node:test");
const assert = require("node:assert/strict");
const { createHmac } = require("node:crypto");
const { createDatabase } = require("./database.cjs");
process.env.DATABASE_URL = "postgresql://test:test@127.0.0.1:1/test";
process.env.JWT_SECRET = "test-access-secret-with-at-least-32-characters";
process.env.JWT_REFRESH_SECRET = "test-refresh-secret-with-at-least-32-characters";
process.env.GOOGLE_CLIENT_ID = "test-client";
process.env.NODE_ENV = "test";
process.env.WEB_PROXY_SECRET = "shared-web-proxy-test-secret-over-32-chars";

test("gateway identity rejects forged, expired and cross-endpoint forwarding", () => {
  const { gatewayClientIp } = require("../dist/shared/middleware/gateway.middleware");
  const ip = "203.0.113.10", timestamp = String(Date.now());
  const signature = createHmac("sha256", process.env.WEB_PROXY_SECRET).update(`${timestamp}\n${ip}\nPOST\nauth/login`).digest("hex");
  const headers = { "x-unitrack-client-ip": ip, "x-unitrack-sent-at": timestamp, "x-unitrack-signature": signature };
  const request = { method: "POST", originalUrl: "/api/v1/auth/login", get: name => headers[name] };
  assert.equal(gatewayClientIp(request), ip);
  request.originalUrl = "/api/v1/auth/refresh";
  assert.equal(gatewayClientIp(request), undefined);
  request.originalUrl = "/api/v1/auth/login";
  headers["x-unitrack-client-ip"] = "203.0.113.11";
  assert.equal(gatewayClientIp(request), undefined);
  headers["x-unitrack-client-ip"] = ip;
  headers["x-unitrack-sent-at"] = "1";
  assert.equal(gatewayClientIp(request), undefined);
});

test("database limits are shared across replicas and rotations tolerate short concurrent retries", async () => {
  const { client, db, schema } = await createDatabase();
  require("../dist/db").db = db;
  const { DatabaseRateStore } = require("../dist/lib/rate-limit-store");
  const { authRepository } = require("../dist/modules/auth/auth.repository");
  const { authService } = require("../dist/modules/auth/auth.service");
  const { generateRefreshToken } = require("../dist/lib/jwt");
  try {
    const first = new DatabaseRateStore("test"), second = new DatabaseRateStore("test");
    first.init({ windowMs: 60000 }); second.init({ windowMs: 60000 });
    const hits = await Promise.all([first.increment("ip-a"), second.increment("ip-a")]);
    assert.deepEqual(hits.map(hit => hit.totalHits).sort(), [1, 2]);
    assert.equal((await second.increment("ip-b")).totalHits, 1);
    await first.resetKey("ip-a");
    assert.equal((await second.increment("ip-a")).totalHits, 1);
    const [user] = await db.insert(schema.users).values({ email: "rotation@example.invalid", name: "Rotation" }).returning();
    const original = generateRefreshToken({ userId: user.id });
    await authRepository.saveRefreshToken(user.id, original, new Date(Date.now() + 86400000));
    const results = await Promise.all([authService.refresh(original), authService.refresh(original), authService.refresh(original)]);
    assert.equal(new Set(results.map(result => result.refreshToken)).size, 1, "replicas return the same active replacement");
    const active = await authRepository.findRefreshToken(results[0].refreshToken);
    assert.equal(active.revokedAt, null);
    await client.exec("UPDATE refresh_tokens SET revoked_at = now() - interval '11 seconds' WHERE revoked_at IS NOT NULL");
    await assert.rejects(authService.refresh(original), /oturumlar sonlandırıldı/);
    assert.ok((await authRepository.findRefreshToken(results[0].refreshToken)).revokedAt, "reuse outside grace still revokes sessions");
  } finally { await client.close(); }
});
