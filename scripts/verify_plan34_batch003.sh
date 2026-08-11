#!/usr/bin/env bash
set -euo pipefail

ALERT="app/src/main/java/com/lias/remote/ui/components/HigAlertDialog.kt"
grep -q 'CupertinoAlertDialog(' "$ALERT"
grep -q 'destructive(' "$ALERT"
grep -q 'cancel(' "$ALERT"

for f in \
  app/src/main/java/com/lias/remote/ui/screens/devices/DeviceDetailScreen.kt \
  app/src/main/java/com/lias/remote/ui/screens/identity/IdentityReviewScreen.kt \
  app/src/main/java/com/lias/remote/ui/screens/settings/ConnectionSettingsScreen.kt
do
  grep -q 'CupertinoNavigateBackButton(' "$f"
  grep -q '@OptIn(ExperimentalCupertinoApi::class)' "$f"
done

echo "PASS: Plan 3.4 Batch 003 static gate passed."
