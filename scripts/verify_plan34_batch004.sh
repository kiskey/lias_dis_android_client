#!/usr/bin/env bash
set -euo pipefail
FILE="app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt"

grep -q 'CupertinoActionSheet(' "$FILE"
grep -q '"Apply Block All"' "$FILE"
grep -q 'destructive(' "$FILE"
grep -q 'cancel(' "$FILE"
grep -q $'delay(\n                150' "$FILE"

echo "PASS: Plan 3.4 Batch 004 static gate passed."
