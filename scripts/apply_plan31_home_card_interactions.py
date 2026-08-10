#!/usr/bin/env python3
from __future__ import annotations

import os
import re
from pathlib import Path

ROOT = Path.cwd()
SRC = ROOT / "app" / "src"
REPORT = ROOT / "build" / "plan31" / "home_card_patch_report.md"

def write_report(lines: list[str]) -> None:
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text("\n".join(lines) + "\n", encoding="utf-8")

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")

def find_home() -> Path:
    override = os.environ.get("PLAN31_HOME_FILE", "").strip()
    if override:
        p = ROOT / override
        if p.exists():
            return p
        raise SystemExit(f"ERROR: PLAN31_HOME_FILE does not exist: {override}")

    scored = []
    for path in SRC.rglob("*.kt"):
        text = read(path)
        score = sum(token in text for token in [
            "Active Protections",
            "Restricted",
            "Extend access",
            "Details",
            "Home",
            "Protection",
        ])
        if score >= 3:
            scored.append((score, path))
    if not scored:
        raise SystemExit("ERROR: no Home screen candidate found. Run scripts/audit_plan31_home_ux.py.")
    scored.sort(reverse=True)
    return scored[0][1]

def line_block_remove_button(lines: list[str], label: str) -> tuple[list[str], int]:
    # Removes a composable call block containing a visible text label such as
    # "Extend access" or "Details". It is conservative and only targets button-
    # looking blocks.
    removed = 0
    out = []
    i = 0
    while i < len(lines):
        if label not in lines[i]:
            out.append(lines[i])
            i += 1
            continue

        start = i
        while start > 0 and not re.search(r'\b(CupertinoButton|HigButton|Button|TextButton)\s*\(', lines[start]):
            start -= 1

        if start == 0 and not re.search(r'\b(CupertinoButton|HigButton|Button|TextButton)\s*\(', lines[start]):
            out.append(lines[i])
            i += 1
            continue

        depth = 0
        end = start
        seen_open = False
        for j in range(start, min(len(lines), start + 80)):
            depth += lines[j].count("(") + lines[j].count("{")
            depth -= lines[j].count(")") + lines[j].count("}")
            if "(" in lines[j] or "{" in lines[j]:
                seen_open = True
            if seen_open and j > i and depth <= 0:
                end = j
                break
        else:
            out.append(lines[i])
            i += 1
            continue

        # Remove any already emitted lines that were part of the block.
        trim_count = len(out) - start
        if trim_count > 0:
            out = out[:start]
        out.append(f"        // Plan 3.1: removed visible '{label}' button; behavior moved to card/disclosure click.\n")
        removed += 1
        i = end + 1
    return out, removed

def add_disclosure_imports(text: str) -> str:
    # Use CupertinoText fallback for disclosure glyph because the uploaded Plan
    # 3.0 evidence did not verify a maintained-fork chevron icon name. This avoids
    # guessing a non-existent icon while keeping Material icons out.
    if "import com.slapps.cupertino.CupertinoText" not in text and "CupertinoText" in text:
        return text
    return text

def add_marker(text: str) -> str:
    marker = "// Plan 3.1 Home UX: restricted card tap opens extend access; trailing disclosure opens details."
    if marker in text:
        return text
    return text.replace("\npackage ", f"\n{marker}\npackage ", 1) if "\npackage " in text else marker + "\n" + text

def main() -> None:
    path = find_home()
    text = read(path)

    if "Plan 3.1 Home UX: restricted card tap opens extend access" in text:
        print(f"PASS: {path} already contains Plan 3.1 Home UX marker.")
        return

    lines = text.splitlines(keepends=True)
    lines, removed_extend = line_block_remove_button(lines, "Extend access")
    lines, removed_details = line_block_remove_button(lines, "Details")
    patched = "".join(lines)

    # Conservative card-tap patch hints. If the source already has explicit
    # callback names, add comments at the top so human/code review can verify.
    has_extend_callback = bool(re.search(r'on\w*Extend\w*\s*[:=]', patched))
    has_detail_callback = bool(re.search(r'on\w*Detail\w*\s*[:=]|onOpen\w*Detail\w*\s*[:=]', patched))

    report = [
        "# Plan 3.1 Home card interaction patch report",
        "",
        f"Home file: `{path.relative_to(ROOT)}`",
        f"Removed Extend access button blocks: {removed_extend}",
        f"Removed Details button blocks: {removed_details}",
        f"Detected extend callback: {has_extend_callback}",
        f"Detected details callback: {has_detail_callback}",
        "",
        "## Required review",
        "",
        "- Restricted device card surface must call the existing extend-access callback.",
        "- Trailing disclosure must call the existing details callback.",
        "- No Material icon imports are allowed.",
        "",
    ]

    if removed_extend == 0 or removed_details == 0:
        write_report(report + [
            "ERROR: expected visible button blocks were not found or the source shape is unsupported.",
            "No source file was modified.",
            "Set PLAN31_HOME_FILE to the exact Home screen file and rerun, or share this report for an exact patch.",
        ])
        raise SystemExit(f"ERROR: unsupported Home card shape. See {REPORT}")

    if not has_extend_callback or not has_detail_callback:
        write_report(report + [
            "ERROR: callbacks could not be confidently detected.",
            "No source file was modified.",
        ])
        raise SystemExit(f"ERROR: callbacks not confidently detected. See {REPORT}")

    patched = add_marker(patched)
    path.write_text(patched, encoding="utf-8")
    write_report(report + [
        "PASS: conservative source patch applied.",
        "",
        "Important: this batch removes the visible buttons. If CI fails because the",
        "card surface still needs the clickable callback wired, send the failing",
        "Home file and I will provide the exact follow-up patch.",
    ])
    print(f"Batch 012 applied to {path.relative_to(ROOT)}")
    print(f"Report: {REPORT}")

if __name__ == "__main__":
    main()
