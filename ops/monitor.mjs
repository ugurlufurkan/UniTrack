import { readFile, writeFile, mkdir } from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

export function advance(previous = {}, healthy, now = Date.now()) {
  const failures = healthy ? 0 : (previous.failures || 0) + 1;
  const active = previous.active || false;
  const alert = healthy ? (active ? 'recovered' : null) : (failures >= 3 && (!active || now - (previous.lastAlert || 0) >= 900000) ? 'failed' : null);
  return { failures, active: healthy ? false : active || failures >= 3, lastAlert: alert ? now : previous.lastAlert || 0, alert, pending: alert || previous.pending || null };
}

export async function cycle() {
  const stateFile = process.env.MONITOR_STATE || '/state/health.json';
  let state = {};
  try { state = JSON.parse(await readFile(stateFile, 'utf8')); } catch {}
  const targets = {
    api: process.env.API_HEALTH_URL || 'http://backend:5000/api/health',
    web: process.env.WEB_HEALTH_URL || 'http://web:3000/api/health',
  };
  if (process.env.BACKUP_ENABLED === 'true') targets.backup = 'backup-status';
  let allHealthy = true;
  for (const [name, url] of Object.entries(targets)) {
    let healthy = false;
    try {
      if (name === 'backup') {
        const timestamp = Number(await readFile(process.env.BACKUP_STATUS_FILE || '/status/last-success', 'utf8')) * 1000;
        const age = Date.now() - timestamp;
        healthy = age >= 0 && age < 26 * 3600000;
      } else healthy = (await fetch(url, { signal: AbortSignal.timeout(5000) })).ok;
    } catch {}
    allHealthy &&= healthy;
    const next = advance(state[name], healthy);
    if (next.alert) {
      const event = { service: name, status: next.alert, timestamp: new Date().toISOString() };
      console.log(JSON.stringify(event));
    }
    if (next.pending && process.env.ENABLE_EXTERNAL_ALERTS === 'true' && process.env.ALERT_WEBHOOK_URL) {
      const event = { service: name, status: next.pending, timestamp: new Date().toISOString() };
        try {
          const response = await fetch(process.env.ALERT_WEBHOOK_URL, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...event, text: `UniTrack ${name}: ${next.alert}` }), signal: AbortSignal.timeout(5000) });
          if (!response.ok) throw new Error('Delivery rejected');
          next.pending = null;
        } catch { console.error(JSON.stringify({ service: name, error: 'Alert delivery failed; retry pending' })); }
    } else if (process.env.ENABLE_EXTERNAL_ALERTS !== 'true') next.pending = null;
    state[name] = next;
  }
  await mkdir(path.dirname(stateFile), { recursive: true });
  const temporary = `${stateFile}.tmp`;
  await writeFile(temporary, JSON.stringify(state));
  const { rename } = await import('node:fs/promises');
  await rename(temporary, stateFile);
  return allHealthy;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  if (process.argv.includes('--once')) { if (!await cycle()) process.exitCode = 1; }
  else { while (true) { try { await cycle(); } catch { console.error('Monitor state update failed'); } await new Promise(resolve => setTimeout(resolve, 60000)); } }
}
