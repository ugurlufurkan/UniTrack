import { Router } from "express";
import { and, eq, gt, isNull } from "drizzle-orm";
import { db } from "../../db";
import { pushSubscriptions, pushWorkerState, refreshTokens, users } from "../../db/schema";
import { authMiddleware } from "../../shared/middleware/auth.middleware";
import { validate } from "../../shared/middleware/validate.middleware";
import { AppError } from "../../shared/errors/AppError";
import { pushConfiguration, subscriptionSchema, unsubscribeSchema } from "./push.config";

export async function pushHealthy() {
  if (!pushConfiguration()) return false;
  const [worker] = await db.select().from(pushWorkerState).where(eq(pushWorkerState.id, "default"));
  return !!worker && Date.now() - worker.lastSeen.getTime() < 120000;
}
const router = Router();
router.use(authMiddleware);
router.get("/config", async (_, res) => {
  const config = pushConfiguration();
  const enabled = !!config && await pushHealthy();
  res.json({ enabled, publicKey: enabled ? config?.publicKey : undefined });
});
router.post("/subscriptions", validate(subscriptionSchema), async (req, res) => {
  if (!pushConfiguration()) throw new AppError("Hatırlatmalar şu anda kullanılamıyor.", 503);
  const user = req.user!;
  const subscription = req.body as import("zod").infer<typeof subscriptionSchema>;
  await db.transaction(async tx => {
    await tx.select({ id: users.id }).from(users).where(eq(users.id, user.userId)).for("no key update");
    const [session] = await tx.select().from(refreshTokens).where(and(eq(refreshTokens.id, user.sessionId), eq(refreshTokens.userId, user.userId), isNull(refreshTokens.revokedAt), gt(refreshTokens.expiresAt, new Date()))).for("update");
    if (!session) throw AppError.conflict("Oturum yenileniyor. Lütfen tekrar dene.");
    const existing = await tx.select({ endpoint: pushSubscriptions.endpoint }).from(pushSubscriptions).where(eq(pushSubscriptions.userId, user.userId));
    if (existing.length >= 5 && !existing.some(item => item.endpoint === subscription.endpoint)) throw AppError.conflict("En fazla 5 cihazda bildirim açabilirsin.");
    await tx.insert(pushSubscriptions).values({ userId: user.userId, tokenId: session.id, endpoint: subscription.endpoint, ...subscription.keys }).onConflictDoUpdate({ target: pushSubscriptions.endpoint, set: { userId: user.userId, tokenId: session.id, ...subscription.keys } });
  });
  res.status(201).json({ success: true });
});
router.delete("/subscriptions", validate(unsubscribeSchema), async (req, res) => {
  await db.delete(pushSubscriptions).where(and(eq(pushSubscriptions.userId, req.user!.userId), eq(pushSubscriptions.endpoint, req.body.endpoint)));
  res.status(204).send();
});
export default router;
