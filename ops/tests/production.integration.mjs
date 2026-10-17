import { execFileSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { createRequire } from 'node:module';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const requireBackend = createRequire(path.join(root, 'backend/package.json'));
const fixtureKeys = requireBackend('web-push').generateVAPIDKeys();
const suffix = randomUUID().slice(0, 8);
const names = { db: `unitrack-check-db-${suffix}`, api: `unitrack-check-api-${suffix}`, web: `unitrack-check-web-${suffix}`, push: `unitrack-check-push-${suffix}` };
const created = [];
const docker = (...args) => execFileSync('docker', args, { cwd: root, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'], timeout: 300000 });
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
try {
  if (process.argv.includes('--build')) {
    docker('build', '-t', 'unitrack-backend-check:local', 'backend');
    docker('build', '-t', 'unitrack-web-check:local', 'web');
  }
  docker('run', '-d', '--name', names.db, '-p', '127.0.0.1::5432', '-e', 'POSTGRES_PASSWORD=fixture-password', '-e', 'POSTGRES_DB=unitrack_test', 'postgres:17-alpine');
  created.push(names.db);
  let ready = false;
  for (let n = 0; n < 60; n++) {
    try { docker('exec', names.db, 'pg_isready', '-h', '127.0.0.1', '-U', 'postgres', '-d', 'unitrack_test'); ready = true; break; } catch { await pause(250); }
  }
  if (!ready) throw new Error('Isolated PostgreSQL did not start');
  const port = docker('port', names.db, '5432/tcp').trim().split(':').at(-1);
  execFileSync(process.execPath, ['--test', 'backend/tests/postgres.integration.cjs'], {
    cwd: root, stdio: 'inherit', env: { ...process.env, TEST_DATABASE_URL: `postgresql://postgres:fixture-password@127.0.0.1:${port}/unitrack_test` },
  });
  const env = {
    NODE_ENV: 'production', DATABASE_URL: 'postgresql://postgres:fixture-password@127.0.0.1:5432/unitrack_test',
    JWT_SECRET: 'a'.repeat(64), JWT_REFRESH_SECRET: 'b'.repeat(64), WEB_PROXY_SECRET: 'c'.repeat(64),
    FRONTEND_URL: 'https://students.example.invalid', SMTP_HOST: 'smtp.example.invalid', SMTP_PORT: '587',
    SMTP_USER: 'fixture', SMTP_PASS: 'fixture-password', SMTP_FROM: 'noreply@example.invalid',
    VAPID_PUBLIC_KEY: fixtureKeys.publicKey, VAPID_PRIVATE_KEY: fixtureKeys.privateKey, VAPID_SUBJECT: 'mailto:fixture@example.invalid',
  };
  docker('run', '-d', '--name', names.api, '--network', `container:${names.db}`, ...Object.entries(env).flatMap(([key, value]) => ['-e', `${key}=${value}`]), 'unitrack-backend-check:local');
  created.push(names.api);
  docker('run', '-d', '--name', names.push, '--network', `container:${names.db}`, ...Object.entries(env).flatMap(([key, value]) => ['-e', `${key}=${value}`]), 'unitrack-backend-check:local', 'node', 'dist/workers/push.js');
  created.push(names.push);
  docker('run', '-d', '--name', names.web, '--network', `container:${names.db}`, '-e', 'API_URL=http://127.0.0.1:5000/api/v1', '-e', `WEB_PROXY_SECRET=${env.WEB_PROXY_SECRET}`, '-e', 'WEB_CLIENT_IP_HEADER=x-forwarded-for', 'unitrack-web-check:local');
  created.push(names.web);
  const smoke = `
    const assert = (await import('node:assert/strict')).default;
    const base = 'http://127.0.0.1:3000';
    let ready = false;
    for (let n=0;n<60;n++) { try { if ((await fetch(base+'/api/health')).ok) { ready=true;break; } } catch {} await new Promise(r=>setTimeout(r,500)); }
    assert.ok(ready,'production readiness failed');
    assert.equal((await fetch('http://127.0.0.1:5000/api/v1/auth/me',{headers:{Origin:'https://untrusted.example.invalid'}})).status,403);
    const headers = { Origin:base, 'X-UniTrack-Web':'1', 'x-forwarded-for':'203.0.113.10', 'Content-Type':'application/json' };
    const result = await fetch(base+'/api/student/auth/register',{method:'POST',headers,body:JSON.stringify({name:'Container test',email:'container@example.invalid',password:'FixturePassword123!'})});
    const body = await result.json(); assert.equal(result.status,200); assert.equal(body.accessToken,undefined);
    headers.Cookie = result.headers.getSetCookie().map(c=>c.split(';')[0]).join('; ');
    assert.equal((await fetch(base+'/api/student/auth/me',{headers})).status,200);
    let pushReady = false;
    for (let n=0;n<20;n++) { const response = await fetch(base+'/api/student/push/config',{headers}); if ((await response.json()).enabled) { pushReady=true;break; } await new Promise(r=>setTimeout(r,500)); }
    assert.ok(pushReady,'push worker heartbeat/configuration failed');
    assert.equal((await fetch(base+'/api/student/auth/me',{method:'DELETE',headers})).status,204);
    console.log('Production Docker readiness, push worker, signed gateway, registration and account cleanup passed');
  `;
  execFileSync('docker', ['exec', '-i', names.web, 'node', '--input-type=module'], { input: smoke, encoding: 'utf8', stdio: ['pipe', 'inherit', 'inherit'], timeout: 60000 });
} finally {
  for (const name of created.reverse()) { try { docker('rm', '-f', '-v', name); } catch {} }
}
