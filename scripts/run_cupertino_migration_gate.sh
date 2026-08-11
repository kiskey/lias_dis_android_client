#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# File: scripts/run_cupertino_migration_gate.sh
# Version: 30.10.0
#
# Purpose:
#   Local Plan 3.0 gate for wrapperless checkouts.
#
# Behavior:
#   - Always runs static migration validation.
#   - Does not require ./gradlew or system Gradle.
#   - If system Gradle is installed and RUN_LOCAL_GRADLE=1 is set, it can
#     optionally run the heavy Gradle tasks locally.
#   - The authoritative compile/test/lint/release/dependency gate is
#     .github/workflows/lias-android-cupertino-plan30.yml.
# =====================================================================

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

scripts/verify_cupertino_plan30.sh

if [[ "${RUN_LOCAL_GRADLE:-0}" != "1" ]]; then
  echo "PASS: local wrapperless Plan 3.0 gate complete."
  echo "Next: commit/push and run GitHub Actions workflow: LIAS Android Cupertino Plan 3.0."
  exit 0
fi

if command -v gradle >/dev/null 2>&1; then
  GRADLE_CMD=(gradle)
elif [[ -x "./gradlew" ]]; then
  GRADLE_CMD=(./gradlew)
else
  echo "ERROR: RUN_LOCAL_GRADLE=1 was set, but neither system gradle nor ./gradlew is available." >&2
  exit 1
fi

"${GRADLE_CMD[@]}" \
  --no-daemon \
  :app:compileDebugKotlin \
  :app:compileReleaseKotlin \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleRelease \
  :app:dependencies --configuration debugRuntimeClasspath \
  :app:dependencies --configuration releaseRuntimeClasspath

echo "PASS: optional local Gradle Plan 3.0 gate complete."
