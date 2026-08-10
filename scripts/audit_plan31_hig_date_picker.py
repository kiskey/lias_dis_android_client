#!/usr/bin/env python3
from __future__ import annotations
import json, subprocess
from pathlib import Path

ROOT = Path.cwd()
COMP = ROOT / "app/src/main/java/com/lias/remote/ui/components/HigDatePicker.kt"
BUILD = ROOT / "app/build.gradle.kts"
OUT = ROOT / "build/plan31/hig_date_picker_audit.json"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace") if path.exists() else ""

text = read(COMP)
build = read(BUILD)
try:
    diff = subprocess.run(["git", "diff", "--name-only"], cwd=ROOT, text=True, capture_output=True, check=False)
    changed = [line.strip() for line in diff.stdout.splitlines() if line.strip()]
except Exception:
    changed = ["git diff unavailable"]

sensitive_prefixes = ("app/src/main/java/com/lias/remote/core/", "app/src/main/java/com/lias/remote/repositories/")
sensitive_changed = [p for p in changed if p.startswith(sensitive_prefixes)]

report = {
    "checks": {
        "wrapper_exists": COMP.exists(),
        "imports_cupertino_date_picker": "import com.slapps.cupertino.CupertinoDatePicker" in text,
        "supports_wheel": "HigDatePickerMode.Wheel" in text and "DatePickerStyle.Wheel()" in text,
        "supports_pager": "HigDatePickerMode.Pager" in text and "DatePickerStyle.Pager()" in text,
        "wheel_default": "mode: HigDatePickerMode = HigDatePickerMode.Wheel" in text,
        "uses_remember_state": "rememberCupertinoDatePickerState(" in text,
        "has_on_date_selected": "onDateSelected: (Long) -> Unit" in text,
        "no_adaptive_dependency_added": "cupertino-adaptive" not in build and "libs.cupertino.adaptive" not in build,
        "no_native_dependency_added": "cupertino-native" not in build and "libs.cupertino.native" not in build,
    },
    "sensitive_changed_files": sensitive_changed,
}
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

failures = [k for k, ok in report["checks"].items() if not ok]
if sensitive_changed:
    failures.append("sensitive_changed_files")
if failures:
    raise SystemExit("ERROR: Plan 3.1 HigDatePicker audit failed: " + ", ".join(failures))
