// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigSheets.kt
// Version: 21.0.0
//
// Purpose:
//   HIG-inspired bottom modal surface.
//
// Batch 21:
//   - Android Back dismisses the current modal.
//   - Modal content exposes pane semantics.
//   - Scrim gets an explicit accessibility dismissal action.
//   - Scrim and sheet are separate siblings; an empty clickable handler
//     is no longer used solely to stop event propagation.
//   - Navigation-bar inset respected.
//   - Sheet header title is exposed as a heading.
//   - Cancel/trailing actions inherit 48dp targets from HigTextButton.
// ====================================================================

package com.lias.remote.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun HigModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = "Modal sheet",
    content: @Composable ColumnScope.() -> Unit
) {

    BackHandler(
        enabled =
            true,
        onBack =
            onDismiss
    )

    val scrimInteraction =
        remember {
            MutableInteractionSource()
        }

    AnimatedVisibility(
        visible =
            true,
        enter =
            fadeIn() +
                slideInVertically {
                    it
                },
        exit =
            fadeOut() +
                slideOutVertically {
                    it
                }
    ) {

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            /*
             * Scrim is its own dismissal target.
             *
             * The sheet content is no longer nested inside this
             * clickable node, removing the need for onClick = {}.
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black
                                .copy(
                                    alpha =
                                        0.40f
                                )
                        )
                        .semantics {

                            role =
                                Role.Button

                            contentDescription =
                                "Dismiss $accessibilityLabel"
                        }
                        .clickable(
                            interactionSource =
                                scrimInteraction,
                            indication =
                                null,
                            onClick =
                                onDismiss
                        )
            )

            Column(
                modifier =
                    modifier
                        .align(
                            Alignment.BottomCenter
                        )
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
                        .semantics {
                            paneTitle =
                                accessibilityLabel
                        }
                        .navigationBarsPadding()
                        .padding(
                            bottom =
                                12.dp
                        )
            ) {

                Box(
                    modifier =
                        Modifier
                            .align(
                                Alignment
                                    .CenterHorizontally
                            )
                            .padding(
                                top =
                                    8.dp,
                                bottom =
                                    12.dp
                            )
                            .width(
                                HigSpec
                                    .SheetHandleWidth
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
                            .padding(
                                vertical =
                                    HigSpec
                                        .SheetHandleHeight /
                                        2
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
                        12.dp,
                    vertical =
                        2.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier.weight(
                    1f
                ),
            contentAlignment =
                Alignment.CenterStart
        ) {

            HigTextButton(
                text =
                    "Cancel",
                onClick =
                    onCancel
            )
        }

        CupertinoText(
            text =
                title,
            style =
                HigTypography.headline,
            fontWeight =
                FontWeight.SemiBold,
            color =
                LiasThemeColors.label,
            modifier =
                Modifier
                    .weight(
                        2f
                    )
                    .semantics {
                        heading()
                    }
        )

        Box(
            modifier =
                Modifier.weight(
                    1f
                ),
            contentAlignment =
                Alignment.CenterEnd
        ) {

            trailingAction
                ?.invoke()
                ?: Spacer(
                    modifier =
                        Modifier.width(
                            44.dp
                        )
                )
        }
    }
}
