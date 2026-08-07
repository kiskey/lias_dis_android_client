// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigAlertDialog.kt
// Version: 1.0.0
// Purpose: Cupertino-styled alert dialog wrapper matching iOS HIG design.
// Migration: Wraps CupertinoDialog to provide same API as Material3 AlertDialog.
// Verification: §7 protocol - confirmed CupertinoDialog exists in RobinPcrd fork v3.3.1
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.robinpcrd.cupertino.CupertinoDialog

@Composable
fun HigAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null
) {
    CupertinoDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        buttons = {
            // iOS style: cancel button first (left), then action button (right)
            dismissButton?.let { dismiss ->
                dismiss()
            }
            confirmButton()
        }
    )
}
