const { test } = require("node:test");
const assert = require("node:assert/strict");
const { createDatabase } = require("./database.cjs");

test("account export includes task checklists and excludes other accounts and credentials", async () => {
  const { db, client, schema } = await createDatabase();
  require("../dist/db").db = db;
  const { exportAllUserData } = require("../dist/modules/export/export.service");
  try {
    const [owner, stranger] = await db.insert(schema.users).values([{ email: "export@example.invalid", name: "Export", password: "private-password-hash", passwordResetToken: "private-reset-hash" }, { email: "another@example.invalid", name: "Another" }]).returning();
    const [event, other] = await db.insert(schema.events).values([{ userId: owner.id, title: "Own task", type: "project", startAt: new Date() }, { userId: stranger.id, title: "Other private task", type: "project", startAt: new Date() }]).returning();
    await db.insert(schema.eventChecklistItems).values([{ eventId: event.id, title: "First subtask", sortOrder: 0 }, { eventId: event.id, title: "Finished subtask", sortOrder: 1, isDone: true }, { eventId: other.id, title: "Other private subtask" }]);
    const snapshot = await exportAllUserData(owner.id);
    assert.equal(snapshot.events.length, 1);
    assert.deepEqual(snapshot.events[0].checklist.map(item => [item.title, item.isDone]), [["First subtask", false], ["Finished subtask", true]]);
    const text = JSON.stringify(snapshot);
    for (const excluded of ["Other private", "private-password-hash", "private-reset-hash"]) assert.equal(text.includes(excluded), false);
  } finally { await client.close(); }
});
