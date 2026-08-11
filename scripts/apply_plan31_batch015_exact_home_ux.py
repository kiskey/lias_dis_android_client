#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path.cwd()

HOME = ROOT / "app/src/main/java/com/lias/remote/ui/screens/home/HomeScreen.kt"
NAV_ROUTES = ROOT / "app/src/main/java/com/lias/remote/ui/navigation/NavigationRoutes.kt"
NAV_HOST = ROOT / "app/src/main/java/com/lias/remote/ui/navigation/LiasNavHost.kt"
DEVICES = ROOT / "app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt"
REPORT = ROOT / "build/plan31/batch015_exact_home_ux_report.md"

def read(path: Path) -> str:
    if not path.exists():
        raise SystemExit(f"ERROR: missing required file: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")

def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")

def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"ERROR: expected exactly one match for {label}, found {count}.")
    return text.replace(old, new, 1)

def insert_after_once(text: str, marker: str, addition: str, label: str) -> str:
    if addition.strip() in text:
        return text
    count = text.count(marker)
    if count != 1:
        raise SystemExit(f"ERROR: expected exactly one insertion marker for {label}, found {count}.")
    return text.replace(marker, marker + addition, 1)

def patch_home() -> None:
    text = read(HOME)

    if "Plan 3.1 exact Home UX patch" in text:
        print("HomeScreen.kt already patched.")
        return

    text = insert_after_once(
        text,
        "import androidx.compose.foundation.background\n",
        "import androidx.compose.foundation.clickable\n",
        "Home clickable import",
    )

    text = insert_after_once(
        text,
        "import androidx.compose.ui.Modifier\n",
        "import androidx.compose.ui.semantics.Role\n",
        "Home semantics role import",
    )

    text = replace_once(
        text,
        """fun HomeScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit,
    onNavigateToTab: (LiasScreen) -> Unit,
    onNavigateToIdentityReview: () -> Unit = {}
) {""",
        """fun HomeScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit,
    onNavigateToTab: (LiasScreen) -> Unit,
    onNavigateToDevicesForTag: (String) -> Unit,
    onNavigateToIdentityReview: () -> Unit = {}
) {""",
        "HomeScreen signature",
    )

    text = replace_once(
        text,
        """                                onClick = {
                                    onNavigateToTab(LiasScreen.Devices)
                                }""",
        """                                onClick = {
                                    onNavigateToDevicesForTag(protection.tag.id)
                                }""",
        "Active Protections tag click",
    )

    old_func = """@Composable
private fun RestrictedDeviceRow(
    device: Device,
    presentation: AccessPresentation,
    primaryTag: Tag?,
    showDivider: Boolean,
    onResume: () -> Unit,
    onExtend: () -> Unit,
    onDetails: () -> Unit
) {
    Column {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeIconBubble(
                        icon = primaryTag.homeIconKind().imageVector(),
                        tint = homeTagAccent(primaryTag)
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    Column {
                        CupertinoText(
                            text = device.displayName,
                            style = HigTypography.headline,
                            color = LiasThemeColors.label
                        )
                        CupertinoText(
                            text =
                                device.currentIP.ifBlank {
                                    if (device.online) "Online" else "Offline"
                                },
                            style = HigTypography.caption,
                            color = LiasThemeColors.tertiaryLabel
                        )
                    }
                }

                StatusPill(
                    text = presentation.label,
                    tone = presentation.tone
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    presentation.canResumePause ->
                        HigButton(
                            text = "Resume",
                            onClick = onResume,
                            style = HigButtonStyle.Primary,
                            modifier = Modifier.weight(1f)
                        )

                    presentation.canExtend ->
                        HigButton(
                            text = "Extend Access",
                            onClick = onExtend,
                            style = HigButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                }

                HigButton(
                    text = "Details",
                    onClick = onDetails,
                    style = HigButtonStyle.Gray,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (showDivider) {
            HomeDivider()
        }
    }
}
"""

    new_func = """@Composable
private fun RestrictedDeviceRow(
    device: Device,
    presentation: AccessPresentation,
    primaryTag: Tag?,
    showDivider: Boolean,
    onResume: () -> Unit,
    onExtend: () -> Unit,
    onDetails: () -> Unit
) {
    Column {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClick = onExtend
                    )
                    .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeIconBubble(
                        icon = primaryTag.homeIconKind().imageVector(),
                        tint = homeTagAccent(primaryTag)
                    )

                    Spacer(modifier = Modifier.size(8.dp))

                    Column {
                        CupertinoText(
                            text = device.displayName,
                            style = HigTypography.headline,
                            color = LiasThemeColors.label
                        )
                        CupertinoText(
                            text =
                                device.currentIP.ifBlank {
                                    if (device.online) "Online" else "Offline"
                                },
                            style = HigTypography.caption,
                            color = LiasThemeColors.tertiaryLabel
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        text = presentation.label,
                        tone = presentation.tone
                    )

                    CupertinoText(
                        text = "›",
                        style = HigTypography.title3,
                        color = LiasThemeColors.tertiaryLabel,
                        modifier =
                            Modifier
                                .clickable(
                                    role = Role.Button,
                                    onClick = onDetails
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            if (presentation.canResumePause) {
                Spacer(modifier = Modifier.height(10.dp))

                HigButton(
                    text = "Resume",
                    onClick = onResume,
                    style = HigButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showDivider) {
            HomeDivider()
        }
    }
}
"""

    text = replace_once(text, old_func, new_func, "RestrictedDeviceRow exact function")

    text = text.replace(
        "// Purpose:\n//   LIAS operational overview using server-authoritative access state.\n// ====================================================================",
        "// Purpose:\n//   LIAS operational overview using server-authoritative access state.\n//\n// Plan 3.1 exact Home UX patch:\n//   - Restricted device card tap opens extend-access sheet.\n//   - Trailing disclosure opens device details.\n//   - Active Protection group card navigates to Devices filtered by tag.\n// ====================================================================",
        1,
    )

    write(HOME, text)
    print("Patched HomeScreen.kt")

