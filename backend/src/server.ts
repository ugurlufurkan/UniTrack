import dotenv from "dotenv";

dotenv.config({ quiet: true });

import app from "./app";
import logger from "./config/logger";
import { closeDatabase } from "./db";
import { env } from "./config/env";
import { cleanupEphemeralRecords } from "./lib/maintenance";

const PORT = env.PORT;

const server = app.listen(PORT, () => {
    logger.info(`🚀 Server running on port ${PORT}`);
});
const cleanup = () => cleanupEphemeralRecords().catch(err => logger.error({ err }, "Ephemeral record cleanup failed"));
void cleanup();
setInterval(cleanup, 3600000).unref();

let closing = false;
function shutdown(exitCode: number) {
    if (closing) return;
    closing = true;
    app.locals.draining = true;
    const deadline = setTimeout(() => process.exit(1), 15000);
    deadline.unref();
    server.close(async () => {
        try { await closeDatabase(); process.exit(exitCode); }
        catch (err) { logger.error({ err }, "Database shutdown failed"); process.exit(1); }
    });
    server.closeIdleConnections();
}
process.on("SIGTERM", () => shutdown(0));
process.on("SIGINT", () => shutdown(0));

// ──────────────────────────────────────────────────────────────
// Global safety nets — without these, an unexpected error
// silently kills the process and leaves zero trace in the logs.
// ──────────────────────────────────────────────────────────────

process.on("unhandledRejection", (reason) => {
    logger.fatal({ err: reason }, "Unhandled Promise Rejection");
    // Let the process crash so the orchestrator (Docker, PM2, systemd)
    // can restart it cleanly. Swallowing this silently is worse.
    shutdown(1);
});

process.on("uncaughtException", (error) => {
    logger.fatal({ err: error }, "Uncaught Exception");
    shutdown(1);
});
