const path = require("node:path");
const { PGlite } = require("@electric-sql/pglite");
const { drizzle } = require("drizzle-orm/pglite");
const { migrate } = require("drizzle-orm/pglite/migrator");

async function createDatabase() {
  const client = new PGlite();
  const schema = require("../dist/db/schema");
  const db = drizzle(client, { schema });
  await migrate(db, { migrationsFolder: path.resolve(__dirname, "../drizzle") });
  return { client, db, schema };
}
module.exports = { createDatabase };
