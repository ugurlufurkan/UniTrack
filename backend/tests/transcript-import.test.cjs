const { test } = require("node:test");
const assert = require("node:assert/strict");
const { createDatabase } = require("./database.cjs");
process.env.DATABASE_URL = "postgresql://test:test@127.0.0.1:1/test";
process.env.JWT_SECRET = "test-access-secret-with-at-least-32-characters";
process.env.JWT_REFRESH_SECRET = "test-refresh-secret-with-at-least-32-characters";
process.env.GOOGLE_CLIENT_ID = "test-client";
process.env.NODE_ENV = "test";

test("transcript import is atomic, repeatable and isolated by account", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const { importSemesters } = require("../dist/modules/transcript-import/transcript-import.service");
  try {
    const [user, other] = await db.insert(schema.users).values([
      { email: "import@example.invalid", name: "Import" }, { email: "other@example.invalid", name: "Other" },
    ]).returning();
    const semesters = [{ year: 2026, term: "Güz", courses: [{ name: "Matematik", credit: 3, grade: "AA" }, { name: "  Matematik  ", credit: 3, grade: "AA" }] }];
    assert.deepEqual(await importSemesters(user.id, semesters), { importedSemesters: 1, importedCourses: 1, skippedCourses: 1 });
    assert.equal((await importSemesters(user.id, semesters)).importedCourses, 0);
    assert.equal((await importSemesters(other.id, semesters)).importedCourses, 1);
    assert.equal((await db.select().from(schema.courses)).length, 2);
    await client.exec("ALTER TABLE courses ADD CONSTRAINT reject_failure CHECK (name <> 'FAIL')");
    await assert.rejects(importSemesters(user.id, [{ year: 2027, term: "Bahar", courses: [{ name: "Good", credit: 3, grade: "BA" }, { name: "FAIL", credit: 3, grade: "AA" }] }]));
    assert.equal((await db.select().from(schema.semesters)).length, 2, "failed semester rolled back");
    assert.equal((await db.select().from(schema.courses)).length, 2, "earlier courses rolled back");
    const grades = await db.select().from(schema.courses);
    assert.equal(grades[0].gradePoint, 4);
  } finally { await client.close(); }
});
