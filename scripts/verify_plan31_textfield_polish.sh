#!/usr/bin/env bash
set -euo pipefail

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main/java" ]]; then
  echo "ERROR: run from repository root." >&2
  exit 1
fi

python3 scripts/audit_plan31_textfield_polish.py

echo "PASS: Plan 3.1 text-field polish static audit passed."
echo "NOTE: remote GitHub Actions must still compile/test/lint/package."
