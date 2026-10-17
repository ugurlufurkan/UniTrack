import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import ts from 'typescript';

function client(fetch) {
  let revision = 'initial',
    cleared = 0,
    expired = 0;
  const device = {
    clearDeviceData: async () => {
      cleared++;
    },
    stopBrowserPush: async () => {},
    markPendingLogout() {},
    hasPendingLogout: () => false,
    flushPendingLogout: async () => true,
    clearPendingLogout() {},
    sessionRevision: () => revision,
    advanceSessionRevision: () => {
      revision = `next-${revision}`;
    },
  };
  const exported = {};
  const source = ts.transpileModule(
    fs.readFileSync(new URL('../lib/api.ts', import.meta.url), 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } },
  ).outputText;
  vm.runInNewContext(source, {
    module: { exports: exported },
    exports: exported,
    require: (name) => {
      if (name === './device-storage') return device;
      throw new Error(name);
    },
    fetch,
    Headers,
    FormData,
    DOMException,
    Event,
    navigator: {},
    window: {
      dispatchEvent() {
        expired++;
      },
    },
  });
  return { ...exported, cleared: () => cleared, expired: () => expired };
}

test('an old mutation is never retried using a newly signed-in accounts cookies', async () => {
  let release, started;
  const entered = new Promise((resolve) => {
    started = resolve;
  });
  const delayed = new Promise((resolve) => {
    release = resolve;
  });
  let mutations = 0;
  const api = client(async (url) => {
    if (url.endsWith('/settings')) {
      mutations++;
      if (mutations === 1) {
        started();
        return delayed;
      }
      return Response.json({ targetGpa: 3.8 });
    }
    if (url.endsWith('/auth/login'))
      return Response.json({ success: true, user: { id: 'another-student' } });
    if (url.endsWith('/auth/me')) return Response.json({ user: { userId: 'another-student' } });
    throw new Error(`Unexpected request ${url}`);
  });
  const pending = api.api('settings', { method: 'PATCH', body: '{"targetGpa":3.8}' });
  await entered;
  await api.api('auth/login', { method: 'POST', body: '{}' });
  release(Response.json({ message: 'Old account expired' }, { status: 401 }));
  await assert.rejects(pending, (error) => error.status === 409);
  assert.equal(mutations, 1);
  assert.equal(api.expired(), 0, 'the new account must not receive an old expiry event');
});

test('a rejected password change keeps device preferences and push consent intact', async () => {
  const api = client(async (url) =>
    Response.json(
      url.endsWith('/auth/me') ? { user: { userId: 'student' } } : { message: 'Wrong password' },
      { status: url.endsWith('/auth/me') ? 200 : 401 },
    ),
  );
  await assert.rejects(api.api('auth/change-password', { method: 'POST', body: '{}' }));
  assert.equal(api.cleared(), 0);
});
