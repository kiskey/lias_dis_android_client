#!/usr/bin/env python3
from pathlib import Path
import sys

actions = Path(
    "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt"
).read_text(encoding="utf-8")

segmented = Path(
    "app/src/main/java/com/lias/remote/ui/components/SegmentedControl.kt"
).read_text(encoding="utf-8")

checks = {
    "global_action_sheet_retained":
        "CupertinoActionSheet(" in actions,
    "global_title_title3":
        '"Global Access",' in actions
        and "HigTypography.title3" in actions,
    "global_title_semibold":
        "FontWeight.SemiBold" in actions,
    "global_message_body":
        '"Controls every non-infrastructure device on this LIAS server.",'
        in actions
        and "HigTypography.body" in actions,
    "global_message_medium":
        "FontWeight.Medium" in actions,
    "native_save_retained":
        '"Save"' in actions
        and "default(" in actions,
    "native_cancel_retained":
        '"Cancel"' in actions
        and "cancel(" in actions,
    "native_destructive_retained":
        '"Apply Block All"' in actions
        and "destructive(" in actions,
    "segmented_subheadline":
        "HigTypography.subheadline" in segmented,
    "segmented_semibold":
        "FontWeight.SemiBold" in segmented,
    "segmented_min_height":
        ".heightIn(min = 48.dp)" in segmented,
    "segmented_slanoss_retained":
        "CupertinoSegmentedControl(" in segmented
        and "CupertinoSegmentedControlTab(" in segmented,
}

bad = [k for k,v in checks.items() if not v]

for k,v in checks.items():
    print(f"{'PASS' if v else 'FAIL'}: {k}")

if bad:
    print(
        "ERROR: Plan 3.5 Batch 003 failed: "
        + ", ".join(bad),
        file=sys.stderr,
    )
    sys.exit(1)

print("PASS: Plan 3.5 Batch 003 static gate passed.")
