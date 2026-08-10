#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path.cwd()
APP = ROOT / "app" / "src" / "main" / "java"
OUT = ROOT / "build" / "plan31" / "text_search_alert_audit.json"

HIG_FIELD = APP / "com" / "lias" / "remote" / "ui" / "components" / "HigField.kt"
HIG_SCAFFOLD = APP / "com" / "lias" / "remote" / "ui" / "components" / "HigLargeTitleScaffold.kt"
HIG_ALERT = APP / "com" / "lias" / "remote" / "ui" / "components" / "HigAlertDialog.kt"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace") if path.exists() else ""

def rel(path: Path) -> str:
    return str(path.relative_to(ROOT))

def find_files(pattern: str) -> list[str]:
    rx = re.compile(pattern, re.MULTILINE)
    found: list[str] = []
    for p in APP.rglob("*.kt"):
        if rx.search(read(p)):
            found.append(rel(p))
    return sorted(found)

def search_adapter_uses_canvas_glyphs(text: str) -> bool:
    start = text.find("fun HigSearchField(")
    if start < 0:
        return False
    block = text[start:]
    return bool(re.search(r"\bCanvas\s*\(", block))

hig_field = read(HIG_FIELD)
hig_scaffold = read(HIG_SCAFFOLD)
hig_alert = read(HIG_ALERT)

all_canvas_refs = find_files(r"\bCanvas\s*\(")
adapter_canvas_refs = [
    p for p in all_canvas_refs
    if p.endswith("HigField.kt") or p.endswith("HigLargeTitleScaffold.kt")
]

report = {
    "files": {
        "HigField.kt": rel(HIG_FIELD) if HIG_FIELD.exists() else None,
        "HigLargeTitleScaffold.kt": rel(HIG_SCAFFOLD) if HIG_SCAFFOLD.exists() else None,
        "HigAlertDialog.kt": rel(HIG_ALERT) if HIG_ALERT.exists() else None,
    },
    "checks": {
        "hig_field_uses_cupertino_text_field": "com.slapps.cupertino.CupertinoTextField" in hig_field,
        "hig_field_uses_text_field_value": "TextFieldValue" in hig_field,
        "hig_field_has_reconcile_editor_value": "reconcileEditorValue" in hig_field,
        "search_uses_cursor_safe_text_field": "CursorSafeTextField(" in hig_scaffold,
        "search_uses_magnifying_glass_icon": "MagnifyingGlass" in hig_scaffold and "com.slapps.cupertino.icons" in hig_scaffold,
        "search_uses_xmark_circle_icon": "XmarkCircle" in hig_scaffold and "com.slapps.cupertino.icons" in hig_scaffold,
        "search_has_ime_search": "ImeAction.Search" in hig_scaffold,
        "search_adapter_has_no_canvas_glyphs": not search_adapter_uses_canvas_glyphs(hig_scaffold),
        "hig_alert_is_lias_owned_adapter": "fun HigAlertDialog(" in hig_alert and "com.slapps.cupertino.CupertinoButton" in hig_alert,
    },
    "old_cupertino_refs": find_files(r"io\.github\.alexzhirkevich"),
    "material_alert_dialog_refs": find_files(
        r"androidx\.compose\.material(\d|3)?\..*AlertDialog|androidx\.compose\.material3\..*AlertDialog"
    ),
    "platform_dialog_refs": find_files(r"androidx\.compose\.ui\.window\.Dialog|Popup\("),
    "platform_dialog_refs_allowed_note": (
        "HigAlertDialog and schedule picker Dialog/Popup usage are audited separately; "
        "Plan 3.1 does not require replacing editable/adaptive dialogs blindly."
    ),
    "all_canvas_refs": all_canvas_refs,
    "adapter_canvas_refs_that_would_fail": adapter_canvas_refs,
    "basic_text_field_refs": find_files(r"\bBasicTextField\b"),
    "short_confirmation_candidates": find_files(
        r"Delete|Remove|Confirm|Reject|Reopen|Revoke|Split|Merge|Cancel|destructive|cannot be undone|Are you sure"
    ),
}

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

failures: list[str] = []

for key, ok in report["checks"].items():
    if not ok:
        failures.append(key)

if report["old_cupertino_refs"]:
    failures.append("old_cupertino_refs")

if report["material_alert_dialog_refs"]:
    failures.append("material_alert_dialog_refs")

if report["adapter_canvas_refs_that_would_fail"]:
    failures.append("adapter_canvas_refs_that_would_fail")

adapter_basic = [
    p for p in report["basic_text_field_refs"]
    if p.endswith("HigField.kt") or p.endswith("HigLargeTitleScaffold.kt")
]
if adapter_basic:
    failures.append("adapter_basic_text_field_refs")

if failures:
    raise SystemExit(
        "ERROR: Plan 3.1 text/search/alert audit failed: " +
        ", ".join(failures)
    )
