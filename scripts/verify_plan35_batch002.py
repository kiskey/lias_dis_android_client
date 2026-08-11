#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(".")

def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")

def segment(rel, function_name):
    src = read(rel)
    start = src.index(f"fun {function_name}(")
    candidates = [
        x for x in (
            src.find("\n@Composable\n", start + 1),
            src.find("\n@OptIn(", start + 1),
        )
        if x != -1
    ]
    end = min(candidates) if candidates else len(src)
    return src[start:end]

expected = {
    (
        "app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt",
        "ScheduleEditorSheet",
    ): "Editor",
    (
        "app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt",
        "PolicyWizardSheet",
    ): "Editor",
    (
        "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt",
        "ExtendAccessSheet",
    ): "Editor",
    (
        "app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt",
        "PauseSheet",
    ): "Editor",
    (
        "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt",
        "OnboardingSheet",
    ): "Editor",
    (
        "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt",
        "SecurityAlertSheet",
    ): "Editor",
    (
        "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt",
        "FocusedPickerDialog",
    ): "Picker",
    (
        "app/src/main/java/com/lias/remote/ui/screens/identity/IdentityReviewScreen.kt",
        "IdentityCandidateSheet",
    ): "Editor",
    (
        "app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt",
        "TagEditorSheet",
    ): "Compact",
    (
        "app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt",
        "MoveTagSheet",
    ): "Compact",
    (
        "app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt",
        "UserAssignmentSheet",
    ): "Compact",
}

checks = {}

for (rel, fn), profile in expected.items():
    s = segment(rel, fn)
    key = f"{fn}:{profile}"
    checks[key] = (
        f"HigSheetPresentation.{profile}" in s
    )

picker = read(
    "app/src/main/java/com/lias/remote/ui/screens/schedules/"
    "SchedulePickerSheets.kt"
)
checks["picker_portal_retained"] = (
    "HigModalSheetPortal(" in picker
    and "HigModalSheet(" not in picker
)
checks["picker_time_wire"] = '"%02d:%02d"' in picker
checks["picker_date_wire"] = (
    ".toLocalDate()" in picker
    and ".toString()" in picker
)

for rel in [
    "app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt",
]:
    checks[f"compact_scroll:{rel}"] = ".verticalScroll(" in read(rel)

bad = [k for k,v in checks.items() if not v]
for k,v in checks.items():
    print(f"{'PASS' if v else 'FAIL'}: {k}")

if bad:
    print(
        "ERROR: Plan 3.5 Batch 002 classification failed: "
        + ", ".join(bad),
        file=sys.stderr,
    )
    sys.exit(1)

print("PASS: Plan 3.5 Batch 002 static gate passed.")
