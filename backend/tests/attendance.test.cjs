const { test } = require("node:test");
const assert = require("node:assert/strict");
const { createDatabase } = require("./database.cjs");

test("simultaneous saves produce one weekly record and course limits preserve existing records", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const attendance = require("../dist/modules/attendance/attendance.repository");
  try {
    const [owner, stranger] = await db.insert(schema.users).values([{ email: "attendance@example.invalid", name: "Owner" }, { email: "stranger@example.invalid", name: "Stranger" }]).returning();
    const [semester] = await db.insert(schema.semesters).values({ userId: owner.id, year: 2026, term: "Güz" }).returning();
    const [course] = await db.insert(schema.courses).values({ semesterId: semester.id, name: "Attendance", credit: 3 }).returning();
    const input = { weekNumber: 14, date: "2026-10-17", attendedHours: 2, absentHours: 1, excusedHours: 0 };
    await Promise.all([attendance.upsertRecord(owner.id, course.id, input), attendance.upsertRecord(owner.id, course.id, input)]);
    const detail = await attendance.findCourseAttendance(owner.id, course.id);
    assert.equal(detail.records.length, 1);
    assert.equal(detail.absentHours, 1);
    await assert.rejects(attendance.findCourseAttendance(stranger.id, course.id));
    await assert.rejects(attendance.upsertRecord(stranger.id, course.id, input));
    await assert.rejects(attendance.updateCourseSettings(owner.id, course.id, { totalWeeks: 13 }));
    await assert.rejects(attendance.updateCourseSettings(owner.id, course.id, { weeklyHours: 2 }));
    await attendance.updateRecord(owner.id, detail.records[0].id, { attendedHours: 1 });
    await attendance.updateCourseSettings(owner.id, course.id, { weeklyHours: 2 });
    const updated = await attendance.findCourseAttendance(owner.id, course.id);
    assert.equal(updated.weeklyHours, 2);
    assert.equal(updated.attendedHours, 1);
    assert.equal(updated.absentHours, 1);
  } finally { await client.close(); }
});
