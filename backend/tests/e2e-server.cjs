// Isolated real Express application over embedded PostgreSQL. No production data.
Object.assign(process.env, {
  DATABASE_URL: "postgresql://test:test@127.0.0.1:1/test",
  JWT_SECRET: "e2e-access-secret-at-least-32-characters",
  JWT_REFRESH_SECRET: "e2e-refresh-secret-at-least-32-characters",
  WEB_PROXY_SECRET: "e2e-web-proxy-secret-at-least-32-characters",
  GOOGLE_CLIENT_ID: "e2e-client",
  NODE_ENV: "test",
});
const { createDatabase } = require("./database.cjs");
(async () => {
  const { client, db } = await createDatabase();
  require("../dist/db").db = db;
  const app = require("../dist/app").default;
  const server = app.listen(5105, "127.0.0.1");
  const stop = () => server.close(async () => { await client.close(); process.exit(0); });
  process.on("SIGINT", stop); process.on("SIGTERM", stop);
})().catch(error => { console.error(error); process.exit(1); });
