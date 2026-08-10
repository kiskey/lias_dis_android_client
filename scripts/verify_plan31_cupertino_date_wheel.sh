#!/usr/bin/env bash
set -euo pipefail
python3 scripts/audit_plan31_cupertino_date_wheel.py
echo "PASS: Plan 3.1 Cupertino date wheel static audit passed."
