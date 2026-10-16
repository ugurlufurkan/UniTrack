import type { Store, Options } from "express-rate-limit";
import { sql } from "drizzle-orm";
import { db } from "../db";
import { createHash } from "node:crypto";

// PostgreSQL is the shared authority; limits survive restarts and are shared
// by API replicas. Each limiter owns a distinct namespace.
export class DatabaseRateStore implements Store {
  localKeys = false;
  private windowMs = 60000;
  constructor(public readonly prefix: string) {}
  private bucket(key: string) { return `${this.prefix}:${createHash("sha256").update(key).digest("hex")}`; }
  init(options: Options) { this.windowMs = options.windowMs; }
  async increment(key: string) {
    const bucket = this.bucket(key);
    const result = await db.execute(sql`
      INSERT INTO rate_limit_buckets (key, hits, reset_at)
      VALUES (${bucket}, 1, now() + ${this.windowMs} * interval '1 millisecond')
      ON CONFLICT (key) DO UPDATE SET
        hits = CASE WHEN rate_limit_buckets.reset_at <= now() THEN 1 ELSE rate_limit_buckets.hits + 1 END,
        reset_at = CASE WHEN rate_limit_buckets.reset_at <= now() THEN EXCLUDED.reset_at ELSE rate_limit_buckets.reset_at END
      RETURNING hits, reset_at
    `);
    const row = result.rows[0] as { hits: number; reset_at: Date | string };
    return { totalHits: row.hits, resetTime: new Date(row.reset_at) };
  }
  async decrement(key: string) {
    await db.execute(sql`UPDATE rate_limit_buckets SET hits = greatest(0, hits - 1) WHERE key = ${this.bucket(key)}`);
  }
  async resetKey(key: string) {
    await db.execute(sql`DELETE FROM rate_limit_buckets WHERE key = ${this.bucket(key)}`);
  }
}
