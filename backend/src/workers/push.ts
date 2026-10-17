import "dotenv/config";
import { setTimeout } from "node:timers/promises";
import { runPushCycle } from "../modules/push/push.jobs";
import { closeDatabase } from "../db";
import logger from "../config/logger";
import { pushConfiguration } from "../modules/push/push.config";
const stop = new AbortController();
process.on("SIGTERM", () => stop.abort()); process.on("SIGINT", () => stop.abort());
(async () => {
  if (!pushConfiguration()) { logger.fatal("Valid VAPID keys and subject are required for the push worker"); process.exit(1); }
  let lastScan = 0;
  while (!stop.signal.aborted) {
    let sent = 0;
    try {
      const scan = Date.now() - lastScan >= 30000;
      sent = await runPushCycle(undefined, new Date(), stop.signal, scan);
      if (scan) lastScan = Date.now();
    }
    catch { logger.error("Push worker cycle failed; check database and VAPID configuration"); }
    await setTimeout(sent ? 50 : 30000, undefined, { signal: stop.signal }).catch(() => {});
  }
  await closeDatabase();
})().catch(() => process.exit(1));
