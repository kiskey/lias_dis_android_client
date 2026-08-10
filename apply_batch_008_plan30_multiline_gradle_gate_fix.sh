#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# Batch: 008
# Name: Plan 3.0 multiline Gradle contract gate fix
# Run from: repository root
#
# Purpose:
#   Fixes the Plan 3.0 local verification gate so it correctly accepts
#   the LIAS repo's established multiline Gradle style, for example:
#
#       minSdk =
#           26
#
#   Previous gate versions incorrectly required assignments like
#   `minSdk = 26` to appear on one physical line.
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
# Version: 30.0.2
#
# Purpose:
#   Local static verification for Compose Cupertino Plan 3.0.
#
# Fixes:
#   30.0.1 scoped the old-Cupertino string scan to app source and
#   Gradle build inputs only.
#
#   30.0.2 accepts the repo's multiline Gradle Kotlin DSL assignment
#   style for minSdk, compileSdk, targetSdk, versionCode, and jvmTarget.
#
# Scope:
#   This is a verification script only. It must not change source files
#   or build outputs except temporary grep files under /tmp.
# =====================================================================

ROOT_DIR="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${ROOT_DIR}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_file() {
  [[ -f "$1" ]] || fail "missing required file: $1"
}

require_file gradle/libs.versions.toml
require_file app/build.gradle.kts
require_file build.gradle.kts
require_file settings.gradle.kts
require_file gradle/wrapper/gradle-wrapper.properties
require_file gradle.properties

# Normalize Gradle Kotlin DSL formatting so both one-line and multiline
# assignments are checked correctly. This intentionally removes comments
# and whitespace only for static contract checks.
compact_file() {
  local file="$1"
  sed -E 's://.*$::' "${file}" | tr -d '[:space:]'
}

require_compact_contains() {
  local file="$1"
  local compact_pattern="$2"
  local message="$3"

  if ! compact_file "${file}" | grep -q -- "${compact_pattern}"; then
    fail "${message}"
  fi
}

TMP_OLD="/tmp/lias_old_cupertino_plan30_refs.txt"
TMP_NEW="/tmp/lias_new_cupertino_plan30_refs.txt"
TMP_OOS="/tmp/lias_oos_cupertino_plan30_refs.txt"
: > "${TMP_OLD}"
: > "${TMP_NEW}"
: > "${TMP_OOS}"

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
  fail "no source/build files found to verify"
fi

if grep -Hn 'io.github.alexzhirkevich' "${scan_files[@]}" > "${TMP_OLD}" 2>/dev/null; then
  cat "${TMP_OLD}" >&2
  fail "old Compose Cupertino package/group remains in app source or Gradle inputs"
fi

if ! grep -Hn 'com.slapps.cupertino' "${scan_files[@]}" > "${TMP_NEW}" 2>/dev/null; then
  fail "maintained fork package com.slapps.cupertino was not found in app source"
fi

if grep -HnE 'cupertino-adaptive|cupertino-native|cupertino-decompose' "${scan_files[@]}" > "${TMP_OOS}" 2>/dev/null; then
  cat "${TMP_OOS}" >&2
  fail "out-of-scope Cupertino module dependency detected"
fi

# Version catalog / dependency contract.
grep -q '^agp = "8\.10\.1"' gradle/libs.versions.toml || fail "AGP must be pinned to 8.10.1"
grep -q '^kotlin = "2\.2\.0"' gradle/libs.versions.toml || fail "Kotlin must be pinned to 2.2.0"
grep -q '^composeBom = "2025\.06\.00"' gradle/libs.versions.toml || fail "Compose BOM must be pinned to 2025.06.00"
grep -q '^kotlinxSerialization = "1\.7\.3"' gradle/libs.versions.toml || fail "kotlinx.serialization JSON must be pinned to 1.7.3"
grep -q '^cupertino = "2\.3\.1"' gradle/libs.versions.toml || fail "Cupertino must be pinned to 2.3.1"
grep -q 'group = "io.github.schott12521", name = "cupertino"' gradle/libs.versions.toml || fail "cupertino artifact must use io.github.schott12521"
grep -q 'group = "io.github.schott12521", name = "cupertino-icons-extended"' gradle/libs.versions.toml || fail "cupertino-icons-extended artifact must use io.github.schott12521"

# Android build contract. These checks accept both:
#   minSdk = 26
# and:
#   minSdk =
#       26
require_compact_contains app/build.gradle.kts 'compileSdk=36' 'compileSdk must remain 36'
require_compact_contains app/build.gradle.kts 'minSdk=26' 'minSdk must remain 26'
require_compact_contains app/build.gradle.kts 'targetSdk=35' 'targetSdk must remain 35'
require_compact_contains app/build.gradle.kts 'sourceCompatibility=JavaVersion.VERSION_17' 'Java source compatibility must remain 17'
require_compact_contains app/build.gradle.kts 'targetCompatibility=JavaVersion.VERSION_17' 'Java target compatibility must remain 17'
require_compact_contains app/build.gradle.kts 'jvmTarget="17"' 'Kotlin JVM target must remain 17'

# Gradle wrapper contract.
grep -q 'gradle-8\.11\.1-bin\.zip' gradle/wrapper/gradle-wrapper.properties || fail "Gradle wrapper must remain 8.11.1"

printf '%s\n' "PASS: Plan 3.0 static migration guard passed."
EOS

chmod +x scripts/verify_cupertino_plan30.sh

cat > scripts/run_cupertino_migration_gate.sh <<'EOS'
#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# File: scripts/run_cupertino_migration_gate.sh
# Version: 30.0.2
#
# Purpose:
#   Runs local Plan 3.0 static, dependency, compile, lint, test, and
#   packaging checks.
#
# Scope:
#   Verification only. Does not edit application source.
# =====================================================================

ROOT_DIR="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "${ROOT_DIR}"

scripts/verify_cupertino_plan30.sh

mkdir -p build

./gradlew --version

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

if ! grep -RIn 'io.github.schott12521' \
  build/debugRuntimeClasspath-plan30.txt \
  build/releaseRuntimeClasspath-plan30.txt >/dev/null 2>&1; then
  echo "ERROR: maintained Compose Cupertino fork not found in resolved runtime classpath." >&2
  exit 1
fi

./gradlew \
  :app:compileDebugKotlin \
  :app:compileReleaseKotlin \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleRelease

printf '%s\n' "PASS: Plan 3.0 local migration gate passed."
printf '%s\n' "Reports:"
printf '%s\n' "  build/debugRuntimeClasspath-plan30.txt"
printf '%s\n' "  build/releaseRuntimeClasspath-plan30.txt"
EOS

chmod +x scripts/run_cupertino_migration_gate.sh

printf '%s\n' "Batch 008 applied: Plan 3.0 gate now accepts multiline Gradle Kotlin DSL assignments."
printf '%s\n' "Run: scripts/run_cupertino_migration_gate.sh"
