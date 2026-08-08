// ====================================================================
// File: HigSheets.kt
// Version: 3.0.1 (HIG Redesign Fix)
// Purpose: Fixed content lambda type inference for ModalBottomSheet.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import com.lias.remote.ui.theme.HigSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HigModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = HigSpec.SheetCorner, topEnd = HigSpec.SheetCorner),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .width(HigSpec.SheetHandleWidth)
                    .height(HigSpec.SheetHandleHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            content()
        }
    }
}

@Composable
fun HigSheetHeader(
    title: String,
    onCancel: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HigTextButton(text = "Cancel", onClick = onCancel)
        androidx.compose.material3.Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )
        trailingAction?.invoke() ?: Spacer(modifier = Modifier.width(60.dp))
    }
}
