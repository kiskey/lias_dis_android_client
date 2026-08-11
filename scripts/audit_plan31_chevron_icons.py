#!/usr/bin/env python3
from __future__ import annotations

import json, subprocess
from pathlib import Path

ROOT = Path.cwd()
FILES = [
    ROOT / "app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt",
    ROOT / "app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt",
]
OUT = ROOT / "build/plan31/chevron_icons_audit.json"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace") if path.exists() else ""

try:
    diff = subprocess.run(["git", "diff", "--name-only"], cwd=ROOT, text=True, capture_output=True, check=False)
    changed = [line.strip() for line in diff.stdout.splitlines() if line.strip()]
except Exception:
    changed = ["git diff unavailable"]

sensitive_prefixes = ("app/src/main/java/com/lias/remote/core/", "app/src/main/java/com/lias/remote/repositories/")
sensitive_changed = [p for p in changed if p.startswith(sensitive_prefixes)]

per_file = {}
for path in FILES:
    text = read(path)
    per_file[str(path.relative_to(ROOT))] = {
        "has_cupertino_icon_import": "import com.slapps.cupertino.CupertinoIcon" in text,
        "has_cupertino_icons_import": "import com.slapps.cupertino.icons.CupertinoIcons" in text,
        "has_chevron_forward_import": "import com.slapps.cupertino.icons.outlined.ChevronForward" in text,
        "uses_chevron_forward": "ChevronForward" in text,
        "no_text_disclosure": 'text = "›"' not in text,
        "no_material_icons": "androidx.compose.material.icons" not in text,
    }

report = {"files": per_file, "sensitive_changed_files": sensitive_changed}
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

failures = []
for path, checks in per_file.items():
    for key, ok in checks.items():
        if not ok:
            failures.append(f"{path}:{key}")
if sensitive_changed:
    failures.append("sensitive_changed_files")
if failures:
    raise SystemExit("ERROR: Plan 3.1 ChevronForward icon audit failed: " + ", ".join(failures))
