const { test } = require("node:test");
const assert = require("node:assert/strict");
process.env.DATABASE_URL = "postgresql://test:test@127.0.0.1:1/test";
process.env.JWT_SECRET = "test-access-secret-with-at-least-32-characters";
process.env.JWT_REFRESH_SECRET =
  "test-refresh-secret-with-at-least-32-characters";
process.env.GOOGLE_CLIENT_ID = "test-client";
process.env.NODE_ENV = "test";
const google = require("../dist/lib/google");
const { authService } = require("../dist/modules/auth/auth.service");
const { authRepository } = require("../dist/modules/auth/auth.repository");
const { db } = require("../dist/db");

test("Google sign-in links a verified email to its existing student account", async () => {
  const originals = {
    verify: google.verifyGoogleToken,
    findGoogle: authRepository.findByGoogleId,
    findEmail: authRepository.findByEmail,
    update: authRepository.updateUser,
    save: authRepository.saveRefreshToken,
    insert: db.insert,
  };
  let updatedId;
  google.verifyGoogleToken = async () => ({
    sub: "google-student",
    email: "student@example.test",
    email_verified: true,
  });
  authRepository.findByGoogleId = async () => undefined;
  authRepository.findByEmail = async () => ({
    id: "existing-student",
    email: "student@example.test",
    name: "Student",
    googleId: null,
  });
  authRepository.updateUser = async (id, data) => {
    updatedId = id;
    return { id, email: "student@example.test", name: "Student", ...data };
  };
  authRepository.saveRefreshToken = async () => {};
  db.insert = () => ({ values: async () => {} });
  try {
    const result = await authService.googleLogin("test-google-token");
    assert.equal(updatedId, "existing-student");
    assert.equal(result.user.id, "existing-student");
    assert.equal(result.user.isEmailVerified, true);
  } finally {
    google.verifyGoogleToken = originals.verify;
    authRepository.findByGoogleId = originals.findGoogle;
    authRepository.findByEmail = originals.findEmail;
    authRepository.updateUser = originals.update;
    authRepository.saveRefreshToken = originals.save;
    db.insert = originals.insert;
  }
});

test("Google login rejects an unverified email before linking accounts", async () => {
  const original = google.verifyGoogleToken;
  google.verifyGoogleToken = async () => ({
    sub: "google-student",
    email: "student@example.test",
    email_verified: false,
  });
  try {
    await assert.rejects(authService.googleLogin("token"), { statusCode: 401 });
  } finally {
    google.verifyGoogleToken = original;
  }
});
