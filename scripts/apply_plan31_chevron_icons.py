#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re

ROOT = Path.cwd()
HOME = ROOT / "app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt"
DEVICES = ROOT / "app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt"
REPORT = ROOT / "build/plan31/batch034_chevron_icons_report.md"

def read(path: Path) -> str:
    if not path.exists():
        raise SystemExit(f"ERROR: missing {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")

def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")

def add_import(text: str, imp: str) -> str:
    line = f"import {imp}\n"
    if line in text:
        return text
    imports = list(re.finditer(r"^import .+$", text, flags=re.MULTILINE))
    if not imports:
        raise SystemExit("ERROR: no import block found")
    last = imports[-1]
    return text[:last.end()] + "\n" + line.rstrip("\n") + text[last.end():]

def chevron_block(callback: str) -> str:
    return f'''CupertinoIcon(
                        imageVector =
                            CupertinoIcons
                                .Outlined
                                .ChevronForward,
                        contentDescription =
                            "Details",
                        tint =
                            LiasThemeColors
                                .tertiaryLabel,
                        modifier =
                            Modifier
                                .clickable(
                                    role =
                                        Role.Button,
                                    onClick =
                                        {callback}
                                )
                                .padding(
                                    horizontal =
                                        6.dp,
                                    vertical =
                                        4.dp
                                )
                    )'''

def patch_disclosures(text: str) -> str:
    # Matches the current Plan 3.1 text disclosure shape in Home/Devices.
    pattern = re.compile(
        r'''CupertinoText\(\s*
            text\s*=\s*"›",\s*
            style\s*=\s*HigTypography\.title3,\s*
            color\s*=\s*LiasThemeColors\s*\.tertiaryLabel,\s*
            modifier\s*=\s*Modifier\s*
                \.clickable\(\s*
                    role\s*=\s*Role\.Button,\s*
                    onClick\s*=\s*(onDetail|onDetails|onDetailDevice)\s*
                \)\s*
                \.padding\(\s*
                    horizontal\s*=\s*6\.dp,\s*
                    vertical\s*=\s*4\.dp\s*
                \)\s*
        \)''',
        re.MULTILINE | re.VERBOSE,
    )
    return pattern.sub(lambda m: chevron_block(m.group(1)), text)

def patch_file(path: Path) -> None:
    text = read(path)
    if "Plan 3.1 ChevronForward icon adoption" not in text:
        text = text.replace(
            "package ",
            "// Plan 3.1 ChevronForward icon adoption:\n//   - Replaces temporary text disclosure with Slanoss Cupertino icon.\n\npackage ",
            1,
        )

    text = add_import(text, "com.slapps.cupertino.CupertinoIcon")
    text = add_import(text, "com.slapps.cupertino.icons.CupertinoIcons")
    text = add_import(text, "com.slapps.cupertino.icons.outlined.ChevronForward")

    patched = patch_disclosures(text)
    if 'text = "›"' in patched:
        raise SystemExit(
            f"ERROR: {path.relative_to(ROOT)} still contains text = \"›\". "
            "Manual review needed because disclosure block shape was unexpected."
        )
    write(path, patched)

patch_file(HOME)
patch_file(DEVICES)

problems = []
for path in [HOME, DEVICES]:
    text = read(path)
    rel = path.relative_to(ROOT)
    for token in [
        "import com.slapps.cupertino.CupertinoIcon",
        "import com.slapps.cupertino.icons.CupertinoIcons",
        "import com.slapps.cupertino.icons.outlined.ChevronForward",
        "ChevronForward",
    ]:
        if token not in text:
            problems.append(f"{rel}: missing {token}")
    if 'text = "›"' in text:
        problems.append(f"{rel}: text disclosure remains")
    if "androidx.compose.material.icons" in text:
        problems.append(f"{rel}: Material icons found")

REPORT.parent.mkdir(parents=True, exist_ok=True)
if problems:
    REPORT.write_text("# Batch 034 failed\n\n" + "\n".join(f"- {p}" for p in problems) + "\n", encoding="utf-8")
    raise SystemExit(f"ERROR: Batch 034 failed. See {REPORT}")

REPORT.write_text(
    "# Batch 034 passed\n\n"
    "- Home and Devices use CupertinoIcons.Outlined.ChevronForward.\n"
    "- Temporary text disclosure `›` removed from Home/Devices.\n",
    encoding="utf-8",
)
print(f"PASS: Batch 034 applied. Report: {REPORT}")
