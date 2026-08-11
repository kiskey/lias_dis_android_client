#!/usr/bin/env python3

from pathlib import Path
import json
import os
import re
import subprocess
import sys

ROOT = Path(".")

HIG = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/components/HigSheets.kt"
).read_text(encoding="utf-8")

PAUSE = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt"
).read_text(encoding="utf-8")

EXTEND = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt"
).read_text(encoding="utf-8")

PICKER = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens/schedules/"
    "SchedulePickerSheets.kt"
).read_text(encoding="utf-8")

DATE_ADAPTER = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/components/HigDatePicker.kt"
).read_text(encoding="utf-8")

LIBS = (
    ROOT /
    "gradle/libs.versions.toml"
).read_text(encoding="utf-8")


def function_region(source: str, marker: str) -> str:
    if source.count(marker) != 1:
        raise SystemExit(
            f"ERROR: expected exactly one function marker {marker!r}"
        )

    start = source.index(marker)

    candidates = [
        index
        for index in (
            source.find("\n@Composable\n", start + len(marker)),
            source.find("\n@OptIn(", start + len(marker)),
            source.find("\nprivate fun ", start + len(marker)),
            source.find("\nfun ", start + len(marker)),
        )
        if index != -1
    ]

    end = min(candidates) if candidates else len(source)

    return source[start:end]


modal = function_region(
    HIG,
    "fun HigModalSheet(",
)

portal = function_region(
    HIG,
    "fun HigModalSheetPortal(",
)

extend_fn = function_region(
    EXTEND,
    "fun ExtendAccessSheet(",
)

focused_picker = function_region(
    PICKER,
    "private fun FocusedPickerDialog(",
)

checks = {
    "cupertino_231":
        re.search(
            r'^cupertino\s*=\s*"2\.3\.1"\s*$',
            LIBS,
            flags=re.MULTILINE,
        ) is not None,

    # Shared adapter.
    "interaction_enum":
        "enum class HigSheetContentInteraction" in HIG,

    "resize_mapping":
        "PresentationContentInteraction.Resize" in HIG
        and "HigSheetContentInteraction.ResizeSheet" in HIG,

    "scroll_mapping":
        "PresentationContentInteraction.Scroll" in HIG
        and "HigSheetContentInteraction.ScrollContent" in HIG,

    "modal_has_default_policy":
        "HigSheetContentInteraction.ResizeSheet" in modal,

    "modal_forwards_policy":
        ".toCupertinoInteraction()" in modal,

    "portal_has_default_policy":
        "HigSheetContentInteraction.ResizeSheet" in portal,

    "portal_forwards_policy":
        re.search(
            r"contentInteraction\s*=\s*"
            r"contentInteraction",
            portal,
        ) is not None,

    "sheet_swipe_retained":
        re.search(
            r"sheetSwipeEnabled\s*=\s*true",
            HIG,
        ) is not None,

    # Pause.
    "pause_compact":
        "HigSheetPresentation.Compact" in PAUSE,

    "pause_fixed_60":
        re.search(
            r"onConfirm\s*\(\s*60\s*\)",
            PAUSE,
        ) is not None,

    "pause_button_retained":
        '"Pause for 1 Hour"' in PAUSE
        and "HigButtonStyle.Danger" in PAUSE,

    "pause_copy_concise":
        (
            '"Pauses internet for 1 hour. You can resume access early at any time."'
            in PAUSE
        ),

    "pause_disclaimer_removed":
        "Infrastructure devices cannot be paused." not in PAUSE,

    # Extend.
    "extend_compact":
        "HigSheetPresentation.Compact" in extend_fn,

    "extend_scroll_content":
        "HigSheetContentInteraction.ScrollContent" in extend_fn,

    "extend_wheel_retained":
        "CupertinoWheelPicker(" in extend_fn,

    "extend_apply_retained":
        "trailingAction" in extend_fn
        and '"Apply"' in extend_fn,

    "extend_cancel_retained":
        '"Cancel Extended Access"' in extend_fn
        and "isDestructive" in extend_fn,

    # Schedule date/time picker.
    "date_time_portal_retained":
        "HigModalSheetPortal(" in focused_picker,

    "date_time_picker_profile_retained":
        "HigSheetPresentation.Picker" in focused_picker,

    "date_time_scrollcontent_override":
        "HigSheetContentInteraction.ScrollContent" in focused_picker,

    "date_time_overflow_scroll_retained":
        ".verticalScroll(" in focused_picker,

    "time_wheel_retained":
        "LazyColumn(" in PICKER
        and "rememberSnapFlingBehavior(" in PICKER
        and ".height(WHEEL_HEIGHT)" in PICKER,

    "date_wheel_adapter_retained":
        "HigDatePickerMode.Wheel" in PICKER
        and "CupertinoDatePicker(" in DATE_ADAPTER
        and "DatePickerStyle.Wheel()" in DATE_ADAPTER,

    "time_wire_retained":
        '"%02d:%02d"' in PICKER,

    "date_wire_retained":
        ".toLocalDate()" in PICKER
        and ".toString()" in PICKER,
}

# Exactly the two approved screen-level users may opt into ScrollContent:
# Extend Access and the schedule picker portal caller.
screen_scrollcontent = []

for file in (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens"
).rglob("*.kt"):
    text = file.read_text(
        encoding="utf-8",
        errors="replace",
    )

    if "HigSheetContentInteraction.ScrollContent" in text:
        screen_scrollcontent.append(
            str(file.relative_to(ROOT))
        )

screen_scrollcontent.sort()

expected_scrollcontent = sorted(
    [
        "app/src/main/java/com/lias/remote/ui/screens/"
        "ExtendAccessSheet.kt",
        "app/src/main/java/com/lias/remote/ui/screens/schedules/"
        "SchedulePickerSheets.kt",
    ]
)

checks["approved_scrollcontent_users_only"] = (
    screen_scrollcontent ==
    expected_scrollcontent
)

# Domain/API scope guard.
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
    "screen_scrollcontent_users": screen_scrollcontent,
    "changed_files": changed,
    "forbidden_contract_domain_changes": forbidden,
}

out = (
    ROOT /
    "build/plan35/pause_extend_interaction.json"
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
        "ERROR: Plan 3.5 interaction gate failed:",
        file=sys.stderr,
    )

    for name in bad:
        print(
            f" - {name}",
            file=sys.stderr,
        )

    sys.exit(1)

print(
    "PASS: Plan 3.5 Pause/Extend/Schedule picker interaction gate passed."
)
