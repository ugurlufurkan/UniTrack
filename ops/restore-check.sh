#!/bin/sh
set -eu
set -o pipefail
: "${BACKUP_FILE:?Set the backup filename}"
: "${AGE_IDENTITY_FILE:=/run/secrets/backup_identity}"
case "$BACKUP_FILE" in unitrack-*.dump.age) ;; *) echo 'Invalid backup filename' >&2; exit 1;; esac
case "$BACKUP_FILE" in */*|*..*) echo 'Backup filename must not include a directory' >&2; exit 1;; esac
cd /backups
expected="$(cut -d ' ' -f 1 "$BACKUP_FILE.sha256")"
actual="$(sha256sum "$BACKUP_FILE" | cut -d ' ' -f 1)"
test "$expected" = "$actual" || { echo 'Backup checksum mismatch' >&2; exit 1; }
target="unitrack_restore_check_$(date -u +%Y%m%d%H%M%S)_$$"
# Only this script's newly created disposable database is ever dropped.
created=0
cleanup() { if [ "$created" = 1 ]; then dropdb --maintenance-db=postgres "$target"; fi; }
trap cleanup EXIT
createdb --maintenance-db=postgres "$target"
created=1
age -d -i "$AGE_IDENTITY_FILE" "$BACKUP_FILE" |
  pg_restore --dbname="$target" --single-transaction --exit-on-error --no-owner --no-acl
psql --dbname="$target" --set=ON_ERROR_STOP=1 --quiet --command='SELECT count(*) FROM users; SELECT count(*) FROM courses; SELECT count(*) FROM semesters; SELECT count(*) FROM refresh_tokens;' >/dev/null
if [ -n "${RESTORE_EXPECTED_USERS:-}" ]; then
  test "$(psql --dbname="$target" -At --command='SELECT count(*) FROM users')" = "$RESTORE_EXPECTED_USERS"
fi
if [ -n "${RESTORE_EXPECTED_COURSES:-}" ]; then
  test "$(psql --dbname="$target" -At --command='SELECT count(*) FROM courses')" = "$RESTORE_EXPECTED_COURSES"
fi
printf 'Restore verification passed in disposable database\n'
