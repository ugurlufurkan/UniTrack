const { test } = require("node:test");
const assert = require("node:assert/strict");
const path = require("node:path");
const { migrate } = require("drizzle-orm/pglite/migrator");
const { createDatabase } = require("./database.cjs");

test("complete migration history installs into empty PostgreSQL and is repeatable", async () => {
  const { client, db, schema } = await createDatabase();
  try {
    await migrate(db, { migrationsFolder: path.resolve(__dirname, "../drizzle") });
    const tables = await client.query("SELECT tablename FROM pg_tables WHERE schemaname = 'public'");
    for (const table of Object.values(schema).filter(value => value?.[Symbol.for("drizzle:Name")])) {
      assert.ok(tables.rows.some(row => row.tablename === table[Symbol.for("drizzle:Name")]), table[Symbol.for("drizzle:Name")]);
    }
    const [user] = await db.insert(schema.users).values({ email: "migration@example.invalid", name: "Migration test" }).returning();
    const [semester] = await db.insert(schema.semesters).values({ userId: user.id, year: 2026, term: "Güz" }).returning();
    const [course] = await db.insert(schema.courses).values({ semesterId: semester.id, name: "Test", credit: 3 }).returning();
    assert.equal(course.totalWeeks, 14);
    assert.equal(course.weeklyHours, 3);
    await db.insert(schema.attendanceRecords).values({ userId: user.id, courseId: course.id, weekNumber: 1, date: new Date(), attendedHours: 3 });
    await assert.rejects(client.query("INSERT INTO courses (semester_id, name, credit) VALUES ('00000000-0000-4000-8000-000000000000', 'Orphan', 3)"));
  } finally { await client.close(); }
});
