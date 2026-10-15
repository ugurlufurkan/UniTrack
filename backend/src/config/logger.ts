import pino from "pino";
import pinoHttp from "pino-http";

const logger = pino({
  level: process.env.NODE_ENV === "production" ? "info" : "debug",
  redact: ['req.headers.authorization', 'req.headers.cookie', 'res.headers["set-cookie"]'],
});

export const httpLogger = pinoHttp({
  logger,
});

export default logger;
