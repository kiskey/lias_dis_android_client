#!/usr/bin/env python3

from pathlib import Path
import ast
import json
import os
import re
import subprocess
import sys

ROOT = Path(".")
PAUSE_REL = "app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt"
EXTEND_REL = "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt"

pause = (ROOT / PAUSE_REL).read_text(encoding="utf-8")
extend = (ROOT / EXTEND_REL).read_text(encoding="utf-8")


def classification_value(rel: str, assignment_name: str, key: tuple[str, str]):
    source = (ROOT / rel).read_text(encoding="utf-8")
    tree = ast.parse(source)

    for node in tree.body:
        if not isinstance(node, ast.Assign):
            continue
        if not any(
            isinstance(target, ast.Name) and target.id == assignment_name
            for target in node.targets
        ):
            continue
        if not isinstance(node.value, ast.Dict):
            return None

        for key_node, value_node in zip(node.value.keys, node.value.values):
            if not (
                isinstance(key_node, ast.Tuple)
                and len(key_node.elts) == 2
                and all(
                    isinstance(item, ast.Constant)
                    and isinstance(item.value, str)
                    for item in key_node.elts
                )
                and isinstance(value_node, ast.Constant)
                and isinstance(value_node.value, str)
            ):
                continue

            current_key = (
                key_node.elts[0].value,
                key_node.elts[1].value,
            )
            if current_key == key:
                return value_node.value
    return None


checks = {
    "pause_compact": "HigSheetPresentation.Compact" in pause,
    "pause_fixed_60": re.search(r"onConfirm\s*\(\s*60\s*\)", pause) is not None,
    "pause_danger_action": '"Pause for 1 Hour"' in pause and "HigButtonStyle.Danger" in pause,
    "pause_completion_ordering": "rememberHigAnimatedCompletion" in pause,

    "extend_compact": "HigSheetPresentation.Compact" in extend,
    "extend_wheel": "CupertinoWheelPicker(" in extend,
    "extend_picker_state": "rememberCupertinoPickerState(" in extend,
    "extend_finite_picker": re.search(r"infinite\s*=\s*false", extend) is not None,
    "extend_min_5": re.search(r"EXTEND_MIN_MINUTES\s*=\s*5", extend) is not None,
    "extend_max_120": re.search(r"EXTEND_MAX_MINUTES\s*=\s*120", extend) is not None,
    "extend_step_5": re.search(r"EXTEND_STEP_MINUTES\s*=\s*5", extend) is not None,
    "extend_default_30": re.search(r"EXTEND_DEFAULT_MINUTES\s*=\s*30", extend) is not None,
    "extend_five_row_height": re.search(r"EXTEND_PICKER_HEIGHT\s*=\s*160\.dp", extend) is not None,
    "extend_apply_in_header": "HigSheetHeader(" in extend and "trailingAction" in extend and '"Apply"' in extend,
    "extend_no_slider": "CupertinoSlider" not in extend and "mutableFloatStateOf" not in extend,
    "extend_no_quick_picks": "quickPicks" not in extend,
    "extend_no_bottom_primary": "HigButton(" not in extend and "HigButtonStyle" not in extend,
    "extend_identity_filter_retained": "TemporaryAccessKind.EXTEND" in extend,
    "extend_active_status_retained": "rememberTemporaryMinutesLeft(" in extend and "remaining" in extend,
    "extend_cancel_retained": '"Cancel Extended Access"' in extend and "isDestructive" in extend,
    "extend_completion_ordering": "rememberHigAnimatedCompletion" in extend,
    "extend_confirm_selected_minutes": re.search(r"onConfirm\s*\(\s*selectedMinutes\s*\)", extend) is not None,
}

pause_key = (PAUSE_REL, "PauseSheet")
extend_key = (EXTEND_REL, "ExtendAccessSheet")

checks["batch002_pause_compact"] = classification_value(
    "scripts/verify_plan35_batch002.py", "expected", pause_key
) == "Compact"
checks["batch002_extend_compact"] = classification_value(
    "scripts/verify_plan35_batch002.py", "expected", extend_key
) == "Compact"
checks["final_pause_compact"] = classification_value(
    "scripts/verify_plan35_adaptive_sheet_presentation.py", "classification", pause_key
) == "Compact"
checks["final_extend_compact"] = classification_value(
    "scripts/verify_plan35_adaptive_sheet_presentation.py", "classification", extend_key
) == "Compact"

if os.environ.get("GITHUB_ACTIONS") == "true":
    changed = subprocess.check_output(
        ["git", "diff", "--name-only", "HEAD^", "HEAD"], text=True
    ).splitlines()
else:
    changed = subprocess.check_output(
        ["git", "diff", "--name-only", "origin/luna"], text=True
    ).splitlines()

forbidden_prefixes = (
    "app/src/main/java/com/lias/remote/core/network/",
    "app/src/main/java/com/lias/remote/core/models/",
    "app/src/main/java/com/lias/remote/core/policy/",
    "app/src/main/java/com/lias/remote/core/schedule/",
    "app/src/main/java/com/lias/remote/repositories/",
)
forbidden = [p for p in changed if p.startswith(forbidden_prefixes)]
checks["no_contract_domain_changes"] = not forbidden

report = {
    "checks": checks,
    "changed_files": changed,
    "forbidden_contract_domain_changes": forbidden,
}

out = ROOT / "build/plan35/pause_extend_refinement.json"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

bad = [name for name, ok in checks.items() if not ok]
if bad:
    print("ERROR: Plan 3.5 Pause/Extend refinement gate failed:", file=sys.stderr)
    for name in bad:
        print(f" - {name}", file=sys.stderr)
    sys.exit(1)

print("PASS: Plan 3.5 Compact Pause + Minimal Extend Access gate passed.")
