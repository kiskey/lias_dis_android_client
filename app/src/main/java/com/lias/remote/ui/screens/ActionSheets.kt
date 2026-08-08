// ====================================================================
// File: ActionSheets.kt
// Version: 3.1.0 (HIG Redesign)
// Purpose: Contains all HIG Modal Bottom Sheets (Onboarding, Security,
//          GlobalSwitch, Pause, ExtendAccess) to fulfill the redesign
//          plan without omissions.
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import com.lias.remote.ui.theme.SystemBlueDark
import com.lias.remote.ui.theme.SystemIndigoDark

@Composable
fun OnboardingSheet(onComplete: () -> Unit) {
    HigModalSheet(onDismiss = onComplete) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(SystemBlueDark, SystemIndigoDark))),
                contentAlignment = Alignment.Center
            ) { Text("🛡", style = MaterialTheme.typography.headlineLarge, color = Color.White) }

            Text("Welcome to LIAS", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurface)
            Text("Secure your family's internet in 3 simple steps.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
    ) {
        Text(num.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.W700)
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.W600, modifier = Modifier.padding(top = 2.dp))
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
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
            Text("🚨", style = MaterialTheme.typography.headlineLarge)
            Text("Security Alert", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurface)
            Text(alert.alertType.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
            ) {
                Text("DETAILS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.W600)
                Text(alert.details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 6.dp))
            }

            Text("This may indicate a device impersonating another on your network. Choose how to respond:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

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
            Text("🌐", style = MaterialTheme.typography.headlineLarge)
            Text("Global Access Switch", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onSurface)
            Text("Controls every non-infrastructure device on your network.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
            ) {
                Text("CHOOSE MODE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.W600)
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
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)).padding(12.dp)
                ) {
                    Text("⚠️ Block All requires confirmation and will disconnect all family devices immediately.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.W500)
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
            Text(targetLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
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

            Text("${selectedMinutes.toInt()} minutes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.W800)
            Text("All internet traffic will be blocked for this device until the timer expires.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

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
            Text(targetLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurface)
            Text(targetSubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            currentExtension?.let { ext ->
                val minsLeft = ExtendHelper.minutesUntil(ext.expiresAt)
                Text("Active extension: ${minsLeft}m left", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.W600)
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
                Text("${selectedMinutes.toInt()} minutes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.W700)
                Slider(
                    value = selectedMinutes,
                    onValueChange = { selectedMinutes = it },
                    valueRange = 1f..120f,
                    steps = 118,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
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
