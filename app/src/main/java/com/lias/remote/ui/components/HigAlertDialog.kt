// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigAlertDialog.kt
// Version: 29.0.0
//
// Purpose:
//   Reusable Apple-style alert dialog.
//
// Changes:
//   - Preserves all existing call sites through defaults.
//   - Adds optional composable content for editable dialogs.
//   - Adds confirmEnabled.
//   - Avoids forcing Material AlertDialog into the Cupertino UI.
//   - Keeps destructive-action semantics explicit.
//   - Editable alerts expand adaptively and avoid the keyboard/system bars.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
    confirmEnabled: Boolean = true,
    onCancel: () -> Unit = {},
    content: (@Composable () -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier =
                    Modifier
                        .widthIn(
                            max =
                                if (content == null) {
                                    290.dp
                                } else {
                                    360.dp
                                }
                        )
                        .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            14.dp
                        )
                    )
                    .background(
                        LiasThemeColors.secondaryBackground
                    )
            ) {
                Column(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                CupertinoText(
                    text = title,
                    style =
                        HigTypography.headline,
                    fontWeight =
                        FontWeight.Bold,
                    textAlign =
                        TextAlign.Center,
                    color =
                        LiasThemeColors.label,
                    modifier =
                        Modifier.padding(
                            top = 18.dp,
                            start = 18.dp,
                            end = 18.dp,
                            bottom =
                                if (message.isBlank()) {
                                    10.dp
                                } else {
                                    4.dp
                                }
                        )
                )

                if (message.isNotBlank()) {
                    CupertinoText(
                        text = message,
                        style =
                            HigTypography.subheadline,
                        color =
                            LiasThemeColors.secondaryLabel,
                        textAlign =
                            TextAlign.Center,
                        modifier =
                            Modifier.padding(
                                start = 18.dp,
                                end = 18.dp,
                                bottom =
                                    if (content == null) {
                                        16.dp
                                    } else {
                                        10.dp
                                    }
                            )
                    )
                }

                content?.let { dialogContent ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
                                )
                    ) {
                        dialogContent()
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(
                                LiasThemeColors.separator
                            )
                )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                    CupertinoButton(
                        onClick = {
                            onCancel()
                            onDismissRequest()
                        },
                        colors =
                            CupertinoButtonDefaults
                                .plainButtonColors(
                                    contentColor =
                                        LiasThemeColors.blue
                                ),
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(
                                    vertical = 10.dp
                                )
                    ) {
                        CupertinoText(
                            text = cancelText,
                            style =
                                HigTypography.body
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .width(0.5.dp)
                                .height(48.dp)
                                .background(
                                    LiasThemeColors.separator
                                )
                    )

                    CupertinoButton(
                        onClick = {
                            if (confirmEnabled) {
                                onConfirm()
                                onDismissRequest()
                            }
                        },
                        enabled =
                            confirmEnabled,
                        colors =
                            CupertinoButtonDefaults
                                .plainButtonColors(
                                    contentColor =
                                        if (isDestructive) {
                                            LiasThemeColors.red
                                        } else {
                                            LiasThemeColors.blue
                                        }
                                ),
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(
                                    vertical = 10.dp
                                )
                    ) {
                        CupertinoText(
                            text = confirmText,
                            style =
                                HigTypography.headline,
                            fontWeight =
                                FontWeight.SemiBold,
                            color =
                                if (!confirmEnabled) {
                                    LiasThemeColors.tertiaryLabel
                                } else if (isDestructive) {
                                    LiasThemeColors.red
                                } else {
                                    LiasThemeColors.blue
                                }
                        )
                    }
                    }
                }
            }
        }
    }
}
