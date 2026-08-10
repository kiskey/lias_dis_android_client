#!/usr/bin/env bash
set -euo pipefail

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main/java" ]]; then
  echo "ERROR: run from repository root." >&2
  exit 1
fi

python3 scripts/audit_plan31_text_search_alerts.py

echo "PASS: Plan 3.1 text/search/alert static audit passed."
echo "NOTE: DetailedWeekGrid Canvas is permitted because it is schedule-grid rendering, not search/clear icon drawing."
echo "NOTE: HigAlertDialog's Dialog host is permitted for LIAS-owned editable/adaptive dialogs; short-confirmation migration remains source-specific."
echo "NOTE: compile/test/lint/release packaging must still pass in GitHub Actions."