def patch_navigation_routes() -> None:
    text = read(NAV_ROUTES)

    if "DEVICES_BY_TAG" in text:
        print("NavigationRoutes.kt already patched.")
        return

    text = insert_after_once(
        text,
        """    const val DEVICES =
        "devices"
""",
        """
    const val DEVICES_BY_TAG =
        "devices/tag/{tagId}"
""",
        "DEVICES_BY_TAG constant",
    )

    text = insert_after_once(
        text,
        """    fun deviceDetail(
        pdid: String
    ): String =
        "device_detail/${
            Uri.encode(
                pdid
            )
        }"
""",
        """

    fun devicesByTag(
        tagId: String
    ): String =
        "devices/tag/${
            Uri.encode(
                tagId
            )
        }"
""",
        "devicesByTag function",
    )

    write(NAV_ROUTES, text)
    print("Patched NavigationRoutes.kt")

def patch_nav_host() -> None:
    text = read(NAV_HOST)

    if "onNavigateToDevicesForTag" in text:
        print("LiasNavHost.kt already patched.")
        return

    text = replace_once(
        text,
        """                        onNavigateToTab = {
                            screen ->

                            navController.navigate(
                                screen.route
                            ) {

                                popUpTo(
                                    navController
                                        .graph
                                        .findStartDestination()
                                        .id
                                ) {
                                    saveState =
                                        true
                                }

                                launchSingleTop =
                                    true

                                restoreState =
                                    true
                            }
                        },
                        onNavigateToIdentityReview = {""",
        """                        onNavigateToTab = {
                            screen ->

                            navController.navigate(
                                screen.route
                            ) {

                                popUpTo(
                                    navController
                                        .graph
                                        .findStartDestination()
                                        .id
                                ) {
                                    saveState =
                                        true
                                }

                                launchSingleTop =
                                    true

                                restoreState =
                                    true
                            }
                        },
                        onNavigateToDevicesForTag = {
                            tagId ->

                            navController.navigate(
                                NavigationRoutes
                                    .devicesByTag(
                                        tagId
                                    )
                            ) {

                                popUpTo(
                                    navController
                                        .graph
                                        .findStartDestination()
                                        .id
                                ) {
                                    saveState =
                                        true
                                }

                                launchSingleTop =
                                    true
                            }
                        },
                        onNavigateToIdentityReview = {""",
        "HomeScreen call",
    )

    text = replace_once(
        text,
        """                composable(
                    NavigationRoutes.DEVICES
                ) {

                    DevicesScreen(
                        viewModel =
                            liasViewModel,
                        onNavigateToDeviceDetail = {
                            pdid ->

                            navController.navigate(
                                NavigationRoutes
                                    .deviceDetail(
                                        pdid
                                    )
                            )
                        }
                    )
                }
""",
        """                composable(
                    NavigationRoutes.DEVICES
                ) {

                    DevicesScreen(
                        viewModel =
                            liasViewModel,
                        onNavigateToDeviceDetail = {
                            pdid ->

                            navController.navigate(
                                NavigationRoutes
                                    .deviceDetail(
                                        pdid
                                    )
                            )
                        }
                    )
                }

                composable(
                    route =
                        NavigationRoutes.DEVICES_BY_TAG,
                    arguments =
                        listOf(
                            navArgument(
                                "tagId"
                            ) {
                                type =
                                    NavType.StringType
                            }
                        )
                ) {
                    entry ->

                    val tagId =
                        entry.arguments
                            ?.getString(
                                "tagId"
                            )

                    DevicesScreen(
                        viewModel =
                            liasViewModel,
                        initialTagId =
                            tagId,
                        onNavigateToDeviceDetail = {
                            pdid ->

                            navController.navigate(
                                NavigationRoutes
                                    .deviceDetail(
                                        pdid
                                    )
                            )
                        }
                    )
                }
""",
        "Devices route block",
    )

    write(NAV_HOST, text)
    print("Patched LiasNavHost.kt")

