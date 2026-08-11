#!/usr/bin/env python3

from pathlib import Path
import re
import sys

ROOT = Path(".")

h = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/components/HigSheets.kt"
).read_text(encoding="utf-8")

picker = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens/schedules/"
    "SchedulePickerSheets.kt"
).read_text(encoding="utf-8")

editor = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens/schedules/"
    "ScheduleEditorSheet.kt"
).read_text(encoding="utf-8")

actions = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt"
).read_text(encoding="utf-8")

checks = {
    # Shared sheet geometry.
    "medium_large_detents":
        "PresentationDetent.Medium" in h
        and "PresentationDetent.Large" in h,

    "full_height_anchor_surface":
        "Modifier.fillMaxSize()" in h,

    "visible_detent_viewport":
        "sheetViewportFraction" in h
        and "0.5f" in h
        and "1.0f" in h
        and ".fillMaxHeight(" in h,

    "visible_nav_insets":
        ".navigationBarsPadding()" in h
        and ".imePadding()" in h,

    "sheet_completion_guard":
        "completionInFlight" in h,

    # Nested picker safety.
    "central_portal_exists":
        "fun HigModalSheetPortal(" in h,

    "portal_is_full_window":
        "DialogProperties(" in h
        and "usePlatformDefaultWidth =" in h
        and "decorFitsSystemWindows =" in h,

    "platform_dismiss_disabled":
        "dismissOnBackPress =" in h
        and "false" in h
        and "dismissOnClickOutside =" in h,

    "picker_uses_portal":
        "HigModalSheetPortal(" in picker,

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

    "schedule_editor_still_scrollable":
        ".verticalScroll(" in editor,

    "time_wire_unchanged":
        '"%02d:%02d"' in picker,

    "date_wire_unchanged":
        ".toLocalDate()" in picker
        and ".toString()" in picker,

    "time_wheel_bounded":
        ".height(WHEEL_HEIGHT)" in picker,

    # Global Policy readability.
    "global_action_sheet_retained":
        "CupertinoActionSheet(" in actions,

    "global_title_headline":
        '"Global Access",' in actions
        and "HigTypography.headline" in actions,

    "global_message_subheadline":
        '"Controls every non-infrastructure device on this LIAS server.",'
        in actions
        and "HigTypography.subheadline" in actions,

    "global_actions_retained":
        '"Save"' in actions
        and '"Cancel"' in actions
        and '"Apply Block All"' in actions,
}

bad = [
    name
    for name, ok in checks.items()
    if not ok
]

for name, ok in checks.items():
    print(
        f"{'PASS' if ok else 'FAIL'}: {name}"
    )

if bad:
    print(
        "ERROR: sheet regression stabilization gate failed:",
        ", ".join(bad),
        file=sys.stderr,
    )
    sys.exit(1)

print(
    "PASS: Plan 3.3 sheet regression stabilization gate passed."
)
