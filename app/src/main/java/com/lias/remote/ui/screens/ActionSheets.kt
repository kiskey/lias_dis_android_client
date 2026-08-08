// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt
// Version: 26.0.0
//
// Purpose:
//   Shared non-device-specific modal actions.
//
// Batch 26:
//   - Removes the obsolete duplicate PauseSheet implementation.
//   - Batch 24 PauseSheet.kt is now the ONE Pause implementation.
//   - Extend Access remains configurable 1..120 minutes.
//   - Extension cancellation remains explicit.
//   - No fake client-side Pause durations.
//   - Removes emoji UI.
// ====================================================================

package com.lias.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.ExtensionInfo
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.SecurityAlertPayload
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.lias.remote.ui.theme.SystemBlueDark
import com.lias.remote.ui.theme.SystemIndigoDark
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoSlider
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ExclamationmarkTriangle
import io.github.alexzhirkevich.cupertino.icons.outlined.Shield

@Composable
fun OnboardingSheet(
    onComplete: () -> Unit
) {

    HigModalSheet(
        onDismiss =
            onComplete,
        accessibilityLabel =
            "Welcome to LIAS"
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
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            80.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                20.dp
                            )
                        )
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    SystemBlueDark,
                                    SystemIndigoDark
                                )
                            )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                CupertinoIcon(
                    imageVector =
                        CupertinoIcons
                            .Outlined
                            .Shield,
                    contentDescription =
                        null,
                    tint =
                        Color.White,
                    modifier =
                        Modifier.size(
                            40.dp
                        )
                )
            }

            CupertinoText(
                text =
                    "Welcome to LIAS",
                style =
                    HigTypography.title2,
                fontWeight =
                    FontWeight.Bold,
                color =
                    LiasThemeColors.label
            )

            CupertinoText(
                text =
                    "Protect Internet access with device groups, schedules, and server-authoritative rules.",
                style =
                    HigTypography.body,
                color =
                    LiasThemeColors.secondaryLabel,
                textAlign =
                    TextAlign.Center
            )

            OnboardingStep(
                number =
                    "1",
                title =
                    "Protect Infrastructure",
                description =
                    "Keep routers, gateways, DNS servers, and other critical devices in the Infrastructure group."
            )

            OnboardingStep(
                number =
                    "2",
                title =
                    "Build Schedules",
                description =
                    "Create reusable downtime or allowed-hours schedules."
            )

            OnboardingStep(
                number =
                    "3",
                title =
                    "Apply Policies",
                description =
                    "Attach schedules or access rules to device groups without duplicating schedule definitions."
            )

            HigButton(
                text =
                    "Continue",
                onClick =
                    onComplete,
                style =
                    HigButtonStyle.Primary,
                modifier =
                    Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OnboardingStep(
    number: String,
    title: String,
    description: String
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        12.dp
                    )
                )
                .background(
                    LiasThemeColors
                        .tertiaryBackground
                )
                .padding(
                    16.dp
                )
    ) {

        CupertinoText(
            text =
                "STEP $number",
            style =
                HigTypography.caption,
            color =
                LiasThemeColors.blue,
            fontWeight =
                FontWeight.Bold
        )

        CupertinoText(
            text =
                title,
            style =
                HigTypography.headline,
            color =
                LiasThemeColors.label,
            fontWeight =
                FontWeight.SemiBold,
            modifier =
                Modifier.padding(
                    top =
                        2.dp
                )
        )

        CupertinoText(
            text =
                description,
            style =
                HigTypography.subheadline,
            color =
                LiasThemeColors.secondaryLabel,
            modifier =
                Modifier.padding(
                    top =
                        4.dp
                )
        )
    }
}

