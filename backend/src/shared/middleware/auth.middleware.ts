import { NextFunction, Request, Response } from "express";
import { verifyAccessToken } from "../../lib/jwt";

declare global {
  namespace Express {
    interface Request {
      user?: {
        userId: string;
        email: string;
      };
    }
  }
}

export function authMiddleware(
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

  try {
    const payload = verifyAccessToken(token);

    req.user = {
      userId: payload.userId,
      email: payload.email,
    };

    next();
  } catch {
    return res.status(401).json({
      success: false,
      message: "Geçersiz veya süresi dolmuş token.",
    });
  }
}