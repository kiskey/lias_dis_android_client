#!/usr/bin/env python3
from pathlib import Path
import json
import re
import sys

ROOT = Path.cwd()
SRC = ROOT / "app/src/main/java"
OUT = ROOT / "build/plan33/modal_integration_audit_remote_exact.json"

files = list(SRC.rglob("*.kt"))
texts = {
    p: p.read_text(encoding="utf-8", errors="replace")
    for p in files
}

def direct_dialog(t):
    return (
        "import androidx.compose.ui.window.Dialog" in t
        or re.search(r'(?<![A-Za-z0-9_])Dialog\s*\(', t) is not None
    )

picker = ROOT / "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt"
hig = ROOT / "app/src/main/java/com/lias/remote/ui/components/HigSheets.kt"

immediate_expected = sorted([
    "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt",
])

legacy_expected = sorted([
    "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt",
    "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt",
])

immediate_import = (
    "import com.lias.remote.ui.components."
    "rememberHigImmediateCompletion"
)
legacy_import = (
    "import com.lias.remote.ui.components."
    "rememberHigAnimatedCompletion"
)

screen_root = ROOT / "app/src/main/java/com/lias/remote/ui/screens"
actual_immediate = []
actual_legacy = []

for file in screen_root.rglob("*.kt"):
    t = texts[file]
    rel = str(file.relative_to(ROOT))
    if immediate_import in t:
        actual_immediate.append(rel)
    if legacy_import in t:
        actual_legacy.append(rel)

actual_immediate.sort()
actual_legacy.sort()

checks = {
    "hig_slanoss": "CupertinoBottomSheetScaffold(" in texts[hig],
    "hig_show_hide": "sheetState.show()" in texts[hig] and "sheetState.hide()" in texts[hig],
    "completion_guard": "completionInFlight" in texts[hig],
    "immediate_completion_helper": "fun rememberHigImmediateCompletion(" in texts[hig],
    "legacy_animated_helper_retained": "fun rememberHigAnimatedCompletion(" in texts[hig],
    "exact_immediate_inventory": actual_immediate == immediate_expected,
    "exact_legacy_inventory": actual_legacy == legacy_expected,
    "picker_no_standalone_dialog": not direct_dialog(texts[picker]),
    "picker_hig_sheet": (
        "HigModalSheetPortal(" in texts[picker]
        and "fun HigModalSheetPortal(" in texts[hig]
    ),
    "picker_full_window_portal": (
        "DialogProperties(" in texts[hig]
        and "usePlatformDefaultWidth =" in texts[hig]
        and "decorFitsSystemWindows =" in texts[hig]
    ),
    "no_material_modal_bottom_sheet": not any(
        "androidx.compose.material3.ModalBottomSheet" in t
        or re.search(r'(?<![A-Za-z0-9_])ModalBottomSheet\s*\(', t)
        for t in texts.values()
    ),
}

deps = ""
for p in [ROOT / "gradle/libs.versions.toml", ROOT / "app/build.gradle.kts"]:
    if p.exists():
        deps += p.read_text(encoding="utf-8", errors="replace")
checks["no_cupertino_adaptive"] = "cupertino-adaptive" not in deps
checks["no_cupertino_native"] = "cupertino-native" not in deps

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(
    json.dumps({
        "checks": checks,
        "actual_immediate": actual_immediate,
        "expected_immediate": immediate_expected,
        "actual_legacy": actual_legacy,
        "expected_legacy": legacy_expected,
    }, indent=2),
    encoding="utf-8",
)
print(OUT.read_text())

bad = [k for k,v in checks.items() if not v]
if bad:
    print("ERROR: modal integration completion audit failed:")
    for x in bad:
        print(" -", x)
    sys.exit(1)

print("PASS: modal integration mixed-completion audit passed.")
