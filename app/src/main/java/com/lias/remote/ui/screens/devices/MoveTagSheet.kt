// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/MoveTagSheet.kt
// Version: 15.0.0
//
// Purpose:
//   Safe multi-tag assignment sheet.
//
// Critical fix:
//   The previous UI visually showed a lock beside infrastructure but
//   its row remained clickable and could actually toggle the tag.
//
// New behavior:
//   - Existing infrastructure assignment is displayed but immutable.
//   - A normal device cannot grant itself infrastructure immunity.
//   - generic is mutually exclusive with meaningful tags.
//   - Empty normal selection becomes generic.
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
            device.safeTags
                .ifEmpty {
                    listOf(
                        ConfigurationSafety
                            .GENERIC_TAG_ID
                    )
                }
        }

    val isInfrastructureDevice =
        ConfigurationSafety
            .INFRASTRUCTURE_TAG_ID in
            originalTags

    val selectedTagIds =
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
                    it.id ==
                        ConfigurationSafety
                            .INFRASTRUCTURE_TAG_ID
                }.thenByDescending {
                    it.builtin
                }.thenBy {
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
                        horizontal =
                            24.dp,
                        vertical =
                            16.dp
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
                            LiasThemeColors
                                .tertiaryLabel
                    )
                }

                HigTextButton(
                    text =
                        "Done",
                    onClick = {

                        val mutable =
                            selectedTagIds
                                .toMutableList()

                        if (
                            isInfrastructureDevice &&
                            ConfigurationSafety
                                .INFRASTRUCTURE_TAG_ID !in
                            mutable
                        ) {
                            /*
                             * Defensive final preservation even though
                             * the row itself cannot be toggled.
                             */
                            mutable.add(
                                ConfigurationSafety
                                    .INFRASTRUCTURE_TAG_ID
                            )
                        }

                        if (
                            mutable.isEmpty()
                        ) {
                            mutable.add(
                                ConfigurationSafety
                                    .GENERIC_TAG_ID
                            )
                        }

                        if (
                            mutable.size >
                                1
                        ) {
                            mutable.remove(
                                ConfigurationSafety
                                    .GENERIC_TAG_ID
                            )
                        }

                        onConfirm(
                            mutable.distinct()
                        )
                    }
                )
            }

            if (
                isInfrastructureDevice
            ) {

                CupertinoText(
                    text =
                        "This device is protected infrastructure. Infrastructure immunity cannot be removed here.",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors
                            .secondaryLabel
                )

            } else {

                CupertinoText(
                    text =
                        "Choose one or more groups. Infrastructure immunity is managed separately from normal device classification.",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors
                            .secondaryLabel
                )
            }

            CupertinoText(
                text =
                    "TAG GROUPS",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors
                        .tertiaryLabel
            )

            GroupedListCard {

                sortedTags
                    .forEachIndexed {
                            index,
                            tag ->

                        val infrastructure =
                            tag.id ==
                                ConfigurationSafety
                                    .INFRASTRUCTURE_TAG_ID

                        val selected =
                            selectedTagIds
                                .contains(
                                    tag.id
                                )

                        val selectable =
                            !infrastructure

                        val tagColor =
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

                                    infrastructure &&
                                        isInfrastructureDevice ->
                                        "Protected · Always online"

                                    infrastructure ->
                                        "Protected system classification"

                                    tag.builtin ->
                                        "Built-in system tag"

                                    else ->
                                        "Custom tag"
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
                                                tagColor
                                            )
                                )
                            },
                            trailingContent = {

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    if (
                                        infrastructure
                                    ) {

                                        CupertinoIcon(
                                            imageVector =
                                                CupertinoIcons
                                                    .Outlined
                                                    .Lock,
                                            contentDescription =
                                                "Protected",
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
                                                    7.dp
                                                )
                                        )
                                    }

                                    if (
                                        selected
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
                                                    infrastructure
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
                                    sortedTags
                                        .lastIndex,
                            onClick =
                                if (
                                    selectable
                                ) {
                                    {

                                        if (
                                            tag.id ==
                                            ConfigurationSafety
                                                .GENERIC_TAG_ID
                                        ) {

                                            selectedTagIds
                                                .clear()

                                            selectedTagIds
                                                .add(
                                                    ConfigurationSafety
                                                        .GENERIC_TAG_ID
                                                )

                                        } else {

                                            selectedTagIds
                                                .remove(
                                                    ConfigurationSafety
                                                        .GENERIC_TAG_ID
                                                )

                                            if (
                                                selected
                                            ) {
                                                selectedTagIds
                                                    .remove(
                                                        tag.id
                                                    )
                                            } else {
                                                selectedTagIds
                                                    .add(
                                                        tag.id
                                                    )
                                            }

                                            if (
                                                selectedTagIds
                                                    .isEmpty()
                                            ) {
                                                selectedTagIds
                                                    .add(
                                                        ConfigurationSafety
                                                            .GENERIC_TAG_ID
                                                    )
                                            }

                                            /*
                                             * Infrastructure remains
                                             * untouched for existing
                                             * infrastructure devices.
                                             */
                                            if (
                                                isInfrastructureDevice &&
                                                ConfigurationSafety
                                                    .INFRASTRUCTURE_TAG_ID !in
                                                selectedTagIds
                                            ) {
                                                selectedTagIds
                                                    .add(
                                                        ConfigurationSafety
                                                            .INFRASTRUCTURE_TAG_ID
                                                    )
                                            }
                                        }
                                    }
                                } else {
                                    null
                                }
                        )
                    }
            }

            CupertinoText(
                text =
                    "Tags can affect multiple LIAS rules. Effective access is recalculated by the server after changes.",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors
                        .tertiaryLabel
            )
        }
    }
}
