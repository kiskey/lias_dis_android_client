#!/usr/bin/env bash
set -euo pipefail
python3 scripts/audit_plan31_devices_card_ux.py
echo "PASS: Plan 3.1 Devices card UX static audit passed."
