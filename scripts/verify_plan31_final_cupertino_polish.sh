#!/usr/bin/env bash
set -euo pipefail
python3 scripts/audit_plan31_final_cupertino_polish.py
if [[ -x scripts/verify_plan31_chevron_icons.sh ]]; then scripts/verify_plan31_chevron_icons.sh; fi
if [[ -x scripts/verify_plan31_hig_date_picker.sh ]]; then scripts/verify_plan31_hig_date_picker.sh; fi
echo "PASS: Final Plan 3.1 Cupertino polish static gate passed."
