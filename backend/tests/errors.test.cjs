const { test } = require("node:test");
const assert = require("node:assert/strict");
Object.assign(process.env, { NODE_ENV: "test", DATABASE_URL: "postgresql://test:test@127.0.0.1:1/test", JWT_SECRET: "error-test-access-secret-over-32-characters", JWT_REFRESH_SECRET: "error-test-refresh-secret-over-32-characters" });

test("invalid or oversized JSON is rejected without logging credential-bearing parser errors", () => {
  const { errorMiddleware } = require("../dist/shared/middleware/error.middleware");
  const logger = require("../dist/config/logger").default;
  const original = logger.error;
  let logs = 0;
  logger.error = () => { logs++; };
  try {
    for (const [type, expected] of [["entity.parse.failed", 400], ["entity.too.large", 413]]) {
      let status, body;
      const response = { status(value) { status = value; return this; }, json(value) { body = value; return this; } };
      const error = Object.assign(new SyntaxError("fixture password must never appear in logs"), { type, statusCode: expected, body: '{"password":"private-fixture"' });
      errorMiddleware(error, { path: "/auth/login", method: "POST" }, response, () => {});
      assert.equal(status, expected);
      assert.equal(JSON.stringify(body).includes("private-fixture"), false);
    }
    assert.equal(logs, 0);
  } finally { logger.error = original; }
});
