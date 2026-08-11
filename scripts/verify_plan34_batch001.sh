#!/usr/bin/env bash
set -euo pipefail
LIBS="gradle/libs.versions.toml"
MANIFEST="app/src/main/AndroidManifest.xml"
NAV="app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt"

grep -q 'activityCompose = "1.9.0"' "$LIBS"
grep -q 'navigationCompose = "2.8.9"' "$LIBS"
grep -q 'cupertino = "2.3.1"' "$LIBS"
grep -q 'android:enableOnBackInvokedCallback="true"' "$MANIFEST"
grep -q 'CupertinoNavigationBar {' "$NAV"
grep -q 'CupertinoNavigationBarItem(' "$NAV"
grep -q 'private fun rootTabRoute(' "$NAV"
grep -q 'private fun ConfiguredLiasApp(' "$NAV"
grep -q '"lias-configuration-root"' "$NAV"
grep -q 'EnterTransition.None' "$NAV"
grep -q 'ExitTransition.None' "$NAV"

if grep -q 'private fun RowScope.TabItem(' "$NAV"; then
  echo "ERROR: custom tab item remains."
  exit 1
fi

echo "PASS: Plan 3.4 Batch 001 static gate passed."
