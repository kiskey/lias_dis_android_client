#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re

ROOT = Path.cwd()
FILE = ROOT / "app/src/main/java/com/lias/remote/ui/components/HigLargeTitleScaffold.kt"
REPORT = ROOT / "build/plan31/batch024_search_chrome_report.md"

def read() -> str:
    if not FILE.exists():
        raise SystemExit(f"ERROR: missing {FILE.relative_to(ROOT)}")
    return FILE.read_text(encoding="utf-8")

def write(text: str) -> None:
    FILE.write_text(text, encoding="utf-8")

def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"ERROR: expected one match for {label}, found {count}")
    return text.replace(old, new, 1)

text = read()

if "Plan 3.1 search-field polish" in text:
    print("Batch 024 already applied.")
else:
    text = text.replace(
        "//   Apple-inspired collapsible large-title application scaffold.\n//",
        "//   Apple-inspired collapsible large-title application scaffold.\n//\n// Plan 3.1 search-field polish:\n//   - Keeps CursorSafeTextField for cursor/selection stability.\n//   - Uses maintained-fork Cupertino search/clear icons.\n//   - Refines iOS search-field chrome without Canvas glyphs.\n//",
        1,
    )

    old_row = """    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(
                    min =
                        48.dp
                )
                .clip(RoundedCornerShape(10.dp))
                .background(LiasThemeColors.fill2)
                .padding(start = 12.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
"""

    new_row = """    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(
                    min =
                        48.dp
                )
                .clip(
                    RoundedCornerShape(
                        13.dp
                    )
                )
                .background(
                    LiasThemeColors
                        .tertiaryBackground
                )
                .padding(
                    start =
                        12.dp,
                    end =
                        2.dp
                ),
        verticalAlignment = Alignment.CenterVertically
    ) {
"""

    text = replace_once(text, old_row, new_row, "HigSearchField row chrome")

    old_mod = """            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
"""

    new_mod = """            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(
                        min =
                            44.dp
                    )
"""

    text = replace_once(text, old_mod, new_mod, "search textfield modifier formatting")

    write(text)

text = read()
search = text[text.find("fun HigSearchField("):] if "fun HigSearchField(" in text else ""
problems = []

for token in [
    "Plan 3.1 search-field polish",
    "CursorSafeTextField(",
    "CupertinoIcons",
    "MagnifyingGlass",
    "XmarkCircle",
    "ImeAction.Search",
]:
    if token not in search and token not in text:
        problems.append(f"missing {token}")

if re.search(r"\bCanvas\s*\(", search):
    problems.append("Canvas found inside HigSearchField")
if "BasicTextField" in search:
    problems.append("BasicTextField found inside HigSearchField")
if "androidx.compose.material.icons" in text:
    problems.append("Material icons found")

REPORT.parent.mkdir(parents=True, exist_ok=True)
if problems:
    REPORT.write_text(
        "# Batch 024 failed\n\n" + "\n".join(f"- {p}" for p in problems) + "\n",
        encoding="utf-8",
    )
    raise SystemExit(f"ERROR: Batch 024 failed. See {REPORT}")

REPORT.write_text(
    "# Batch 024 passed\n\n"
    "- HigSearchField chrome softened to iOS-like search surface.\n"
    "- CursorSafeTextField retained.\n"
    "- Cupertino search/clear icons retained.\n"
    "- ImeAction.Search retained.\n"
    "- No Canvas search glyphs introduced.\n",
    encoding="utf-8",
)
print(f"PASS: Batch 024 applied. Report: {REPORT}")
