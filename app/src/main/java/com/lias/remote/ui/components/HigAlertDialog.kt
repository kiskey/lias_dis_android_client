// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigAlertDialog.kt
// Version: 1.2.0
// Purpose: Cupertino-styled alert dialog wrapper matching iOS HIG design.
// Audit Fixes:
//   1. Replaced CupertinoDialog with CupertinoAlertDialog.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.runtime.Composable
import io.github.robinpcrd.cupertino.CupertinoAlertDialog

@Composable
fun HigAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null
) {
    CupertinoAlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        message = text,
        buttons = {
            dismissButton?.invoke()
            confirmButton()
        }
    )
}
