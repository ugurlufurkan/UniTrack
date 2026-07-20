import jwt from "jsonwebtoken";
import { createHash } from "crypto";
import { env } from "../config/env";

export interface AccessTokenPayload {
  userId: string;
  email: string;
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