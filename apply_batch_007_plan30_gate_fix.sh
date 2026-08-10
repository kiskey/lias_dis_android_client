#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# Batch: 007
# Name: Plan 3.0 gate self-scan fix
# Run from: repository root
#
# Purpose:
#   Fixes Batch 003 local static gate so it does not fail by finding
#   the old Cupertino package string inside patch scripts, docs,
#   workflow YAML, or the verifier itself.
#
# Contract:
#   Verification/tooling-only change. No app behavior, REST/SSE model,
#   navigation route, persisted setting, policy, schedule, PDID, or
#   production Kotlin API change.
# =====================================================================

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main" ]]; then
  echo "ERROR: run this script from the repository root." >&2
  exit 1
fi

mkdir -p scripts

cat > scripts/verify_cupertino_plan30.sh <<'EOS'
#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# File: scripts/verify_cupertino_plan30.sh
# Version: 30.0.1
#
# Purpose:
#   Local static verification for Compose Cupertino Plan 3.0.
#
# Version 30.0.1 fix:
#   The old verifier scanned the entire repository and therefore found
#   literal references to the legacy Cupertino group inside patch
#   scripts, workflow checks, docs, and this verifier itself. That made
#   the gate fail even when app source and Gradle dependency inputs were
#   correctly migrated.
#
#   This verifier intentionally scans only code/build inputs that can
#   affect the Android app build:
#     - app/src/**/*.kt
#     - app/build.gradle.kts
#     - build.gradle.kts
#     - settings.gradle.kts
#     - gradle/libs.versions.toml
#     - gradle.properties
#
#   Dependency-report checks remain in run_cupertino_migration_gate.sh.
# =====================================================================

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if [[ ! -f "settings.gradle.kts" || ! -d "app/src" ]]; then
  echo "ERROR: run from inside the LIAS Android repository." >&2
  exit 1
fi

TMP_OLD="/tmp/lias_old_cupertino_plan30_refs.txt"
TMP_NEW="/tmp/lias_new_cupertino_plan30_refs.txt"
: > "${TMP_OLD}"
: > "${TMP_NEW}"

scan_files=()

while IFS= read -r -d '' file; do
  scan_files+=("${file}")
done < <(find app/src -type f -name '*.kt' -print0)

for file in \
  app/build.gradle.kts \
  build.gradle.kts \
  settings.gradle.kts \
  gradle/libs.versions.toml \
  gradle.properties; do
  if [[ -f "${file}" ]]; then
    scan_files+=("${file}")
  fi
done

if [[ "${#scan_files[@]}" -eq 0 ]]; then
  echo "ERROR: no source/build files found to verify." >&2
  exit 1
fi

if grep -Hn 'io.github.alexzhirkevich' "${scan_files[@]}" > "${TMP_OLD}" 2>/dev/null; then
  cat "${TMP_OLD}" >&2
  echo "ERROR: old Compose Cupertino package/group remains in app source or Gradle inputs." >&2
  exit 1
fi

if ! grep -Hn 'com.slapps.cupertino' "${scan_files[@]}" > "${TMP_NEW}" 2>/dev/null; then
  echo "ERROR: maintained fork package com.slapps.cupertino was not found in app source." >&2
  exit 1
fi

if grep -n 'io.github.alexzhirkevich' gradle/libs.versions.toml >/dev/null 2>&1; then
  echo "ERROR: version catalog still references old Cupertino group." >&2
  exit 1
fi

if ! grep -n 'io.github.schott12521' gradle/libs.versions.toml >/dev/null 2>&1; then
  echo "ERROR: version catalog does not reference maintained Cupertino fork group." >&2
  exit 1
fi

if ! grep -n '^cupertino = "2\.3\.1"' gradle/libs.versions.toml >/dev/null 2>&1; then
  echo "ERROR: Cupertino version is not pinned to 2.3.1." >&2
  exit 1
fi

if ! grep -n '^kotlin = "2\.2\.0"' gradle/libs.versions.toml >/dev/null 2>&1; then
  echo "ERROR: Kotlin version is not pinned to 2.2.0." >&2
  exit 1
