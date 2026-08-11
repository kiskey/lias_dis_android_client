#!/usr/bin/env bash
set -euo pipefail
FILE="app/src/main/java/com/lias/remote/ui/components/HigSheets.kt"

grep -q 'navigationBarsPadding()' "$FILE"
grep -q 'imePadding()' "$FILE"
grep -q 'BackHandler(' "$FILE"
grep -q 'dismissOnClickOutside' "$FILE"
grep -q 'sheetSwipeEnabled' "$FILE"
grep -q 'paneTitle' "$FILE"
grep -q 'applyContentScaling' "$FILE"

echo "PASS: Plan 3.3 Batch 004 gesture/accessibility gate passed."
