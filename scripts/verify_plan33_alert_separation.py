#!/usr/bin/env python3
from pathlib import Path
import json, sys

ROOT = Path.cwd()
ALERT = ROOT / "app/src/main/java/com/lias/remote/ui/components/HigAlertDialog.kt"
IDENTITY = ROOT / "app/src/main/java/com/lias/remote/ui/screens/identity/IdentityReviewScreen.kt"
RULES = ROOT / "app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt"
OUT = ROOT / "build/plan33/alert_separation_audit.json"

for p in [ALERT, IDENTITY, RULES]:
    if not p.exists():
        raise SystemExit(f"ERROR: missing {p.relative_to(ROOT)}")

a = ALERT.read_text(encoding="utf-8")
i = IDENTITY.read_text(encoding="utf-8")
r = RULES.read_text(encoding="utf-8")

checks = {
    "alert_uses_compose_dialog": "import androidx.compose.ui.window.Dialog" in a and "Dialog(" in a,
    "alert_uses_cupertino_controls": "CupertinoButton(" in a and "CupertinoText(" in a,
    "alert_not_bottom_sheet": "HigModalSheet" not in a and "CupertinoBottomSheetScaffold" not in a,
    "alert_not_material3_alert":
        "import androidx.compose.material3.AlertDialog" not in a,
    "identity_decisions_use_hig_alert": "HigAlertDialog(" in i,
    "rule_delete_uses_hig_alert": "HigAlertDialog(" in r,
}

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps({"checks": checks}, indent=2), encoding="utf-8")
print(json.dumps({"checks": checks}, indent=2))

bad = [k for k,v in checks.items() if not v]
if bad:
    print("ERROR: alert separation gate failed:")
    for x in bad:
        print(" -", x)
    sys.exit(1)

print("PASS: Plan 3.3 Batch 006 alert separation audit passed.")
