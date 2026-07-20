import express from "express";
import swaggerUi from "swagger-ui-express";
import helmet from "helmet";
import cors from "cors";
import compression from "compression";
import hpp from "hpp";

import swaggerDocument from "./docs/swagger";
import authRoutes from "./modules/auth/auth.routes";
import semesterRoutes from "./modules/semester/semester.routes";
import courseRoutes from "./modules/course/course.routes";
import gpaRoutes from "./modules/gpa/gpa.routes";
import statisticsRoutes from "./modules/statistics/statistics.routes";
import dashboardRoutes from "./modules/dashboard/dashboard.routes";
import transcriptRoutes from "./modules/transcript/transcript.routes";
import gradeScaleRoutes from "./modules/grade-scale/gradeScale.routes";
import calendarRoutes from "./modules/calendar/calendar.routes";
import attendanceRoutes from "./modules/attendance/attendance.routes";
import taskRoutes from "./modules/task/task.routes";
import settingsRoutes from "./modules/settings/settings.routes";
import exportRoutes from "./modules/export/export.routes";
import contentRoutes from "./modules/content/content.routes";

import { notFoundMiddleware } from "./shared/middleware/notFound.middleware";
import { errorMiddleware } from "./shared/middleware/error.middleware";
import { httpLogger } from "./config/logger";
import logger from "./config/logger";
import { rateLimiter } from "./shared/middleware/rateLimiter.middleware";
import { env } from "./config/env";

const app = express();

app.disable("x-powered-by");
app.use(httpLogger);
app.use(helmet({
  contentSecurityPolicy: env.NODE_ENV === "production" ? undefined : false,
  crossOriginEmbedderPolicy: false,
  crossOriginResourcePolicy: { policy: "cross-origin" },
  hsts: env.NODE_ENV === "production" ? {
    maxAge: 31536000,
    includeSubDomains: true,
    preload: true
  } : false,
}));

// CORS configuration
// - production: strict allow-list from ALLOWED_ORIGINS (comma separated), no wildcard.
// - development: origin is reflected (not "*") so that credentials: true keeps working
//   (browsers reject "Access-Control-Allow-Origin: *" together with credentialed requests).
const allowedOrigins = env.NODE_ENV === "production"
  ? (process.env.ALLOWED_ORIGINS?.split(",").map((o) => o.trim()).filter(Boolean) ?? ["https://unitrack.app"])
  : ["http://localhost:3000", "http://localhost:8081"];

app.use(cors({
  origin: (origin, callback) => {
    // Same-origin / non-browser requests (curl, mobile app via OkHttp) send no Origin header.
    if (!origin) return callback(null, true);

    if (env.NODE_ENV !== "production") {
      // Reflect the calling origin in dev instead of using "*", so credentials keep working.
      return callback(null, true);
    }

    if (allowedOrigins.includes(origin)) {
      return callback(null, true);
    }

    logger.warn({ origin }, "Blocked by CORS");
    callback(new Error("Not allowed by CORS"));
  },
  credentials: true,
  methods: ["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"],
  allowedHeaders: ["Content-Type", "Authorization", "X-Requested-With"],
  exposedHeaders: ["Content-Range", "X-Content-Range"],
  maxAge: 86400,
}));

app.use(compression());
app.use(hpp());

// Rate limiting
app.use(rateLimiter);

app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true, limit: "10mb" }));

app.use("/api/docs", swaggerUi.serve, swaggerUi.setup(swaggerDocument));

app.get("/api/health", async (_, res) => {
  try {
    // Check database connection
    const { db } = await import("./db/index.js");
    await db.execute("SELECT 1");

    res.status(200).json({
      success: true,
      message: "UniTrack API is healthy 🚀",
      timestamp: new Date().toISOString(),
      environment: env.NODE_ENV,
      version: "3.0.0",
      database: "connected",
    });
  } catch (error) {
    res.status(503).json({
      success: false,
      message: "Service unhealthy",
      timestamp: new Date().toISOString(),
      database: "disconnected",
    });
  }
});

// --- API versioning ---------------------------------------------------
// All feature routes live under /api/v1/*. We also mount the same router
// under the old unversioned /api/* prefix so existing clients (e.g. the
// current Android build) don't break, but we tag those responses with a
// Deprecation header so we can track/remove the alias later.
const v1Router = express.Router();

v1Router.use("/auth", authRoutes);
v1Router.use("/semesters", semesterRoutes);
v1Router.use("/courses", courseRoutes);
v1Router.use("/gpa", gpaRoutes);
v1Router.use("/statistics", statisticsRoutes);
v1Router.use("/dashboard", dashboardRoutes);
v1Router.use("/transcript", transcriptRoutes);
v1Router.use("/grade-scale", gradeScaleRoutes);
v1Router.use("/calendar", calendarRoutes);
v1Router.use("/attendance", attendanceRoutes);
v1Router.use("/tasks", taskRoutes);
v1Router.use("/settings", settingsRoutes);
v1Router.use("/export", exportRoutes);
v1Router.use("/content", contentRoutes);

app.use("/api/v1", v1Router);

// Legacy unversioned alias — deprecated, remove once all clients are on /api/v1.
app.use(
  "/api",
  (req, res, next) => {
    res.setHeader("Deprecation", "true");
    res.setHeader("Link", `</api/v1${req.originalUrl.replace(/^\/api/, "")}>; rel="successor-version"`);
    next();
  },
  v1Router
);

app.use(notFoundMiddleware);
app.use(errorMiddleware);

export default app;