import jwt from "jsonwebtoken";
import { createHash, createHmac, randomUUID } from "crypto";
import { env } from "../config/env";

export interface AccessTokenPayload {
  userId: string;
  email: string;
  sessionId?: string;
}

export interface RefreshTokenPayload {
  userId: string;
}

export function generateAccessToken(
  payload: AccessTokenPayload
): string {
  return jwt.sign(payload, env.JWT_SECRET, {
    expiresIn: "15m",
  });
}

export function generateRefreshToken(
  payload: RefreshTokenPayload
): string {
  return jwt.sign(payload, env.JWT_REFRESH_SECRET, {
    expiresIn: "30d",
    jwtid: randomUUID(),
  });
}

export function verifyAccessToken(
  token: string
): AccessTokenPayload {
  return jwt.verify(
    token,
    env.JWT_SECRET
  ) as AccessTokenPayload;
}

// Reconstructable for a short retry window without persisting a raw token.
// An unpredictable HMAC jti binds the replacement to the previous credential.
export function generateRotatedRefreshToken(userId: string, previous: string, at: Date): string {
  return jwt.sign({ userId, iat: Math.floor(at.getTime() / 1000) }, env.JWT_REFRESH_SECRET, {
    expiresIn: "30d",
    jwtid: createHmac("sha256", env.JWT_REFRESH_SECRET).update(previous).digest("hex"),
  });
}

export function verifyRefreshToken(
  token: string
): RefreshTokenPayload {
  return jwt.verify(
    token,
    env.JWT_REFRESH_SECRET
  ) as RefreshTokenPayload;
}

// We only ever need to look refresh tokens up by exact match, never decode
// them back out of the DB, so a deterministic one-way hash is enough — the
// raw JWT itself is never persisted. If the database leaks, the stored
// values are useless to an attacker without also knowing the original token.
export function hashRefreshToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}

export function hashRecoveryToken(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}
