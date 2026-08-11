#!/usr/bin/env python3

from pathlib import Path
import sys

p = Path(
    "app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt"
)

text = p.read_text(
    encoding="utf-8"
)

checks = {
    "cupertino_navigation_bar":
        "CupertinoNavigationBar {" in text,

    "cupertino_navigation_item":
        "CupertinoNavigationBarItem(" in text,

    "icon_graphics_scale":
        "Modifier.graphicsLayer {" in text,

    "scale_x_1_20":
        "scaleX =" in text
        and "1.20f" in text,

    "scale_y_1_20":
        "scaleY =" in text
        and text.count("1.20f") >= 2,

    "five_tab_routes_preserved":
        all(
            token in text
            for token in (
                "LiasScreen.Home",
                "LiasScreen.Devices",
                "LiasScreen.Schedules",
                "LiasScreen.Rules",
                "LiasScreen.Settings",
            )
        ),

    "root_tab_logic_preserved":
        "private fun rootTabRoute(" in text,

    "custom_tab_not_reintroduced":
        "private fun RowScope.TabItem(" not in text,
}

bad = [
    name
    for name, ok in checks.items()
    if not ok
]

for name, ok in checks.items():
    print(
        f"{'PASS' if ok else 'FAIL'}: {name}"
    )

if bad:
    print(
        "ERROR: bottom navigation icon-size gate failed: "
        + ", ".join(bad),
        file=sys.stderr,
    )
    sys.exit(1)

print(
    "PASS: Plan 3.4 bottom navigation icon-size gate passed."
)