def patch_devices() -> None:
    text = read(DEVICES)

    if "initialTagId: String? = null" in text:
        print("DevicesScreen.kt already patched.")
        return

    text = replace_once(
        text,
        """fun DevicesScreen(
    viewModel: LiasViewModel,
    onNavigateToDeviceDetail: (String) -> Unit
) {""",
        """fun DevicesScreen(
    viewModel: LiasViewModel,
    initialTagId: String? = null,
    onNavigateToDeviceDetail: (String) -> Unit
) {""",
        "DevicesScreen signature",
    )

    text = replace_once(
        text,
        """    var searchQuery by
        remember {
            mutableStateOf(
                ""
            )
        }
""",
        """    val initialTag =
        remember(
            state.tags,
            initialTagId
        ) {
            state.tags
                .firstOrNull {
                    it.id == initialTagId
                }
        }

    var searchQuery by
        remember(
            initialTag?.name
        ) {
            mutableStateOf(
                initialTag
                    ?.name
                    .orEmpty()
            )
        }
""",
        "search initialization",
    )

    text = replace_once(
        text,
        """            searchQuery
        ) {

            buildDeviceSections(
                devices =
                    state.devices,
                tags =
                    state.tags,
                query =
                    searchQuery
            )
        }
""",
        """            searchQuery,
            initialTagId
        ) {

            buildDeviceSections(
                devices =
                    state.devices,
                tags =
                    state.tags,
                query =
                    searchQuery,
                selectedTagId =
                    initialTagId
            )
        }
""",
        "sections remember/build",
    )

    text = replace_once(
        text,
        """fun DevicesScreen(
    viewModel: LiasViewModel,
    initialTagId: String? = null,
    onNavigateToDeviceDetail: (String) -> Unit
) {""",
        """fun DevicesScreen(
    viewModel: LiasViewModel,
    initialTagId: String? = null,
    onNavigateToDeviceDetail: (String) -> Unit
) {""",
        "idempotence no-op marker",
    ) if False else text

    text = replace_once(
        text,
        """private fun buildDeviceSections(
    devices: List<Device>,
    tags: List<Tag>,
    query: String
): List<DeviceSection> {""",
        """private fun buildDeviceSections(
    devices: List<Device>,
    tags: List<Tag>,
    query: String,
    selectedTagId: String? = null
): List<DeviceSection> {""",
        "buildDeviceSections signature",
    )

    text = replace_once(
        text,
        """    val filtered =
        if (
            query.isBlank()
        ) {

            devices

        } else {

            devices.filter {
                device ->

                device.displayName
                    .contains(
                        query,
                        ignoreCase =
                            true
                    ) ||
                    device.currentMAC
                        .contains(
                            query,
                            ignoreCase =
                                true
                        ) ||
                    device.currentIP
                        .contains(
                            query,
                            ignoreCase =
                                true
                        ) ||
                    device.hostname
                        .contains(
                            query,
                            ignoreCase =
                                true
                        )
            }
        }
""",
        """    val tagScopedDevices =
        selectedTagId
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                tagId ->

                devices.filter {
                    device ->

                    device.safeTags.any {
                        it.equals(
                            tagId,
                            ignoreCase =
                                true
                        )
                    }
                }
            }
            ?: devices

    val filtered =
        if (
            query.isBlank()
        ) {

            tagScopedDevices

        } else {

            tagScopedDevices.filter {
                device ->

                device.displayName
                    .contains(
                        query,
                        ignoreCase =
                            true
                    ) ||
                    device.currentMAC
                        .contains(
                            query,
                            ignoreCase =
                                true
                        ) ||
                    device.currentIP
                        .contains(
                            query,
                            ignoreCase =
                                true
                        ) ||
                    device.hostname
                        .contains(
                            query,
                            ignoreCase =
                                true
                        )
            }
        }
""",
        "filtered block",
    )

    write(DEVICES, text)
    print("Patched DevicesScreen.kt")

