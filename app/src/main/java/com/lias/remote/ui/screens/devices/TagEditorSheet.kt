// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/TagEditorSheet.kt
// Version: 27.2.0
//
// Purpose:
//   Create/edit custom LIAS tags.
//
// Corrections:
//   - New tag ID is NOT fabricated by Android.
//   - LIAS derives the canonical custom ID from the name.
//   - Built-in tags are visibly read-only.
//   - Custom tag deletion is dependency-aware.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Tag
import com.lias.remote.core.util.TagDependencyImpact
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigConfiguredField
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.TagDeleteSheet
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoText

@Composable
fun TagEditorSheet(
    initialTag: Tag?,
    dependencyImpact: TagDependencyImpact? = null,
    onDismiss: () -> Unit,
    onSave: (Tag) -> Unit,
    onDelete: (() -> Unit)? = null
) {

    val existing =
        initialTag != null

    val readOnly =
        initialTag?.builtin ==
            true

    var name by
        remember(
            initialTag
        ) {
            mutableStateOf(
                initialTag
                    ?.name
                    .orEmpty()
            )
        }

    var selectedColor by
        remember(
            initialTag
        ) {
            mutableStateOf(
                initialTag
                    ?.color
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "#0A84FF"
            )
        }

    var showDeleteSheet by
        remember {
            mutableStateOf(
                false
            )
        }

    val presetColors =
        listOf(
            "#0A84FF",
            "#5856D6",
            "#FF9500",
            "#FF2D55",
            "#00C7BE",
            "#30D158",
            "#FFCC00",
            "#8E8E93"
        )

    val normalizedName =
        name.trim()

    val changed =
        if (
            initialTag == null
        ) {
            normalizedName.isNotBlank()
        } else {
            normalizedName !=
                initialTag.name ||
                !selectedColor.equals(
                    initialTag.color,
                    ignoreCase = true
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

            HigSheetHeader(
                title =
                    when {
                        !existing ->
                            "New Tag"

                        readOnly ->
                            "System Tag"

                        else ->
                            "Edit Tag"
                    },
                onCancel =
                    onDismiss,
                trailingAction = {

                    if (
                        !readOnly
                    ) {
                        HigButton(
                            text = "Save",
                            onClick = {

                                if (
                                    normalizedName.isBlank()
                                ) {
                                    return@HigButton
                                }

                                onSave(
                                    Tag(
                                        /*
                                         * For create, the submitted ID
                                         * is deliberately empty.
                                         *
                                         * LIAS tagMgr.Create derives
                                         * the canonical ID from Name.
                                         */
                                        id =
                                            initialTag
                                                ?.id
                                                .orEmpty(),
                                        name =
                                            normalizedName,
                                        color =
                                            selectedColor,
                                        precedence =
                                            initialTag
                                                ?.precedence
                                                ?: 50,
                                        builtin =
                                            initialTag
                                                ?.builtin
                                                ?: false
                                    )
                                )
                            },
                            enabled =
                                changed &&
                                    normalizedName
                                        .isNotBlank(),
                            style =
                                HigButtonStyle.Primary
                        )
                    }
                }
            )

            if (
                readOnly
            ) {

                CupertinoText(
                    text =
                        "This is a built-in LIAS classification. Its identity, name and color are managed by the server.",
                    style =
                        HigTypography.subheadline,
                    color =
                        LiasThemeColors.secondaryLabel
                )
            }

            HigConfiguredField(
                value =
                    name,
                onValueChange = {
                    if (
                        !readOnly
                    ) {
                        name = it
                    }
                },
                label =
                    "Tag Name",
                placeholder =
                    "e.g. Nursery",
                keyboardOptions =
                    KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
            )

            Column(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                CupertinoText(
                    text =
                        "BADGE COLOR",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.tertiaryLabel,
                    modifier =
                        Modifier.padding(
                            bottom = 8.dp
                        )
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    presetColors.forEach {
                            colorHex ->

                        val selected =
                            selectedColor.equals(
                                colorHex,
                                ignoreCase = true
                            )

                        val color =
                            try {
                                Color(
                                    android.graphics.Color
                                        .parseColor(
                                            colorHex
                                        )
                                )
                            } catch (
                                _: Exception
                            ) {
                                Color.Gray
                            }

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        36.dp
                                    )
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        color
                                    )
                                    .then(
                                        if (
                                            selected
                                        ) {
                                            Modifier.border(
                                                width = 2.dp,
                                                color =
                                                    LiasThemeColors.label,
                                                shape =
                                                    CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable(
                                        enabled =
                                            !readOnly
                                    ) {
                                        selectedColor =
                                            colorHex
                                    }
                        )
                    }
                }
            }

            if (
                existing &&
                !readOnly &&
                dependencyImpact != null &&
                onDelete != null
            ) {

                CupertinoText(
                    text =
                        dependencyImpact.summary,
                    style =
                        HigTypography.caption,
                    color =
                        if (
                            dependencyImpact.canDeleteSafely
                        ) {
                            LiasThemeColors.secondaryLabel
                        } else {
                            LiasThemeColors.orange
                        }
                )

                HigButton(
                    text =
                        if (
                            dependencyImpact.canDeleteSafely
                        ) {
                            "Delete Tag"
                        } else {
                            "Review Delete Dependencies"
                        },
                    onClick = {
                        showDeleteSheet =
                            true
                    },
                    style =
                        if (
                            dependencyImpact.canDeleteSafely
                        ) {
                            HigButtonStyle.Danger
                        } else {
                            HigButtonStyle.Gray
                        },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (
        showDeleteSheet &&
        dependencyImpact != null
    ) {

        TagDeleteSheet(
            impact =
                dependencyImpact,
            onDismiss = {
                showDeleteSheet =
                    false
            },
            onDelete = {

                showDeleteSheet =
                    false

                onDelete?.invoke()
            }
        )
    }
}
