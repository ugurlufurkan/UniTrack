import { sql } from "drizzle-orm";
import { db } from "../db";

export async function cleanupEphemeralRecords() {
  await db.execute(sql`DELETE FROM rate_limit_buckets WHERE reset_at < now() - interval '1 day'`);
  await db.execute(sql`DELETE FROM refresh_tokens WHERE expires_at < now() - interval '7 days'`);
}
