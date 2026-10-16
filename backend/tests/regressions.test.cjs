const { test } = require("node:test");
const assert = require("node:assert/strict");

process.env.DATABASE_URL = "postgresql://test:test@127.0.0.1:1/test";
process.env.JWT_SECRET = "test-access-secret-with-at-least-32-characters";
process.env.JWT_REFRESH_SECRET =
  "test-refresh-secret-with-at-least-32-characters";
process.env.GOOGLE_CLIENT_ID = "test-client";
process.env.NODE_ENV = "test";

const { generateRefreshToken, verifyRefreshToken } = require("../dist/lib/jwt");
const { validate } = require("../dist/shared/middleware/validate.middleware");
const {
  settingsUpdateSchema,
} = require("../dist/shared/validation/settings.schema");
const { loginSchema } = require("../dist/modules/auth/auth.validator");
const {
  createScheduleSchema,
  createEventSchema,
} = require("../dist/modules/calendar/calendar.validator");
const {
  upsertAttendanceRecordSchema,
} = require("../dist/modules/attendance/attendance.validator");
const { calculateGrade } = require("../dist/shared/utils/grade.util");
const { validatePasswordStrength } = require("../dist/lib/password");
const { authService } = require("../dist/modules/auth/auth.service");
const { authRepository } = require("../dist/modules/auth/auth.repository");

test("fractional credits are rejected before reaching integer database columns", () => {
  const { createCourseSchema, updateCourseSchema } = require("../dist/modules/course/course.validator");
  const input = { semesterId: "11111111-1111-4111-8111-111111111111", name: "Matematik", credit: 3.5, components: [{ name: "Final", weight: 100, score: 80 }] };
  assert.equal(createCourseSchema.safeParse(input).success, false);
  assert.equal(updateCourseSchema.safeParse({ credit: 3.5 }).success, false);
  assert.equal(createCourseSchema.safeParse({ ...input, credit: 3 }).success, true);
});

test("same-second refresh tokens are unique and verify", () => {
  const tokens = Array.from({ length: 30 }, () =>
    generateRefreshToken({ userId: "student" }),
  );
  assert.equal(new Set(tokens).size, tokens.length);
  assert.equal(verifyRefreshToken(tokens[0]).userId, "student");
});

test("validation middleware passes transformed dates and default values to controllers", () => {
  const request = { body: { examPeriodStart: "2026-10-15" } };
  let called = false;
  validate(settingsUpdateSchema)(request, {}, () => {
    called = true;
  });
  assert.ok(called);
  assert.ok(request.body.examPeriodStart instanceof Date);
  const record = { body: { weekNumber: 1, date: "2026-10-15" } };
  validate(upsertAttendanceRecordSchema)(record, {}, () => {});
  assert.equal(record.body.absentHours, 0);
  assert.equal(record.body.attendedHours, 0);
});

test("auth rejects non-string input and normalizes email", () => {
  assert.equal(
    loginSchema.safeParse({ email: {}, password: [] }).success,
    false,
  );
  assert.equal(
    loginSchema.parse({ email: " Student@Example.com ", password: "Test123!" })
      .email,
    "student@example.com",
  );
});

test("invalid times, reverse intervals and invalid attendance dates are rejected", () => {
  const base = {
    courseId: "11111111-1111-4111-8111-111111111111",
    dayOfWeek: 1,
    startTime: "09:00",
    endTime: "10:00",
  };
  assert.ok(createScheduleSchema.safeParse(base).success);
  assert.equal(
    createScheduleSchema.safeParse({ ...base, startTime: "99:99" }).success,
    false,
  );
  assert.equal(
    createScheduleSchema.safeParse({ ...base, endTime: "08:00" }).success,
    false,
  );
  assert.equal(
    createEventSchema.safeParse({
      title: "Sınav",
      type: "exam",
      startAt: "2026-10-15T12:00:00Z",
      endAt: "2026-10-14T12:00:00Z",
    }).success,
    false,
  );
  assert.equal(
    upsertAttendanceRecordSchema.safeParse({ weekNumber: 1, date: "broken" })
      .success,
    false,
  );
});

test("ungraded courses remain ungraded; weighted scores and zero grades are calculated", () => {
  assert.equal(
    calculateGrade([
      { weight: 40, score: 90 },
      { weight: 60, score: null },
    ]).average,
    null,
  );
  assert.deepEqual(
    calculateGrade([
      { weight: 40, score: 80 },
      { weight: 60, score: 90 },
    ]),
    { average: 86, letterGrade: "BA", gradePoint: 3.5, passed: true },
  );
  assert.equal(calculateGrade([{ weight: 100, score: 0 }]).letterGrade, "FF");
});

test("short passwords cannot pass strength validation by including four character categories", () => {
  assert.equal(validatePasswordStrength("Aa1!").isValid, false);
  assert.equal(validatePasswordStrength("Strong123!").isValid, true);
  assert.equal(
    validatePasswordStrength("Ş".repeat(40) + "Aa1!").isValid,
    false,
  );
});

test("verification consumes the token and clears its expiration with SQL null", async () => {
  const originalFind = authRepository.findByEmailVerificationToken;
  const originalUpdate = authRepository.updateUser;
  const { db } = require("../dist/db");
  const originalInsert = db.insert;
  let patch;
  authRepository.findByEmailVerificationToken = async () => ({
    id: "student",
    emailVerificationExpiresAt: new Date(Date.now() + 60000),
  });
  authRepository.updateUser = async (_id, values) => {
    patch = values;
  };
  db.insert = () => ({ values: async () => {} });
  try {
    await authService.verifyEmail("single-use-token");
    assert.equal(patch.emailVerificationToken, null);
    assert.equal(patch.emailVerificationExpiresAt, null);
    assert.equal(patch.isEmailVerified, true);
  } finally {
    authRepository.findByEmailVerificationToken = originalFind;
    authRepository.updateUser = originalUpdate;
    db.insert = originalInsert;
  }
});

test("resetting a password consumes the reset token and revokes sessions", async () => {
  const originals = {
    find: authRepository.findByPasswordResetToken,
    update: authRepository.updateUser,
    revoke: authRepository.revokeAllRefreshTokens,
  };
  const { db } = require("../dist/db");
  const originalInsert = db.insert;
  let patch, revoked;
  authRepository.findByPasswordResetToken = async () => ({
    id: "student",
    passwordResetExpiresAt: new Date(Date.now() + 60000),
  });
  authRepository.updateUser = async (_id, values) => {
    patch = values;
  };
  authRepository.revokeAllRefreshTokens = async (id) => {
    revoked = id;
  };
  db.insert = () => ({ values: async () => {} });
  try {
    await authService.resetPassword("single-use-token", "StrongPassword123!");
    assert.equal(patch.passwordResetToken, null);
    assert.equal(patch.passwordResetExpiresAt, null);
    assert.equal(revoked, "student");
  } finally {
    authRepository.findByPasswordResetToken = originals.find;
    authRepository.updateUser = originals.update;
    authRepository.revokeAllRefreshTokens = originals.revoke;
    db.insert = originalInsert;
  }
});
