#!/usr/bin/env python3
from __future__ import annotations
from pathlib import Path
ROOT = Path.cwd()
FILE = ROOT / "app/src/main/java/com/lias/remote/ui/screens/devices/DevicesScreen.kt"
REPORT = ROOT / "build/plan31/batch029_devices_card_ux_report.md"
def read(): return FILE.read_text(encoding="utf-8")
def write(t): FILE.write_text(t, encoding="utf-8")
def replace_once(text, old, new, label):
    c = text.count(old)
    if c != 1:
        raise SystemExit(f"ERROR: expected one match for {label}, found {c}")
    return text.replace(old, new, 1)
if not FILE.exists():
    raise SystemExit(f"ERROR: missing {FILE.relative_to(ROOT)}")
text = read()
if "Plan 3.1 Devices card UX" not in text:
    text = text.replace("//   Canonical device inventory.\n//", "//   Canonical device inventory.\n//\n// Plan 3.1 Devices card UX:\n//   - Card tap opens current-state Extend/Manage or Pause modal.\n//   - Details button becomes trailing disclosure.\n//   - Home tag deep-link uses tag scope without prefilled text filter.\n//", 1)
if "import androidx.compose.foundation.clickable" not in text:
    text = text.replace("import androidx.compose.foundation.border\n", "import androidx.compose.foundation.border\nimport androidx.compose.foundation.clickable\n", 1)
if "import androidx.compose.ui.semantics.Role" not in text:
    text = text.replace("import androidx.compose.ui.Modifier\n", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.semantics.Role\n", 1)
old_search = '''    var searchQuery by
        remember(
            initialTag?.name
        ) {
            mutableStateOf(
                initialTag
                    ?.name
                    .orEmpty()
            )
        }
'''
new_search = '''    var searchQuery by
        remember(
            initialTagId
        ) {
            mutableStateOf("")
        }
'''
if old_search in text:
    text = replace_once(text, old_search, new_search, "initial tag search state")
elif 'remember(\n            initialTagId\n        ) {\n            mutableStateOf("")' not in text:
    raise SystemExit("ERROR: searchQuery block shape unsupported")
old_call = '''                    DeviceCardItem(
                        device =
                            device,
                        presentation =
                            presentation,
                        onExtend = {
                            activeDeviceForExtend =
                                device
                        },
                        onPause = {
                            activeDeviceForPause =
                                device
                        },
                        onResume = {
                            viewModel
                                .unpauseDeviceInternet(
                                    device.pdid
                                )
                        },
                        onDetail = {
                            onNavigateToDeviceDetail(
                                device.pdid
                            )
                        }
                    )
'''
new_call = '''                    DeviceCardItem(
                        device =
                            device,
                        presentation =
                            presentation,
                        onPrimaryAction = {
                            when {
                                presentation.canManageExtension ||
                                    presentation.canExtend -> {
                                    activeDeviceForExtend =
                                        device
                                }

                                presentation.canPause -> {
                                    activeDeviceForPause =
                                        device
                                }
                            }
                        },
                        onResume = {
                            viewModel
                                .unpauseDeviceInternet(
                                    device.pdid
                                )
                        },
                        onDetail = {
                            onNavigateToDeviceDetail(
                                device.pdid
                            )
                        }
                    )
'''
if old_call in text:
    text = replace_once(text, old_call, new_call, "DeviceCardItem call")
elif "onPrimaryAction =" not in text:
    raise SystemExit("ERROR: DeviceCardItem call shape unsupported")
start = text.find("@Composable\nprivate fun DeviceCardItem(")
end = text.find("\nprivate fun buildDeviceSections(", start)
if start < 0 or end < 0:
    raise SystemExit("ERROR: could not locate DeviceCardItem function")
new_function = '''@Composable
private fun DeviceCardItem(
    device: Device,
    presentation: AccessPresentation,
    onPrimaryAction: () -> Unit,
    onResume: () -> Unit,
    onDetail: () -> Unit
) {

    val hasModalPrimaryAction =
        presentation.canManageExtension ||
            presentation.canExtend ||
            presentation.canPause

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp,
                    vertical =
                        4.dp
                )
                .background(
                    color =
                        LiasThemeColors
                            .secondaryBackground,
                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
                )
                .border(
                    width =
                        0.5.dp,
                    color =
                        LiasThemeColors.separator,
                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
                )
                .then(
                    if (hasModalPrimaryAction) {
                        Modifier.clickable(
                            role =
                                Role.Button,
                            onClick =
                                onPrimaryAction
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(
                    14.dp
                )
    ) {

        Column {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.Top
            ) {

                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        StatusDot(
                            isOnline =
                                device.online,
                            isPaused =
                                presentation.isPaused
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )

                        CupertinoText(
                            text =
                                device.displayName,
                            style =
                                HigTypography.headline,
                            color =
                                LiasThemeColors.label
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                2.dp
                            )
                    )

                    CupertinoText(
                        text =
                            listOfNotNull(
                                device.currentIP
                                    .takeIf {
                                        it.isNotBlank()
                                    },
                                device.vendor
                                    .takeIf {
                                        it.isNotBlank()
                                    }
                            )
                                .joinToString(
                                    " · "
                                )
                                .ifBlank {
                                    "No network details"
                                },
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors
                                .tertiaryLabel
                    )
                }

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    StatusPill(
                        text =
                            presentation.label,
                        tone =
                            presentation.tone
                    )

                    CupertinoText(
                        text = "›",
                        style =
                            HigTypography.title3,
                        color =
                            LiasThemeColors
                                .tertiaryLabel,
                        modifier =
                            Modifier
                                .clickable(
                                    role =
                                        Role.Button,
                                    onClick =
                                        onDetail
                                )
                                .padding(
                                    horizontal =
                                        6.dp,
                                    vertical =
                                        4.dp
                                )
                    )
                }
            }

            if (
                presentation.canResumePause
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                HigButton(
                    text =
                        "Resume",
                    onClick =
                        onResume,
                    style =
                        HigButtonStyle.Primary,
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        }
    }
}
'''
text = text[:start] + new_function + text[end:]
write(text)
text = read()
device_card = text[text.find("private fun DeviceCardItem("):]
problems = []
for token in ["Plan 3.1 Devices card UX", "import androidx.compose.foundation.clickable", "import androidx.compose.ui.semantics.Role", "onPrimaryAction", "presentation.canManageExtension", "presentation.canExtend", "presentation.canPause", 'text = "›"', '"Resume"']:
    if token not in text:
        problems.append(f"missing {token}")
for forbidden in ['"Extend Access"', '"Manage Access"', 'text =\n                                "Pause"', 'text =\n                            "Details"', 'text =\n                                "Details"']:
    if forbidden in device_card:
        problems.append(f"forbidden visible action remains in DeviceCardItem: {forbidden}")
if 'initialTag\n                    ?.name' in text:
    problems.append("searchQuery still initializes to initial tag name")
REPORT.parent.mkdir(parents=True, exist_ok=True)
if problems:
    REPORT.write_text("# Batch 029 failed\n\n" + "\n".join(f"- {p}" for p in problems) + "\n", encoding="utf-8")
    raise SystemExit(f"ERROR: Batch 029 failed. See {REPORT}")
REPORT.write_text("# Batch 029 passed\n\n- Devices card-level click now opens dynamic Extend/Manage/Pause modal.\n- Details button replaced with trailing disclosure text `›`.\n- Resume remains explicit for paused devices because it is immediate mutation.\n- Home tag deep-link search starts blank; tag scoping remains selectedTagId-based.\n", encoding="utf-8")
print(f"PASS: Batch 029 applied. Report: {REPORT}")
