#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path.cwd()
APP = ROOT / "app" / "src" / "main" / "java"
OUT = ROOT / "build" / "plan31" / "textfield_polish_audit.json"

HIG_FIELD = APP / "com" / "lias" / "remote" / "ui" / "components" / "HigField.kt"
SCAFFOLD = APP / "com" / "lias" / "remote" / "ui" / "components" / "HigLargeTitleScaffold.kt"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace") if path.exists() else ""

def rel(path: Path) -> str:
    return str(path.relative_to(ROOT))

def grep(pattern: str, roots=None) -> list[str]:
    roots = roots or [APP]
    rx = re.compile(pattern, re.MULTILINE)
    hits = []
    for root in roots:
        if not root.exists():
            continue
        for p in root.rglob("*.kt"):
            if rx.search(read(p)):
                hits.append(rel(p))
    return sorted(set(hits))

hig = read(HIG_FIELD)
scaffold = read(SCAFFOLD)

modified_sensitive = []
for cmd in ["git", "diff", "--name-only"]:
    pass

# Use git diff if available to detect accidental sensitive file edits.
try:
    import subprocess
    result = subprocess.run(
        ["git", "diff", "--name-only"],
        cwd=ROOT,
        check=False,
        text=True,
        capture_output=True,
    )
    changed = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    sensitive_prefixes = (
        "app/src/main/java/com/lias/remote/core/",
        "app/src/main/java/com/lias/remote/repositories/",
        "app/src/main/java/com/lias/remote/ui/navigation/",
    )
    allowed_sensitive_exact = set()
    modified_sensitive = [
        p for p in changed
        if p.startswith(sensitive_prefixes) and p not in allowed_sensitive_exact
    ]
except Exception:
    modified_sensitive = ["git diff unavailable"]

search_block = scaffold[scaffold.find("fun HigSearchField("):] if "fun HigSearchField(" in scaffold else ""

report = {
    "files": {
        "HigField.kt": rel(HIG_FIELD) if HIG_FIELD.exists() else None,
        "HigLargeTitleScaffold.kt": rel(SCAFFOLD) if SCAFFOLD.exists() else None,
    },
    "checks": {
        "hig_field_uses_cupertino_textfield": "import com.slapps.cupertino.CupertinoTextField" in hig,
        "hig_field_uses_textfieldvalue": "TextFieldValue" in hig,
        "hig_field_keeps_reconcile": "reconcileEditorValue" in hig,
        "hig_field_has_cupertino_group_chrome_marker": "Plan 3.1 text-field polish" in hig,
        "hig_field_no_basic_textfield": "BasicTextField" not in hig,
        "search_uses_cursor_safe_textfield": "CursorSafeTextField(" in search_block,
        "search_uses_cupertino_icons": "MagnifyingGlass" in search_block and "XmarkCircle" in search_block,
        "search_no_canvas": not bool(re.search(r"\bCanvas\s*\(", search_block)),
    },
    "old_cupertino_refs": grep(r"io\.github\.alexzhirkevich"),
    "material_textfield_refs": grep(r"androidx\.compose\.material(\d|3)?\..*(TextField|OutlinedTextField)|androidx\.compose\.material3\..*(TextField|OutlinedTextField)"),
    "material_icon_refs": grep(r"androidx\.compose\.material\.icons"),
    "basic_textfield_refs": grep(r"\bBasicTextField\b"),
    "sensitive_changed_files": modified_sensitive,
}

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

failures = []
for key, ok in report["checks"].items():
    if not ok:
        failures.append(key)

for key in ["old_cupertino_refs", "material_textfield_refs", "material_icon_refs"]:
    if report[key]:
        failures.append(key)

# BasicTextField outside the adapter is still suspicious; fail loudly so it is
# reviewed before acceptance.
if report["basic_textfield_refs"]:
    failures.append("basic_textfield_refs")

if report["sensitive_changed_files"]:
    failures.append("sensitive_changed_files")

if failures:
    raise SystemExit("ERROR: Plan 3.1 text-field polish audit failed: " + ", ".join(failures))
