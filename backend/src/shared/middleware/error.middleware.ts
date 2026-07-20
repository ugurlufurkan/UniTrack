import { NextFunction, Request, Response } from "express";
import { ZodError } from "zod";
import { AppError } from "../errors/AppError";
import logger from "../../config/logger";
import { env } from "../../config/env";

export const errorMiddleware = (
  err: any,
  req: Request,
  res: Response,
  next: NextFunction
): Response | void => {
  if (res.headersSent) {
    return next(err);
  }

  // Known, operational errors we threw ourselves (AppError.badRequest, etc.)
  if (err instanceof AppError) {
    if (!err.isOperational) {
      logger.error({ err, path: req.path, method: req.method }, "Operational-flagged AppError with isOperational=false");
    }
    return res.status(err.statusCode).json({
      success: false,
      message: err.message,
    });
  }

  // Zod validation errors that escaped the `validate` middleware
  // (e.g. thrown manually inside a service instead of the route-level schema).
  if (err instanceof ZodError) {
    return res.status(400).json({
      success: false,
      message: "Validation Error",
      errors: err.issues,
    });
  }

  // Postgres errors (via node-postgres / drizzle-orm), identified by SQLSTATE code.
  // https://www.postgresql.org/docs/current/errcodes-appendix.html
  if (typeof err?.code === "string") {
    if (err.code === "23505") {
      return res.status(409).json({
        success: false,
        message: "Bu kayıt zaten mevcut.",
      });
    }

    if (err.code === "23503") {
      return res.status(409).json({
        success: false,
        message: "İlişkili kayıt bulunamadığı için işlem tamamlanamadı.",
      });
    }

    if (err.code === "23502") {
      return res.status(400).json({
        success: false,
        message: "Zorunlu bir alan eksik.",
      });
    }
  }

  // Anything else is an unexpected/programmer error — log the full detail
  // server-side, but never forward the raw error message to the client:
  // it can leak internals (file paths, query fragments, stack traces, etc).
  logger.error(
    { err, path: req.path, method: req.method, userId: req.user?.userId },
    "Unhandled error"
  );

  const statusCode = typeof err?.statusCode === "number" ? err.statusCode : 500;

  return res.status(statusCode).json({
    success: false,
    message:
      env.NODE_ENV === "production"
        ? "Beklenmeyen bir hata oluştu."
        : err?.message || "Internal Server Error",
  });
};