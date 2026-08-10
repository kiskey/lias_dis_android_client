#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# Batch: 003
# Name: Plan 3.0 local migration guard scripts
# Run from: repository root
#
# Scope:
#   Adds local verification scripts only.
#   Does not change application behavior, LIAS API contracts, UI flows,
#   persisted settings, navigation, REST/SSE handling, or engine ownership.
# =====================================================================

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main" ]]; then
  echo "ERROR: run this script from the repository root." >&2
  exit 1
fi

mkdir -p scripts

cat > scripts/verify_cupertino_plan30.sh <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT"

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

GREP_CMD=(grep -RIn --exclude-dir=.git --exclude-dir=.gradle --exclude-dir=build --exclude='*.zip' --exclude='*.jar' --exclude='*.class')

if "${GREP_CMD[@]}" 'io\.github\.alexzhirkevich' . >/tmp/lias_old_cupertino_refs.txt 2>/dev/null; then
  cat /tmp/lias_old_cupertino_refs.txt >&2
  fail "old Compose Cupertino package/group remains"
fi

if "${GREP_CMD[@]}" 'io.github.alexzhirkevich' . >/tmp/lias_old_cupertino_literal_refs.txt 2>/dev/null; then
  cat /tmp/lias_old_cupertino_literal_refs.txt >&2
  fail "old Compose Cupertino literal namespace remains"
fi

grep -q 'agp = "8\.10\.1"' gradle/libs.versions.toml || fail "AGP must be pinned to 8.10.1"
grep -q 'kotlin = "2\.2\.0"' gradle/libs.versions.toml || fail "Kotlin must be pinned to 2.2.0"
grep -q 'composeBom = "2025\.06\.00"' gradle/libs.versions.toml || fail "Compose BOM must be pinned to 2025.06.00"
grep -q 'kotlinxSerialization = "1\.7\.3"' gradle/libs.versions.toml || fail "kotlinx.serialization JSON must be pinned to 1.7.3"
grep -q 'cupertino = "2\.3\.1"' gradle/libs.versions.toml || fail "Cupertino must be pinned to 2.3.1"
grep -q 'group = "io.github.schott12521", name = "cupertino"' gradle/libs.versions.toml || fail "cupertino artifact must use io.github.schott12521"
grep -q 'group = "io.github.schott12521", name = "cupertino-icons-extended"' gradle/libs.versions.toml || fail "cupertino-icons-extended artifact must use io.github.schott12521"

grep -q 'minSdk[[:space:]]*=[[:space:]]*26' app/build.gradle.kts || fail "minSdk must remain 26"
grep -q 'compileSdk[[:space:]]*=[[:space:]]*36' app/build.gradle.kts || fail "compileSdk must remain 36"
grep -q 'targetSdk[[:space:]]*=[[:space:]]*35' app/build.gradle.kts || fail "targetSdk must remain 35"
grep -q 'JavaVersion.VERSION_17' app/build.gradle.kts || fail "Java source/target compatibility must remain 17"
grep -q 'jvmTarget[[:space:]]*=' app/build.gradle.kts || fail "Kotlin JVM target must remain declared"
grep -q '"17"' app/build.gradle.kts || fail "Kotlin JVM target must remain 17"
grep -q 'gradle-8\.11\.1-bin\.zip' gradle/wrapper/gradle-wrapper.properties || fail "Gradle wrapper must remain 8.11.1"

if ! "${GREP_CMD[@]}" 'com\.slapps\.cupertino' app/src >/tmp/lias_new_cupertino_refs.txt 2>/dev/null; then
  fail "no maintained-fork com.slapps.cupertino imports found under app/src"
fi

if "${GREP_CMD[@]}" 'cupertino-adaptive\|cupertino-native\|cupertino-decompose' . >/tmp/lias_out_of_scope_cupertino_modules.txt 2>/dev/null; then
  cat /tmp/lias_out_of_scope_cupertino_modules.txt >&2
  fail "out-of-scope Cupertino module dependency detected"
fi

echo "PASS: Plan 3.0 static migration guard passed."
EOF

cat > scripts/run_cupertino_migration_gate.sh <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT"

chmod +x scripts/verify_cupertino_plan30.sh
scripts/verify_cupertino_plan30.sh

mkdir -p build

./gradlew --version
./gradlew :app:dependencies --configuration debugRuntimeClasspath > build/debugRuntimeClasspath-plan30.txt
./gradlew :app:dependencies --configuration releaseRuntimeClasspath > build/releaseRuntimeClasspath-plan30.txt
./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleRelease

if grep -RIn 'io.github.alexzhirkevich' build/debugRuntimeClasspath-plan30.txt build/releaseRuntimeClasspath-plan30.txt; then
  echo "ERROR: old Cupertino group found in dependency reports." >&2
  exit 1
fi

if ! grep -RIn 'io.github.schott12521' build/debugRuntimeClasspath-plan30.txt build/releaseRuntimeClasspath-plan30.txt >/dev/null; then
  echo "ERROR: maintained Cupertino fork not found in dependency reports." >&2
  exit 1
fi

echo "PASS: Plan 3.0 Gradle migration gate completed."
echo "Reports:"
echo "  build/debugRuntimeClasspath-plan30.txt"
echo "  build/releaseRuntimeClasspath-plan30.txt"
EOF

chmod +x scripts/verify_cupertino_plan30.sh scripts/run_cupertino_migration_gate.sh

echo "Batch 003 applied: local Plan 3.0 verification scripts added."
echo "Run: scripts/run_cupertino_migration_gate.sh"
