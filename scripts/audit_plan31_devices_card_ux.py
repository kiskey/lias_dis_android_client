#!/usr/bin/env python3
from __future__ import annotations
import json, subprocess
from pathlib import Path
ROOT = Path.cwd()
APP = ROOT / "app" / "src" / "main" / "java"
DEVICES = APP / "com" / "lias" / "remote" / "ui" / "screens" / "devices" / "DevicesScreen.kt"
OUT = ROOT / "build" / "plan31" / "devices_card_ux_audit.json"
def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace") if path.exists() else ""
def rel(path: Path) -> str:
    return str(path.relative_to(ROOT))
text = read(DEVICES)
device_card = text[text.find("private fun DeviceCardItem("):] if "private fun DeviceCardItem(" in text else ""
try:
    diff = subprocess.run(["git", "diff", "--name-only"], cwd=ROOT, text=True, capture_output=True, check=False)
    changed = [line.strip() for line in diff.stdout.splitlines() if line.strip()]
except Exception:
    changed = ["git diff unavailable"]
sensitive_prefixes = ("app/src/main/java/com/lias/remote/core/", "app/src/main/java/com/lias/remote/repositories/")
sensitive_changed = [p for p in changed if p.startswith(sensitive_prefixes)]
visible_forbidden = ['"Extend Access"', '"Manage Access"', 'text =\n                                "Pause"', 'text =\n                            "Details"', 'text =\n                                "Details"']
report = {
  "files": {"DevicesScreen.kt": rel(DEVICES) if DEVICES.exists() else None},
  "checks": {
    "has_plan_marker": "Plan 3.1 Devices card UX" in text,
    "imports_clickable": "import androidx.compose.foundation.clickable" in text,
    "imports_role": "import androidx.compose.ui.semantics.Role" in text,
    "search_query_starts_blank": "remember(\n            initialTagId" in text and "mutableStateOf(\"\")" in text,
    "tag_scope_still_uses_selected_tag_id": "selectedTagId =\n                    initialTagId" in text,
    "device_card_has_card_click": ".clickable(" in device_card and "onPrimaryAction" in device_card,
    "device_card_has_disclosure": 'text = "›"' in device_card,
    "device_card_keeps_resume_only": '"Resume"' in device_card,
    "device_card_no_visible_extend_manage_pause_details_buttons": not any(x in device_card for x in visible_forbidden),
    "no_material_icons": "androidx.compose.material.icons" not in text,
  },
  "sensitive_changed_files": sensitive_changed,
}
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))
failures = [k for k,v in report["checks"].items() if not v]
if report["sensitive_changed_files"]:
    failures.append("sensitive_changed_files")
if failures:
    raise SystemExit("ERROR: Plan 3.1 Devices card UX audit failed: " + ", ".join(failures))