def verify() -> None:
    problems = []
    for path in [HOME, NAV_ROUTES, NAV_HOST, DEVICES]:
        text = read(path)
        if "io.github.alexzhirkevich" in text:
            problems.append(f"{path.relative_to(ROOT)} contains old Cupertino namespace")
        if "androidx.compose.material.icons" in text:
            problems.append(f"{path.relative_to(ROOT)} imports Material icons")

    home_text = read(HOME)
    for forbidden in [
        'text = "Extend Access"',
        'text = "Manage Access"',
        'text = "Details"',
    ]:
        # HomeRefreshError has Retry only; Details/Extend are removed from restricted rows.
        if forbidden in home_text:
            problems.append(f"HomeScreen.kt still contains restricted-card visible button label: {forbidden}")

    if "onNavigateToDevicesForTag(protection.tag.id)" not in home_text:
        problems.append("HomeScreen.kt does not navigate Active Protection tag cards with tag id")

    if "DEVICES_BY_TAG" not in read(NAV_ROUTES):
        problems.append("NavigationRoutes.kt missing DEVICES_BY_TAG")

    if "initialTagId =" not in read(NAV_HOST):
        problems.append("LiasNavHost.kt missing initialTagId route handoff")

    if "selectedTagId: String? = null" not in read(DEVICES):
        problems.append("DevicesScreen.kt missing selectedTagId filtering")

    REPORT.parent.mkdir(parents=True, exist_ok=True)
    if problems:
        REPORT.write_text(
            "# Batch 015 verification failed\n\n" +
            "\n".join(f"- {p}" for p in problems) +
            "\n",
            encoding="utf-8",
        )
        raise SystemExit(f"ERROR: Batch 015 verification failed. See {REPORT}")

    REPORT.write_text(
        "# Batch 015 verification passed\n\n"
        "- Restricted Device visible Extend/Manage/Details buttons removed from HomeScreen.\n"
        "- Restricted Device card tap opens existing extend callback.\n"
        "- Restricted Device trailing disclosure opens existing details callback.\n"
        "- Active Protection tag click navigates to Devices route with tag id.\n"
        "- DevicesScreen supports optional tag-scoped filtering.\n"
        "- No Material icon import added.\n"
        "- No old Cupertino namespace added.\n",
        encoding="utf-8",
    )
    print(f"PASS: Batch 015 verification passed. Report: {REPORT}")

def main() -> None:
    patch_home()
    patch_navigation_routes()
    patch_nav_host()
    patch_devices()
    verify()

if __name__ == "__main__":
    main()
