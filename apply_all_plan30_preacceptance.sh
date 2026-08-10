#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# Apply all LIAS Android Compose Cupertino Plan 3.0 pre-acceptance patches.
#
# Run from repo root. This is reset-safe for the user's current state:
# it recreates scripts/, .github/workflows/, docs/, and build/dependency
# files required for Plan 3.0. It intentionally does NOT run Batch 006
# because Batch 006 is post-acceptance only.
# =====================================================================

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main" ]]; then
  echo "ERROR: run this script from the repository root." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

run_batch() {
  local script="$1"
  echo "==> ${script}"
  bash "${SCRIPT_DIR}/${script}"
}

run_batch apply_batch_001_cupertino_toolchain.sh
run_batch apply_batch_002_cupertino_namespace.sh
run_batch apply_batch_003_plan30_validation_tools.sh
run_batch apply_batch_004_plan30_github_actions.sh
run_batch apply_batch_005_plan30_acceptance_matrix.sh
run_batch apply_batch_007_plan30_gate_fix.sh
run_batch apply_batch_008_plan30_multiline_gradle_gate_fix.sh
run_batch apply_batch_009_gradle_runner_fallback.sh
run_batch apply_batch_010_remote_actions_no_gradlew.sh

echo
scripts/run_cupertino_migration_gate.sh

echo
echo "All Plan 3.0 pre-acceptance patches applied."
echo "Next: git status; git add .; git commit; git push origin luna; check GitHub Actions."
echo "Do NOT run Batch 006 until local static gate, remote GitHub Actions, and runtime acceptance matrix pass."
