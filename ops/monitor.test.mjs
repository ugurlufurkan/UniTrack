import { test } from 'node:test';
import assert from 'node:assert/strict';
import { advance, cycle } from './monitor.mjs';
import { mkdtemp, rm, rmdir } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';

test('monitor alerts after three failures, throttles unchanged state and reports recovery once', () => {
  let state = advance({}, false, 1000);
  assert.equal(state.alert, null);
  state = advance(state, false, 2000);
  state = advance(state, false, 3000);
  assert.equal(state.alert, 'failed');
  state = advance(state, false, 4000);
  assert.equal(state.alert, null);
  state = advance(state, false, 904000);
  assert.equal(state.alert, 'failed');
  state = advance(state, true, 905000);
  assert.equal(state.alert, 'recovered');
  assert.equal(advance(state, true, 906000).alert, null);
  assert.equal(advance(state, true, 906000).pending, 'recovered', 'undelivered recovery must survive the next healthy cycle');
});

test('retrying an undelivered alert preserves the status in its message', async () => {
  const directory = await mkdtemp(path.join(tmpdir(), 'unitrack-monitor-'));
  const stateFile = path.join(directory, 'state.json');
  const originalFetch = globalThis.fetch;
  const configuration = {
    MONITOR_STATE: stateFile, API_HEALTH_URL: 'https://api.example.invalid/health',
    WEB_HEALTH_URL: 'https://web.example.invalid/health', ENABLE_EXTERNAL_ALERTS: 'true',
    ALERT_WEBHOOK_URL: 'https://alerts.example.invalid/', BACKUP_ENABLED: 'false', PUSH_ENABLED: 'false',
  };
  const saved = Object.fromEntries(Object.keys(configuration).map(key => [key, process.env[key]]));
  Object.assign(process.env, configuration);
  const sent = [];
  globalThis.fetch = async (url, options) => {
    if (url === configuration.ALERT_WEBHOOK_URL) {
      sent.push(JSON.parse(options.body));
      return new Response('', { status: sent.length === 1 ? 503 : 200 });
    }
    return new Response('', { status: url === configuration.API_HEALTH_URL ? 503 : 200 });
  };
  try {
    for (let n = 0; n < 4; n++) await cycle();
    assert.equal(sent.length, 2);
    assert.deepEqual(sent.map(item => ({ status: item.status, text: item.text })), [
      { status: 'failed', text: 'UniTrack api: failed' },
      { status: 'failed', text: 'UniTrack api: failed' },
    ]);
  } finally {
    globalThis.fetch = originalFetch;
    for (const [key, value] of Object.entries(saved)) { if (value === undefined) delete process.env[key]; else process.env[key] = value; }
    await rm(stateFile, { force: true });
    await rm(`${stateFile}.tmp`, { force: true });
    await rmdir(directory);
  }
});
