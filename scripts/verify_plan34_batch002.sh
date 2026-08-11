#!/usr/bin/env bash
set -euo pipefail
FILE="app/src/main/java/com/lias/remote/ui/components/HigLargeTitleScaffold.kt"

grep -q 'CupertinoTopAppBar(' "$FILE"
grep -q 'CupertinoNavigationTitle(' "$FILE"
grep -q 'hasNavigationTitle' "$FILE"
grep -q 'isTopBarTransparent' "$FILE"

if grep -q 'firstVisibleItemScrollOffset' "$FILE"; then
  echo "ERROR: old binary title collapse remains."
  exit 1
fi

for f in \
  app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt \
  app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt \
  app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulesScreen.kt \
  app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt \
  app/src/main/java/com/lias/remote/ui/screens/settings/SettingsScreen.kt \
  app/src/main/java/com/lias/remote/ui/screens/identity/IdentityReviewScreen.kt
do
  grep -q '"cupertino-navigation-header"' "$f"
  grep -q 'navigationHeader()' "$f"
done

echo "PASS: Plan 3.4 Batch 002 static gate passed."
