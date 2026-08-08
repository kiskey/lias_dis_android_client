// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt
// Version: 19.0.0
//
// Purpose:
//   Safe multi-tag classification editor.
//
// Batch 19:
//   - Clear multi-tag semantics.
//   - generic disappears when meaningful tags exist.
//   - infrastructure cannot be granted or removed here.
//   - Explicitly explains that all selected tags affect policies even
//     though Devices screen groups each device only once.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.device.DevicePresentation
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.core.util.ConfigurationSafety
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Checkmark
import io.github.alexzhirkevich.cupertino.icons.outlined.Lock

@Composable
fun MoveTagSheet(
    device: Device,
    allTags: List<Tag>,
    onDismiss: () -> Unit,
    onConfirm: (tagIds: List<String>) -> Unit
) {

    val originalTags =
        remember(
            device
        ) {
            DevicePresentation
                .normalizedTagIds(
                    device
                )
        }

    val infrastructureDevice =
        ConfigurationSafety
            .INFRASTRUCTURE_TAG_ID in
            originalTags

    val selected =
        remember(
            device
        ) {

            mutableStateListOf<String>()
                .apply {
                    addAll(
                        originalTags
                    )
                }
        }

    val sortedTags =
        remember(
            allTags
        ) {

            allTags.sortedWith(
                compareByDescending<Tag> {
                    it.precedence
                }
                    .thenBy {
                        it.name.lowercase()
                    }
            )
        }

    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 16.dp
                    )
                    .verticalScroll(
                        rememberScrollState()
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                HigTextButton(
                    text =
                        "Cancel",
                    onClick =
                        onDismiss
                )

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    CupertinoText(
                        text =
                            "Device Tags",
                        style =
                            HigTypography.headline,
                        fontWeight =
                            FontWeight.Bold
                    )

                    CupertinoText(
                        text =
                            device.displayName,
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors.tertiaryLabel
                    )
                }

                HigTextButton(
                    text =
                        "Done",
                    onClick = {

                        val finalTags =
                            selected
                                .filter {
                                    it.isNotBlank()
                                }
                                .distinct()
                                .toMutableList()

                        if (
                            infrastructureDevice &&
                            ConfigurationSafety
                                .INFRASTRUCTURE_TAG_ID !in
                            finalTags
                        ) {
                            finalTags.add(
                                ConfigurationSafety
                                    .INFRASTRUCTURE_TAG_ID
                            )
                        }

                        if (
                            finalTags.size >
                            1
                        ) {
                            finalTags.remove(
                                ConfigurationSafety
                                    .GENERIC_TAG_ID
                            )
                        }

                        if (
                            finalTags.isEmpty()
                        ) {
                            finalTags.add(
                                ConfigurationSafety
                                    .GENERIC_TAG_ID
                            )
                        }

                        onConfirm(
                            finalTags
                        )
                    }
                )
            }

            CupertinoText(
                text =
                    if (
                        infrastructureDevice
                    ) {
                        "Infrastructure protection is locked. You may change other classifications, but this device remains always online."
                    } else {
                        "A device can belong to several groups. Every selected tag can participate in LIAS policy evaluation."
                    },
                style =
                    HigTypography.subheadline,
                color =
                    LiasThemeColors.secondaryLabel
            )

            CupertinoText(
                text =
                    "The Devices screen displays a multi-tag device once under its highest-precedence group; its other tags are still active.",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )

            GroupedListCard {

                sortedTags
                    .forEachIndexed {
                            index,
                            tag ->

                        val isInfrastructure =
                            tag.id ==
                                ConfigurationSafety
                                    .INFRASTRUCTURE_TAG_ID

                        val checked =
                            tag.id in
                                selected

                        val color =
                            try {

                                Color(
                                    android.graphics.Color
                                        .parseColor(
                                            tag.color
                                        )
                                )

                            } catch (
                                _: Exception
                            ) {
                                Color.Gray
                            }

                        GroupedListRow(
                            primaryText =
                                tag.name,
                            secondaryText =
                                when {

                                    isInfrastructure &&
                                        infrastructureDevice ->
                                        "Protected · Always online"

                                    isInfrastructure ->
                                        "Protected system classification"

                                    tag.builtin ->
                                        "Built-in classification"

                                    else ->
                                        "Custom classification"
                                },
                            leadingContent = {

                                Box(
                                    modifier =
                                        Modifier
                                            .size(
                                                12.dp
                                            )
                                            .clip(
                                                CircleShape
                                            )
                                            .background(
                                                color
                                            )
                                )
                            },
                            trailingContent = {

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    if (
                                        isInfrastructure
                                    ) {

                                        CupertinoIcon(
                                            imageVector =
                                                CupertinoIcons
                                                    .Outlined
                                                    .Lock,
                                            contentDescription =
                                                "Protected infrastructure",
                                            tint =
                                                LiasThemeColors
                                                    .tertiaryLabel,
                                            modifier =
                                                Modifier.size(
                                                    16.dp
                                                )
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.size(
                                                    6.dp
                                                )
                                        )
                                    }

                                    if (
                                        checked
                                    ) {

                                        CupertinoIcon(
                                            imageVector =
                                                CupertinoIcons
                                                    .Outlined
                                                    .Checkmark,
                                            contentDescription =
                                                "Selected",
                                            tint =
                                                if (
                                                    isInfrastructure
                                                ) {
                                                    LiasThemeColors
                                                        .secondaryLabel
                                                } else {
                                                    LiasThemeColors.blue
                                                }
                                        )
                                    }
                                }
                            },
                            showDivider =
                                index <
                                    sortedTags.lastIndex,
                            onClick =
                                if (
                                    isInfrastructure
                                ) {
                                    null
                                } else {
                                    {

                                        if (
                                            tag.id ==
                                            ConfigurationSafety
                                                .GENERIC_TAG_ID
                                        ) {

                                            selected.clear()

                                            if (
                                                infrastructureDevice
                                            ) {
                                                selected.add(
                                                    ConfigurationSafety
                                                        .INFRASTRUCTURE_TAG_ID
                                                )
                                            } else {
                                                selected.add(
                                                    ConfigurationSafety
                                                        .GENERIC_TAG_ID
                                                )
                                            }

                                        } else {

                                            selected.remove(
                                                ConfigurationSafety
                                                    .GENERIC_TAG_ID
                                            )

                                            if (
                                                tag.id in
                                                selected
                                            ) {
                                                selected.remove(
                                                    tag.id
                                                )
                                            } else {
                                                selected.add(
                                                    tag.id
                                                )
                                            }

                                            if (
                                                selected.isEmpty()
                                            ) {
                                                selected.add(
                                                    ConfigurationSafety
                                                        .GENERIC_TAG_ID
                                                )
                                            }
                                        }
                                    }
                                }
                        )
                    }
            }
        }
    }
}
