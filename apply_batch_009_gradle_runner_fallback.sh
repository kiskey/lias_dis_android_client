#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# Batch: 009
# Name: Plan 3.0 Gradle runner fallback
# Run from: repository root
#
# Purpose:
#   Fix the local Plan 3.0 gate so a checkout without ./gradlew can still
#   run using a system Gradle installation.
#
# Scope:
#   - Verification tooling only.
#   - No app source behavior changes.
#   - No LIAS API, REST, SSE, PDID, repository, persistence, navigation,
#     policy, or schedule changes.
# =====================================================================

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main" ]]; then
  echo "ERROR: run this script from the repository root." >&2
  exit 1
fi

mkdir -p scripts

cat > scripts/run_cupertino_migration_gate.sh <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# File: scripts/run_cupertino_migration_gate.sh
# Version: 30.0.3
#
# Purpose:
#   Local Plan 3.0 verification gate for the Compose Cupertino maintained
#   fork migration.
#
# Behavior:
#   - Runs static migration guard.
#   - Uses ./gradlew when present.
#   - Falls back to system gradle when wrapper script is absent.
#   - Gives a clear repair message if neither runner is available.
# =====================================================================

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

scripts/verify_cupertino_plan30.sh

if [[ -x "./gradlew" ]]; then
  GRADLE_RUNNER="./gradlew"
elif [[ -f "./gradlew" ]]; then
  chmod +x ./gradlew
  GRADLE_RUNNER="./gradlew"
elif command -v gradle >/dev/null 2>&1; then
  GRADLE_RUNNER="gradle"
else
  cat >&2 <<'MSG'
ERROR: no Gradle runner found.

This checkout does not contain ./gradlew, and no system gradle command is available.

Fix options:

1. If the repository should contain the wrapper, restore it from Git:
   git checkout -- gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties

2. If wrapper files are absent from the branch, install Gradle 8.11.1 locally
   and rerun this gate.

3. If you intentionally use system Gradle, ensure `gradle --version` works
   and uses a Java 17 runtime.
MSG
  exit 1
fi

echo "Using Gradle runner: ${GRADLE_RUNNER}"

"${GRADLE_RUNNER}" \
  :app:compileDebugKotlin \
  :app:compileReleaseKotlin \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleRelease

mkdir -p build

"${GRADLE_RUNNER}" \
  :app:dependencies \
  --configuration debugRuntimeClasspath \
  > build/debugRuntimeClasspath-plan30.txt

"${GRADLE_RUNNER}" \
  :app:dependencies \
  --configuration releaseRuntimeClasspath \
  > build/releaseRuntimeClasspath-plan30.txt

if grep -RIn 'io.github.alexzhirkevich' \
  build/debugRuntimeClasspath-plan30.txt \
  build/releaseRuntimeClasspath-plan30.txt; then

  echo "ERROR: old Compose Cupertino dependency remains in resolved runtime classpath." >&2
  exit 1
fi

if ! grep -RIn 'io.github.schott12521.*cupertino' \
  build/debugRuntimeClasspath-plan30.txt \
  build/releaseRuntimeClasspath-plan30.txt >/dev/null; then

  echo "ERROR: maintained Compose Cupertino fork was not found in resolved runtime classpath." >&2
  exit 1
fi

echo "PASS: Plan 3.0 local static, compile, unit, lint, release packaging, and dependency gates passed."
EOF

chmod +x scripts/run_cupertino_migration_gate.sh

echo "Batch 009 applied: local gate now falls back to system Gradle when ./gradlew is absent."
echo "Run: scripts/run_cupertino_migration_gate.sh"
