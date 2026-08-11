#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path.cwd()
SRC = ROOT / "app/src/main/java"
OUT = ROOT / "build/plan32/lias2_contract_sync_audit.json"

files = list(SRC.rglob("*.kt"))
texts = {p: p.read_text(encoding="utf-8", errors="replace") for p in files}
combined = "\n".join(texts.values())

def paths(token):
    return [str(p.relative_to(ROOT)) for p,t in texts.items() if token in t]

checks = {
    "identity_review_screen": bool(paths("fun IdentityReviewScreen(")),
    "identity_detail_model": "IdentityCandidateDetail" in combined,
    "identity_decision_model": bool(paths("data class IdentityCandidateDecisionRequest")),
    "confirm_action": "confirmIdentityCandidate" in combined,
    "reject_action": "rejectIdentityCandidate" in combined,
    "reopen_action": "reopenIdentityCandidate" in combined,
    "pagination": "nextCursor" in combined,
    "decision_note": "decisionNote" in combined or "decision_note" in combined,
    "expected_source": "expectedSourcePdid" in combined or "expected_source_pdid" in combined,
    "expected_target": "expectedTargetPdid" in combined or "expected_target_pdid" in combined,
    "expected_updated": "expectedUpdatedAt" in combined or "expected_updated_at" in combined,
    "policy_validate": "POLICIES_VALIDATE" in combined,
    "conflict_model": "data class Conflict(" in combined,
    "mixed_timezone_hard_ux": "LIAS Cannot Merge Mixed Timezones" in combined,
    "stale_refresh_ux": "Plan 3.2 stale candidate refresh" in combined,
}

report = {
    "checks": checks,
    "decision_model_files": paths("data class IdentityCandidateDecisionRequest"),
    "identity_action_files": paths("fun EventRepository.confirmIdentityCandidate"),
    "policy_wizard_files": paths("fun PolicyWizardSheet("),
}
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

required_existing = [
    "identity_review_screen",
    "identity_detail_model",
    "identity_decision_model",
    "confirm_action",
    "reject_action",
    "reopen_action",
    "pagination",
    "decision_note",
    "policy_validate",
    "conflict_model",
]
missing = [k for k in required_existing if not checks[k]]
if missing:
    raise SystemExit("ERROR: existing Android LIAS 2.0 surface incomplete: " + ", ".join(missing))
