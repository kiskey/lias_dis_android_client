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
import com.lias.remote.core.util.ExtendHelper
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
fun OnboardingSheet(onComplete: () -> Unit) {
    HigModalSheet(onDismiss = onComplete) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(SystemBlueDark, SystemIndigoDark))),
                contentAlignment = Alignment.Center
            ) {
                CupertinoIcon(
                    imageVector = CupertinoIcons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            CupertinoText("Welcome to LIAS", style = HigTypography.title2, fontWeight = FontWeight.Bold, color = LiasThemeColors.label)
            CupertinoText("Secure your family's internet in 3 simple steps.", style = HigTypography.body, color = LiasThemeColors.secondaryLabel, textAlign = TextAlign.Center)

            OnboardingStep("Step 1", "Tag Your Router", "Assign the \"Infrastructure\" tag to your router/gateway to prevent accidental lockouts.")
            OnboardingStep("Step 2", "Create a Schedule", "Set up a \"Bedtime\" schedule — e.g., block 22:00 to 06:00 nightly.")
            OnboardingStep("Step 3", "Apply to Devices", "Tag your kids' devices as \"Kids\" and attach the schedule policy.")

            HigButton(text = "Got It!", onClick = onComplete, style = HigButtonStyle.Primary, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun OnboardingStep(num: String, title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LiasThemeColors.tertiaryBackground)
            .padding(16.dp)
    ) {
        CupertinoText(num.uppercase(), style = HigTypography.caption, color = LiasThemeColors.blue, fontWeight = FontWeight.Bold)
        CupertinoText(title, style = HigTypography.headline, color = LiasThemeColors.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
        CupertinoText(desc, style = HigTypography.subheadline, color = LiasThemeColors.secondaryLabel, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun SecurityAlertSheet(alert: SecurityAlertPayload, onDismiss: () -> Unit, onBlock: () -> Unit, onTrust: () -> Unit) {
    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CupertinoIcon(
                imageVector = CupertinoIcons.Outlined.ExclamationmarkTriangle,
                contentDescription = "Alert",
                tint = LiasThemeColors.red,
                modifier = Modifier.size(48.dp)
            )
            CupertinoText("Security Alert", style = HigTypography.title2, fontWeight = FontWeight.Bold, color = LiasThemeColors.label)
            CupertinoText(alert.alertType.replace("_", " ").replaceFirstChar { it.uppercase() }, style = HigTypography.subheadline, color = LiasThemeColors.secondaryLabel, textAlign = TextAlign.Center)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LiasThemeColors.tertiaryBackground)
                    .padding(16.dp)
            ) {
                CupertinoText("DETAILS", style = HigTypography.caption, color = LiasThemeColors.secondaryLabel, fontWeight = FontWeight.SemiBold)
                CupertinoText(alert.details, style = HigTypography.body, color = LiasThemeColors.label, modifier = Modifier.padding(top = 6.dp))
            }

            CupertinoText("This may indicate a device impersonating another on your network. Choose how to respond:", style = HigTypography.subheadline, color = LiasThemeColors.secondaryLabel, textAlign = TextAlign.Center)

            HigButton(text = "Block Suspicious Device", onClick = onBlock, style = HigButtonStyle.Danger, modifier = Modifier.fillMaxWidth())
            HigButton(text = "Mark as Trusted (DHCP Rebind)", onClick = onTrust, style = HigButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            HigTextButton(text = "Investigate Later", onClick = onDismiss, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun GlobalSwitchSheet(currentPolicy: Policy, onDismiss: () -> Unit, onSave: (Policy) -> Unit) {
    var selectedAction by remember { mutableStateOf(currentPolicy.action) }

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CupertinoIcon(
                imageVector = CupertinoIcons.Outlined.Shield,
                contentDescription = null,
                tint = LiasThemeColors.blue,
                modifier = Modifier.size(48.dp)
            )
            CupertinoText("Global Access Switch", style = HigTypography.title2, fontWeight = FontWeight.Bold, color = LiasThemeColors.label)
            CupertinoText("Controls every non-infrastructure device on your network.", style = HigTypography.body, color = LiasThemeColors.secondaryLabel, textAlign = TextAlign.Center)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LiasThemeColors.tertiaryBackground)
                    .padding(16.dp)
            ) {
                CupertinoText("CHOOSE MODE", style = HigTypography.caption, color = LiasThemeColors.secondaryLabel, fontWeight = FontWeight.SemiBold)
                SegmentedControl(
                    options = listOf("Allow All", "Schedule", "Block All"),
                    selectedOption = when(selectedAction) { "allow" -> "Allow All"; "block" -> "Block All"; else -> "Schedule" },
                    onOptionSelected = { selection ->
                        selectedAction = when(selection) { "Allow All" -> "allow"; "Block All" -> "block"; else -> "schedule" }
                    },
                    isDestructive = true,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (selectedAction == "block") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LiasThemeColors.red.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    CupertinoText("⚠️ Block All requires confirmation and will disconnect all family devices immediately.", style = HigTypography.subheadline, color = LiasThemeColors.red, fontWeight = FontWeight.Medium)
                }
            }

            HigButton(
                text = "Save",
                onClick = { onSave(currentPolicy.copy(action = selectedAction, enabled = true)) },
                style = if (selectedAction == "block") HigButtonStyle.Danger else HigButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PauseSheet(targetLabel: String, onDismiss: () -> Unit, onConfirm: (minutes: Int) -> Unit) {
    var selectedMinutes by remember { mutableFloatStateOf(60f) }
    val quickPicks = listOf(15, 30, 60, 120)

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(title = "Pause Internet", onCancel = onDismiss)
            CupertinoText(targetLabel, style = HigTypography.title2, fontWeight = FontWeight.SemiBold, color = LiasThemeColors.secondaryLabel)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickPicks.forEach { pick ->
                    val isSelected = selectedMinutes.toInt() == pick
                    HigButton(
                        text = if (pick >= 60) "${pick / 60}h" else "${pick}m",
                        onClick = { selectedMinutes = pick.toFloat() },
                        style = if (isSelected) HigButtonStyle.Danger else HigButtonStyle.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            CupertinoText("${selectedMinutes.toInt()} minutes", style = HigTypography.title1, fontWeight = FontWeight.ExtraBold)
            CupertinoText("All internet traffic will be blocked for this device until the timer expires.", style = HigTypography.subheadline, color = LiasThemeColors.secondaryLabel, textAlign = TextAlign.Center)

            HigButton(
                text = "Pause for " + if (selectedMinutes >= 60) "${selectedMinutes.toInt() / 60} Hour${if (selectedMinutes > 60) "s" else ""}" else "$selectedMinutes Minutes",
                onClick = { onConfirm(selectedMinutes.toInt()) },
                style = HigButtonStyle.Danger,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ExtendAccessSheet(
    targetLabel: String,
    targetSubtitle: String,
    currentExtension: ExtensionInfo?,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit,
    onCancelExtension: (() -> Unit)? = null
) {
    var selectedMinutes by remember { mutableFloatStateOf(30f) }
    val quickPicks = listOf(15, 30, 60, 120)

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(title = "Extend Access", onCancel = onDismiss)
            CupertinoText(targetLabel, style = HigTypography.title2, fontWeight = FontWeight.SemiBold, color = LiasThemeColors.label)
            CupertinoText(targetSubtitle, style = HigTypography.subheadline, color = LiasThemeColors.secondaryLabel)

            currentExtension?.let { ext ->
                val minsLeft = ExtendHelper.minutesUntil(ext.expiresAt)
                CupertinoText("Active extension: ${minsLeft}m left", style = HigTypography.subheadline, color = LiasThemeColors.blue, fontWeight = FontWeight.SemiBold)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickPicks.forEach { pick ->
                    val isSelected = selectedMinutes.toInt() == pick
                    HigButton(
                        text = "${pick}m",
                        onClick = { selectedMinutes = pick.toFloat() },
                        style = if (isSelected) HigButtonStyle.Primary else HigButtonStyle.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CupertinoText("${selectedMinutes.toInt()} minutes", style = HigTypography.title1, fontWeight = FontWeight.Bold)
                CupertinoSlider(
                    value = selectedMinutes,
                    onValueChange = { selectedMinutes = it },
                    valueRange = 1f..120f,
                    steps = 118,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            HigButton(
                text = "Allow for ${selectedMinutes.toInt()} Minutes",
                onClick = { onConfirm(selectedMinutes.toInt()) },
                style = HigButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            if (currentExtension != null && onCancelExtension != null) {
                HigTextButton(text = "Cancel current extension", onClick = onCancelExtension, isDestructive = true, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
