#!/usr/bin/env python3

from pathlib import Path
import sys

p = Path(
    "app/src/main/java/com/lias/remote/ui/components/HigSheets.kt"
)

text = p.read_text(
    encoding="utf-8"
)

checks = {
    "cupertino_scaffold":
        "CupertinoBottomSheetScaffold(" in text,

    "medium_initial_detent_available":
        "PresentationDetent.Medium" in text,

    "large_expansion_available":
        "PresentationDetent.Large" in text,

    "medium_before_large":
        text.find("PresentationDetent.Medium")
        < text.find("PresentationDetent.Large"),

    "full_height_sheet_surface":
        "Modifier.fillMaxSize()" in text,

    "detent_safe_visible_viewport":
        "sheetViewportFraction" in text
        and ".fillMaxHeight(" in text,

    "nested_sheet_full_window_portal":
        "fun HigModalSheetPortal(" in text
        and "DialogProperties(" in text,

    "explicit_cupertino_grabber":
        "CupertinoBottomSheetDefaults" in text
        and ".DragHandle()" in text,

    "hidden_initial_state":
        "CupertinoSheetValue.Hidden" in text,

    "animated_show":
        "sheetState.show()" in text,

    "animated_hide":
        "sheetState.hide()" in text,

    "swipe_down":
        "sheetSwipeEnabled" in text,

    "outside_tap":
        "dismissOnClickOutside" in text,

    "android_back":
        "BackHandler(" in text,

    "completion_race_guard":
        "completionInFlight" in text,

    "nav_bar_insets":
        "navigationBarsPadding()" in text,

    "ime_insets":
        "imePadding()" in text,

    "no_scaffold_scaling":
        "applyContentScaling =\n                false" in text,
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
        "ERROR: bottom-attached sheet geometry gate failed:",
        ", ".join(bad),
        file=sys.stderr,
    )
    sys.exit(1)

print(
    "PASS: Plan 3.3 bottom-attached Cupertino sheet "
    "geometry gate passed."
)
