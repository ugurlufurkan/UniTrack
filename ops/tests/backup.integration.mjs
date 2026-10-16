import { execFileSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import fs from 'node:fs';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const outputRoot = path.join(root, 'ops/.test-output');
fs.mkdirSync(outputRoot, { recursive: true, mode: 0o700 });
const directory = fs.mkdtempSync(path.join(outputRoot, 'backup-'));
const name = `unitrack-backup-check-${randomUUID().slice(0, 8)}`;
let created = false;
const docker = (...args) => execFileSync('docker', args, { cwd: root, encoding: 'utf8', timeout: 300000 });
try {
  docker('build', '-f', 'ops/Backup.Dockerfile', '-t', 'unitrack-backup-check:local', 'ops');
  docker('run', '-d', '--name', name, '-p', '127.0.0.1::5432', '-e', 'POSTGRES_PASSWORD=fixture-password', '-e', 'POSTGRES_DB=unitrack_test', 'postgres:17-alpine');
  created = true;
  let ready = false;
  for (let n = 0; n < 60; n++) {
    try { docker('exec', name, 'pg_isready', '-h', '127.0.0.1', '-U', 'postgres', '-d', 'unitrack_test'); ready = true; break; }
    catch { await new Promise(resolve => setTimeout(resolve, 250)); }
  }
  assert.ok(ready);
  const port = docker('port', name, '5432/tcp').trim().split(':').at(-1);
  execFileSync(process.execPath, ['--test', 'backend/tests/postgres.integration.cjs'], { cwd: root, stdio: 'inherit', env: { ...process.env, TEST_DATABASE_URL: `postgresql://postgres:fixture-password@127.0.0.1:${port}/unitrack_test` } });
  const user = randomUUID(), semester = randomUUID();
  docker('exec', name, 'psql', '-U', 'postgres', '-d', 'unitrack_test', '-v', 'ON_ERROR_STOP=1', '-c', `INSERT INTO users (id, email, name) VALUES ('${user}', 'backup@example.invalid', 'Backup account'); INSERT INTO semesters (id,user_id,year,term) VALUES ('${semester}','${user}',2026,'Güz'); INSERT INTO courses (semester_id,name,credit) VALUES ('${semester}','Backup course',3);`);
  const mount = `${directory}:/backups`;
  docker('run', '--rm', '-v', mount, '--entrypoint', 'age-keygen', 'unitrack-backup-check:local', '-o', '/backups/identity');
  const recipient = docker('run', '--rm', '-v', mount, '--entrypoint', 'age-keygen', 'unitrack-backup-check:local', '-y', '/backups/identity').trim();
  const common = ['run', '--rm', '--network', `container:${name}`, '-v', mount, '-e', 'PGHOST=127.0.0.1', '-e', 'PGUSER=postgres', '-e', 'PGPASSWORD=fixture-password'];
  console.log(docker(...common, '-e', 'PGDATABASE=unitrack_test', '-e', `AGE_RECIPIENT=${recipient}`, 'unitrack-backup-check:local'));
  const archive = fs.readdirSync(directory).find(file => file.endsWith('.dump.age'));
  assert.ok(archive);
  assert.equal(fs.readFileSync(path.join(directory, archive)).includes(Buffer.from('Backup account')), false, 'backup must not contain plaintext student data');
  console.log(docker(...common, '-e', `BACKUP_FILE=${archive}`, '-e', 'AGE_IDENTITY_FILE=/backups/identity', '-e', 'RESTORE_EXPECTED_USERS=1', '-e', 'RESTORE_EXPECTED_COURSES=1', 'unitrack-backup-check:local', '/scripts/restore-check.sh'));
  const databases = docker('exec', name, 'psql', '-U', 'postgres', '-At', '-c', "SELECT datname FROM pg_database WHERE datname LIKE 'unitrack_restore_check_%'");
  assert.equal(databases.trim(), '', 'temporary restore database must be removed');
  console.log('Encrypted PostgreSQL backup and verified restore passed');
} finally {
  if (created) { try { docker('rm', '-f', '-v', name); } catch {} }
  const resolved = fs.realpathSync(directory);
  if (!resolved.startsWith(`${fs.realpathSync(outputRoot)}${path.sep}`)) throw new Error('Unsafe temporary cleanup path');
  fs.rmSync(resolved, { recursive: true, force: true });
}
