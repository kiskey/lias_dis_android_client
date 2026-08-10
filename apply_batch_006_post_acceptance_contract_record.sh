#!/usr/bin/env bash
set -euo pipefail

# =====================================================================
# Batch: 006
# Name: Post-acceptance contract baseline recorder
# Run from: repository root
#
# Scope:
#   Updates docs/lias_engine_contract.md only after Plan 3.0 has passed.
#   This script is intentionally guarded and requires an explicit flag.
# =====================================================================

if [[ "${1:-}" != "--accepted" ]]; then
  cat >&2 <<'EOF'
ERROR: Batch 006 is post-acceptance only.

Run this only after:
  - Batch 003 static/local gates pass;
  - Batch 004 GitHub Actions gate passes;
  - Batch 005 runtime acceptance matrix is reviewed and accepted.

Then run:
  ./apply_batch_006_post_acceptance_contract_record.sh --accepted
EOF
  exit 1
fi

if [[ ! -f "settings.gradle.kts" || ! -d "app/src/main" ]]; then
  echo "ERROR: run this script from the repository root." >&2
  exit 1
fi

if [[ ! -f "docs/lias_engine_contract.md" ]]; then
  echo "ERROR: docs/lias_engine_contract.md not found." >&2
  exit 1
fi

if grep -q 'Compose Cupertino Plan 3.0 accepted baseline' docs/lias_engine_contract.md; then
  echo "Batch 006 already appears to be applied."
  exit 0
fi

python3 - <<'PY'
from pathlib import Path
path = Path('docs/lias_engine_contract.md')
text = path.read_text()
needle = '## Current stable Android contract\n'
insert = '''\n## Compose Cupertino Plan 3.0 accepted baseline\n\nStatus: accepted dependency and namespace migration record  \nRecorded version: 30.0.0\n\nThe Android app has completed the Plan 3.0 maintained-fork migration with the following stable UI dependency baseline:\n\n| Area | Accepted baseline |\n| --- | --- |\n| AGP | 8.10.1 |\n| Kotlin / Compose compiler plugin | 2.2.0 |\n| Gradle wrapper | 8.11.1 |\n| Java/JVM | 17 |\n| Android SDKs | minSdk 26, compileSdk 36, targetSdk 35 |\n| Compose BOM | 2025.06.00 |\n| Cupertino artifacts | `io.github.schott12521:cupertino:2.3.1` and `io.github.schott12521:cupertino-icons-extended:2.3.1` |\n| Cupertino Kotlin package | `com.slapps.cupertino` |\n| kotlinx.serialization JSON | 1.7.3 |\n\nThis record documents the accepted dependency baseline only. It does not authorize or record any LIAS server API change, Android repository ownership change, REST/SSE behavior change, navigation route change, persisted setting change, PDID keying change, identity workflow change, policy/schedule semantic change, or user-visible behavior change. Plan 3.1 UX adoption remains separate and requires explicit approval.\n\n'''
if needle not in text:
    raise SystemExit('Could not find insertion point: ## Current stable Android contract')
text = text.replace(needle, insert + needle, 1)
path.write_text(text)
PY

echo "Batch 006 applied: post-acceptance baseline recorded in docs/lias_engine_contract.md."
