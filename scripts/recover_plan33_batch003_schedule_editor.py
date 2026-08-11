#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path.cwd()
P = ROOT / "app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt"
OUT = ROOT / "build/plan33/batch003_schedule_editor_recovery.md"

if not P.exists():
    raise SystemExit("ERROR: ScheduleEditorSheet.kt missing")

text = P.read_text(encoding="utf-8")

if "import com.lias.remote.ui.components.rememberHigAnimatedCompletion" not in text:
    anchor = "import com.lias.remote.ui.components.HigModalSheet\n"
    if anchor not in text:
        raise SystemExit("ERROR: HigModalSheet import anchor missing")
    text = text.replace(
        anchor,
        anchor + "import com.lias.remote.ui.components.rememberHigAnimatedCompletion\n",
        1,
    )

if "val animatedComplete =" not in text:
    anchor = '''    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

'''
    insert = anchor + '''        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

'''
    if anchor not in text:
        raise SystemExit("ERROR: HigModalSheet body shape differs")
    text = text.replace(anchor, insert, 1)

pattern = re.compile(
    r'(?P<indent>[ \t]*)onSave\(\s*\n'
    r'[ \t]*draft\.toSchedule\(\s*\n'
    r'[ \t]*initialSchedule\s*\n'
    r'[ \t]*\)\s*\n'
    r'[ \t]*\)',
    re.MULTILINE,
)

def replacement(match):
    i = match.group("indent")
    return (
        f"{i}val schedule =\n"
        f"{i}    draft.toSchedule(\n"
        f"{i}        initialSchedule\n"
        f"{i}    )\n\n"
        f"{i}animatedComplete {{\n"
        f"{i}    onSave(\n"
        f"{i}        schedule\n"
        f"{i}    )\n"
        f"{i}}}"
    )

text, patched = pattern.subn(replacement, text)

P.write_text(text, encoding="utf-8")
text = P.read_text(encoding="utf-8")

animated_save_count = len(
    re.findall(
        r'animatedComplete\s*\{\s*onSave\(\s*schedule\s*\)',
        text,
        re.MULTILINE,
    )
)
direct_save_count = len(
    re.findall(
        r'onSave\(\s*draft\.toSchedule\(\s*initialSchedule\s*\)\s*\)',
        text,
        re.MULTILINE,
    )
)

checks = {
    "completion_import": "rememberHigAnimatedCompletion" in text,
    "completion_helper": "val animatedComplete =" in text,
    "both_save_paths_animated": animated_save_count == 2,
    "no_direct_draft_save_left": direct_save_count == 0,
}

bad = [k for k,v in checks.items() if not v]
OUT.parent.mkdir(parents=True, exist_ok=True)

if bad:
    OUT.write_text(
        "# Recovery failed\n\n"
        f"- newly patched direct calls: {patched}\n"
        f"- animated save paths: {animated_save_count}\n"
        f"- direct save paths left: {direct_save_count}\n\n" +
        "\n".join(f"- FAIL: {x}" for x in bad) + "\n",
        encoding="utf-8",
    )
    print(OUT.read_text())
    sys.exit(1)

OUT.write_text(
    "# Plan 3.3 Batch 003 ScheduleEditor recovery passed\n\n"
    f"- Newly patched direct save calls: {patched}\n"
    "- Header Save uses animated completion.\n"
    "- Bottom Create/Save Changes uses animated completion.\n"
    "- No direct `onSave(draft.toSchedule(...))` remains.\n",
    encoding="utf-8",
)
print(OUT.read_text())
