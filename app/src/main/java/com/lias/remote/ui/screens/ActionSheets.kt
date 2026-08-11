// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/ActionSheets.kt
// Version: 35.3.0
//
// Purpose:
//   Shared non-device-specific modal actions.
//
// Canonical ownership:
//   PauseSheet.kt        -> Pause
//   ExtendAccessSheet.kt -> temporary Allow / Extend
//
// This file intentionally owns neither of those functions.
// ====================================================================

package com.lias.remote.ui.screens

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.lias.remote.core.models.Policy
import com.lias.remote.core.models.SecurityAlertPayload
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetPresentation
import com.lias.remote.ui.components.rememberHigAnimatedCompletion
import com.lias.remote.ui.components.rememberHigAnimatedDismiss
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.SegmentedControl
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.lias.remote.ui.theme.SystemBlueDark
import com.lias.remote.ui.theme.SystemIndigoDark
import com.slapps.cupertino.CupertinoIcon
import com.slapps.cupertino.CupertinoActionSheet
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.cancel
import com.slapps.cupertino.default
import com.slapps.cupertino.destructive
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.icons.CupertinoIcons
import com.slapps.cupertino.icons.outlined.ExclamationmarkTriangle
import com.slapps.cupertino.icons.outlined.Shield

@Composable
fun OnboardingSheet(
    onComplete: () -> Unit
) {
    HigModalSheet(
        presentation =
            HigSheetPresentation.Editor,
        onDismiss = onComplete,
        accessibilityLabel = "Welcome to LIAS"
    ) {
        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onComplete
            )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(SystemBlueDark, SystemIndigoDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CupertinoIcon(
                    imageVector = CupertinoIcons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            CupertinoText(
                text = "Welcome to LIAS",
                style = HigTypography.title2,
                fontWeight = FontWeight.Bold,
                color = LiasThemeColors.label
            )

            CupertinoText(
                text = "Protect Internet access with device groups, schedules, and server-authoritative rules.",
                style = HigTypography.body,
                color = LiasThemeColors.secondaryLabel,
                textAlign = TextAlign.Center
            )

            OnboardingStep(
                number = "1",
                title = "Protect Infrastructure",
                description = "Keep routers, gateways, DNS servers, and other critical devices in the Infrastructure group."
            )
            OnboardingStep(
                number = "2",
                title = "Build Schedules",
                description = "Create reusable downtime or allowed-hours schedules."
            )
            OnboardingStep(
                number = "3",
                title = "Apply Policies",
                description = "Attach schedules or access rules to device groups without duplicating schedule definitions."
            )

            HigButton(
                text = "Continue",
                onClick = {
                    animatedComplete {
                        onComplete()
                    }
                },
                style = HigButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LiasThemeColors.tertiaryBackground)
            .padding(16.dp)
    ) {
        CupertinoText(
            text = "STEP $number",
            style = HigTypography.caption,
            color = LiasThemeColors.blue,
            fontWeight = FontWeight.Bold
        )
        CupertinoText(
            text = title,
            style = HigTypography.headline,
            color = LiasThemeColors.label,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp)
        )
        CupertinoText(
            text = description,
            style = HigTypography.subheadline,
            color = LiasThemeColors.secondaryLabel,
            modifier = Modifier.padding(top = 4.dp)
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
        presentation =
            HigSheetPresentation.Editor,
        onDismiss = onDismiss,
        accessibilityLabel = "Security Alert"
    ) {
        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

        val animatedDismiss =
            rememberHigAnimatedDismiss(
                fallback =
                    onDismiss
            )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CupertinoIcon(
                imageVector = CupertinoIcons.Outlined.ExclamationmarkTriangle,
                contentDescription = null,
                tint = LiasThemeColors.red,
                modifier = Modifier.size(48.dp)
            )

            CupertinoText(
                text = "Security Alert",
                style = HigTypography.title2,
                fontWeight = FontWeight.Bold,
                color = LiasThemeColors.label
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LiasThemeColors.tertiaryBackground)
                    .padding(16.dp)
            ) {
                CupertinoText(
                    text = alert.alertType
                        .replace("_", " ")
                        .replaceFirstChar { it.uppercase() },
                    style = HigTypography.caption,
                    color = LiasThemeColors.tertiaryLabel
                )
                CupertinoText(
                    text = alert.details.ifBlank {
                        "LIAS detected unusual network behavior."
                    },
                    style = HigTypography.body,
                    color = LiasThemeColors.label,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            HigButton(
                text = "Block Device",
                onClick = {
                    animatedComplete {
                        onBlock()
                    }
                },
                style = HigButtonStyle.Danger,
                modifier = Modifier.fillMaxWidth()
            )
            HigButton(
                text = "Mark as Trusted",
                onClick = {
                    animatedComplete {
                        onTrust()
                    }
                },
                style = HigButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
            HigTextButton(
                text = "Investigate Later",
                onClick = animatedDismiss
            )
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun GlobalSwitchSheet(
    currentPolicy: Policy,
    onDismiss: () -> Unit,
    onSave: (Policy) -> Unit
) {
    var selectedAction by remember(
        currentPolicy.id,
        currentPolicy.action
    ) {
        mutableStateOf(currentPolicy.action)
    }

    var visible by
        remember {
            mutableStateOf(
                false
            )
        }

    var closing by
        remember {
            mutableStateOf(
                false
            )
        }

    val scope =
        rememberCoroutineScope()

    LaunchedEffect(
        Unit
    ) {
        visible =
            true
    }

    /*
     * Slanoss 2.3.1 CupertinoDialogs.kt defines ActionSheet exit as
     * a 150ms tween. Keep the parent alive for exactly that transition.
     */
    fun closeThen(
        action: () -> Unit
    ) {
        if (
            closing
        ) {
            return
        }

        closing =
            true
        visible =
            false

        scope.launch {
            delay(
                150
            )
            action()
        }
    }

    CupertinoActionSheet(
        visible =
            visible,
        onDismissRequest = {
            closeThen(
                onDismiss
            )
        },
        title = {
            CupertinoText(
                text =
                    "Global Access",
                style =
                    HigTypography.title3,
                fontWeight =
                    FontWeight.SemiBold
            )
        },
        message = {
            CupertinoText(
                text =
                    "Controls every non-infrastructure device on this LIAS server.",
                style =
                    HigTypography.body,
                fontWeight =
                    FontWeight.Medium
            )
        },
        content = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            16.dp
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {
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
            }
        },
        buttons = {
            if (
                selectedAction ==
                "block"
            ) {
                destructive(
                    onClick = {
                        val updated =
                            currentPolicy.copy(
                                action =
                                    selectedAction,
                                enabled =
                                    true
                            )

                        closeThen {
                            onSave(
                                updated
                            )
                        }
                    }
                ) {
                    CupertinoText(
                        text =
                            "Apply Block All"
                    )
                }
            } else {
                default(
                    onClick = {
                        val updated =
                            currentPolicy.copy(
                                action =
                                    selectedAction,
                                enabled =
                                    true
                            )

                        closeThen {
                            onSave(
                                updated
                            )
                        }
                    }
                ) {
                    CupertinoText(
                        text =
                            "Save"
                    )
                }
            }

            cancel(
                onClick = {
                    closeThen(
                        onDismiss
                    )
                }
            ) {
                CupertinoText(
                    text =
                        "Cancel"
                )
            }
        }
    )
}
