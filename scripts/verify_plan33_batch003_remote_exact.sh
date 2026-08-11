#!/usr/bin/env bash
set -euo pipefail

python3 - <<'PY'
from pathlib import Path
import re
import sys

ROOT = Path(".")

def txt(rel):
    p = ROOT / rel
    if not p.exists():
        print("ERROR: missing", rel)
        sys.exit(1)
    return p.read_text(encoding="utf-8")

checks = {}
h = txt("app/src/main/java/com/lias/remote/ui/components/HigSheets.kt")

checks["hig_sheet_lifecycle"] = (
    "CupertinoBottomSheetScaffold(" in h
    and "sheetState.show()" in h
    and "sheetState.hide()" in h
)
checks["immediate_completion_helper"] = "fun rememberHigImmediateCompletion(" in h
checks["legacy_animated_completion_retained"] = "fun rememberHigAnimatedCompletion(" in h
checks["completion_guard"] = "completionInFlight" in h and "!completionInFlight" in h

immediate_expected = [
    "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt",
]
for rel in immediate_expected:
    checks["immediate_completion:" + rel] = "rememberHigImmediateCompletion" in txt(rel)

animated_expected = [
    "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt",
]
for rel in animated_expected:
    checks["legacy_animated_completion:" + rel] = "rememberHigAnimatedCompletion" in txt(rel)

s = txt("app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt")
checks["schedule_two_save_paths"] = len(re.findall(
    r'immediateComplete\s*\{\s*onSave\(\s*schedule\s*\)',
    s,
    re.MULTILINE,
)) == 2

p = txt("app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt")
r = txt("app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt")
checks["policy_suspend_success_contract"] = "onSave: suspend (Policy) -> Boolean" in p
checks["policy_server_progress"] = (
    "var isSaving by" in p
    and "CupertinoActivityIndicator(" in p
    and '"Saving…"' in p
    and "dismissEnabled =" in p
    and "!isSaving" in p
)
checks["policy_immediate_after_success"] = (
    "rememberHigImmediateCompletion(" in p
    and "immediateComplete {" in p
    and "rememberHigAnimatedDismiss" not in p
)
checks["rules_api_success"] = "ApiResult.Success" in r

picker = txt("app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt")
checks["picker_hig_modal"] = (
    "HigModalSheetPortal(" in picker
    and "fun HigModalSheetPortal(" in h
)
checks["picker_immediate_completion"] = "rememberHigImmediateCompletion(" in picker
checks["picker_no_dialog_import"] = "import androidx.compose.ui.window.Dialog" not in picker
checks["picker_no_standalone_dialog"] = re.search(
    r'(?<![A-Za-z0-9_])Dialog\s*\(',
    picker,
) is None
checks["picker_no_animated_visibility"] = "AnimatedVisibility(" not in picker
checks["picker_time_contract"] = '"%02d:%02d"' in picker
checks["picker_date_contract"] = ".toLocalDate()" in picker and ".toString()" in picker

bad = [k for k,v in checks.items() if not v]
for k,v in checks.items():
    print(f"{'PASS' if v else 'FAIL'}: {k}")

if bad:
    print("ERROR: Plan 3.3 Batch003 completion state is incomplete:")
    for x in bad:
        print(" -", x)
    sys.exit(1)

print("PASS: Plan 3.3 Batch003 mixed immediate/animated completion gate passed.")
PY
