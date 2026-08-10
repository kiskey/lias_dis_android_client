#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# Batch: 010
# Name: Remote Actions build path without local Gradle wrapper
# Run from: repository root
#
# Purpose:
#   The user's local checkout intentionally does not contain ./gradlew.
#   This batch makes the migration support match that workflow:
#
#   - local gate performs static Plan 3.0 checks and does NOT require
#     Gradle/gradlew;
#   - GitHub Actions installs Gradle 8.11.1 and runs the full Android
#     compile/test/lint/release/dependency gates remotely;
#   - no app source behavior, REST/SSE contract, PDID handling,
#     navigation, persistence, policy, or schedule semantics change.
# =====================================================================

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main" ]]; then
  echo "ERROR: run this script from the repository root." >&2
  exit 1
fi

mkdir -p scripts .github/workflows docs

cat > scripts/verify_cupertino_plan30.sh <<'EOF'
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
EOF

chmod +x scripts/verify_cupertino_plan30.sh

cat > scripts/run_cupertino_migration_gate.sh <<'EOF'
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
EOF

chmod +x scripts/run_cupertino_migration_gate.sh

cat > .github/workflows/lias-android-cupertino-plan30.yml <<'EOF'
name: LIAS Android Cupertino Plan 3.0

on:
  push:
    branches:
      - luna
      - main
      - master
  pull_request:
  workflow_dispatch:

jobs:
  plan30-android-validation:
    name: Plan 3.0 Android validation
    runs-on: ubuntu-latest
    timeout-minutes: 45

    permissions:
      contents: read

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Gradle 8.11.1
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.11.1'

      - name: Static Plan 3.0 guard
        run: scripts/verify_cupertino_plan30.sh

      - name: Compile debug Kotlin
        run: gradle --no-daemon :app:compileDebugKotlin

      - name: Compile release Kotlin
        run: gradle --no-daemon :app:compileReleaseKotlin

      - name: Unit and contract tests
        run: gradle --no-daemon :app:testDebugUnitTest

      - name: Lint debug
        run: gradle --no-daemon :app:lintDebug

      - name: Release shrinking and packaging
        run: gradle --no-daemon :app:assembleRelease

      - name: Dependency reports
        run: |
          mkdir -p build/plan30-reports
          gradle --no-daemon :app:dependencies --configuration debugRuntimeClasspath \
            > build/plan30-reports/debugRuntimeClasspath.txt
          gradle --no-daemon :app:dependencies --configuration releaseRuntimeClasspath \
            > build/plan30-reports/releaseRuntimeClasspath.txt

      - name: Verify maintained Cupertino dependency only
        run: |
          if grep -RIn 'io.github.alexzhirkevich' build/plan30-reports; then
            echo "Old Compose Cupertino group remains in runtime dependency graph." >&2
            exit 1
          fi

          grep -RIn 'io.github.schott12521.*cupertino' build/plan30-reports

      - name: Upload Plan 3.0 reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: plan30-gradle-reports
          path: |
            build/plan30-reports
            app/build/reports
EOF

cat > docs/compose_cupertino_plan30_remote_actions.md <<'EOF'
# Compose Cupertino Plan 3.0 remote Actions workflow

Status: local wrapperless workflow support

## Purpose

This project checkout may not include `./gradlew`. Plan 3.0 validation is
therefore split into:

1. local static migration validation; and
2. full remote Android validation in GitHub Actions using installed Gradle 8.11.1.

## Local command

Run from the repository root:

```bash
scripts/run_cupertino_migration_gate.sh
```

This validates:

- Kotlin `2.2.0`;
- AGP `8.10.1`;
- Compose BOM `2025.06.00`;
- serialization JSON `1.7.3`;
- Cupertino fork `io.github.schott12521:*:2.3.1`;
- `compileSdk 36`, `minSdk 26`, `targetSdk 35`;
- Java/JVM 17;
- absence of the old Cupertino namespace/group from app/build inputs;
- presence of the new `com.slapps.cupertino` namespace in app source.

## Remote command

Commit and push the migration branch, then run:

```bash
git status
git add .
git commit -m "Migrate Compose Cupertino Plan 3.0"
git push origin luna
```

GitHub Actions workflow:

```text
LIAS Android Cupertino Plan 3.0
```

Remote validation runs:

- `:app:compileDebugKotlin`;
- `:app:compileReleaseKotlin`;
- `:app:testDebugUnitTest`;
- `:app:lintDebug`;
- `:app:assembleRelease`;
- debug/release runtime dependency reports;
- old/new Cupertino dependency graph assertions.

## Acceptance rule

Do not run the post-acceptance contract recorder until:

- local static validation passes;
- the GitHub Actions workflow passes;
- the runtime acceptance matrix is reviewed and accepted.

EOF

echo "Batch 010 applied: local gate is wrapperless; GitHub Actions installs Gradle 8.11.1 for full validation."
echo "Run locally:"
echo "  scripts/run_cupertino_migration_gate.sh"
echo "Then commit/push and run:"
echo "  LIAS Android Cupertino Plan 3.0"
