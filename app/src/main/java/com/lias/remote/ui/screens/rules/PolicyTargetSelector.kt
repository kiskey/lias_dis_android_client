// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/rules/PolicyTargetSelector.kt
// Version: 9.0.0
//
// Purpose:
//   HIG-style target selection for a LIAS policy.
//
// Safety:
//   - infrastructure tag is never selectable.
//   - devices carrying infrastructure are never selectable.
//   - global scope is not offered for new rules.
// ====================================================================

package com.lias.remote.ui.screens.rules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun PolicyTargetSelector(
    type: String,
    selectedTargetId: String,
    tags: List<Tag>,
    devices: List<Device>,
    onTargetSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier.fillMaxWidth()
    ) {

        when (
            type.lowercase()
        ) {

            "global" -> {
                GroupedListCard {
                    GroupedListRow(
                        primaryText =
                            "All Devices",
                        secondaryText =
                            "Infrastructure devices remain immune.",
                        trailingContent = {
                            CupertinoText(
                                text =
                                    "Global",
                                style =
                                    HigTypography.subheadline,
                                color =
                                    LiasThemeColors.blue,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    )
                }
            }

            "tag" -> {

                val selectableTags =
                    tags
                        .filterNot {
                            it.id ==
                                "infrastructure"
                        }
                        .sortedBy {
                            it.name.lowercase()
                        }

                if (
                    selectableTags.isEmpty()
                ) {
                    GroupedListCard {
                        GroupedListRow(
                            primaryText =
                                "No Available Tags",
                            secondaryText =
                                "Create a tag from Devices before adding a tag rule."
                        )
                    }
                } else {
                    GroupedListCard {

                        selectableTags
                            .forEachIndexed { index, tag ->

                                val selected =
                                    tag.id ==
                                        selectedTargetId

                                GroupedListRow(
                                    primaryText =
                                        tag.name,
                                    secondaryText =
                                        tag.id,
                                    trailingContent = {
                                        if (selected) {
                                            CupertinoText(
                                                text =
                                                    "✓",
                                                style =
                                                    HigTypography.headline,
                                                color =
                                                    LiasThemeColors.blue,
                                                fontWeight =
                                                    FontWeight.Bold
                                            )
                                        }
                                    },
                                    showDivider =
                                        index <
                                            selectableTags.lastIndex,
                                    onClick = {
                                        onTargetSelected(
                                            tag.id
                                        )
                                    }
                                )
                            }
                    }
                }
            }

            "device" -> {

                val selectableDevices =
                    devices
                        .filterNot {
                            it.safeTags
                                .contains(
                                    "infrastructure"
                                )
                        }
                        .sortedWith(
                            compareByDescending<Device> {
                                it.online
                            }.thenBy {
                                it.displayName
                                    .lowercase()
                            }
                        )

                if (
                    selectableDevices.isEmpty()
                ) {
                    GroupedListCard {
                        GroupedListRow(
                            primaryText =
                                "No Available Devices",
                            secondaryText =
                                "No non-infrastructure devices are available."
                        )
                    }
                } else {
                    GroupedListCard {

                        selectableDevices
                            .forEachIndexed { index, device ->

                                val selected =
                                    device.pdid ==
                                        selectedTargetId

                                val detail =
                                    buildString {

                                        if (
                                            device.online
                                        ) {
                                            append(
                                                "Online"
                                            )
                                        } else {
                                            append(
                                                "Offline"
                                            )
                                        }

                                        if (
                                            device.currentIP
                                                .isNotBlank()
                                        ) {
                                            append(
                                                " · "
                                            )
                                            append(
                                                device.currentIP
                                            )
                                        }

                                        if (
                                            device.vendor
                                                .isNotBlank()
                                        ) {
                                            append(
                                                " · "
                                            )
                                            append(
                                                device.vendor
                                            )
                                        }
                                    }

                                GroupedListRow(
                                    primaryText =
                                        device.displayName,
                                    secondaryText =
                                        detail,
                                    trailingContent = {
                                        if (selected) {
                                            CupertinoText(
                                                text =
                                                    "✓",
                                                style =
                                                    HigTypography.headline,
                                                color =
                                                    LiasThemeColors.blue,
                                                fontWeight =
                                                    FontWeight.Bold
                                            )
                                        }
                                    },
                                    showDivider =
                                        index <
                                            selectableDevices.lastIndex,
                                    onClick = {
                                        onTargetSelected(
                                            device.pdid
                                        )
                                    }
                                )
                            }
                    }
                }
            }
        }
    }
}
