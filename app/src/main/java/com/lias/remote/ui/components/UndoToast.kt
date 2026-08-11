// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/UndoToast.kt
// Version: 16.0.0
//
// Purpose:
//   One-shot HIG-style Undo presentation.
//
// Batch 16 safety contract:
//
//   UndoState is for LOCAL or otherwise provably reversible actions.
//
//   It MUST NOT be used to:
//     - recreate a deleted server Policy from stale cached data
//     - recreate deleted schedules/tags
//     - replay an old Device object
//     - overwrite a newer LIAS server mutation
//
// Remote mutation recovery must come from current authoritative state.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

class UndoState(
    val message: String,
    private val action: () -> Unit
) {

    private val consumed =
        AtomicBoolean(false)

    fun consumeUndo() {
        if (
            consumed.compareAndSet(
                false,
                true
            )
        ) {
            action()
        }
    }
}

@Composable
fun UndoToast(
    undoState: UndoState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible =
            undoState != null,
        enter =
            slideInVertically {
                it
            } +
                fadeIn(),
        exit =
            slideOutVertically {
                it
            } +
                fadeOut(),
        modifier =
            modifier
    ) {

        LaunchedEffect(
            undoState
        ) {

            if (
                undoState != null
            ) {
                delay(
                    5_000L
                )

                onDismiss()
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
            contentAlignment =
                Alignment.BottomCenter
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                12.dp
                            )
                        )
                        .background(
                            Color(
                                0xFF48484A
                            )
                        )
                        .padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                CupertinoText(
                    text =
                        undoState
                            ?.message
                            .orEmpty(),
                    color =
                        Color.White,
                    style =
                        HigTypography.body,
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                if (
                    undoState != null
                ) {

                    CupertinoButton(
                        onClick = {

                            undoState
                                .consumeUndo()

                            onDismiss()
                        },
                        colors =
                            CupertinoButtonDefaults
                                .plainButtonColors(
                                    contentColor =
                                        LiasThemeColors.blue
                                )
                    ) {

                        CupertinoText(
                            text = "UNDO",
                            style =
                                HigTypography.headline,
                            color =
                                LiasThemeColors.blue
                        )
                    }
                }
            }
        }
    }
}
