#!/usr/bin/env python3

from pathlib import Path
import json
import os
import re
import subprocess
import sys

ROOT = Path(".")

HIG = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/components/HigSheets.kt"
).read_text(encoding="utf-8")

BUTTON = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/components/HigButton.kt"
).read_text(encoding="utf-8")

ACTIONS = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt"
).read_text(encoding="utf-8")

POLICY = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens/rules/"
    "PolicyWizardSheet.kt"
).read_text(encoding="utf-8")

LIBS = (
    ROOT /
    "gradle/libs.versions.toml"
).read_text(encoding="utf-8")


def find_matching(text, open_index, open_char, close_char):
    if open_index < 0 or text[open_index] != open_char:
        raise SystemExit(
            f"ERROR: expected {open_char!r} at {open_index}"
        )

    depth = 0
    i = open_index
    state = "code"

    while i < len(text):
        ch = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ""

        if state == "code":
            if ch == "/" and nxt == "/":
                state = "line_comment"
                i += 2
                continue
            if ch == "/" and nxt == "*":
                state = "block_comment"
                i += 2
                continue
            if ch == '"':
                state = "string"
                i += 1
                continue
            if ch == "'":
                state = "char"
                i += 1
                continue

            if ch == open_char:
                depth += 1
            elif ch == close_char:
                depth -= 1
                if depth == 0:
                    return i

        elif state == "line_comment":
            if ch == "\n":
                state = "code"
        elif state == "block_comment":
            if ch == "*" and nxt == "/":
                state = "code"
                i += 2
                continue
        elif state == "string":
            if ch == "\\":
                i += 2
                continue
            if ch == '"':
                state = "code"
        elif state == "char":
            if ch == "\\":
                i += 2
                continue
            if ch == "'":
                state = "code"

        i += 1

    raise SystemExit(
        f"ERROR: unbalanced {open_char}{close_char}"
    )


def top_function(text, name):
    matches = list(
        re.finditer(
            rf"(?m)^fun {re.escape(name)}\(",
            text,
        )
    )

    if len(matches) != 1:
        raise SystemExit(
            f"ERROR: top-level fun {name} is not unique"
        )

    start = matches[0].start()
    open_paren = text.find("(", start)
    close_paren = find_matching(text, open_paren, "(", ")")
    body_open = text.find("{", close_paren)
    body_close = find_matching(text, body_open, "{", "}")

    return text[start:body_close + 1]


def local_function(parent_text, marker):
    if parent_text.count(marker) != 1:
        raise SystemExit(
            f"ERROR: local function marker {marker!r} is not unique"
        )

    start = parent_text.index(marker)
    open_paren = parent_text.find("(", start)
    close_paren = find_matching(parent_text, open_paren, "(", ")")
    body_open = parent_text.find("{", close_paren)
    body_close = find_matching(parent_text, body_open, "{", "}")

    return parent_text[start:body_close + 1]


modal = top_function(HIG, "HigModalSheet")
header = top_function(HIG, "HigSheetHeader")

immediate = local_function(
    modal,
    "fun requestImmediateHeaderCancel()",
)
animated = local_function(
    modal,
    "fun requestAnimatedDismiss()",
)
completion = local_function(
    modal,
    "fun requestAnimatedCompletion(",
)

hide_index = completion.find("sheetState.hide()")
action_index = completion.find("action()")

screens_root = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens"
)

header_callers = []
screen_immediate_imports = []

for file in screens_root.rglob("*.kt"):
    text = file.read_text(
        encoding="utf-8",
        errors="replace",
    )

    if "HigSheetHeader(" in text:
        header_callers.append(
            str(file.relative_to(ROOT))
        )

    if "rememberHigImmediateHeaderCancel" in text:
        screen_immediate_imports.append(
            str(file.relative_to(ROOT))
        )

header_callers.sort()
screen_immediate_imports.sort()

