package com.lias.remote.ui.screens.devices

import androidx.compose.runtime.Composable
import com.lias.remote.core.models.ExtensionInfo
import com.lias.remote.ui.screens.ExtendAccessSheet as CoreExtendAccessSheet

@Composable
fun ExtendAccessSheet(
    targetLabel: String,
    targetSubtitle: String,
    currentExtension: ExtensionInfo?,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit,
    onCancelExtension: (() -> Unit)? = null
) {
    CoreExtendAccessSheet(
        targetLabel = targetLabel,
        targetSubtitle = targetSubtitle,
        currentExtension = currentExtension,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onCancelExtension = onCancelExtension
    )
}
