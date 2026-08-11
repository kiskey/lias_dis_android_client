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

POLICY = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens/rules/"
    "PolicyWizardSheet.kt"
).read_text(encoding="utf-8")

ACTIONS = (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt"
).read_text(encoding="utf-8")

LIBS = (
    ROOT /
    "gradle/libs.versions.toml"
).read_text(encoding="utf-8")


def find_matching_brace(source: str, open_index: int) -> int:
    depth = 0
    state = "code"
    i = open_index

    while i < len(source):
        ch = source[i]
        nxt = source[i + 1] if i + 1 < len(source) else ""

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
            if ch == "{":
                depth += 1
            elif ch == "}":
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

    raise SystemExit("ERROR: unbalanced function braces")


def exact_function_region(source: str, marker: str) -> str:
    if source.count(marker) != 1:
        raise SystemExit(
            f"ERROR: marker {marker!r} not unique"
        )

    start = source.index(marker)
    open_brace = source.find("{", start)
    if open_brace == -1:
        raise SystemExit(
            f"ERROR: missing function body for {marker!r}"
        )

    close_brace = find_matching_brace(
        source,
        open_brace,
    )
    return source[start:close_brace + 1]


modal = exact_function_region(
    HIG,
    "fun HigModalSheet(",
)
immediate = exact_function_region(
    modal,
    "fun requestImmediateCompletion(",
)
policy = exact_function_region(
    POLICY,
    "fun PolicyWizardSheet(",
)
policy_button = exact_function_region(
    POLICY,
    "private fun PolicySaveButton(",
)

old_import = (
    "import com.lias.remote.ui.components."
    "rememberHigAnimatedCompletion"
)
new_import = (
    "import com.lias.remote.ui.components."
    "rememberHigImmediateCompletion"
)

legacy_screen_users = []
immediate_screen_users = []

for file in (
    ROOT /
    "app/src/main/java/com/lias/remote/ui/screens"
).rglob("*.kt"):
    text = file.read_text(
        encoding="utf-8",
        errors="replace",
    )
    rel = str(file.relative_to(ROOT))

    if old_import in text:
        legacy_screen_users.append(rel)
    if new_import in text:
        immediate_screen_users.append(rel)

legacy_screen_users.sort()
immediate_screen_users.sort()

expected_immediate = sorted(
    [
        "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt",
        "app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt",
        "app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt",
        "app/src/main/java/com/lias/remote/ui/screens/rules/PolicyWizardSheet.kt",
        "app/src/main/java/com/lias/remote/ui/screens/schedules/ScheduleEditorSheet.kt",
        "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt",
    ]
)

expected_legacy = sorted(
    [
        "app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt",
        "app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt",
        "app/src/main/java/com/lias/remote/ui/screens/PauseSheet.kt",
        "app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt",
    ]
)

action_index = immediate.find("action()")
dismiss_index = immediate.find("deliverParentDismissOnce()")
server_index = policy.find("onSave(")
complete_index = policy.find("immediateComplete {")

checks = {
    "cupertino_2_3_1":
        re.search(
            r'^cupertino\s*=\s*"2\.3\.1"\s*$',
            LIBS,
            flags=re.MULTILINE,
        ) is not None,

    "immediate_helper_exists":
        "fun rememberHigImmediateCompletion(" in HIG,

    "immediate_provider_wired":
        "LocalHigImmediateCompletion provides" in modal
        and "::requestImmediateCompletion" in modal,

    "immediate_guard":
        "parentDismissDelivered" in immediate
        and "completionInFlight" in immediate,

    "callback_before_parent":
        action_index >= 0
        and dismiss_index >= 0
        and action_index < dismiss_index,

    "no_immediate_hide":
        "sheetState.hide()" not in immediate,

    "no_immediate_coroutine_delay":
        "coroutineScope" not in immediate
        and ".launch" not in immediate
        and "delay(" not in immediate,

    "legacy_animated_adapter_retained":
        "fun rememberHigAnimatedCompletion(" in HIG,

    "exact_legacy_completion_users":
        legacy_screen_users == expected_legacy,

    "exact_immediate_completion_users":
        immediate_screen_users == expected_immediate,

    "dismiss_gate_supported":
        "dismissEnabled: Boolean = true" in modal
        and "confirmValueChange =" in modal
        and "CupertinoSheetValue.Hidden" in modal,

    "policy_is_saving":
        "var isSaving by" in policy,

    "policy_dismiss_locked_while_saving":
        re.search(
            r"dismissEnabled\s*=\s*!isSaving",
            policy,
        ) is not None,

    "policy_duplicate_guard":
        re.search(
            r"if\s*\(\s*isSaving\s*\)\s*\{\s*return",
            policy,
        ) is not None,

    "policy_server_before_dismiss":
        server_index >= 0
        and complete_index >= 0
        and server_index < complete_index,

    "policy_false_keeps_sheet":
        re.search(
            r"else\s*\{\s*isSaving\s*=\s*false",
            policy,
        ) is not None,

    "policy_native_spinner":
        "CupertinoActivityIndicator(" in policy_button,

    "policy_spinner_18dp":
        "18.dp" in policy_button,

    "policy_spinner_overlay":
        "Box(" in policy_button
        and "Alignment.CenterStart" in policy_button
        and "Modifier.fillMaxWidth()" in policy_button,

    "policy_saving_label":
        '"Saving…"' in policy_button,

    "policy_button_disabled":
        re.search(
            r"enabled\s*=\s*enabled\s*&&\s*!saving",
            policy_button,
        ) is not None,

    "policy_two_save_slots":
        policy.count("PolicySaveButton(") == 2,

    "global_action_sheet_150ms_retained":
        "CupertinoActionSheet(" in ACTIONS
        and re.search(
            r"delay\(\s*150\s*\)",
            ACTIONS,
        ) is not None,

    "header_cancel_immediate_retained":
        "fun requestImmediateHeaderCancel()" in modal,

    "back_animated_retained":
        "fun requestAnimatedDismiss()" in modal
        and "BackHandler(" in modal,

    "sheet_swipe_retained":
        re.search(
            r"sheetSwipeEnabled\s*=\s*true",
            modal,
        ) is not None,
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
    "immediate_screen_users": immediate_screen_users,
    "expected_immediate_screen_users": expected_immediate,
    "legacy_screen_users": legacy_screen_users,
    "expected_legacy_screen_users": expected_legacy,
    "changed_files": changed,
    "forbidden_contract_domain_changes": forbidden,
}

out = (
    ROOT /
    "build/plan35/immediate_completion_policy_progress.json"
)
out.parent.mkdir(
    parents=True,
    exist_ok=True,
)
out.write_text(
    json.dumps(
        report,
        indent=2,
    ),
    encoding="utf-8",
)

print(
    json.dumps(
        report,
        indent=2,
    )
)

bad = [
    name
    for name, ok in checks.items()
    if not ok
]

if bad:
    print(
        "ERROR: immediate completion / Policy progress gate failed:",
        file=sys.stderr,
    )
    for name in bad:
        print(
            f" - {name}",
            file=sys.stderr,
        )
    sys.exit(1)

print(
    "PASS: immediate completion + Policy server-progress stabilization gate passed."
)
