import { createECDH, ECDH } from "node:crypto";
import { z } from "zod";
import { wallTime, fromWallTime } from "../../lib/calendar-time";

export function pushConfiguration() {
  const publicKey = process.env.VAPID_PUBLIC_KEY, privateKey = process.env.VAPID_PRIVATE_KEY, subject = process.env.VAPID_SUBJECT;
  if (!publicKey || !privateKey || !subject || !/^(mailto:[^\s@]+@[^\s@]+|https:\/\/[^\s]+)$/.test(subject)) return;
  try {
    const pair = createECDH("prime256v1"); pair.setPrivateKey(Buffer.from(privateKey, "base64url"));
    if (pair.getPublicKey().toString("base64url") !== publicKey) return;
    return { publicKey, privateKey, subject };
  } catch { return; }
}

const endpoint = z.string().url().max(2048).refine(value => {
  const url = new URL(value);
  const host = url.hostname;
  return url.protocol === "https:" && !url.username && !url.password && (!url.port || url.port === "443") &&
    (host === "fcm.googleapis.com" || host === "web.push.apple.com" || host === "updates.push.services.mozilla.com" || host.endsWith(".notify.windows.com"));
}, "Bu tarayıcının bildirim hizmeti desteklenmiyor.");
export const subscriptionSchema = z.object({ endpoint, keys: z.object({
  p256dh: z.string().regex(/^[A-Za-z0-9_-]{87}$/).refine(value => { try { ECDH.convertKey(Buffer.from(value, "base64url"), "prime256v1"); return true; } catch { return false; } }),
  auth: z.string().regex(/^[A-Za-z0-9_-]{22}$/),
}) });
export const unsubscribeSchema = z.object({ endpoint });

export function dueOccurrences(start: Date, recurrence: string, offsetMs: number, now: Date, windowMs = 300000) {
  const time = now.getTime(), target = time + offsetMs;
  const zone = process.env.PUSH_TIMEZONE || process.env.APP_TIMEZONE || "Europe/Istanbul";
  const localStart = wallTime(start, zone), localTarget = wallTime(new Date(target), zone);
  const values: Date[] = [];
  if (recurrence === "daily" || recurrence === "weekly") {
    const period = (recurrence === "daily" ? 1 : 7) * 86400000;
    const index = Math.max(0, Math.floor((localTarget.getTime() - localStart.getTime()) / period));
    for (const i of [index - 1, index, index + 1]) if (i >= 0) values.push(fromWallTime(new Date(localStart.getTime() + i * period), zone));
  } else if (recurrence === "monthly") {
    const months = Math.max(0, (localTarget.getUTCFullYear() - localStart.getUTCFullYear()) * 12 + localTarget.getUTCMonth() - localStart.getUTCMonth());
    for (const i of [months - 1, months, months + 1]) if (i >= 0) {
      const year = localStart.getUTCFullYear(), month = localStart.getUTCMonth() + i;
      const day = Math.min(localStart.getUTCDate(), new Date(Date.UTC(year, month + 1, 0)).getUTCDate());
      values.push(fromWallTime(new Date(Date.UTC(year, month, day, localStart.getUTCHours(), localStart.getUTCMinutes(), localStart.getUTCSeconds(), localStart.getUTCMilliseconds())), zone));
    }
  } else values.push(start);
  return values.map(occurrenceAt => ({ occurrenceAt, dueAt: new Date(occurrenceAt.getTime() - offsetMs) }))
    .filter(item => item.dueAt.getTime() <= time && item.dueAt.getTime() >= time - windowMs);
}
