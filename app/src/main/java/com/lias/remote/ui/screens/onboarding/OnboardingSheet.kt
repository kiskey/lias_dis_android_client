// ====================================================================
// File: OnboardingSheet.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: 3-card first-run onboarding. Preserves DataStore persistence.
//          Tag router → Schedule → Apply to devices.
// ====================================================================

package com.lias.remote.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.theme.SystemBlueDark
import com.lias.remote.ui.theme.SystemIndigoDark

@Composable
fun OnboardingSheet(
    onComplete: () -> Unit
) {
    HigModalSheet(onDismiss = onComplete) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gradient Shield Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SystemBlueDark, SystemIndigoDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🛡", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            }

            Text(
                text = "Welcome to LIAS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Secure your family's internet in 3 simple steps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Step 1
            OnboardingStepCard(
                stepNum = "Step 1",
                title = "Tag Your Router",
                description = "Assign the \"Infrastructure\" tag to your router/gateway to prevent accidental lockouts."
            )

            // Step 2
            OnboardingStepCard(
                stepNum = "Step 2",
                title = "Create a Schedule",
                description = "Set up a \"Bedtime\" schedule — e.g., block 22:00 to 06:00 nightly."
            )

            // Step 3
            OnboardingStepCard(
                stepNum = "Step 3",
                title = "Apply to Devices",
                description = "Tag your kids' devices as \"Kids\" and attach the schedule policy."
            )

            HigButton(
                text = "Got It!",
                onClick = onComplete,
                style = HigButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OnboardingStepCard(
    stepNum: String,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text = stepNum.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.W700
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.W600,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
