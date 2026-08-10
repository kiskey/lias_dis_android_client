#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path.cwd()
APP = ROOT / "app" / "src" / "main" / "java"
OUT = ROOT / "build" / "plan31" / "alert_candidates.json"
MD = ROOT / "docs" / "compose_cupertino_plan31_alert_candidates.md"

candidate_terms = re.compile(
    r"Delete|Remove|Confirm|Reject|Reopen|Revoke|Split|Merge|Cancel|destructive|cannot be undone|Are you sure",
    re.I,
)

records = []
material_alerts = []
platform_dialogs = []
forms_or_sheets = []

for p in sorted(APP.rglob("*.kt")):
    text = p.read_text(encoding="utf-8", errors="replace")
    rel = str(p.relative_to(ROOT))

    if "androidx.compose.material" in text and "AlertDialog" in text:
        material_alerts.append(rel)
    if "androidx.compose.ui.window.Dialog" in text or "Popup(" in text:
        platform_dialogs.append(rel)
    if "Sheet(" in text or "Modal" in text:
        forms_or_sheets.append(rel)

    lines = text.splitlines()
    hits = []
    for i, line in enumerate(lines, start=1):
        if candidate_terms.search(line):
            hits.append({"line": i, "text": line.strip()[:220]})
    if hits:
        records.append({"path": rel, "hits": hits[:20]})

report = {
    "material_alerts": material_alerts,
    "platform_dialogs": platform_dialogs,
    "forms_or_sheets": forms_or_sheets,
    "candidate_records": records,
}

OUT.write_text(json.dumps(report, indent=2), encoding="utf-8")

md = [
    "# Plan 3.1 alert candidate audit",
    "",
    "Status: candidate audit, no behavior change",
    "",
    "## Rules",
    "",
    "- Only short non-editing confirmations are candidates for CupertinoAlertDialog.",
    "- Multi-field forms and identity review workflows remain sheets/forms unless separately approved.",
    "- Destructive safeguards and server reconciliation semantics must not change.",
    "",
    "## Material AlertDialog references",
    "",
]
md.extend(f"- `{p}`" for p in material_alerts)
if not material_alerts:
    md.append("- None detected.")

md.extend(["", "## Platform Dialog/Popup references", ""])
md.extend(f"- `{p}`" for p in platform_dialogs)
if not platform_dialogs:
    md.append("- None detected.")

md.extend(["", "## Candidate files by text scan", ""])
for record in records:
    md.append(f"### `{record['path']}`")
    for hit in record["hits"][:10]:
        md.append(f"- L{hit['line']}: `{hit['text']}`")
    md.append("")

MD.write_text("\n".join(md) + "\n", encoding="utf-8")

print(json.dumps(report, indent=2))

if material_alerts:
    raise SystemExit("ERROR: Material AlertDialog references found; replace with LIAS/Cupertino adapter in a source-specific patch.")
