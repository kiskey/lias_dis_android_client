#!/usr/bin/env python3
from __future__ import annotations

import json
import subprocess
from pathlib import Path

ROOT = Path.cwd()
APP = ROOT / "app" / "src" / "main" / "java"
SCHEDULE = APP / "com" / "lias" / "remote" / "ui" / "screens" / "schedules" / "SchedulePickerSheets.kt"
BUILD = ROOT / "app" / "build.gradle.kts"
OUT = ROOT / "build" / "plan31" / "cupertino_date_wheel_audit.json"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace") if path.exists() else ""

def rel(path: Path) -> str:
    return str(path.relative_to(ROOT))

text = read(SCHEDULE)
build = read(BUILD)
start = text.find("fun ScheduleDatePickerSheet(")
end = text.find("@Composable\nprivate fun FocusedPickerDialog", start)
date_block = text[start:end] if start >= 0 and end > start else ""

try:
    diff = subprocess.run(["git", "diff", "--name-only"], cwd=ROOT, text=True, capture_output=True, check=False)
    changed = [line.strip() for line in diff.stdout.splitlines() if line.strip()]
except Exception:
    changed = ["git diff unavailable"]

sensitive_prefixes = (
    "app/src/main/java/com/lias/remote/core/",
    "app/src/main/java/com/lias/remote/repositories/",
)
sensitive_changed = [p for p in changed if p.startswith(sensitive_prefixes)]

report = {
    "files": {"SchedulePickerSheets.kt": rel(SCHEDULE) if SCHEDULE.exists() else None},
    "checks": {
        "has_plan_marker": "Plan 3.1 Cupertino date wheel" in text,
        "imports_cupertino_date_picker": "import com.slapps.cupertino.CupertinoDatePicker" in text,
        "imports_date_picker_style": "import com.slapps.cupertino.DatePickerStyle" in text,
        "imports_remember_state": "import com.slapps.cupertino.rememberCupertinoDatePickerState" in text,
        "imports_experimental_api": "import com.slapps.cupertino.ExperimentalCupertinoApi" in text,
        "date_sheet_uses_cupertino_date_picker": "CupertinoDatePicker(" in date_block,
        "date_sheet_uses_wheel_style": "DatePickerStyle.Wheel()" in date_block,
        "date_sheet_uses_state": "rememberCupertinoDatePickerState(" in date_block,
        "date_sheet_no_sequential_dates_list": "buildList" not in date_block and "cursor.plusDays(1)" not in date_block and "TextWheel(" not in date_block,
        "date_sheet_confirms_yyyy_mm_dd": ".toLocalDate().toString()" in date_block,
        "time_sheet_not_replaced_with_datetime_picker": "CupertinoDateTimePicker" not in text,
        "no_cupertino_adaptive_dependency_added": "libs.cupertino.adaptive" not in build and "cupertino-adaptive" not in build,
        "no_cupertino_native_dependency_added": "libs.cupertino.native" not in build and "cupertino-native" not in build,
    },
    "sensitive_changed_files": sensitive_changed,
}

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

failures = [key for key, ok in report["checks"].items() if not ok]
if report["sensitive_changed_files"]:
    failures.append("sensitive_changed_files")
if failures:
    raise SystemExit("ERROR: Plan 3.1 Cupertino date wheel audit failed: " + ", ".join(failures))
