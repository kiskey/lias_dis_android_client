#!/usr/bin/env python3
from pathlib import Path
import os
import subprocess, json, sys

ROOT = Path(".")

def read(rel):
    p = ROOT / rel
    if not p.exists():
        raise SystemExit(f"ERROR: missing {rel}")
    return p.read_text(encoding="utf-8")

libs = read("gradle/libs.versions.toml")
manifest = read("app/src/main/AndroidManifest.xml")
nav = read("app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt")
large = read("app/src/main/java/com/lias/remote/ui/components/HigLargeTitleScaffold.kt")
alert = read("app/src/main/java/com/lias/remote/ui/components/HigAlertDialog.kt")
actions = read("app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt")
detail = read("app/src/main/java/com/lias/remote/ui/screens/devices/DeviceDetailScreen.kt")
identity = read("app/src/main/java/com/lias/remote/ui/screens/identity/IdentityReviewScreen.kt")
connection = read("app/src/main/java/com/lias/remote/ui/screens/settings/ConnectionSettingsScreen.kt")

checks = {
    "navigation_2_8_9":
        'navigationCompose = "2.8.9"' in libs,
    "activity_unchanged":
        'activityCompose = "1.9.0"' in libs,
    "cupertino_pin":
        'cupertino = "2.3.1"' in libs,
    "predictive_back_manifest":
        'android:enableOnBackInvokedCallback="true"' in manifest,

    "cupertino_tab_bar":
        "CupertinoNavigationBar {" in nav
        and "CupertinoNavigationBarItem(" in nav,
    "custom_tab_removed":
        "private fun RowScope.TabItem(" not in nav,
    "root_tab_family":
        "private fun rootTabRoute(" in nav,
    "peer_tabs_not_push":
        "EnterTransition.None" in nav
        and "ExitTransition.None" in nav,
    "hierarchy_spatial_push":
        "width ->\n                            width" in nav
        and "-width / 3" in nav,
    "connect_root_transition":
        '"lias-configuration-root"' in nav
        and "AnimatedContent(" in nav,

    "cupertino_top_bar":
        "CupertinoTopAppBar(" in large,
    "cupertino_navigation_title":
        "CupertinoNavigationTitle(" in large,
    "navigation_title_coordination":
        "hasNavigationTitle" in large,
    "topbar_translucency":
        "isTopBarTransparent" in large,
    "old_binary_title_removed":
        "firstVisibleItemScrollOffset" not in large
        and "isCollapsed" not in large,

    "device_back":
        "CupertinoNavigateBackButton(" in detail,
    "identity_back":
        "CupertinoNavigateBackButton(" in identity,
    "connection_back":
        "CupertinoNavigateBackButton(" in connection,

    "compact_cupertino_alert":
        "CupertinoAlertDialog(" in alert,
    "alert_destructive_style":
        "destructive(" in alert,
    "editable_alert_compatibility":
        "content ==\n        null" in alert
        and "Dialog(" in alert,

    "cupertino_action_sheet":
        "CupertinoActionSheet(" in actions,
    "action_sheet_destructive":
        '"Apply Block All"' in actions
        and "destructive(" in actions,

    # Navigation motion uses standard finite Compose transitions.
    # Compose's MotionDurationScale applies to these animations, including
    # the system's zero-duration accessibility setting.
    "finite_navigation_motion":
        "rememberInfiniteTransition" not in nav
        and "infiniteRepeatable" not in nav,
}

header_screens = [
    "app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt",
    "app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt",
    "app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulesScreen.kt",
    "app/src/main/java/com/lias/remote/ui/screens/rules/RulesScreen.kt",
    "app/src/main/java/com/lias/remote/ui/screens/settings/SettingsScreen.kt",
    "app/src/main/java/com/lias/remote/ui/screens/identity/IdentityReviewScreen.kt",
]

for rel in header_screens:
    s = read(rel)
    checks[f"navigation_header:{rel}"] = (
        '"cupertino-navigation-header"' in s
        and "navigationHeader()" in s
    )

# Contract/domain scope guard.
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
    "app/src/main/java/com/lias/remote/repositories/",
)

forbidden = [
    path for path in changed
    if path.startswith(forbidden_prefixes)
]

checks["no_contract_domain_changes"] = not forbidden

report = {
    "checks": checks,
    "changed_files": changed,
    "forbidden_contract_domain_changes": forbidden,
}

out = ROOT / "build/plan34/final_audit.json"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(report, indent=2), encoding="utf-8")
print(json.dumps(report, indent=2))

bad = [k for k,v in checks.items() if not v]
if bad:
    print("ERROR: Plan 3.4 final static gate failed:")
    for item in bad:
        print(" -", item)
    sys.exit(1)

print("PASS: Plan 3.4 Cupertino Navigation & Motion Parity static gate passed.")
