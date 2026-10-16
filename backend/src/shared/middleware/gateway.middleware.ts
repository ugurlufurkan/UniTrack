import { createHmac, timingSafeEqual } from "node:crypto";
import { isIP } from "node:net";
import { Request, Response, NextFunction } from "express";

export function gatewayClientIp(req: Request): string | undefined {
  const secret = process.env.WEB_PROXY_SECRET;
  const ip = req.get("x-unitrack-client-ip");
  const timestamp = req.get("x-unitrack-sent-at");
  const signature = req.get("x-unitrack-signature");
  if (!secret || !ip || !isIP(ip) || !timestamp || !signature || !/^[a-f0-9]{64}$/.test(signature)) return;
  if (!Number.isFinite(Number(timestamp)) || Math.abs(Date.now() - Number(timestamp)) > 30000) return;
  const path = req.originalUrl.split("?")[0].replace(/^\/api(?:\/v1)?\//, "");
  const expected = createHmac("sha256", secret).update(`${timestamp}\n${ip}\n${req.method}\n${path}`).digest();
  if (!timingSafeEqual(expected, Buffer.from(signature, "hex"))) return;
  return ip;
}

export function verifyGateway(req: Request, res: Response, next: NextFunction) {
  if (req.get("x-unitrack-signature") && !gatewayClientIp(req)) {
    res.status(403).json({ success: false, message: "İstek kaynağı doğrulanamadı." });
    return;
  }
  next();
}
