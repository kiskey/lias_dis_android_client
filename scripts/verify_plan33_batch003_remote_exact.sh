#!/usr/bin/env bash
set -euo pipefail

python3 - <<'PY'
from pathlib import Path
import re, sys

ROOT = Path(".")
def txt(rel):
    p = ROOT / rel
    if not p.exists():
        print("ERROR: missing", rel)
        sys.exit(1)
    return p.read_text(encoding="utf-8")

checks = {}

h = txt("app/src/main/java/com/lias/remote/ui/components/HigSheets.kt")
checks["hig_batch002"] = "CupertinoBottomSheetScaffold(" in h and "sheetState.show()" in h and "sheetState.hide()" in h
checks["completion_helper"] = "fun rememberHigAnimatedCompletion(" in h
checks["completion_guard"] = "completionInFlight" in h and "!completionInFlight" in h

for rel in [
    "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt",
    "app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt",
]:
    checks["completion:" + rel] = "rememberHigAnimatedCompletion" in txt(rel)

s = txt("app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt")
checks["schedule_two_save_paths"] = len(re.findall(
    r'animatedComplete\s*\{\s*onSave\(\s*schedule\s*\)',
    s,
    re.MULTILINE,
)) == 2

p = txt("app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt")
r = txt("app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt")
checks["policy_suspend_success_contract"] = "onSave: suspend (Policy) -> Boolean" in p
checks["policy_animated_dismiss"] = "animatedDismiss()" in p
checks["rules_api_success"] = "ApiResult.Success" in r

picker = txt("app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt")
checks["picker_hig_modal"] = "HigModalSheet(" in picker
checks["picker_completion"] = "rememberHigAnimatedCompletion(" in picker
checks["picker_no_dialog_import"] = "import androidx.compose.ui.window.Dialog" not in picker
checks["picker_no_standalone_dialog"] = re.search(r'(?<![A-Za-z0-9_])Dialog\s*\(', picker) is None
checks["picker_no_animated_visibility"] = "AnimatedVisibility(" not in picker
checks["picker_time_contract"] = '"%02d:%02d"' in picker
checks["picker_date_contract"] = ".toLocalDate()" in picker and ".toString()" in picker

bad = [k for k,v in checks.items() if not v]
for k,v in checks.items():
    print(f"{'PASS' if v else 'FAIL'}: {k}")

if bad:
    print("ERROR: earlier Batch 003 state is incomplete:")
    for x in bad:
        print(" -", x)
    sys.exit(1)

print("PASS: Plan 3.3 Batch 003 remote-exact gate passed.")
PY
