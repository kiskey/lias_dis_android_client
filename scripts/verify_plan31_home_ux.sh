#!/usr/bin/env bash
set -euo pipefail

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main/java" ]]; then
  echo "ERROR: run from repository root." >&2
  exit 1
fi

python3 scripts/audit_plan31_home_ux.py >/tmp/lias_plan31_home_audit.log
cat /tmp/lias_plan31_home_audit.log

if grep -RIn --include='*.kt' 'io.github.alexzhirkevich' app/src >/tmp/lias_plan31_old_cupertino.txt; then
  cat /tmp/lias_plan31_old_cupertino.txt >&2
  echo "ERROR: old Cupertino namespace remains." >&2
  exit 1
fi

if grep -RIn --include='*.kt' 'androidx.compose.material.icons' app/src/main/java/com/lias/remote >/tmp/lias_plan31_material_icons.txt; then
  cat /tmp/lias_plan31_material_icons.txt >&2
  echo "ERROR: Material icons are present in app source. Plan 3.1 Home cards must stay Cupertino-owned." >&2
  exit 1
fi

echo "PASS: Plan 3.1 Home UX static audit passed."
echo "NOTE: build/test/lint must still pass in GitHub Actions."
