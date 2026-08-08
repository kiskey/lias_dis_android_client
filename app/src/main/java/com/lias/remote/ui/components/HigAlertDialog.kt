// ====================================================================
// File: HigAlertDialog.kt
// Version: 3.0.0 (HIG Redesign)
// Purpose: iOS-style alert dialog with blur backdrop and strict HIG
//          typography. Used for destructive confirmations.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun HigAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    isDestructive: Boolean = false,
    cancelText: String = "Cancel",
    onCancel: () -> Unit = onDismissRequest
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.width(270.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
                
                // iOS style separator
                androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    HigTextButton(
                        text = cancelText,
                        onClick = { onCancel(); onDismissRequest() },
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                    )
                    
                    // Vertical separator
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier
                        .width(0.5.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                    
                    HigTextButton(
                        text = confirmText,
                        onClick = { onConfirm(); onDismissRequest() },
                        isDestructive = isDestructive,
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

private val androidx.compose.ui.Alignment.Companion.CenterHorizontally get() = androidx.compose.ui.Alignment.CenterHorizontally
