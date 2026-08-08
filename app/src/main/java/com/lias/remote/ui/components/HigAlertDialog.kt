package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoButton
import io.github.alexzhirkevich.cupertino.CupertinoButtonDefaults
import io.github.alexzhirkevich.cupertino.CupertinoText

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
        Box(
            modifier = Modifier
                .width(270.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LiasThemeColors.secondaryBackground)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CupertinoText(
                    text = title,
                    style = HigTypography.headline,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = LiasThemeColors.label,
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
                )
                CupertinoText(
                    text = message,
                    style = HigTypography.subheadline,
                    color = LiasThemeColors.secondaryLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(LiasThemeColors.separator)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CupertinoButton(
                        onClick = { onCancel(); onDismissRequest() },
                        colors = CupertinoButtonDefaults.plainButtonColors(contentColor = LiasThemeColors.blue),
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                    ) {
                        CupertinoText(text = cancelText, style = HigTypography.body)
                    }
                    
                    Spacer(
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(44.dp)
                            .background(LiasThemeColors.separator)
                    )
                    
                    CupertinoButton(
                        onClick = { onConfirm(); onDismissRequest() },
                        colors = CupertinoButtonDefaults.plainButtonColors(
                            contentColor = if (isDestructive) LiasThemeColors.red else LiasThemeColors.blue
                        ),
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                    ) {
                        CupertinoText(
                            text = confirmText,
                            style = HigTypography.headline,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
