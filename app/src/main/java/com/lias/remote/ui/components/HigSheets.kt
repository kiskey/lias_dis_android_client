// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/components/HigSheets.kt
// Version: 27.0.0
//
// Purpose:
//   Shared HIG-style modal sheet infrastructure.
//
// Batch 27:
//   - Adds accessibilityLabel used by Batches 24–26.
//   - Preserves all existing callers because parameter is optional.
//   - Adds pane semantics for assistive technologies.
//   - Backdrop remains dismissible.
//   - Sheet body consumes pointer taps to prevent accidental dismissal.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoText

@Composable
fun HigModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {

    val backdropInteractionSource =
        remember {
            MutableInteractionSource()
        }

    val sheetInteractionSource =
        remember {
            MutableInteractionSource()
        }

    AnimatedVisibility(
        visible =
            true,
        enter =
            fadeIn() +
                slideInVertically {
                    fullHeight ->

                    fullHeight
                },
        exit =
            fadeOut() +
                slideOutVertically {
                    fullHeight ->

                    fullHeight
                }
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .background(
                        Color.Black.copy(
                            alpha =
                                0.40f
                        )
                    )
                    .clickable(
                        interactionSource =
                            backdropInteractionSource,
                        indication =
                            null,
                        onClick =
                            onDismiss
                    ),
            contentAlignment =
                Alignment.BottomCenter
        ) {

            Column(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                topStart =
                                    HigSpec.SheetCorner,
                                topEnd =
                                    HigSpec.SheetCorner
                            )
                        )
                        .background(
                            LiasThemeColors
                                .secondaryBackground
                        )
                        .then(
                            if (
                                accessibilityLabel
                                    .isNullOrBlank()
                            ) {

                                Modifier

                            } else {

                                Modifier.semantics {

                                    paneTitle =
                                        accessibilityLabel
                                }
                            }
                        )
                        .clickable(
                            interactionSource =
                                sheetInteractionSource,
                            indication =
                                null,
                            onClick = {
                                /*
                                 * Consume clicks so they never reach
                                 * the dismissible backdrop.
                                 */
                            }
                        )
                        .padding(
                            bottom =
                                24.dp
                        )
            ) {

                Box(
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterHorizontally
                            )
                            .padding(
                                top =
                                    8.dp,
                                bottom =
                                    12.dp
                            )
                            .width(
                                HigSpec.SheetHandleWidth
                            )
                            .height(
                                HigSpec.SheetHandleHeight
                            )
                            .clip(
                                RoundedCornerShape(
                                    3.dp
                                )
                            )
                            .background(
                                LiasThemeColors
                                    .tertiaryLabel
                            )
                )

                content()
            }
        }
    }
}

@Composable
fun HigSheetHeader(
    title: String,
    onCancel: () -> Unit,
    trailingAction:
        (@Composable () -> Unit)? =
        null
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp,
                    vertical =
                        8.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        HigTextButton(
            text =
                "Cancel",
            onClick =
                onCancel
        )

        CupertinoText(
            text =
                title,
            style =
                HigTypography.headline,
            fontWeight =
                FontWeight.SemiBold,
            color =
                LiasThemeColors.label
        )

        if (
            trailingAction !=
            null
        ) {

            trailingAction()

        } else {

            /*
             * Keeps title visually centered against the Cancel button.
             */
            Spacer(
                modifier =
                    Modifier.width(
                        60.dp
                    )
            )
        }
    }
}
