const { test } = require("node:test");
const assert = require("node:assert/strict");
const { createDatabase } = require("./database.cjs");

test("concurrent planning edits preserve independent fields and unchanged reminder identities", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const calendar = require("../dist/modules/calendar/calendar.repository");
  const tasks = require("../dist/modules/task/task.repository");
  try {
    const [user] = await db.insert(schema.users).values({ email: "plans@example.invalid", name: "Plans" }).returning();
    const event = await calendar.createEvent(user.id, { title: "Original", type: "project", startAt: new Date().toISOString(), notifications: [{ daysBefore: 0, hoursBefore: 0, minutesBefore: 15 }] });
    await Promise.all([calendar.updateEvent(user.id, event.id, { title: "Renamed" }), calendar.updateEvent(user.id, event.id, { status: "completed" })]);
    const updated = await calendar.findEventById(user.id, event.id);
    assert.equal(updated.title, "Renamed");
    assert.equal(updated.status, "completed");
    const unchanged = await calendar.updateEvent(user.id, event.id, { notifications: [{ daysBefore: 0, hoursBefore: 0, minutesBefore: 15 }] });
    assert.equal(unchanged.notifications[0].id, event.notifications[0].id, "ordinary edits must not reset the durable delivery identity");
    const item = await tasks.createChecklistItem(user.id, event.id, { title: "Original subtask" });
    await Promise.all([tasks.updateChecklistItem(user.id, item.id, { title: "Renamed subtask" }), tasks.updateChecklistItem(user.id, item.id, { isDone: true })]);
    const task = await tasks.findTaskById(user.id, event.id);
    assert.equal(task.checklist[0].title, "Renamed subtask");
    assert.equal(task.checklist[0].isDone, true);
    const [semester] = await db.insert(schema.semesters).values({ userId: user.id, year: 2026, term: "Güz" }).returning();
    const [course] = await db.insert(schema.courses).values({ semesterId: semester.id, name: "Course", credit: 3 }).returning();
    const schedule = await calendar.createSchedule(user.id, { courseId: course.id, dayOfWeek: 1, startTime: "09:00", endTime: "11:00" });
    await Promise.all([calendar.updateSchedule(user.id, schedule.id, { dayOfWeek: 2 }), calendar.updateSchedule(user.id, schedule.id, { startTime: "10:00" })]);
    const [slot] = await calendar.findAllSchedule(user.id);
    assert.equal(slot.dayOfWeek, 2);
    assert.equal(slot.startTime, "10:00");
  } finally { await client.close(); }
});
