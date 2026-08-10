#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# File: scripts/verify_cupertino_plan30.sh
# Version: 30.10.0
#
# Purpose:
#   Static Plan 3.0 migration guard.
#
# Notes:
#   This script intentionally does not require ./gradlew or system Gradle.
#   Full compilation, tests, lint, release packaging, and dependency report
#   validation are performed by GitHub Actions.
# =====================================================================

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

normalize_file() {
  local file="$1"
  tr -d '[:space:]' < "${file}"
}

[[ -f "gradle/libs.versions.toml" ]] || fail "gradle/libs.versions.toml missing."
[[ -f "app/build.gradle.kts" ]] || fail "app/build.gradle.kts missing."
[[ -f "settings.gradle.kts" ]] || fail "settings.gradle.kts missing."

CATALOG="$(normalize_file gradle/libs.versions.toml)"
APP_BUILD="$(normalize_file app/build.gradle.kts)"

case "${CATALOG}" in
  *'kotlin="2.2.0"'*) ;;
  *) fail "Kotlin 2.2.0 contract not found in gradle/libs.versions.toml." ;;
esac

case "${CATALOG}" in
  *'agp="8.10.1"'*) ;;
  *) fail "AGP 8.10.1 contract not found in gradle/libs.versions.toml." ;;
esac

case "${CATALOG}" in
  *'composeBom="2025.06.00"'*) ;;
  *) fail "Compose BOM 2025.06.00 contract not found in gradle/libs.versions.toml." ;;
esac

case "${CATALOG}" in
  *'kotlinxSerialization="1.7.3"'*) ;;
  *) fail "kotlinx.serialization JSON 1.7.3 contract not found in gradle/libs.versions.toml." ;;
esac

case "${CATALOG}" in
  *'cupertino="2.3.1"'*) ;;
  *) fail "Cupertino 2.3.1 pin not found in gradle/libs.versions.toml." ;;
esac

case "${CATALOG}" in
  *'group="io.github.schott12521",name="cupertino"'*|*'group="io.github.schott12521",name="cupertino",version.ref="cupertino"'*) ;;
  *) fail "Maintained Cupertino group io.github.schott12521 not found for cupertino." ;;
esac

case "${CATALOG}" in
  *'group="io.github.schott12521",name="cupertino-icons-extended"'*|*'group="io.github.schott12521",name="cupertino-icons-extended",version.ref="cupertino"'*) ;;
  *) fail "Maintained Cupertino group io.github.schott12521 not found for cupertino-icons-extended." ;;
esac

case "${APP_BUILD}" in
  *'compileSdk=36'*) ;;
  *) fail "compileSdk 36 contract not found in app/build.gradle.kts." ;;
esac

case "${APP_BUILD}" in
  *'minSdk=26'*) ;;
  *) fail "minSdk 26 contract not found in app/build.gradle.kts." ;;
esac

case "${APP_BUILD}" in
  *'targetSdk=35'*) ;;
  *) fail "targetSdk 35 contract not found in app/build.gradle.kts." ;;
esac

case "${APP_BUILD}" in
  *'sourceCompatibility=JavaVersion.VERSION_17'*)
    ;;
  *) fail "Java sourceCompatibility VERSION_17 not found in app/build.gradle.kts." ;;
esac

case "${APP_BUILD}" in
  *'targetCompatibility=JavaVersion.VERSION_17'*)
    ;;
  *) fail "Java targetCompatibility VERSION_17 not found in app/build.gradle.kts." ;;
esac

case "${APP_BUILD}" in
  *'jvmTarget="17"'*)
    ;;
  *) fail "Kotlin jvmTarget 17 not found in app/build.gradle.kts." ;;
esac

# Do not scan docs, generated patch scripts, workflow YAML, or this verifier
# for old Cupertino literals. Those files may legitimately describe what is
# prohibited. Scan only actual app/build inputs.
if grep -RIn \
  --include='*.kt' \
  --include='*.kts' \
  --include='*.toml' \
  --exclude-dir='.git' \
  --exclude-dir='build' \
  --exclude-dir='.gradle' \
  'io.github.alexzhirkevich' \
  app gradle build.gradle.kts settings.gradle.kts gradle.properties \
  >/tmp/lias_plan30_old_cupertino_refs.txt 2>/dev/null; then

  cat /tmp/lias_plan30_old_cupertino_refs.txt >&2
  fail "old Compose Cupertino package/group remains in app/build inputs."
fi

if grep -RIn \
  --include='*.kt' \
  --include='*.kts' \
  --exclude-dir='.git' \
  --exclude-dir='build' \
  --exclude-dir='.gradle' \
  'com.slapps.cupertino' \
  app/src \
  >/tmp/lias_plan30_new_cupertino_refs.txt 2>/dev/null; then
  :
else
  fail "new Compose Cupertino namespace com.slapps.cupertino not found under app/src."
fi

echo "PASS: Plan 3.0 static migration guard passed."
echo "NOTE: Gradle build/test/lint/release/dependency gates are remote-only in GitHub Actions."
