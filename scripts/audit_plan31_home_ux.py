#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path.cwd()
SRC = ROOT / "app" / "src"
OUT = ROOT / "build" / "plan31" / "home_ux_audit.json"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")

def score_home(text: str) -> int:
    score = 0
    for token in [
        "Active Protections",
        "Restricted",
        "Extend access",
        "Details",
        "Home",
        "Vacation",
        "Protection",
    ]:
        if token in text:
            score += 1
    return score

def score_nav(text: str) -> int:
    score = 0
    for token in ["NavHost", "composable", "Devices", "Home", "Navigation"]:
        if token in text:
            score += 1
    return score

def score_devices(text: str) -> int:
    score = 0
    for token in ["DevicesScreen", "Devices", "Tag", "Filter", "Search"]:
        if token in text:
            score += 1
    return score

kt_files = sorted(SRC.rglob("*.kt"))

home_candidates = []
nav_candidates = []
devices_candidates = []
cupertino_import_gaps = []
material_icon_imports = []
old_cupertino_refs = []

for path in kt_files:
    rel = str(path.relative_to(ROOT))
    text = read(path)
    hs = score_home(text)
    ns = score_nav(text)
    ds = score_devices(text)

    if hs >= 3:
        home_candidates.append({"path": rel, "score": hs})
    if ns >= 3:
        nav_candidates.append({"path": rel, "score": ns})
    if ds >= 3:
        devices_candidates.append({"path": rel, "score": ds})

    if "io.github.alexzhirkevich" in text:
        old_cupertino_refs.append(rel)
    if re.search(r"import\s+androidx\.compose\.material\.icons", text):
        material_icon_imports.append(rel)
    if "Cupertino" in text and "com.slapps.cupertino" not in text and "package " in text:
        cupertino_import_gaps.append(rel)

report = {
    "home_candidates": home_candidates[:10],
    "nav_candidates": nav_candidates[:10],
    "devices_candidates": devices_candidates[:10],
    "old_cupertino_refs": old_cupertino_refs,
    "material_icon_imports": material_icon_imports,
    "cupertino_import_gaps": cupertino_import_gaps[:30],
    "recommended_env_overrides": {
        "PLAN31_HOME_FILE": home_candidates[0]["path"] if home_candidates else "",
        "PLAN31_NAV_FILE": nav_candidates[0]["path"] if nav_candidates else "",
        "PLAN31_DEVICES_FILE": devices_candidates[0]["path"] if devices_candidates else "",
    },
}

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

if not home_candidates:
    raise SystemExit("ERROR: no Home-screen candidate found. Set PLAN31_HOME_FILE manually.")
if old_cupertino_refs:
    raise SystemExit("ERROR: old Cupertino namespace remains in Kotlin source.")
