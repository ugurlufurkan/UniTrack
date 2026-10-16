import rateLimit, { ipKeyGenerator } from "express-rate-limit";
import { verifyAccessToken } from "../../lib/jwt";
import { gatewayClientIp } from "./gateway.middleware";
import { DatabaseRateStore } from "../../lib/rate-limit-store";

const identityKey = (req: import("express").Request) => {
  const token = req.headers.authorization?.replace(/^Bearer /, "");
  if (token) {
    try {
      return `user:${verifyAccessToken(token).userId}`;
    } catch {
      /* Fall back to IP. */
    }
  }
  return ipKeyGenerator(gatewayClientIp(req) || req.ip || "127.0.0.1");
};
const authKey = (req: import("express").Request) =>
  `${identityKey(req)}:${typeof req.body?.email === "string" ? req.body.email.trim().toLowerCase() : "google"}`;

// General rate limiter for all requests
export const rateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  limit: 600,
  keyGenerator: identityKey,
  store: new DatabaseRateStore("general"),
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    message: "Too many requests, please try again later.",
  },
  skip: (req) => {
    // Skip rate limiting in development
    return process.env.NODE_ENV === "development";
  },
});

// Strict rate limiter for authentication endpoints
export const authRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  limit: 5, // 5 auth requests per window
  keyGenerator: authKey,
  store: new DatabaseRateStore("auth"),
  skipSuccessfulRequests: true,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    message: "Too many authentication attempts, please try again later.",
  },
  skip: (req) => {
    return process.env.NODE_ENV === "development";
  },
});

// Password reset rate limiter (very strict)
export const passwordResetRateLimiter = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 hour
  limit: 3, // 3 password reset requests per hour
  keyGenerator: authKey,
  store: new DatabaseRateStore("password"),
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    message: "Too many password reset attempts, please try again later.",
  },
  skip: (req) => {
    return process.env.NODE_ENV === "development";
  },
});
