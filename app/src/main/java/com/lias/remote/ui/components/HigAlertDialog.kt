// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigAlertDialog.kt
// Version: 1.1.0
// Purpose: Cupertino-styled alert dialog wrapper matching iOS HIG design.
// Migration: Wraps CupertinoDialog to provide same API as Material3 AlertDialog.
// ====================================================================

package com.lias.remote.ui.components

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
        message = text,
        buttons = {
            dismissButton?.invoke()
            confirmButton()
        }
    )
}