@Composable
fun SecurityAlertSheet(
    alert: SecurityAlertPayload,
    onDismiss: () -> Unit,
    onBlock: () -> Unit,
    onTrust: () -> Unit
) {

    HigModalSheet(
        onDismiss =
            onDismiss,
        accessibilityLabel =
            "Security Alert"
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
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            CupertinoIcon(
                imageVector =
                    CupertinoIcons
                        .Outlined
                        .ExclamationmarkTriangle,
                contentDescription =
                    null,
                tint =
                    LiasThemeColors.red,
                modifier =
                    Modifier.size(
                        48.dp
                    )
            )

            CupertinoText(
                text =
                    "Security Alert",
                style =
                    HigTypography.title2,
                fontWeight =
                    FontWeight.Bold,
                color =
                    LiasThemeColors.label
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                12.dp
                            )
                        )
                        .background(
                            LiasThemeColors
                                .tertiaryBackground
                        )
                        .padding(
                            16.dp
                        )
            ) {

                CupertinoText(
                    text =
                        alert.alertType
                            .replace(
                                "_",
                                " "
                            )
                            .replaceFirstChar {
                                it.uppercase()
                            },
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors
                            .tertiaryLabel
                )

                CupertinoText(
                    text =
                        alert.details
                            .ifBlank {
                                "LIAS detected unusual network behavior."
                            },
                    style =
                        HigTypography.body,
                    color =
                        LiasThemeColors.label,
                    modifier =
                        Modifier.padding(
                            top =
                                6.dp
                        )
                )
            }

            /*
             * These callbacks remain in the public surface for
             * compatibility. LiasNavHost currently dismisses them
             * because the supplied LIAS contract does not expose
             * dedicated trust/block mutations for this alert.
             */
            HigButton(
                text =
                    "Block Device",
                onClick =
                    onBlock,
                style =
                    HigButtonStyle.Danger,
                modifier =
                    Modifier.fillMaxWidth()
            )

            HigButton(
                text =
                    "Mark as Trusted",
                onClick =
                    onTrust,
                style =
                    HigButtonStyle.Secondary,
                modifier =
                    Modifier.fillMaxWidth()
            )

            HigTextButton(
                text =
                    "Investigate Later",
                onClick =
                    onDismiss
            )
        }
    }
}

@Composable
fun GlobalSwitchSheet(
    currentPolicy: Policy,
    onDismiss: () -> Unit,
    onSave: (Policy) -> Unit
) {

    var selectedAction by
        remember(
            currentPolicy.id,
            currentPolicy.action
        ) {
            mutableStateOf(
                currentPolicy.action
            )
        }

    HigModalSheet(
        onDismiss =
            onDismiss,
        accessibilityLabel =
            "Global Access Switch"
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
                    "Global Access",
                onCancel =
                    onDismiss
            )

            CupertinoIcon(
                imageVector =
                    CupertinoIcons
                        .Outlined
                        .Shield,
                contentDescription =
                    null,
                tint =
                    LiasThemeColors.blue,
                modifier =
                    Modifier.size(
                        44.dp
                    )
            )

            CupertinoText(
                text =
                    "Controls every non-infrastructure device on this LIAS server.",
                style =
                    HigTypography.body,
                color =
                    LiasThemeColors.secondaryLabel,
                textAlign =
                    TextAlign.Center
            )

            SegmentedControl(
                options =
                    listOf(
                        "Allow All",
                        "Schedule",
                        "Block All"
                    ),
                selectedOption =
                    when (
                        selectedAction
                    ) {

                        "allow" ->
                            "Allow All"

                        "block" ->
                            "Block All"

                        else ->
                            "Schedule"
                    },
                onOptionSelected = {
                    selection ->

                    selectedAction =
                        when (
                            selection
                        ) {

                            "Allow All" ->
                                "allow"

                            "Block All" ->
                                "block"

                            else ->
                                "schedule"
                        }
                },
                isDestructive =
                    true,
                modifier =
                    Modifier.fillMaxWidth()
            )

            if (
                selectedAction ==
                "block"
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                            .background(
                                LiasThemeColors.red
                                    .copy(
                                        alpha =
                                            0.10f
                                    )
                            )
                            .padding(
                                12.dp
                            )
                ) {

                    CupertinoText(
                        text =
                            "Block All immediately blocks every non-infrastructure device.",
                        style =
                            HigTypography.subheadline,
                        color =
                            LiasThemeColors.red,
                        fontWeight =
                            FontWeight.Medium
                    )
                }
            }

            HigButton(
                text =
                    if (
                        selectedAction ==
                        "block"
                    ) {
                        "Apply Block All"
                    } else {
                        "Save"
                    },
                onClick = {

                    onSave(
                        currentPolicy.copy(
                            action =
                                selectedAction,
                            enabled =
                                true
                        )
                    )
                },
                style =
                    if (
                        selectedAction ==
                        "block"
                    ) {
                        HigButtonStyle.Danger
                    } else {
                        HigButtonStyle.Primary
                    },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }
    }
}

