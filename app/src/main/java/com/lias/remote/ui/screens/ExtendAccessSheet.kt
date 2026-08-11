// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/ExtendAccessSheet.kt
// Version: 11.0.0
//
// Purpose:
//   Device/tag temporary Allow editor.
//
// Backend contract:
//   - Duration range: 1..120 minutes.
//   - Submitting again replaces/refreshes the temporary extension.
//   - Current expiration is authoritative from LIAS.
//   - Only reason_tag == extend_access is treated as an extension.
//
// This sheet deliberately does NOT treat a pause's active_extension
// object as an access extension.
// ====================================================================

package com.lias.remote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.ExtensionInfo
import com.lias.remote.core.models.TemporaryAccessKind
import com.lias.remote.core.models.kind
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.rememberHigAnimatedCompletion
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.formatTemporaryDuration
import com.lias.remote.ui.components.rememberTemporaryMinutesLeft
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoSlider
import com.slapps.cupertino.CupertinoText

@Composable
fun ExtendAccessSheet(
    targetLabel: String,
    targetSubtitle: String,
    currentExtension: ExtensionInfo?,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit,
    onCancelExtension: (() -> Unit)? = null
) {

    var selectedMinutes by
        remember {
            mutableFloatStateOf(
                30f
            )
        }

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

    val quickPicks =
        listOf(
            15,
            30,
            60,
            120
        )

    HigModalSheet(
        onDismiss =
            onDismiss
    ) {

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
                            16.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            HigSheetHeader(
                title =
                    if (
                        actualExtension !=
                        null
                    ) {
                        "Extended Access"
                    } else {
                        "Extend Access"
                    },
                onCancel =
                    onDismiss
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
                            minutesLeft == null ->
                                "Temporary access active"

                            minutesLeft <= 0 ->
                                "Temporary access ending"

                            else ->
                                "${formatTemporaryDuration(minutesLeft)} remaining"
                        },
                    style =
                        HigTypography.headline,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        LiasThemeColors.green
                )

                CupertinoText(
                    text =
                        "Choosing another duration replaces the current extension with a new server-managed timer.",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.secondaryLabel,
                    textAlign =
                        TextAlign.Center
                )
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                quickPicks.forEach { pick ->

                    val selected =
                        selectedMinutes
                            .toInt() ==
                            pick

                    HigButton(
                        text =
                            when (pick) {
                                60 ->
                                    "1h"

                                120 ->
                                    "2h"

                                else ->
                                    "${pick}m"
                            },
                        onClick = {
                            selectedMinutes =
                                pick.toFloat()
                        },
                        style =
                            if (selected) {
                                HigButtonStyle.Primary
                            } else {
                                HigButtonStyle.Gray
                            },
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }
            }

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                CupertinoText(
                    text =
                        formatTemporaryDuration(
                            selectedMinutes
                                .toInt()
                        ),
                    style =
                        HigTypography.title1,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        LiasThemeColors.label
                )

                CupertinoSlider(
                    value =
                        selectedMinutes,
                    onValueChange = {
                        selectedMinutes =
                            it
                    },
                    valueRange =
                        1f..120f,
                    steps =
                        118,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    8.dp
                            )
                )
            }

            CupertinoText(
                text =
                    "The timer is maintained by LIAS and continues when this app is closed.",
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel,
                textAlign =
                    TextAlign.Center
            )

            HigButton(
                text =
                    if (
                        actualExtension !=
                        null
                    ) {
                        "Set to ${
                            formatTemporaryDuration(
                                selectedMinutes.toInt()
                            )
                        }"
                    } else {
                        "Allow for ${
                            formatTemporaryDuration(
                                selectedMinutes.toInt()
                            )
                        }"
                    },
                onClick = {
                    val minutes =
                        selectedMinutes
                            .toInt()
                            .coerceIn(
                                1,
                                120
                            )

                    animatedComplete {
                        onConfirm(
                            minutes
                        )
                    }
                },
                style =
                    HigButtonStyle.Primary,
                modifier =
                    Modifier.fillMaxWidth()
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
