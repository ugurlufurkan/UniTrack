const { test } = require("node:test");
const assert = require("node:assert/strict");
const { createDatabase } = require("./database.cjs");
const { eq } = require("drizzle-orm");

test("parallel semester creation and renaming cannot create duplicate terms", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const semesters = require("../dist/modules/semester/semester.service");
  try {
    const [user] = await db.insert(schema.users).values({ email: "semesters@example.invalid", name: "Semesters" }).returning();
    const created = await Promise.allSettled([semesters.create(user.id, 2026, "Güz"), semesters.create(user.id, 2026, "Güz")]);
    assert.equal(created.filter(item => item.status === "fulfilled").length, 1);
    const next = await semesters.create(user.id, 2026, "Bahar");
    await assert.rejects(semesters.update(user.id, next.id, 2026, "Güz"));
    assert.equal((await semesters.getAll(user.id)).length, 2);
  } finally { await client.close(); }
});

test("concurrent course edits retain unrelated fields and match the current default scale", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const courses = require("../dist/modules/course/course.repository");
  const scales = require("../dist/modules/grade-scale/gradeScale.repository");
  try {
    const [owner, stranger] = await db.insert(schema.users).values([{ email: "courses@example.invalid", name: "Courses" }, { email: "other@example.invalid", name: "Other" }]).returning();
    const [semester] = await db.insert(schema.semesters).values({ userId: owner.id, year: 2026, term: "Güz" }).returning();
    const course = await courses.create(owner.id, { semesterId: semester.id, name: "Original name", credit: 3, components: [{ name: "Final", weight: 100, score: 88 }] });
    await Promise.all([courses.update(owner.id, course.id, { credit: 5 }), courses.update(owner.id, course.id, { name: "Updated name" })]);
    let updated = await courses.findOne(owner.id, course.id);
    assert.equal(updated.name, "Updated name");
    assert.equal(updated.credit, 5);
    const custom = [{ letter: "AA", min: 80, point: 4 }, { letter: "FF", min: 0, point: 0 }];
    await Promise.all([courses.update(owner.id, course.id, { components: [{ name: "Final", weight: 100, score: 82 }] }), scales.setDefault(owner.id, custom)]);
    updated = await courses.findOne(owner.id, course.id);
    assert.equal(updated.average, 82);
    assert.equal(updated.gradePoint, 4);
    await assert.rejects(courses.findOne(stranger.id, course.id));
    await assert.rejects(courses.update(stranger.id, course.id, { name: "Hijacked" }));
  } finally { await client.close(); }
});

test("partial exam-period updates validate against saved dates", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const settings = require("../dist/modules/settings/settings.repository");
  try {
    const [user] = await db.insert(schema.users).values({ email: "settings@example.invalid", name: "Settings", examPeriodStart: new Date("2026-10-10"), examPeriodEnd: new Date("2026-10-20") }).returning();
    await assert.rejects(settings.updateSettings(user.id, { examPeriodStart: new Date("2026-10-30") }));
    const unchanged = await settings.getSettings(user.id);
    assert.equal(unchanged.examPeriodStart.toISOString().slice(0, 10), "2026-10-10");
    await settings.updateSettings(user.id, { themePreference: "DARK" });
    const [row] = await db.select().from(schema.users).where(eq(schema.users.id, user.id));
    assert.equal(row.examPeriodEnd.toISOString().slice(0, 10), "2026-10-20");
    assert.equal(row.themePreference, "DARK");
  } finally { await client.close(); }
});