fi

if ! grep -n '^agp = "8\.10\.1"' gradle/libs.versions.toml >/dev/null 2>&1; then
  echo "ERROR: AGP version is not pinned to 8.10.1." >&2
  exit 1
fi

if ! grep -n '^kotlinxSerialization = "1\.7\.3"' gradle/libs.versions.toml >/dev/null 2>&1; then
  echo "ERROR: kotlinx.serialization JSON is not pinned to 1.7.3." >&2
  exit 1
fi

if ! grep -n '^composeBom = "2025\.06\.00"' gradle/libs.versions.toml >/dev/null 2>&1; then
  echo "ERROR: Compose BOM is not pinned to 2025.06.00." >&2
  exit 1
fi

if ! grep -n 'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.11.1-bin.zip' \
  gradle/wrapper/gradle-wrapper.properties >/dev/null 2>&1; then
  echo "ERROR: Gradle wrapper is not pinned to 8.11.1." >&2
  exit 1
fi

if ! grep -RIn 'minSdk[[:space:]]*=[[:space:]]*26' app/build.gradle.kts >/dev/null 2>&1; then
  echo "ERROR: minSdk 26 contract not found in app/build.gradle.kts." >&2
  exit 1
fi

if ! grep -RIn 'targetSdk[[:space:]]*=[[:space:]]*35' app/build.gradle.kts >/dev/null 2>&1; then
  echo "ERROR: targetSdk 35 contract not found in app/build.gradle.kts." >&2
  exit 1
fi

if ! grep -RIn 'compileSdk[[:space:]]*=[[:space:]]*36' app/build.gradle.kts >/dev/null 2>&1; then
  echo "ERROR: compileSdk 36 contract not found in app/build.gradle.kts." >&2
  exit 1
fi

if ! grep -RIn 'JavaVersion.VERSION_17' app/build.gradle.kts >/dev/null 2>&1; then
  echo "ERROR: Java/JVM 17 contract not found in app/build.gradle.kts." >&2
  exit 1
fi

echo "Static Plan 3.0 Cupertino migration checks passed."
EOS

chmod +x scripts/verify_cupertino_plan30.sh

cat > scripts/run_cupertino_migration_gate.sh <<'EOS'
#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# File: scripts/run_cupertino_migration_gate.sh
# Version: 30.0.1
#
# Purpose:
#   Runs local Plan 3.0 static, dependency, compile, lint, test, and
#   packaging checks.
#
# Version 30.0.1 fix:
#   Uses the corrected scoped static verifier from Batch 007.
# =====================================================================

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

scripts/verify_cupertino_plan30.sh

./gradlew \
  :app:dependencies --configuration debugRuntimeClasspath \
  > build/debugRuntimeClasspath-plan30.txt

./gradlew \
  :app:dependencies --configuration releaseRuntimeClasspath \
  > build/releaseRuntimeClasspath-plan30.txt

if grep -RIn 'io.github.alexzhirkevich' \
  build/debugRuntimeClasspath-plan30.txt \
  build/releaseRuntimeClasspath-plan30.txt; then
  echo "ERROR: old Compose Cupertino dependency group remains in resolved runtime classpath." >&2
  exit 1
fi

if ! grep -RIn 'io.github.schott12521.*cupertino.*2.3.1' \
  build/debugRuntimeClasspath-plan30.txt \
  build/releaseRuntimeClasspath-plan30.txt >/dev/null 2>&1; then
  echo "ERROR: maintained Compose Cupertino fork 2.3.1 not found in resolved runtime classpath." >&2
  exit 1
fi

./gradlew \
  :app:compileDebugKotlin \
  :app:compileReleaseKotlin \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleRelease

echo "Plan 3.0 local migration gate passed."
EOS

chmod +x scripts/run_cupertino_migration_gate.sh

echo "Batch 007 applied: Plan 3.0 local gate now ignores docs/patch/workflow self-references and scans only Android source/build inputs."
echo "Run: scripts/run_cupertino_migration_gate.sh"
