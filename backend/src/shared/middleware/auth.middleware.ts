import { NextFunction, Request, Response } from "express";
import { verifyAccessToken } from "../../lib/jwt";
import { db } from "../../db";
import { refreshTokens } from "../../db/schema";
import { and, eq, gt, isNull } from "drizzle-orm";

declare global {
  namespace Express {
    interface Request {
      user?: {
        userId: string;
        email: string;
        sessionId: string;
      };
    }
  }
}

export async function authMiddleware(
  req: Request,
  res: Response,
  next: NextFunction
) {
  let token: string | undefined;

  // Authorization: Bearer xxx
  const authHeader = req.headers.authorization;

  if (authHeader?.startsWith("Bearer ")) {
    token = authHeader.split(" ")[1];
  }

  // Cookie fallback (ileride HTTP Only Cookie kullanacağız)
  if (!token && req.cookies?.accessToken) {
    token = req.cookies.accessToken;
  }

  if (!token) {
    return res.status(401).json({
      success: false,
      message: "Yetkilendirme tokenı bulunamadı.",
    });
  }

  let payload;
  try { payload = verifyAccessToken(token); } catch {
    return res.status(401).json({
      success: false,
      message: "Geçersiz veya süresi dolmuş token.",
    });
  }
  if (!payload.sessionId) return res.status(401).json({ success: false, message: "Oturum yenilenmeli." });
  try {
    const [session] = await db.select({ id: refreshTokens.id }).from(refreshTokens).where(and(
      eq(refreshTokens.id, payload.sessionId), eq(refreshTokens.userId, payload.userId),
      isNull(refreshTokens.revokedAt), gt(refreshTokens.expiresAt, new Date()),
    )).limit(1);
    if (!session) return res.status(401).json({ success: false, message: "Oturum kapatılmış veya süresi dolmuş." });
    req.user = { userId: payload.userId, email: payload.email, sessionId: session.id };
    next();
  } catch (error) { next(error); }
}
