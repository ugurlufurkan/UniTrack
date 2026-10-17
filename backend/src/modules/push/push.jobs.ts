import { and, asc, eq, exists, gt, gte, isNull, ne, notInArray, or, sql } from "drizzle-orm";
import webPush from "web-push";
import { db } from "../../db";
import { events, eventNotifications, pushSubscriptions, pushDeliveries, pushWorkerState, refreshTokens } from "../../db/schema";
import { dueOccurrences, pushConfiguration } from "./push.config";
import logger from "../../config/logger";

type Subscription = { endpoint: string; keys: { p256dh: string; auth: string } };
type Sender = (subscription: Subscription, payload: string) => Promise<unknown>;
export async function runPushCycle(sender?: Sender, now = new Date(), signal?: AbortSignal, materialize = true) {
  const config = pushConfiguration();
  if (!sender && !config) throw new Error("Configure matching VAPID keys and subject before starting the worker");
  const send = sender || ((subscription, payload) => webPush.sendNotification(subscription, payload, { vapidDetails: config!, TTL: 600, timeout: 5000, urgency: "normal" }));
  let cursor: string | undefined;
  while (materialize && !signal?.aborted) {
    const batch = await db.select({ event: events, notification: eventNotifications }).from(eventNotifications)
      .innerJoin(events, eq(events.id, eventNotifications.eventId)).where(and(
        cursor ? gt(eventNotifications.id, cursor) : undefined,
        eq(events.notificationsEnabled, true), notInArray(events.status, ["completed", "cancelled"]),
        or(ne(events.recurrence, "none"), gte(events.startAt, new Date(now.getTime() - 300000))),
        exists(db.select({ id: pushSubscriptions.id }).from(pushSubscriptions).innerJoin(refreshTokens, eq(refreshTokens.id, pushSubscriptions.tokenId)).where(and(eq(pushSubscriptions.userId, events.userId), isNull(refreshTokens.revokedAt), gt(refreshTokens.expiresAt, now)))),
      )).orderBy(asc(eventNotifications.id)).limit(500);
    if (!batch.length) break;
    const jobs: (typeof pushDeliveries.$inferInsert)[] = [];
    const subscribers = new Map<string, { id: string }[]>();
    for (const { event, notification } of batch) {
      const offset = ((notification.daysBefore * 24 + notification.hoursBefore) * 60 + notification.minutesBefore) * 60000;
      const due = dueOccurrences(event.startAt, event.recurrence, offset, now);
      if (!due.length) continue;
      let recipients = subscribers.get(event.userId);
      if (!recipients) {
        recipients = await db.select({ id: pushSubscriptions.id }).from(pushSubscriptions).innerJoin(refreshTokens, eq(refreshTokens.id, pushSubscriptions.tokenId)).where(and(eq(pushSubscriptions.userId, event.userId), isNull(refreshTokens.revokedAt), gt(refreshTokens.expiresAt, now)));
        subscribers.set(event.userId, recipients);
      }
      for (const recipient of recipients) for (const occurrence of due) jobs.push({ subscriptionId: recipient.id, eventId: event.id, notificationId: notification.id, ...occurrence, nextAttemptAt: now });
    }
    if (jobs.length) await db.insert(pushDeliveries).values(jobs).onConflictDoNothing();
    cursor = batch.at(-1)!.notification.id;
    if (batch.length < 500) break;
  }
  await db.execute(sql`UPDATE web_push_deliveries SET failed_at = now() WHERE sent_at IS NULL AND failed_at IS NULL AND (due_at < now() - interval '10 minutes' OR (attempts >= 3 AND (lease_until IS NULL OR lease_until < now())))`);
  const claimed = await db.execute(sql`UPDATE web_push_deliveries SET lease_until = now() + interval '1 minute', attempts = attempts + 1 WHERE id IN (
    SELECT id FROM web_push_deliveries WHERE sent_at IS NULL AND failed_at IS NULL AND next_attempt_at <= now() AND (lease_until IS NULL OR lease_until < now()) ORDER BY due_at LIMIT 5 FOR UPDATE SKIP LOCKED
  ) RETURNING id`);
  const ids = claimed.rows.map(row => (row as { id: string }).id);
  let delivered = 0;
  if (ids.length) {
    for (const id of ids) {
      if (signal?.aborted) break;
      // Read each recipient immediately before delivery; another job may have waited on a slow vendor.
      const [job] = await db.select({ delivery: pushDeliveries, subscription: pushSubscriptions, event: events, notification: eventNotifications, token: refreshTokens }).from(pushDeliveries)
        .innerJoin(pushSubscriptions, eq(pushSubscriptions.id, pushDeliveries.subscriptionId)).innerJoin(events, eq(events.id, pushDeliveries.eventId))
        .innerJoin(eventNotifications, eq(eventNotifications.id, pushDeliveries.notificationId)).innerJoin(refreshTokens, eq(refreshTokens.id, pushSubscriptions.tokenId)).where(eq(pushDeliveries.id, id));
      if (!job) continue;
      const { delivery, subscription, event, notification, token } = job;
      const offset = ((notification.daysBefore * 24 + notification.hoursBefore) * 60 + notification.minutesBefore) * 60000;
      const valid = !token.revokedAt && token.expiresAt > new Date() && subscription.userId === event.userId && event.notificationsEnabled && !["completed", "cancelled"].includes(event.status) &&
        dueOccurrences(event.startAt, event.recurrence, offset, delivery.dueAt, 1).some(item => item.occurrenceAt.getTime() === delivery.occurrenceAt.getTime());
      if (!valid) { await db.update(pushDeliveries).set({ failedAt: now }).where(eq(pushDeliveries.id, delivery.id)); continue; }
      try {
        await send({ endpoint: subscription.endpoint, keys: { p256dh: subscription.p256dh, auth: subscription.auth } }, JSON.stringify({ owner: event.userId, title: "Yaklaşan etkinliğin var", body: event.title.slice(0, 180), url: "/calendar", tag: delivery.id }));
        await db.update(pushDeliveries).set({ sentAt: now, failedAt: null, leaseUntil: null }).where(eq(pushDeliveries.id, delivery.id));
        delivered++;
      } catch (error) {
        const code = (error as { statusCode?: number }).statusCode;
        if (code === 404 || code === 410) await db.delete(pushSubscriptions).where(eq(pushSubscriptions.id, subscription.id));
        else {
          const permanent = code !== undefined && code >= 400 && code < 500 && code !== 429;
          await db.update(pushDeliveries).set({ failedAt: permanent || delivery.attempts >= 3 ? now : null, leaseUntil: null, nextAttemptAt: new Date(now.getTime() + delivery.attempts * 60000) }).where(and(eq(pushDeliveries.id, delivery.id), isNull(pushDeliveries.sentAt)));
          logger.warn({ deliveryId: delivery.id, statusCode: code }, "Push delivery deferred or rejected");
        }
      }
    }
  }
  await db.insert(pushWorkerState).values({ id: "default", lastSeen: new Date() }).onConflictDoUpdate({ target: pushWorkerState.id, set: { lastSeen: new Date() } });
  return delivered;
}
