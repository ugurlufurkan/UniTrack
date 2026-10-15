// Explicit opt-in smoke check: creates one disposable student and removes it in finally.
// Usage: node scripts/smoke-api.mjs http://127.0.0.1:5000/api/v1
import assert from "node:assert/strict";
import { randomUUID, randomBytes } from "node:crypto";

const base = process.argv[2];
if (!base || !/^http:\/\/(127\.0\.0\.1|localhost):\d+\/api\/v1$/.test(base)) {
  throw new Error(
    "Pass the local API URL explicitly. This script creates and deletes a disposable account.",
  );
}
let access;
let refresh;
let checks = 0;
async function request(
  path,
  method = "GET",
  body,
  status = 200,
  token = access,
) {
  const response = await fetch(`${base}/${path}`, {
    method,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body ? { "Content-Type": "application/json" } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(30000),
  });
  const data = response.status === 204 ? null : await response.json();
  assert.equal(
    response.status,
    status,
    `${method} ${path}: ${data?.message || response.status}`,
  );
  checks++;
  return data;
}

try {
  const account = await request("auth/register", "POST", {
    email: `unitrack-smoke-${randomUUID()}@example.invalid`,
    name: "UniTrack Disposable Smoke Test",
    password: `Aa1!${randomBytes(20).toString("hex")}`,
  });
  access = account.accessToken;
  refresh = account.refreshToken;
  const me = await request("auth/me");
  assert.equal(me.user.name, "UniTrack Disposable Smoke Test");
  const semester = await request(
    "semesters",
    "POST",
    { year: 2026, term: "Güz" },
    201,
  );
  const course = await request(
    "courses",
    "POST",
    {
      semesterId: semester.id,
      name: "Disposable Test Course",
      credit: 3,
      components: [
        { name: "Vize", weight: 40, score: null },
        { name: "Final", weight: 60, score: null },
      ],
    },
    201,
  );
  assert.equal(course.average, null);
  const graded = await request(`courses/${course.id}`, "PUT", {
    components: [
      { name: "Vize", weight: 40, score: 80 },
      { name: "Final", weight: 60, score: 90 },
    ],
  });
  assert.equal(graded.average, 86);
  assert.equal(graded.letterGrade, "BA");
  await request("settings", "PATCH", {
    targetGpa: 3.5,
    themePreference: "DARK",
    examPeriodStart: "2026-12-01",
    examPeriodEnd: "2026-12-10",
  });
  const settings = await request("settings");
  assert.equal(settings.targetGpa, 3.5);
  assert.ok(settings.examPeriodStart.startsWith("2026-12-01"));
  await request("settings", "PATCH", { examPeriodStart: "2026-12-20" }, 400);
  const schedule = await request(
    "calendar/schedule",
    "POST",
    { courseId: course.id, dayOfWeek: 1, startTime: "09:00", endTime: "10:00" },
    201,
  );
  await request(
    `calendar/schedule/${schedule.id}`,
    "PUT",
    { endTime: "08:00" },
    400,
  );
  await request(`attendance/courses/${course.id}/settings`, "PUT", {
    weeklyHours: 3,
    totalWeeks: 14,
    attendanceLimitHours: 12,
  });
  await request(
    `attendance/courses/${course.id}/records`,
    "POST",
    { weekNumber: 1, date: "2026-10-15", attendedHours: 2, absentHours: 1 },
    201,
  );
  const attendance = await request(`attendance/courses/${course.id}`);
  assert.equal(attendance.absentHours, 1);
  assert.equal(attendance.excusedHours, 0);
  const event = await request(
    "calendar/events",
    "POST",
    {
      title: "Disposable project",
      type: "project",
      courseId: course.id,
      startAt: "2026-12-05T12:00:00Z",
    },
    201,
  );
  const checklist = await request(
    `tasks/${event.id}/checklist`,
    "POST",
    { title: "Disposable first step" },
    201,
  );
  await request(`tasks/checklist/${checklist.id}`, "PUT", { isDone: true });
  const tasks = await request("tasks");
  assert.ok(tasks.find((item) => item.id === event.id).checklist[0].isDone);
  await request(`calendar/events/${event.id}`, "PUT", { status: "completed" });
  const dashboard = await request("dashboard");
  assert.equal(dashboard.gpa, 3.5);
  assert.equal(dashboard.totalCourses, 1);
  await request("statistics");
  await request("transcript");
  await request("export");
  const renewed = await request("auth/refresh", "POST", {
    refreshToken: refresh,
  });
  assert.notEqual(renewed.refreshToken, refresh);
  access = renewed.accessToken;
  refresh = renewed.refreshToken;
  await request("auth/me");
  console.log(
    `PASS: ${checks} real API checks. Disposable account will now be removed.`,
  );
} finally {
  if (access) {
    await request("auth/me", "DELETE", undefined, 204);
    await request("auth/me", "GET", undefined, 401);
    console.log(
      "PASS: disposable account deleted; subsequent profile access rejected.",
    );
  }
}
