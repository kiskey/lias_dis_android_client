#!/usr/bin/env python3
from __future__ import annotations
import json, subprocess
from pathlib import Path

ROOT = Path.cwd()
HOME = ROOT / "app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt"
DEVICES = ROOT / "app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt"
SCHEDULE = ROOT / "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt"
HIG = ROOT / "app/src/main/java/com/lias/remote/ui/components/HigDatePicker.kt"
BUILD = ROOT / "app/build.gradle.kts"
OUT = ROOT / "build/plan31/final_cupertino_polish_audit.json"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace") if path.exists() else ""

home, devices, schedule, hig, build = map(read, [HOME, DEVICES, SCHEDULE, HIG, BUILD])
start = schedule.find("fun ScheduleDatePickerSheet(")
end = schedule.find("@Composable\nprivate fun FocusedPickerDialog", start)
date_block = schedule[start:end] if start >= 0 and end > start else ""

try:
    diff = subprocess.run(["git", "diff", "--name-only"], cwd=ROOT, text=True, capture_output=True, check=False)
    changed = [line.strip() for line in diff.stdout.splitlines() if line.strip()]
except Exception:
    changed = ["git diff unavailable"]

sensitive_prefixes = ("app/src/main/java/com/lias/remote/core/", "app/src/main/java/com/lias/remote/repositories/")
sensitive_changed = [p for p in changed if p.startswith(sensitive_prefixes)]

raw_picker_occurrences = []
for path in ROOT.joinpath("app/src/main/java").rglob("*.kt"):
    text = read(path)
    if "CupertinoDatePicker(" in text and path != HIG:
        raw_picker_occurrences.append(str(path.relative_to(ROOT)))

report = {
    "chevrons": {
        "home_uses_chevron_forward": "ChevronForward" in home,
        "devices_uses_chevron_forward": "ChevronForward" in devices,
        "home_no_text_disclosure": 'text = "›"' not in home,
        "devices_no_text_disclosure": 'text = "›"' not in devices,
        "no_material_icons_home": "androidx.compose.material.icons" not in home,
        "no_material_icons_devices": "androidx.compose.material.icons" not in devices,
    },
    "date_picker": {
        "hig_date_picker_exists": HIG.exists(),
        "hig_defaults_wheel": "mode: HigDatePickerMode = HigDatePickerMode.Wheel" in hig,
        "hig_owns_raw_cupertino_date_picker": "CupertinoDatePicker(" in hig,
        "schedule_uses_hig_date_picker": "HigDatePicker(" in date_block,
        "schedule_uses_wheel_mode": "HigDatePickerMode.Wheel" in date_block,
        "schedule_confirms_yyyy_mm_dd": ".toLocalDate().toString()" in date_block,
        "schedule_no_sequential_date_list": "buildList" not in date_block and "cursor.plusDays(1)" not in date_block and "TextWheel(" not in date_block,
        "raw_picker_only_in_hig": raw_picker_occurrences == [],
    },
    "dependencies": {
        "no_cupertino_adaptive_added": "cupertino-adaptive" not in build and "libs.cupertino.adaptive" not in build,
        "no_cupertino_native_added": "cupertino-native" not in build and "libs.cupertino.native" not in build,
    },
    "sensitive_changed_files": sensitive_changed,
    "raw_picker_occurrences_outside_hig": raw_picker_occurrences,
}
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

failures = []
for group, checks in report.items():
    if isinstance(checks, dict):
        for key, ok in checks.items():
            if isinstance(ok, bool) and not ok:
                failures.append(f"{group}.{key}")
if sensitive_changed:
    failures.append("sensitive_changed_files")
if raw_picker_occurrences:
    failures.append("raw_picker_occurrences_outside_hig")
if failures:
    raise SystemExit("ERROR: Final Plan 3.1 Cupertino polish audit failed: " + ", ".join(failures))
