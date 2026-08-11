#!/usr/bin/env bash
set -euo pipefail

scripts/verify_plan33_batch003_remote_exact.sh
scripts/verify_plan33_batch004.sh
python3 scripts/audit_plan33_modal_integration_remote_exact.py
python3 scripts/verify_plan33_alert_separation.py

python3 - <<'PY'
from pathlib import Path
import re, sys

ROOT = Path(".")
h = (ROOT / "app/src/main/java/com/lias/remote/ui/components/HigSheets.kt").read_text()
sp = (ROOT / "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt").read_text()
pw = (ROOT / "app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt").read_text()

def direct_dialog(t):
    return (
        "import androidx.compose.ui.window.Dialog" in t
        or re.search(r'(?<![A-Za-z0-9_])Dialog\s*\(', t) is not None
    )

deps = ""
for p in [ROOT / "gradle/libs.versions.toml", ROOT / "app/build.gradle.kts"]:
    if p.exists():
        deps += p.read_text(encoding="utf-8", errors="replace")

checks = {
    "slanos_scaffold": "CupertinoBottomSheetScaffold(" in h,
    "hidden_show": "CupertinoSheetValue.Hidden" in h and "sheetState.show()" in h,
    "animated_hide": "sheetState.hide()" in h,
    "completion_guard": "completionInFlight" in h,
    "completion_helper": "rememberHigAnimatedCompletion" in h,
    "back": "BackHandler(" in h,
    "outside": "dismissOnClickOutside" in h,
    "swipe": "sheetSwipeEnabled" in h,
    "nav_bar": "navigationBarsPadding()" in h,
    "ime": "imePadding()" in h,
    "accessibility": "paneTitle" in h,
    "picker_no_standalone_dialog": not direct_dialog(sp),
    "picker_hig_sheet":
        "HigModalSheetPortal(" in sp
        and "fun HigModalSheetPortal(" in h,
    "date_wire": ".toLocalDate()" in sp and ".toString()" in sp,
    "time_wire": '"%02d:%02d"' in sp,
    "policy_success_only": "onSave: suspend (Policy) -> Boolean" in pw,
    "no_adaptive": "cupertino-adaptive" not in deps,
    "no_native": "cupertino-native" not in deps,
}

bad = [k for k,v in checks.items() if not v]
for k,v in checks.items():
    print(f"{'PASS' if v else 'FAIL'}: {k}")

if bad:
    print("ERROR: final remote-exact gate failed:")
    for x in bad:
        print(" -", x)
    sys.exit(1)

print("PASS: Plan 3.3 remote-exact final static gate passed.")
PY
