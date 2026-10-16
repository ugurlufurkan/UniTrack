import pino from "pino";
import pinoHttp from "pino-http";
import { randomUUID } from "node:crypto";

const logger = pino({
  level: process.env.NODE_ENV === "production" ? "info" : "debug",
  redact: ['req.headers.authorization', 'req.headers.cookie', 'res.headers["set-cookie"]'],
});

export const httpLogger = pinoHttp({
  logger,
  genReqId: (_req, res) => {
    const id = randomUUID();
    res.setHeader("X-Request-ID", id);
    return id;
  },
});

export default logger;
