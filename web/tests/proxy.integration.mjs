// Run after `npm run build`. Uses an isolated fake upstream; never touches PostgreSQL.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { createFixture } from './fixtures/api.mjs';
import { once } from 'node:events';

function fetch(url, options = {}) {
  const headers = new Headers(options.headers);
  headers.set('x-forwarded-for', '203.0.113.10');
  return globalThis.fetch(url, { ...options, headers });
}

test(
  'student gateway: auth, cookies, CSRF, CRUD, refresh and logout failures',
  { timeout: 60000 },
  async () => {
    const { server, state } = createFixture();
    server.listen(0, '127.0.0.1');
    await once(server, 'listening');
    const apiPort = server.address().port;
    const port = 3197;
    const base = `http://127.0.0.1:${port}`;
    const child = spawn(process.execPath, ['.next/standalone/server.js'], {
      cwd: process.cwd(),
      env: {
        ...process.env,
        PORT: String(port),
        HOSTNAME: '127.0.0.1',
        WEB_PROXY_SECRET: 'fixture-web-proxy-secret-at-least-32-chars',
        WEB_CLIENT_IP_HEADER: 'x-forwarded-for',
        API_URL: `http://127.0.0.1:${apiPort}/api/v1`,
      },
      stdio: 'pipe',
      windowsHide: true,
    });
    let output = '';
    child.stdout.on('data', (chunk) => {
      output += chunk;
    });
    child.stderr.on('data', (chunk) => {
      output += chunk;
    });
    try {
      let ready = false;
      for (let attempt = 0; attempt < 100; attempt++) {
        try {
          const response = await fetch(`${base}/login`);
          if (response.ok) {
            ready = true;
            break;
          }
        } catch {
          /* Wait for the owned child server. */
        }
        await new Promise((resolve) => setTimeout(resolve, 150));
      }
      assert.ok(ready, output);
      assert.equal((await fetch(`${base}/`, { redirect: 'manual' })).status, 307);
      const request = (path, options = {}) =>
        fetch(`${base}/api/student/${path}`, {
          ...options,
          headers: {
            Origin: base,
            'Content-Type': 'application/json',
            'X-UniTrack-Web': '1',
            ...options.headers,
          },
        });
      assert.equal((await request('courses')).status, 401);
      assert.equal((await request('auth/refresh', { method: 'POST' })).status, 404);
      assert.equal(
        (
          await request('auth/login', {
            method: 'POST',
            body: '{}',
            headers: { Origin: 'https://attacker.test' },
          })
        ).status,
        403,
      );
      assert.equal(
        (
          await request('auth/login', {
            method: 'POST',
            body: JSON.stringify({ password: 'wrong' }),
          })
        ).status,
        401,
      );
      const login = await request('auth/login', {
        method: 'POST',
        body: JSON.stringify({ email: 'student@example.test', password: 'Strong123!' }),
      });
      assert.equal(login.status, 200);
      const data = await login.json();
      assert.equal(data.accessToken, undefined);
      assert.equal(data.refreshToken, undefined);
      const cookies = login.headers.getSetCookie();
      assert.equal(cookies.length, 2);
      assert.ok(
        cookies.every((cookie) => /HttpOnly/i.test(cookie) && /SameSite=lax/i.test(cookie)),
      );
      let cookie = cookies.map((value) => value.split(';')[0]).join('; ');
      const authRequest = (path, options = {}) =>
        request(path, { ...options, headers: { Cookie: cookie, ...options.headers } });
      assert.equal((await authRequest('auth/me')).status, 200);
      const created = await authRequest('semesters', {
        method: 'POST',
        body: JSON.stringify({ year: 2027, term: 'Bahar' }),
      });
      const semester = await created.json();
      assert.equal(created.status, 201);
      const updated = await authRequest(`semesters/${semester.id}`, {
        method: 'PUT',
        body: JSON.stringify({ year: 2027, term: 'Yaz' }),
      });
      assert.equal(updated.status, 200);
      assert.equal(
        (await authRequest(`semesters/${semester.id}`, { method: 'DELETE' })).status,
        200,
      );
      assert.equal(
        state.requests.find((item) => item.path === 'semesters' && item.method === 'POST')
          .authorization,
        'Bearer test-access',
      );
      const refreshes = await Promise.all(
        [1, 2, 3].map(() =>
          fetch(`${base}/api/session/refresh`, {
            method: 'POST',
            headers: { Origin: base, 'X-UniTrack-Web': '1', Cookie: cookie },
          }),
        ),
      );
      assert.ok(refreshes.every((response) => response.status === 200));
      const refresh = refreshes[0];
      assert.equal(refresh.status, 200);
      assert.equal(state.refreshCount, 1);
      cookie = refresh.headers
        .getSetCookie()
        .map((value) => value.split(';')[0])
        .join('; ');
      assert.equal((await authRequest('auth/me')).status, 200);
      state.failRefresh = true;
      const unavailable = await fetch(`${base}/api/session/refresh`, {
        method: 'POST',
        headers: { Origin: base, 'X-UniTrack-Web': '1', Cookie: cookie },
      });
      assert.equal(unavailable.status, 503);
      assert.equal(unavailable.headers.getSetCookie().length, 0);
      state.failRefresh = false;
      for (const status of [429, 400, 401]) {
        state.refreshStatus = status;
        const limited = await fetch(`${base}/api/session/refresh`, {
          method: 'POST',
          headers: {
            Origin: base,
            'X-UniTrack-Web': '1',
            Cookie: `unitrack_refresh=fixture-${status}`,
          },
        });
        assert.equal(limited.status, status === 400 ? 503 : status);
        if (status !== 401)
          assert.equal(
            limited.headers.getSetCookie().length,
            0,
            'temporary or malformed responses must retain valid credentials',
          );
        else assert.ok(limited.headers.getSetCookie().every((value) => /Max-Age=0/.test(value)));
      }
      state.refreshStatus = 0;
      state.failLogout = true;
      const logout = await authRequest('auth/logout', { method: 'POST' });
      assert.equal(logout.status, 503);
      assert.ok(logout.headers.getSetCookie().every((value) => /Max-Age=0/.test(value)));
    } finally {
      child.kill();
      server.closeAllConnections();
      await new Promise((resolve) => server.close(resolve));
    }
  },
);
