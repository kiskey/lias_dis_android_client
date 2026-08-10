#!/usr/bin/env bash
set -euo pipefail
python3 scripts/audit_plan31_hig_date_picker.py
echo "PASS: Plan 3.1 HigDatePicker static audit passed."
