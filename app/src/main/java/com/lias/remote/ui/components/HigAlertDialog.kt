// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigAlertDialog.kt
// Version: 1.3.0
// Purpose: Alert dialog wrapper matching HIG design and Composable scope.
// Audit Fixes:
//   1. Aligned button invocations with Composable function context.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable

@Composable
fun HigAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}
