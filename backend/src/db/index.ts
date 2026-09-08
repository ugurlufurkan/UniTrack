import { drizzle } from "drizzle-orm/node-postgres";
import { Pool } from "pg";
import logger from "../config/logger";

import * as schema from "./schema";

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

// Idle client'lar beklenmedik şekilde koptuğunda (ör. Neon sleep,
// ağ kesintisi) Pool bu eventi fırlatır. Handler olmazsa process
// "unhandledError" ile çöker. Burada loglayıp bırakıyoruz — Pool
// bozuk client'ı otomatik olarak havuzdan çıkarıp yerine yenisini
// koyacaktır.
pool.on("error", (err) => {
  logger.error({ err }, "Unexpected idle database client error");
});

export const db = drizzle(pool, {
  schema,
});