checks = {
    "cupertino_2_3_1":
        re.search(
            r'^cupertino\s*=\s*"2\.3\.1"\s*$',
            LIBS,
            flags=re.MULTILINE,
        ) is not None,

    "immediate_provider_exists":
        "LocalHigImmediateHeaderCancel" in HIG
        and "fun rememberHigImmediateHeaderCancel(" in HIG,

    "shared_header_uses_immediate":
        "rememberHigImmediateHeaderCancel(" in header
        and "immediateCancel" in header,

    "shared_header_not_animated":
        "rememberHigAnimatedDismiss(" not in header
        and "animatedCancel" not in header,

    "immediate_guard_parent":
        "parentDismissDelivered" in immediate,

    "immediate_guard_completion":
        "completionInFlight" in immediate,

    "immediate_delivers_parent":
        "deliverParentDismissOnce()" in immediate,

    "immediate_no_sheet_hide":
        "sheetState" not in immediate
        and "hide()" not in immediate,

    "immediate_no_coroutine_or_delay":
        "coroutineScope" not in immediate
        and ".launch" not in immediate
        and "delay(" not in immediate,

    "immediate_provider_wired":
        "LocalHigImmediateHeaderCancel provides" in modal
        and "::requestImmediateHeaderCancel" in modal,

    "animated_helper_retained":
        "fun rememberHigAnimatedDismiss(" in HIG,

    "animated_dismiss_hides_sheet":
        "sheetState.hide()" in animated,

    "back_uses_animated_dismiss":
        "BackHandler(" in modal
        and "requestAnimatedDismiss()" in modal,

    "completion_hides_sheet":
        hide_index >= 0,

    "completion_action_retained":
        action_index >= 0,

    "completion_hide_before_action":
        hide_index >= 0
        and action_index >= 0
        and hide_index < action_index,

    "completion_guard_retained":
        "completionInFlight" in modal,

    "sheet_swipe_retained":
        re.search(
            r"sheetSwipeEnabled\s*=\s*true",
            modal,
        ) is not None,

    "button_no_delay":
        "delay(" not in BUTTON,

    "button_no_debounce":
        "debounce" not in BUTTON.lower(),

    "security_secondary_dismiss_still_animated":
        "rememberHigAnimatedDismiss(" in ACTIONS
        and '"Investigate Later"' in ACTIONS
        and "onClick = animatedDismiss" in ACTIONS,

    "global_action_sheet_retained":
        "CupertinoActionSheet(" in ACTIONS,

    "global_150ms_exit_retained":
        re.search(
            r"delay\(\s*150\s*\)",
            ACTIONS,
        ) is not None,

    "policy_success_animated_dismiss_retained":
        "rememberHigAnimatedDismiss(" in POLICY
        and "animatedDismiss()" in POLICY,

    "immediate_primitive_centralized":
        not screen_immediate_imports,

    "shared_header_has_callers":
        bool(header_callers),
}

if os.environ.get("GITHUB_ACTIONS") == "true":
    changed = subprocess.check_output(
        [
            "git",
            "diff",
            "--name-only",
            "HEAD^",
            "HEAD",
        ],
        text=True,
    ).splitlines()
else:
    changed = subprocess.check_output(
        [
            "git",
            "diff",
            "--name-only",
            "origin/luna",
        ],
        text=True,
    ).splitlines()

forbidden_prefixes = (
    "app/src/main/java/com/lias/remote/core/network/",
    "app/src/main/java/com/lias/remote/core/models/",
    "app/src/main/java/com/lias/remote/core/policy/",
    "app/src/main/java/com/lias/remote/core/schedule/",
    "app/src/main/java/com/lias/remote/repositories/",
)

forbidden = [
    path
    for path in changed
    if path.startswith(forbidden_prefixes)
]

checks["no_contract_domain_changes"] = not forbidden

report = {
    "checks": checks,
    "shared_header_callers": header_callers,
    "unexpected_screen_immediate_imports": screen_immediate_imports,
    "changed_files": changed,
    "forbidden_contract_domain_changes": forbidden,
}

out = (
    ROOT /
    "build/plan35/snappy_header_cancel.json"
)

out.parent.mkdir(
    parents=True,
    exist_ok=True,
)

out.write_text(
    json.dumps(report, indent=2),
    encoding="utf-8",
)

print(
    json.dumps(report, indent=2)
)

bad = [
    name
    for name, ok in checks.items()
    if not ok
]

if bad:
    print(
        "ERROR: snappy header-Cancel stabilization gate failed:",
        file=sys.stderr,
    )
    for name in bad:
        print(
            f" - {name}",
            file=sys.stderr,
        )
    sys.exit(1)

print(
    "PASS: snappy header-Cancel stabilization gate passed."
)
