require("dotenv/config");
const fs = require("fs");
const { Pool } = require("pg");

const pool = new Pool({ connectionString: process.env.DATABASE_URL });
const sql = fs.readFileSync("drizzle/0000_absurd_squadron_sinister.sql", "utf8");
const statements = sql.split("--> statement-breakpoint");

async function run() {
  for (const stmt of statements) {
    const trimmed = stmt.trim();
    if (!trimmed) continue;
    try {
      await pool.query(trimmed);
      console.log("OK:", trimmed.slice(0, 60).replace(/\n/g, " "));
    } catch (err) {
      if (err.code === "42P07") {
        console.log("Atlandı (zaten var):", trimmed.slice(0, 60).replace(/\n/g, " "));
      } else {
        console.error("HATA:", err.message, "-- statement:", trimmed.slice(0, 100));
      }
    }
  }
  const res = await pool.query("SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name");
  console.log(res.rows.map((r) => r.table_name));
  await pool.end();
}

run();
