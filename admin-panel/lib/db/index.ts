import { drizzle } from "drizzle-orm/node-postgres";
import { Pool } from "pg";

import * as schema from "./schema";

// UniTrack mobil backend'i İLE AYNI veritabanı (DATABASE_URL aynı Postgres'i
// göstermeli). Admin panel çoğunlukla yeni içerik tablolarına ve kendi
// admin_* auth tablolarına dokunuyor; öğrenci verisine (users, courses...)
// SADECE tek bir yerde, bilerek dokunuyor: doğrulanmış hesap silme
// talepleri (bkz. app/(admin)/deletion-requests/actions.ts) — orada ham
// SQL ile `users` tablosundan silme yapılıyor, cascade FK'ler gerisini
// halleder.
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

export const db = drizzle(pool, { schema });
