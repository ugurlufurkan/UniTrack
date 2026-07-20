import pino from "pino";
import pinoHttp from "pino-http";

const logger = pino({
  level: process.env.NODE_ENV === "production" ? "info" : "debug",
});

export const httpLogger = pinoHttp({
  logger,
});

export default logger;