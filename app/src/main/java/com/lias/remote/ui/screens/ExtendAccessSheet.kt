// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt
// Version: 35.5.2
//
// Purpose:
//   Minimal Cupertino temporary-access duration picker.
//
// Plan 3.5 refinement:
//   - Compact sheet, matching the lightweight Pause presentation.
//   - One Slanoss CupertinoWheelPicker replaces presets + slider.
//   - Cancel is leading and Apply is trailing in the sheet header.
//   - No duplicate bottom primary action.
//   - Active extension state remains visible but concise.
//   - Cancel Extended Access remains a destructive secondary action.
//
// Backend contract preserved:
//   - LIAS accepts duration_minutes in 1..120.
//   - This UI deliberately offers 5-minute increments from 5..120.
//   - Default selection remains 30 minutes.
//   - Submitting again replaces/refreshes the server timer.
//   - Only reason_tag == extend_access is treated as an extension.
// ====================================================================

package com.lias.remote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.ExtensionInfo
import com.lias.remote.core.models.TemporaryAccessKind
import com.lias.remote.core.models.kind
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetContentInteraction
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigSheetPresentation
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.formatTemporaryDuration
import com.lias.remote.ui.components.rememberHigAnimatedCompletion
import com.lias.remote.ui.components.rememberHigImmediateCompletion
import com.lias.remote.ui.components.rememberTemporaryMinutesLeft
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.CupertinoWheelPicker
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.rememberCupertinoPickerState

private const val EXTEND_MIN_MINUTES =
    5

private const val EXTEND_MAX_MINUTES =
    120

private const val EXTEND_STEP_MINUTES =
    5

private const val EXTEND_DEFAULT_MINUTES =
    30

private val EXTEND_PICKER_HEIGHT =
    160.dp

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun ExtendAccessSheet(
    targetLabel: String,
    targetSubtitle: String,
    currentExtension: ExtensionInfo?,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit,
    onCancelExtension: (() -> Unit)? = null
) {
    val durationOptions =
        remember {
            (
                EXTEND_MIN_MINUTES..EXTEND_MAX_MINUTES step
                    EXTEND_STEP_MINUTES
            ).toList()
        }

    val defaultIndex =
        remember(
            durationOptions
        ) {
            durationOptions
                .indexOf(
                    EXTEND_DEFAULT_MINUTES
                )
                .coerceAtLeast(
                    0
                )
        }

    val pickerState =
        rememberCupertinoPickerState(
            infinite =
                false,
            initiallySelectedItemIndex =
                defaultIndex
        )

    val selectedIndex =
        pickerState
            .selectedItemIndex(
                durationOptions.size
            )
            .coerceIn(
                durationOptions.indices
            )

    val selectedMinutes =
        durationOptions[
            selectedIndex
        ]

    val actualExtension =
        currentExtension
            ?.takeIf {
                it.kind ==
                    TemporaryAccessKind.EXTEND
            }

    val minutesLeft =
        rememberTemporaryMinutesLeft(
            actualExtension
        )

    HigModalSheet(
        presentation =
            HigSheetPresentation.Compact,
        contentInteraction =
            HigSheetContentInteraction.ScrollContent,
        onDismiss =
            onDismiss,
        accessibilityLabel =
            "Extend Access"
    ) {
        val immediateComplete =
            rememberHigImmediateCompletion(
                fallbackDismiss =
                    onDismiss
            )

        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            24.dp,
                        vertical =
                            12.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            HigSheetHeader(
                title =
                    "Extend Access",
                onCancel =
                    onDismiss,
                trailingAction = {
                    HigTextButton(
                        text =
                            "Apply",
                        onClick = {
                            immediateComplete {
                                onConfirm(
                                    selectedMinutes
                                )
                            }
                        }
                    )
                }
            )

            CupertinoText(
                text =
                    targetLabel,
                style =
                    HigTypography.title2,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    LiasThemeColors.label,
                textAlign =
                    TextAlign.Center
            )

            if (
                targetSubtitle
                    .isNotBlank()
            ) {
                CupertinoText(
                    text =
                        targetSubtitle,
                    style =
                        HigTypography.subheadline,
                    color =
                        LiasThemeColors.secondaryLabel,
                    textAlign =
                        TextAlign.Center
                )
            }

            if (
                actualExtension !=
                    null
            ) {
                CupertinoText(
                    text =
                        when {
                            minutesLeft ==
                                null ->
                                "Extended access is active"

                            minutesLeft <=
                                0 ->
                                "Extended access is ending"

                            else ->
                                "${
                                    formatTemporaryDuration(
                                        minutesLeft
                                    )
                                } remaining"
                        },
                    style =
                        HigTypography.headline,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        LiasThemeColors.green,
                    textAlign =
                        TextAlign.Center
                )

                CupertinoText(
                    text =
                        "Applying a new duration replaces the current LIAS timer.",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.secondaryLabel,
                    textAlign =
                        TextAlign.Center
                )
            }

            CupertinoWheelPicker(
                state =
                    pickerState,
                items =
                    durationOptions,
                height =
                    EXTEND_PICKER_HEIGHT,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                    minutes ->
                CupertinoText(
                    text =
                        formatTemporaryDuration(
                            minutes
                        ),
                    style =
                        HigTypography.title2,
                    fontWeight =
                        FontWeight.Medium,
                    color =
                        LiasThemeColors.label,
                    textAlign =
                        TextAlign.Center
                )
            }

            CupertinoText(
                text =
                    "LIAS keeps this timer active when the app is closed.",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel,
                textAlign =
                    TextAlign.Center
            )

            if (
                actualExtension !=
                    null &&
                onCancelExtension !=
                    null
            ) {
                HigTextButton(
                    text =
                        "Cancel Extended Access",
                    onClick = {
                        animatedComplete {
                            onCancelExtension()
                        }
                    },
                    isDestructive =
                        true
                )
            }
        }
    }
}
