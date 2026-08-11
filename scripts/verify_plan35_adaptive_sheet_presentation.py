#!/usr/bin/env python3

from pathlib import Path
import json
import os
import re
import subprocess
import sys

ROOT = Path(".")

def read(rel):
    p = ROOT / rel
    if not p.exists():
        raise SystemExit(f"ERROR: missing {rel}")
    return p.read_text(encoding="utf-8")

def function_segment(rel, function_name):
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

hig = read(
    "app/src/main/java/com/lias/remote/ui/components/HigSheets.kt"
)
picker = read(
    "app/src/main/java/com/lias/remote/ui/screens/schedules/"
    "SchedulePickerSheets.kt"
)
actions = read(
    "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt"
)
segmented = read(
    "app/src/main/java/com/lias/remote/ui/components/SegmentedControl.kt"
)

checks = {
    # Shared profile architecture.
    "profile_enum":
        "enum class HigSheetPresentation" in hig,
    "compact_medium_large":
        "HigSheetPresentation.Compact" in hig
        and "PresentationDetent.Medium" in hig
        and "PresentationDetent.Large" in hig,
    "picker_fraction_large":
        "HigSheetPresentation.Picker" in hig
        and "PresentationDetent.Fraction(" in hig
        and "0.62f" in hig,
    "editor_large":
        "HigSheetPresentation.Editor" in hig
        and "PresentationDetent.Large" in hig,
    "semantic_detent_mapping":
        "presentation.detents()" in hig,
    "semantic_viewport_mapping":
        "partialViewportFraction" in hig,
    "bottom_safe_area":
        ".navigationBarsPadding()" in hig
        and ".imePadding()" in hig,
    "sheet_motion_retained":
        "sheetState.show()" in hig
        and "sheetState.hide()" in hig,
    "completion_guard_retained":
        "completionInFlight" in hig,
    "back_retained":
        "BackHandler(" in hig,
    "picker_portal_retained":
        "fun HigModalSheetPortal(" in hig
        and "DialogProperties(" in hig,

    # Picker crash regression.
    "picker_uses_portal":
        "HigModalSheetPortal(" in picker,
    "picker_profile":
        "HigSheetPresentation.Picker" in
        function_segment(
            "app/src/main/java/com/lias/remote/ui/screens/schedules/"
            "SchedulePickerSheets.kt",
            "FocusedPickerDialog",
        ),
    "picker_not_direct_nested_sheet":
        re.search(
            r'(?<![A-Za-z0-9_])HigModalSheet\s*\(',
            picker
        ) is None,
    "picker_not_direct_dialog":
        "import androidx.compose.ui.window.Dialog" not in picker
        and re.search(
            r'(?<![A-Za-z0-9_])Dialog\s*\(',
            picker
        ) is None,
    "time_wire_unchanged":
        '"%02d:%02d"' in picker,
    "date_wire_unchanged":
        ".toLocalDate()" in picker
        and ".toString()" in picker,
    "wheel_height_bounded":
        ".height(WHEEL_HEIGHT)" in picker,

    # Readability.
    "global_action_sheet_retained":
        "CupertinoActionSheet(" in actions,
    "global_title_20sp_role":
        '"Global Access",' in actions
        and "HigTypography.title3" in actions,
    "global_message_17sp_role":
        '"Controls every non-infrastructure device on this LIAS server.",'
        in actions
        and "HigTypography.body" in actions,
    "global_actions_native":
        '"Save"' in actions
        and '"Cancel"' in actions
        and '"Apply Block All"' in actions
        and "default(" in actions
        and "cancel(" in actions
        and "destructive(" in actions,
    "segmented_15sp_role":
        "HigTypography.subheadline" in segmented,
    "segmented_semibold":
        "FontWeight.SemiBold" in segmented,
    "segmented_touch_height":
        ".heightIn(min = 48.dp)" in segmented,
    "segmented_slanoss_retained":
        "CupertinoSegmentedControl(" in segmented
        and "CupertinoSegmentedControlTab(" in segmented,

    # Screens should not consume raw Slanoss detents.
    "no_raw_detents_in_screens":
        not any(
            "PresentationDetent." in p.read_text(
                encoding="utf-8",
                errors="replace",
            )
            for p in (
                ROOT / "app/src/main/java/com/lias/remote/ui/screens"
            ).rglob("*.kt")
        ),
}

