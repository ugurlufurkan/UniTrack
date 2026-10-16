const { test } = require("node:test");
const assert = require("node:assert/strict");
const { validateEnvironment } = require("../dist/config/validate-env");
const valid = { DATABASE_URL: "postgresql://test:test@localhost/unitrack_test", JWT_SECRET: "a".repeat(64), JWT_REFRESH_SECRET: "b".repeat(64) };
test("development works without Google and production refuses insecure/incomplete settings", () => {
  assert.equal(validateEnvironment(valid).PORT, 5000);
  assert.throws(() => validateEnvironment({ ...valid, NODE_ENV: "production" }), /FRONTEND_URL/);
  assert.throws(() => validateEnvironment({ ...valid, JWT_REFRESH_SECRET: valid.JWT_SECRET }), /different signing secret/);
  const production = { ...valid, NODE_ENV: "production", FRONTEND_URL: "https://students.example.com", WEB_PROXY_SECRET: "c".repeat(64), SMTP_HOST: "smtp.example.com", SMTP_PORT: "587", SMTP_USER: "user", SMTP_PASS: "test-password", SMTP_FROM: "noreply@example.com" };
  assert.equal(validateEnvironment(production).NODE_ENV, "production");
});
