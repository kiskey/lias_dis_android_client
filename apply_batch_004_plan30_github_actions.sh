#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# Batch: 004
# Name: GitHub Actions Plan 3.0 validation workflow
# Run from: repository root
#
# Scope:
#   Adds a CI workflow for migration validation only.
#   Does not change application behavior or runtime contracts.
# =====================================================================

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main" ]]; then
  echo "ERROR: run this script from the repository root." >&2
  exit 1
fi

mkdir -p .github/workflows

cat > .github/workflows/lias-android-cupertino-plan30.yml <<'EOF'
name: LIAS Android Cupertino Plan 3.0

on:
  pull_request:
    branches:
      - luna
      - main
  push:
    branches:
      - luna
  workflow_dispatch:

permissions:
  contents: read

jobs:
  cupertino-plan30:
    name: Build, test, lint, dependency gate
    runs-on: ubuntu-latest
    timeout-minutes: 45

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Ensure verification scripts are executable
        run: |
          chmod +x scripts/verify_cupertino_plan30.sh || true
          chmod +x scripts/run_cupertino_migration_gate.sh || true

      - name: Static Plan 3.0 boundary checks
        run: scripts/verify_cupertino_plan30.sh

      - name: Gradle version
        run: ./gradlew --version

      - name: Dependency reports
        run: |
          mkdir -p build/plan30-reports
          ./gradlew :app:dependencies --configuration debugRuntimeClasspath > build/plan30-reports/debugRuntimeClasspath.txt
          ./gradlew :app:dependencies --configuration releaseRuntimeClasspath > build/plan30-reports/releaseRuntimeClasspath.txt

      - name: Dependency namespace gate
        run: |
          if grep -RIn 'io.github.alexzhirkevich' build/plan30-reports; then
            echo 'Old Cupertino dependency group resolved. Failing migration gate.' >&2
            exit 1
          fi
          grep -RIn 'io.github.schott12521' build/plan30-reports

      - name: Compile debug and release Kotlin
        run: ./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin

      - name: Unit and contract tests
        run: ./gradlew :app:testDebugUnitTest

      - name: Lint
        run: ./gradlew :app:lintDebug

      - name: Release shrinking and packaging
        run: ./gradlew :app:assembleRelease

      - name: Upload Plan 3.0 reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: lias-cupertino-plan30-reports
          path: |
            build/plan30-reports
            app/build/reports
            app/build/test-results
          if-no-files-found: ignore
EOF

echo "Batch 004 applied: GitHub Actions Plan 3.0 validation workflow added."
