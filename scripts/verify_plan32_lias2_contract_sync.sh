#!/usr/bin/env bash
set -euo pipefail

python3 scripts/audit_plan32_lias2_contract_sync.py

python3 - <<'PY'
from pathlib import Path
import sys
model = Path("app/src/main/java/com/lias/remote/core/network/EngineApiTypes.kt")
identity = Path("app/src/main/java/com/lias/remote/repositories/IdentityRepositoryActions.kt")
wizard = Path("app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt")
presentation = Path("app/src/main/java/com/lias/remote/core/policy/PolicyPresentation.kt")
m = model.read_text(encoding="utf-8")
i = identity.read_text(encoding="utf-8")
w = wizard.read_text(encoding="utf-8")
p = presentation.read_text(encoding="utf-8")
checks = {
    "expected_source_model": '@SerialName("expected_source_pdid")' in m,
    "expected_target_model": '@SerialName("expected_target_pdid")' in m,
    "expected_updated_model": '@SerialName("expected_updated_at")' in m,
    "decision_note_model": '@SerialName("decision_note")' in m,
    "decision_helper": "private fun IdentityCandidateDetail.decisionRequest(" in i,
    "helper_source": "expectedSourcePdid = sourcePdid" in i,
    "helper_target": "expectedTargetPdid = targetPdid" in i,
    "helper_updated": "expectedUpdatedAt = updatedAt" in i,
    "helper_note": "decisionNote = note.trim().take(1024)" in i,
    "stale_refresh": i.count("Plan 3.2 stale candidate refresh") >= 3,
    "review_again": i.count("review it again") >= 3,
    "mixed_timezone": "LIAS Cannot Merge Mixed Timezones" in w,
    "calendar_conflict": "weekly and calendar-date conflicts" in w,
    "fail_closed": "fails closed to Block" in w,
    "validation_blocks_save": "validationError != null" in w,
    "validation_disables_save": "validationError == null" in w,
    "humanizer": "fun serverValidationMessage(" in p,
}
bad = [k for k,v in checks.items() if not v]
if bad:
    print("ERROR: Plan 3.2 final gate failed:")
    for x in bad:
        print(" -", x)
    sys.exit(1)

all_text = "\n".join(p.read_text(encoding="utf-8", errors="replace") for p in Path("app/src/main/java").rglob("*.kt"))
forbidden = [
    "resolvePolicyConflictLocally",
    "evaluateScheduleBundleLocally",
    "chooseWinningScheduleConflict",
]
bad2 = [x for x in forbidden if x in all_text]
if bad2:
    print("ERROR: Android-side authority violation:", bad2)
    sys.exit(1)
print("PASS: Plan 3.2 LIAS 2.0 contract synchronization static gate passed.")
PY
