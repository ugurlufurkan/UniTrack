import { z } from "zod";

const optional = (value: unknown) => value === "" ? undefined : value;
const schema = z.object({
  DATABASE_URL: z.string().url().regex(/^postgres(?:ql)?:\/\//, "PostgreSQL URL required"),
  JWT_SECRET: z.string().min(32),
  JWT_REFRESH_SECRET: z.string().min(32),
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  PORT: z.coerce.number().int().min(1).max(65535).default(5000),
  FRONTEND_URL: z.string().url().default("http://localhost:3000"),
  GOOGLE_CLIENT_ID: z.preprocess(optional, z.string().optional()),
  WEB_PROXY_SECRET: z.preprocess(optional, z.string().min(32).optional()),
  SMTP_HOST: z.preprocess(optional, z.string().optional()),
  SMTP_PORT: z.preprocess(optional, z.string().regex(/^\d+$/).refine(value => Number(value) >= 1 && Number(value) <= 65535, "Invalid SMTP port").optional()),
  SMTP_SECURE: z.enum(["true", "false", ""]).optional(),
  SMTP_USER: z.preprocess(optional, z.string().optional()),
  SMTP_PASS: z.preprocess(optional, z.string().optional()),
  SMTP_FROM: z.preprocess(optional, z.string().email().optional()),
  GEMINI_API_KEY: z.preprocess(optional, z.string().optional()),
}).superRefine((config, ctx) => {
  const issue = (key: string, message: string) => ctx.addIssue({ code: "custom", path: [key], message });
  if (config.JWT_SECRET === config.JWT_REFRESH_SECRET) issue("JWT_REFRESH_SECRET", "Use a different signing secret");
  if (config.NODE_ENV === "production") {
    for (const key of ["JWT_SECRET", "JWT_REFRESH_SECRET"] as const) if (config[key].length < 64) issue(key, "Use at least 64 random characters in production");
    if (new URL(config.FRONTEND_URL).protocol !== "https:") issue("FRONTEND_URL", "HTTPS required in production");
    if (config.WEB_PROXY_SECRET === config.JWT_SECRET || config.WEB_PROXY_SECRET === config.JWT_REFRESH_SECRET) issue("WEB_PROXY_SECRET", "Use a separate proxy secret");
    if (config.SMTP_PORT === "465" && config.SMTP_SECURE !== "true") issue("SMTP_SECURE", "Port 465 requires implicit TLS");
    for (const key of ["WEB_PROXY_SECRET", "SMTP_HOST", "SMTP_PORT", "SMTP_USER", "SMTP_PASS", "SMTP_FROM"] as const)
      if (!config[key]) issue(key, "Required in production");
  }
});

export function validateEnvironment(source: NodeJS.ProcessEnv) {
  const parsed = schema.safeParse(source);
  if (!parsed.success) throw new Error(`Invalid environment: ${parsed.error.issues.map(issue => `${issue.path.join(".")}: ${issue.message}`).join("; ")}`);
  return parsed.data;
}
