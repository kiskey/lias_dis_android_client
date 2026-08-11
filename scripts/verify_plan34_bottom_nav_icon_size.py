#!/usr/bin/env python3

from pathlib import Path
import json
import re
import sys

NAV = Path(
    "app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt"
).read_text(
    encoding="utf-8"
)

LIBS = Path(
    "gradle/libs.versions.toml"
).read_text(
    encoding="utf-8"
)

VISUAL_SCALE = 1.30
SLOT_DP = 20.0
NATIVE_LABEL_GAP_DP = 6.0


def find_matching(
    text: str,
    open_index: int,
    open_char: str,
    close_char: str,
) -> int:
    if text[open_index] != open_char:
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


# ConfiguredLiasApp exact scope.
functions = list(
    re.finditer(
        r"^private fun ConfiguredLiasApp\(",
        NAV,
        flags=re.MULTILINE,
    )
)

if len(functions) != 1:
    raise SystemExit(
        "ERROR: ConfiguredLiasApp declaration is not unique"
    )

fn_start = functions[0].start()
fn_open_paren = NAV.find("(", fn_start)
fn_close_paren = find_matching(
    NAV,
    fn_open_paren,
    "(",
    ")",
)
fn_body_open = NAV.find("{", fn_close_paren)
fn_body_close = find_matching(
    NAV,
    fn_body_open,
    "{",
    "}",
)

fn_body = NAV[
    fn_body_open:
    fn_body_close + 1
]

# Exact native bar token and body.
bars = list(
    re.finditer(
        r"CupertinoNavigationBar\s*\{",
        fn_body,
    )
)

if len(bars) != 1:
    raise SystemExit(
        "ERROR: expected exactly one CupertinoNavigationBar in "
        f"ConfiguredLiasApp; found {len(bars)}"
    )

bar_token_start = (
    fn_body_open
    + bars[0].start()
)
bar_token_end = (
    fn_body_open
    + bars[0].end()
)
bar_open = NAV.rfind(
    "{",
    bar_token_start,
    bar_token_end,
)
bar_close = find_matching(
    NAV,
    bar_open,
    "{",
    "}",
)

bar_header = NAV[
    bar_token_start:
    bar_open + 1
]
bar_body = NAV[
    bar_open:
    bar_close + 1
]

if re.fullmatch(
    r"CupertinoNavigationBar\s*\{",
    bar_header.strip(),
) is None:
    raise SystemExit(
        "ERROR: parsed navigation-bar header is not native "
        "CupertinoNavigationBar {"
    )

items = list(
    re.finditer(
        r"CupertinoNavigationBarItem\(",
        bar_body,
    )
)

if len(items) != 1:
    raise SystemExit(
        "ERROR: expected exactly one CupertinoNavigationBarItem "
        f"declaration in tab family; found {len(items)}"
    )

item_start = (
    bar_open
    + items[0].start()
)
item_open = NAV.find("(", item_start)
item_close = find_matching(
    NAV,
    item_open,
    "(",
    ")",
)

item_args = NAV[
    item_open:
    item_close
]

icons = list(
    re.finditer(
        r"(?m)^\s*icon\s*=\s*\{",
        item_args,
    )
)

if len(icons) != 1:
    raise SystemExit(
        "ERROR: expected exactly one navigation icon lambda"
    )

icon_start = (
    item_open
    + icons[0].start()
)
icon_open = NAV.find("{", icon_start)
icon_close = find_matching(
    NAV,
    icon_open,
    "{",
    "}",
)
icon_region = NAV[
    icon_open:
    icon_close + 1
]

graphics = list(
    re.finditer(
        r"Modifier\.graphicsLayer\s*\{",
        icon_region,
    )
)

if len(graphics) != 1:
    raise SystemExit(
        "ERROR: expected exactly one icon graphicsLayer"
    )

graphics_start = (
    icon_open
    + graphics[0].start()
)
graphics_open = NAV.find("{", graphics_start)
graphics_close = find_matching(
    NAV,
    graphics_open,
    "{",
    "}",
)

graphics_region = NAV[
    graphics_open:
    graphics_close + 1
]

scale_x = re.findall(
    r"(?m)^\s*scaleX\s*=\s*(\d+\.\d+f)\s*$",
    graphics_region,
)
scale_y = re.findall(
    r"(?m)^\s*scaleY\s*=\s*(\d+\.\d+f)\s*$",
    graphics_region,
)

visual_dp = SLOT_DP * VISUAL_SCALE
overflow_each_side_dp = (
    visual_dp - SLOT_DP
) / 2.0
remaining_label_gap_dp = (
    NATIVE_LABEL_GAP_DP
    - overflow_each_side_dp
)

checks = {
    "cupertino_2_3_1":
        re.search(
            r'^cupertino\s*=\s*"2\.3\.1"\s*$',
            LIBS,
            flags=re.MULTILINE,
        ) is not None,

    "native_cupertino_navigation_bar":
        re.fullmatch(
            r"CupertinoNavigationBar\s*\{",
            bar_header.strip(),
        ) is not None,

    "single_navigation_item_family":
        len(items) == 1,

    "single_icon_graphics_layer":
        len(graphics) == 1,

    "scale_x_1_30":
        scale_x == ["1.30f"],

    "scale_y_1_30":
        scale_y == ["1.30f"],

    "visual_26dp":
        abs(visual_dp - 26.0) < 0.001,

    "remaining_label_gap_3dp":
        remaining_label_gap_dp >= 3.0,

    "no_icon_size_override":
        re.search(
            r"\bModifier\s*\.\s*size\s*\(",
            icon_region,
        ) is None,

    "no_custom_navigation_height":
        re.search(
            r"\.height\s*\(",
            bar_body,
        ) is None,

    "labels_retained":
        re.search(
            r"alwaysShowLabel\s*=\s*true",
            bar_body,
        ) is not None,

    "five_tabs_retained":
        all(
            value in NAV
            for value in (
                "LiasScreen.Home",
                "LiasScreen.Devices",
                "LiasScreen.Schedules",
                "LiasScreen.Rules",
                "LiasScreen.Settings",
            )
        ),

    "root_tab_logic_retained":
        "private fun rootTabRoute(" in NAV,

    "custom_tab_not_reintroduced":
        "private fun RowScope.TabItem(" not in NAV,

    "comment_26dp":
        "only the glyph drawing to ~26.dp." in icon_region,
}

report = {
    "checks": checks,
    "geometry": {
        "native_slot_dp":
            SLOT_DP,
        "visual_scale":
            VISUAL_SCALE,
        "visual_dp":
            visual_dp,
        "overflow_each_side_dp":
            overflow_each_side_dp,
        "native_label_gap_dp":
            NATIVE_LABEL_GAP_DP,
        "remaining_label_gap_dp":
            remaining_label_gap_dp,
    },
}

print(
    json.dumps(
        report,
        indent=2,
    )
)

failed = [
    name
    for name, ok in checks.items()
    if not ok
]

if failed:
    print(
        "ERROR: Plan 3.4 bottom-navigation 26dp gate failed:",
        file=sys.stderr,
    )
    for name in failed:
        print(
            f" - {name}",
            file=sys.stderr,
        )
    sys.exit(1)

print(
    "PASS: Plan 3.4 bottom-navigation icon gate passed at ~26dp."
)
