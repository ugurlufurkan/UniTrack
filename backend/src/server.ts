import dotenv from "dotenv";

dotenv.config();

import app from "./app";
import logger from "./config/logger";

const PORT = process.env.PORT || 5000;

const server = app.listen(PORT, () => {
    logger.info(`🚀 Server running on port ${PORT}`);
});

// ──────────────────────────────────────────────────────────────
// Global safety nets — without these, an unexpected error
// silently kills the process and leaves zero trace in the logs.
// ──────────────────────────────────────────────────────────────

process.on("unhandledRejection", (reason) => {
    logger.fatal({ err: reason }, "Unhandled Promise Rejection");
    // Let the process crash so the orchestrator (Docker, PM2, systemd)
    // can restart it cleanly. Swallowing this silently is worse.
    server.close(() => process.exit(1));
});

process.on("uncaughtException", (error) => {
    logger.fatal({ err: error }, "Uncaught Exception");
    server.close(() => process.exit(1));
});