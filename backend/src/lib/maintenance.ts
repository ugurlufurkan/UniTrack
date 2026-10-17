import { sql } from "drizzle-orm";
import { db } from "../db";

export async function cleanupEphemeralRecords() {
  await db.execute(sql`DELETE FROM rate_limit_buckets WHERE reset_at < now() - interval '1 day'`);
  await db.execute(sql`DELETE FROM refresh_tokens WHERE expires_at < now() - interval '7 days'`);
  await db.execute(sql`DELETE FROM web_push_subscriptions USING refresh_tokens WHERE web_push_subscriptions.token_id = refresh_tokens.id AND (refresh_tokens.revoked_at IS NOT NULL OR refresh_tokens.expires_at < now())`);
  await db.execute(sql`DELETE FROM web_push_deliveries WHERE sent_at < now() - interval '7 days' OR failed_at < now() - interval '7 days'`);
}
