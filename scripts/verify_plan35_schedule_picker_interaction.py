#!/usr/bin/env python3

from pathlib import Path
import json
import os
import re
import subprocess
import sys

ROOT = Path(".")

HIG_PATH = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/components/HigSheets.kt"
)

PICKER_PATH = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens/schedules/"
    "SchedulePickerSheets.kt"
)

DATE_PATH = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/components/HigDatePicker.kt"
)

HIG = HIG_PATH.read_text(encoding="utf-8")
PICKER = PICKER_PATH.read_text(encoding="utf-8")
DATE = DATE_PATH.read_text(encoding="utf-8")


def region(source: str, marker: str) -> str:
    if source.count(marker) != 1:
        raise SystemExit(
            f"ERROR: marker {marker!r} is not unique"
        )

    start = source.index(marker)

    candidates = [
        p for p in (
            source.find("\n@Composable\n", start + len(marker)),
            source.find("\n@OptIn(", start + len(marker)),
            source.find("\nprivate fun ", start + len(marker)),
            source.find("\nfun ", start + len(marker)),
        )
        if p != -1
    ]

    end = min(candidates) if candidates else len(source)

    return source[start:end]


portal = region(
    HIG,
    "fun HigModalSheetPortal(",
)

modal = region(
    HIG,
    "fun HigModalSheet(",
)

focused = region(
    PICKER,
    "private fun FocusedPickerDialog(",
)

time_sheet = region(
    PICKER,
    "fun ScheduleTimePickerSheet(",
)

date_sheet = region(
    PICKER,
    "fun ScheduleDatePickerSheet(",
)

text_wheel = region(
    PICKER,
    "private fun TextWheel(",
)

checks = {
    # Portal boundary is preserved and now forwards interaction policy.
    "portal_full_window_dialog":
        "Dialog(" in portal
        and "DialogProperties(" in portal
        and "usePlatformDefaultWidth =" in portal
        and "false" in portal,

    "portal_default_resize":
        "HigSheetContentInteraction.ResizeSheet" in portal,

    "portal_forwards_content_interaction":
        re.search(
            r"contentInteraction\s*=\s*contentInteraction",
            portal,
        ) is not None,

    # Focused picker opts into wheel-first nested scrolling.
    "focused_uses_portal":
        "HigModalSheetPortal(" in focused,

    "focused_picker_profile":
        "HigSheetPresentation.Picker" in focused,

    "focused_scroll_content":
        "HigSheetContentInteraction.ScrollContent" in focused,

    "focused_overflow_scroll_retained":
        ".verticalScroll(" in focused
        and "rememberScrollState()" in focused,

    "focused_height_guard_retained":
        ".heightIn(" in focused
        and "maxPickerHeight" in focused,

    # Direct sheet gesture remains possible. This is independent of
    # nested-scroll detent promotion.
    "sheet_swipe_retained":
        re.search(
            r"sheetSwipeEnabled\s*=\s*true",
            HIG,
        ) is not None,

    # Time picker implementation unchanged in architecture.
    "time_two_number_wheels":
        time_sheet.count("NumberWheel(") == 2,

    "time_snap_wheel":
        "LazyColumn(" in text_wheel
        and "rememberSnapFlingBehavior(" in text_wheel,

    "time_wheel_bounded":
        ".height(WHEEL_HEIGHT)" in text_wheel
        and "WHEEL_VISIBLE_ROWS = 5" in PICKER
        and "WHEEL_ROW_HEIGHT = 44.dp" in PICKER,

    "time_wire_unchanged":
        '"%02d:%02d"' in time_sheet,

    # Date picker remains the LIAS adapter over Slanoss Wheel mode.
    "date_uses_hig_adapter":
        "HigDatePicker(" in date_sheet
        and "HigDatePickerMode.Wheel" in date_sheet,

    "date_adapter_is_slanoss_wheel":
        "CupertinoDatePicker(" in DATE
        and "DatePickerStyle.Wheel()" in DATE,

    "date_wire_unchanged":
        ".toLocalDate()" in date_sheet
        and ".toString()" in date_sheet,

    # No direct nested sheet/dialog regression in the picker screen.
    "no_direct_hig_modal_sheet":
        re.search(
            r'(?<![A-Za-z0-9_])HigModalSheet\s*\(',
            PICKER,
        ) is None,

    "no_direct_platform_dialog":
        "import androidx.compose.ui.window.Dialog" not in PICKER
        and re.search(
            r'(?<![A-Za-z0-9_])Dialog\s*\(',
            PICKER,
        ) is None,
}

# Ensure no raw Slanoss PresentationContentInteraction leaks into screens.
checks["screen_uses_lias_adapter_only"] = (
    "PresentationContentInteraction" not in PICKER
)

# Domain/API guard.
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
    path
    for path in changed
    if path.startswith(forbidden_prefixes)
]

checks["no_contract_domain_changes"] = not forbidden

report = {
    "checks": checks,
    "changed_files": changed,
    "forbidden_contract_domain_changes": forbidden,
}

out = (
    ROOT /
    "build/plan35/schedule_picker_interaction.json"
)

out.parent.mkdir(
    parents=True,
    exist_ok=True,
)

out.write_text(
    json.dumps(
        report,
        indent=2,
    ),
    encoding="utf-8",
)

print(
    json.dumps(
        report,
        indent=2,
    )
)

bad = [
    name
    for name, ok in checks.items()
    if not ok
]

if bad:
    print(
        "ERROR: schedule picker interaction gate failed:",
        file=sys.stderr,
    )
    for name in bad:
        print(
            f" - {name}",
            file=sys.stderr,
        )
    sys.exit(1)

print(
    "PASS: schedule date/time picker wheel-scroll isolation gate passed."
)
