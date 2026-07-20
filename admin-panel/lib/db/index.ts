import { drizzle } from "drizzle-orm/node-postgres";
import { Pool } from "pg";

import * as schema from "./schema";

// UniTrack mobil backend'i İLE AYNI veritabanı (DATABASE_URL aynı Postgres'i
// göstermeli). Admin panel sadece yeni içerik tablolarına ve kendi
// admin_* auth tablolarına dokunuyor; öğrenci verisine (users, courses...)
// hiç erişmiyor.
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

export const db = drizzle(pool, { schema });