/*
 * PauseSheet intentionally DOES NOT live in this file anymore.
 *
 * Canonical implementation:
 *   ui/screens/PauseSheet.kt
 *
 * LIAS currently defines Pause as one hour.
 */

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

    val quickPicks =
        listOf(
            15,
            30,
            60,
            120
        )

    HigModalSheet(
        onDismiss =
            onDismiss,
        accessibilityLabel =
            "Extend Access"
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
                    "Extend Access",
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

            CupertinoText(
                text =
                    targetSubtitle,
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors
                        .tertiaryLabel,
                textAlign =
                    TextAlign.Center
            )

            currentExtension
                ?.takeIf {
                    it.reasonTag.equals(
                        "extend_access",
                        ignoreCase =
                            true
                    )
                }
                ?.let {
                    extension ->

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        12.dp
                                    )
                                )
                                .background(
                                    LiasThemeColors.green
                                        .copy(
                                            alpha =
                                                0.10f
                                        )
                                )
                                .padding(
                                    12.dp
                                )
                    ) {

                        CupertinoText(
                            text =
                                "Extension Active",
                            style =
                                HigTypography.headline,
                            color =
                                LiasThemeColors.green,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        CupertinoText(
                            text =
                                if (
                                    extension.minutesLeft >
                                    0
                                ) {
                                    "${extension.minutesLeft} minutes remaining"
                                } else {
                                    "Extension is expiring"
                                },
                            style =
                                HigTypography.subheadline,
                            color =
                                LiasThemeColors
                                    .secondaryLabel
                        )
                    }
                }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                quickPicks.forEach {
                    minutes ->

                    HigButton(
                        text =
                            when (
                                minutes
                            ) {

                                60 ->
                                    "1h"

                                120 ->
                                    "2h"

                                else ->
                                    "${minutes}m"
                            },
                        onClick = {
                            selectedMinutes =
                                minutes
                                    .toFloat()
                        },
                        style =
                            if (
                                selectedMinutes
                                    .toInt() ==
                                minutes
                            ) {
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

            CupertinoText(
                text =
                    "${selectedMinutes.toInt()} minutes",
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
                    value ->

                    selectedMinutes =
                        value
                            .coerceIn(
                                1f,
                                120f
                            )
                },
                valueRange =
                    1f..120f,
                modifier =
                    Modifier.fillMaxWidth()
            )

            CupertinoText(
                text =
                    "LIAS temporarily allows this blocked target. The original policy automatically resumes when the extension expires.",
                style =
                    HigTypography.subheadline,
                color =
                    LiasThemeColors.secondaryLabel,
                textAlign =
                    TextAlign.Center
            )

            HigButton(
                text =
                    if (
                        currentExtension
                            ?.reasonTag
                            ?.equals(
                                "extend_access",
                                ignoreCase =
                                    true
                            ) ==
                        true
                    ) {
                        "Update Extension"
                    } else {
                        "Extend Access"
                    },
                onClick = {

                    onConfirm(
                        selectedMinutes
                            .toInt()
                            .coerceIn(
                                1,
                                120
                            )
                    )
                },
                style =
                    HigButtonStyle.Primary,
                modifier =
                    Modifier.fillMaxWidth()
            )

            if (
                currentExtension
                    ?.reasonTag
                    ?.equals(
                        "extend_access",
                        ignoreCase =
                            true
                    ) ==
                true &&
                onCancelExtension !=
                null
            ) {

                HigButton(
                    text =
                        "Cancel Extension",
                    onClick =
                        onCancelExtension,
                    style =
                        HigButtonStyle.Danger,
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        }
    }
}
