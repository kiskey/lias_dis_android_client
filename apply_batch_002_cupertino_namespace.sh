#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# Batch: 002
# Name: Mechanical Cupertino namespace migration
# Run from: repository root
#
# Scope:
#   - Replaces old Compose Cupertino Kotlin imports/package references:
#       io.github.alexzhirkevich.cupertino -> com.slapps.cupertino
#   - Replaces any leftover old Maven group in Gradle files:
#       io.github.alexzhirkevich -> io.github.schott12521
#   - Does not change call sites, UI behavior, navigation, LIAS API,
#     repository ownership, persisted settings, REST, SSE, PDID, policy,
#     or schedule semantics.
# =====================================================================

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main" ]]; then
  echo "ERROR: run this script from the repository root." >&2
  exit 1
fi

if ! grep -R "io.github.alexzhirkevich.cupertino\|io.github.alexzhirkevich" \
  app gradle build.gradle.kts settings.gradle.kts gradle.properties 2>/dev/null >/tmp/lias_old_cupertino_refs_before.txt; then
  echo "No old Cupertino namespace/group references found. Batch 002 is already applied or unnecessary."
else
  echo "Old Cupertino references found before migration:"
  cat /tmp/lias_old_cupertino_refs_before.txt
fi

# Kotlin/source namespace migration. Safe for import lines and fully-qualified references.
find app/src -type f \( -name '*.kt' -o -name '*.kts' \) -print0 \
  | xargs -0 perl -0pi -e 's/io\.github\.alexzhirkevich\.cupertino/com.slapps.cupertino/g'

# Gradle/script/documented dependency group cleanup inside Android project files.
find . -maxdepth 4 -type f \
  \( -name '*.gradle' -o -name '*.gradle.kts' -o -name 'libs.versions.toml' -o -name '*.toml' -o -name '*.md' \) \
  -not -path './.git/*' -print0 \
  | xargs -0 perl -0pi -e 's/io\.github\.alexzhirkevich/io.github.schott12521/g'

# Guard: no old Cupertino namespace or Maven group should remain in the Android project.
if grep -R "io.github.alexzhirkevich.cupertino\|io.github.alexzhirkevich" \
  app gradle build.gradle.kts settings.gradle.kts gradle.properties 2>/dev/null; then
  echo "ERROR: old Cupertino namespace/group references still remain. See matches above." >&2
  exit 1
fi

# Guard: new dependency group should exist after Batch 001/002.
if ! grep -q 'io.github.schott12521' gradle/libs.versions.toml; then
  echo "ERROR: expected maintained fork group io.github.schott12521 in gradle/libs.versions.toml." >&2
  exit 1
fi

# Guard: source imports should use the fork namespace where Cupertino is referenced.
if grep -R "import com.slapps.cupertino" app/src/main app/src/test 2>/dev/null >/tmp/lias_new_cupertino_imports_after.txt; then
  echo "New Cupertino imports found after migration:"
  cat /tmp/lias_new_cupertino_imports_after.txt
else
  echo "WARNING: no com.slapps.cupertino imports found. Verify whether source files still use Cupertino symbols." >&2
fi

echo "Batch 002 applied. Suggested checks:"
echo "  grep -R \"io.github.alexzhirkevich\" app gradle build.gradle.kts settings.gradle.kts gradle.properties || true"
echo "  ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest"
