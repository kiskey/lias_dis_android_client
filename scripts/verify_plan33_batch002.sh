#!/usr/bin/env bash
set -euo pipefail

FILE="app/src/main/java/com/lias/remote/ui/components/HigSheets.kt"

grep -q 'CupertinoBottomSheetScaffold(' "$FILE"
grep -q 'rememberCupertinoSheetState(' "$FILE"
grep -q 'sheetState.show()' "$FILE"
grep -q 'sheetState.hide()' "$FILE"
grep -q 'Plan 3.3 CupertinoSheetState adapter' "$FILE"
grep -q 'animatedCancel' "$FILE"

if grep -q 'AnimatedVisibility(' "$FILE"; then
  echo "ERROR: old HigModalSheet AnimatedVisibility remains."
  exit 1
fi

echo "PASS: Plan 3.3 Batch 002 static gate passed."
