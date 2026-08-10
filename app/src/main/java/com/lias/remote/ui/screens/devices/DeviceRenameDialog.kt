// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/DeviceRenameDialog.kt
// Version: 27.2.0
//
// Purpose:
//   Functional Cupertino-style device rename dialog.
//
// Fix:
//   The previous implementation stored editable text but never rendered
//   an input control. This implementation uses the content slot added
//   to HigAlertDialog.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import com.lias.remote.ui.components.HigAlertDialog
import com.lias.remote.ui.components.HigConfiguredField

@Composable
fun RenameDeviceDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by
        remember(currentName) {
            mutableStateOf(
                currentName
            )
        }

    val normalized =
        text.trim()

    val canSave =
        normalized.isNotBlank() &&
            normalized != currentName.trim()

    HigAlertDialog(
        onDismissRequest =
            onDismiss,
        title =
            "Rename Device",
        message =
            "Choose a name that makes this device easy to recognize.",
        confirmText =
            "Save",
        confirmEnabled =
            canSave,
        onConfirm = {
            onConfirm(
                normalized
            )
        },
        content = {
            HigConfiguredField(
                value =
                    text,
                onValueChange = {
                    text = it
                },
                label =
                    "Device Name",
                placeholder =
                    "Device name",
                keyboardOptions =
                    KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
            )
        }
    )
}
