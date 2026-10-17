const { test } = require("node:test");
const assert = require("node:assert/strict");
const { createDatabase } = require("./database.cjs");
const { calendarDay } = require("../dist/lib/calendar-time");

test("calendar days use the configured zone, including midnight and DST boundaries", () => {
  const day = calendarDay(new Date("2026-10-17T21:30:00Z"), "Europe/Istanbul");
  assert.equal(day.weekday, 0);
  assert.equal(day.start.toISOString(), "2026-10-17T21:00:00.000Z");
  assert.equal(day.end.toISOString(), "2026-10-18T21:00:00.000Z");
  const dst = calendarDay(new Date("2026-03-29T12:00:00Z"), "Europe/Berlin");
  assert.equal(dst.end - dst.start, 23 * 3600000);
});

test("today's schedule and lessons do not depend on the Docker host timezone", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const calendar = require("../dist/modules/calendar/calendar.repository");
  const saved = process.env.APP_TIMEZONE;
  process.env.APP_TIMEZONE = "Europe/Istanbul";
  try {
    const [user] = await db.insert(schema.users).values({ email: "calendar@example.invalid", name: "Calendar" }).returning();
    const [semester] = await db.insert(schema.semesters).values({ userId: user.id, year: 2026, term: "Güz" }).returning();
    const [course] = await db.insert(schema.courses).values({ semesterId: semester.id, name: "Sunday course", credit: 3 }).returning();
    await db.insert(schema.courseSchedule).values([0, 6].map(dayOfWeek => ({ userId: user.id, courseId: course.id, dayOfWeek, startTime: "09:00", endTime: "10:00" })));
    await db.insert(schema.events).values(["2026-10-17T20:00:00Z", "2026-10-17T21:30:00Z", "2026-10-18T21:00:00Z"].map(startAt => ({ userId: user.id, title: startAt, type: "lesson", startAt: new Date(startAt) })));
    const result = await calendar.getCalendarSummary(user.id, new Date("2026-10-17T21:30:00Z"));
    assert.equal(result.todayClasses.length, 1);
    assert.equal(result.todayClasses[0].dayOfWeek, 0);
    assert.equal(result.todayLessons.length, 1);
    assert.equal(result.todayLessons[0].title, "2026-10-17T21:30:00Z");
  } finally { if (saved === undefined) delete process.env.APP_TIMEZONE; else process.env.APP_TIMEZONE = saved; await client.close(); }
});
