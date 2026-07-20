import { auditLogs } from "../db/schema";
import { db } from "../db";
import { Request } from "express";
import logger from "../config/logger";

export interface AuditLogData {
  userId?: string;
  action: string;
  entity?: string;
  entityId?: string;
  method?: string;
  path?: string;
  statusCode?: number;
  userAgent?: string;
  ipAddress?: string;
  metadata?: Record<string, any>;
}

export async function createAuditLog(data: AuditLogData) {
  try {
    await db.insert(auditLogs).values({
      userId: data.userId,
      action: data.action,
      entity: data.entity,
      entityId: data.entityId,
      method: data.method,
      path: data.path,
      statusCode: data.statusCode,
      userAgent: data.userAgent,
      ipAddress: data.ipAddress,
      metadata: data.metadata as any,
    });
  } catch (error) {
    // Don't throw error for audit logs to avoid breaking main flow
    logger.error({ err: error, action: data.action }, "Failed to create audit log");
  }
}

export function extractDeviceInfo(req: Request): {
  userAgent?: string;
  ipAddress?: string;
} {
  const userAgent = req.headers["user-agent"];
  const ipAddress = 
    req.headers["x-forwarded-for"] as string ||
    req.headers["x-real-ip"] as string ||
    req.socket.remoteAddress ||
    "unknown";

  return {
    userAgent,
    ipAddress: Array.isArray(ipAddress) ? ipAddress[0] : ipAddress,
  };
}

export function detectDeviceType(userAgent?: string): string {
  if (!userAgent) return "unknown";

  const ua = userAgent.toLowerCase();

  if (/mobile|android|iphone|ipod|blackberry|iemobile|opera mini/i.test(ua)) {
    return "mobile";
  }

  if (/tablet|ipad|playbook|kindle/i.test(ua)) {
    return "tablet";
  }

  if (/windows|macintosh|linux|unix/i.test(ua)) {
    return "desktop";
  }

  return "unknown";
}

export function generateDeviceName(userAgent?: string): string {
  if (!userAgent) return "Unknown Device";

  const ua = userAgent.toLowerCase();

  if (/iphone/i.test(ua)) return "iPhone";
  if (/ipad/i.test(ua)) return "iPad";
  if (/android/i.test(ua)) return "Android Device";
  if (/windows/i.test(ua)) return "Windows PC";
  if (/macintosh/i.test(ua)) return "Mac";
  if (/linux/i.test(ua)) return "Linux PC";

  return "Unknown Device";
}