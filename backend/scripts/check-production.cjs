const dotenv = require("dotenv");
dotenv.config({ quiet: true });
const { validateEnvironment } = require("../dist/config/validate-env");
(async () => {
  const env = validateEnvironment({ ...process.env, NODE_ENV: "production" });
  if (process.argv.includes("--smtp")) {
    const nodemailer = require("nodemailer");
    const transport = nodemailer.createTransport({ host: env.SMTP_HOST, port: Number(env.SMTP_PORT), secure: env.SMTP_SECURE === "true", auth: { user: env.SMTP_USER, pass: env.SMTP_PASS } });
    try { await transport.verify(); } finally { transport.close(); }
    console.log("SMTP connection and authentication verified; no message sent.");
  }
  console.log(`Production configuration valid. Google: ${env.GOOGLE_CLIENT_ID ? "configured" : "disabled"}`);
})().catch(error => { console.error(error.code || error.message.replace(/postgres(?:ql)?:\/\/\S+/g, "[database URL]")); process.exitCode = 1; });
