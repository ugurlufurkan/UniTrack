import dotenv from "dotenv";

dotenv.config();

export const env = {
  PORT: Number(process.env.PORT ?? 5000),

  DATABASE_URL: process.env.DATABASE_URL!,

  JWT_SECRET: process.env.JWT_SECRET!,

  JWT_REFRESH_SECRET: process.env.JWT_REFRESH_SECRET!,

  GOOGLE_CLIENT_ID: process.env.GOOGLE_CLIENT_ID!,

  NODE_ENV: process.env.NODE_ENV || "development",

  FRONTEND_URL: process.env.FRONTEND_URL || "http://localhost:3000",

  // Email configuration
  SMTP_HOST: process.env.SMTP_HOST,
  SMTP_PORT: process.env.SMTP_PORT,
  SMTP_SECURE: process.env.SMTP_SECURE,
  SMTP_USER: process.env.SMTP_USER,
  SMTP_PASS: process.env.SMTP_PASS,
  SMTP_FROM: process.env.SMTP_FROM,
};