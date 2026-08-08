package com.lias.remote.ui.screens.devices

import androidx.compose.runtime.Composable
import com.lias.remote.ui.screens.PauseSheet as CorePauseSheet

@Composable
fun PauseSheet(
    targetLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit
) {
    CorePauseSheet(
        targetLabel = targetLabel,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
