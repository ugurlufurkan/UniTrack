#!/bin/sh
set -eu
set -o pipefail
umask 077
: "${AGE_RECIPIENT:?Set the public age recipient before backing up}"
mkdir -p /backups
cd /backups
name="unitrack-$(date -u +%Y%m%dT%H%M%SZ)-$$.dump.age"
temporary="$name.partial"
trap 'rm -f -- "$temporary"' EXIT
pg_dump --format=custom --no-owner --no-acl | age -r "$AGE_RECIPIENT" -o "$temporary"
test -s "$temporary"
mv "$temporary" "$name"
sha256sum "$name" > "$name.sha256"
date -u +%s > /status/last-success
chmod 644 /status/last-success
printf 'Encrypted backup completed: %s\n' "$name"
