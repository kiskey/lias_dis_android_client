// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/connect/ConnectScreen.kt
// Version: 3.0.0
// Purpose: Native iOS Connect Onboarding Screen.
// Audit Fixes:
//   1. Formatted onboarding screen with native iOS gradient shield and HigField inputs.
// ====================================================================

package com.lias.remote.ui.screens.connect

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.SettingsViewModel
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigField
import com.lias.remote.ui.theme.SystemBlueDark
import com.lias.remote.ui.theme.SystemIndigoDark

@Composable
fun ConnectScreen(
    viewModel: SettingsViewModel,
    onConnected: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Diagonal Gradient Shield Icon Container (84dp x 84dp, 22dp corner radius)
        Box(
            modifier = Modifier
                .size(84.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(22.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(SystemBlueDark, SystemIndigoDark)
                    ),
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Connect to LIAS",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.W800,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your home server address to start managing devices, schedules and rules.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HigField(
                value = state.serverUrl,
                onValueChange = { viewModel.updateServerUrl(it) },
                label = "Server URL",
                placeholder = "http://192.168.1.1:8081"
            )

            HigField(
                value = state.authToken,
                onValueChange = { viewModel.updateAuthToken(it) },
                label = "Auth Token (Optional)",
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        HigButton(
            text = "Connect",
            onClick = {
                viewModel.saveSettings()
                onConnected()
            },
            enabled = state.serverUrl.isNotBlank(),
            style = HigButtonStyle.Primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Scan QR code from LIAS dashboard",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable {
                Toast.makeText(context, "QR Scanner feature coming soon", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