# Explicit task classification.
classification = {
    (
        "app/src/main/java/com/lias/remote/ui/screens/schedules/"
        "ScheduleEditorSheet.kt",
        "ScheduleEditorSheet",
    ): "Editor",
    (
        "app/src/main/java/com/lias/remote/ui/screens/rules/"
        "PolicyWizardSheet.kt",
        "PolicyWizardSheet",
    ): "Editor",
    (
        "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt",
        "ExtendAccessSheet",
    ): 'Compact',
    (
        "app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt",
        "PauseSheet",
    ): 'Compact',
    (
        "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt",
        "OnboardingSheet",
    ): "Editor",
    (
        "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt",
        "SecurityAlertSheet",
    ): "Editor",
    (
        "app/src/main/java/com/lias/remote/ui/screens/devices/"
        "TagEditorSheet.kt",
        "TagEditorSheet",
    ): "Compact",
    (
        "app/src/main/java/com/lias/remote/ui/screens/devices/"
        "MoveTagSheet.kt",
        "MoveTagSheet",
    ): "Compact",
    (
        "app/src/main/java/com/lias/remote/ui/screens/devices/"
        "UserAssignmentSheet.kt",
        "UserAssignmentSheet",
    ): "Compact",
    (
        "app/src/main/java/com/lias/remote/ui/screens/identity/IdentityReviewScreen.kt",
        "IdentityCandidateSheet",
    ): "Editor",
}

for (rel, fn), profile in classification.items():
    checks[f"classification:{fn}:{profile}"] = (
        f"HigSheetPresentation.{profile}"
        in function_segment(rel, fn)
    )

# Compact profiles must have scrollable content because lower body content
# may extend beyond Medium; their primary controls are in the header.
for rel in [
    "app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt",
]:
    checks[f"compact_scrollable:{rel}"] = ".verticalScroll(" in read(rel)

# Exact caller inventory: no new/unclassified sheet surface may bypass
# Plan 3.5 semantic presentation without first being audited.
screens = ROOT / "app/src/main/java/com/lias/remote/ui/screens"
actual_sheet_files = set()

for p in screens.rglob("*.kt"):
    t = p.read_text(encoding="utf-8", errors="replace")
    if (
        re.search(
            r'(?<![A-Za-z0-9_])HigModalSheet\s*\(',
            t,
        )
        or re.search(
            r'(?<![A-Za-z0-9_])HigModalSheetPortal\s*\(',
            t,
        )
    ):
        actual_sheet_files.add(
            str(p.relative_to(ROOT))
        )

expected_sheet_files = {
    "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt",
    "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt",
    "app/src/main/java/com/lias/remote/ui/screens/identity/IdentityReviewScreen.kt",
}

checks["sheet_caller_inventory_exact"] = (
    actual_sheet_files ==
    expected_sheet_files
)

unclassified = sorted(
    actual_sheet_files -
    expected_sheet_files
)

missing_sheet_files = sorted(
    expected_sheet_files -
    actual_sheet_files
)

# API/domain boundary.
if os.environ.get("GITHUB_ACTIONS") == "true":
    changed = subprocess.check_output(
        [
            "git",
            "diff",
            "--name-only",
            "HEAD^",
            "HEAD",
        ],
        text=True,
    ).splitlines()
else:
    changed = subprocess.check_output(
        [
            "git",
            "diff",
            "--name-only",
            "origin/luna",
        ],
        text=True,
    ).splitlines()

forbidden_prefixes = (
    "app/src/main/java/com/lias/remote/core/network/",
    "app/src/main/java/com/lias/remote/core/models/",
    "app/src/main/java/com/lias/remote/core/policy/",
    "app/src/main/java/com/lias/remote/core/schedule/",
    "app/src/main/java/com/lias/remote/repositories/",
)

forbidden = [
    p for p in changed
    if p.startswith(forbidden_prefixes)
]
checks["no_contract_domain_changes"] = not forbidden

report = {
    "checks": checks,
    "unclassified_sheet_files": unclassified,
    "missing_expected_sheet_files": missing_sheet_files,
    "changed_files": changed,
    "forbidden_contract_domain_changes": forbidden,
}

out = ROOT / "build/plan35/final_audit.json"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(
    json.dumps(report, indent=2),
    encoding="utf-8",
)
print(json.dumps(report, indent=2))

bad = [k for k,v in checks.items() if not v]
if bad:
    print("ERROR: Plan 3.5 final static gate failed:")
    for item in bad:
        print(" -", item)
    sys.exit(1)

print(
    "PASS: Plan 3.5 Adaptive Cupertino Sheet Presentation "
    "& Readability static gate passed."
)
