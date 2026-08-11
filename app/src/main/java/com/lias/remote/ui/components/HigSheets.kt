// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/components/HigSheets.kt
// Version: 33.6.0
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
//
// Plan 3.3 Batch 002:
//   - Uses Slanoss 2.3.1 CupertinoSheetState / CupertinoBottomSheetScaffold.
//   - Back, outside tap, swipe-down and header Cancel animate before parent removal.
//
// Plan 3.3 Batch 003:
//   - Adds animated completion ordering for Save/Done/Confirm actions.
//   - Prevents Hidden observer from racing completion callbacks.
//
// Plan 3.3 Batch 004:
//   - Adds navigation-bar inset handling alongside IME padding.
//   - Verifies swipe, outside-tap, Back and pane semantics remain active.
//
// Plan 3.3 Sheet Geometry Fix:
//   - Opens modal sheets at the Cupertino Medium detent (~half screen).
//   - Preserves Large as an upward-drag expansion detent.
//   - Measures the shared surface at full height so Medium stays
//     physically attached to the bottom edge instead of floating.
//   - Explicitly retains the Cupertino drag handle with multiple detents.
//
// Plan 3.3 Regression Stabilization:
//   - Keeps the anchor surface full-height but makes the visible content
//     viewport match Medium/Large so bottom actions remain above nav bars.
//   - Adds a full-window Dialog constraint portal for nested picker sheets.
//   - HigModalSheet remains the sole owner of Cupertino sheet lifecycle.
// ====================================================================

package com.lias.remote.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoBottomSheetScaffold
import com.slapps.cupertino.CupertinoBottomSheetScaffoldDefaults
import com.slapps.cupertino.CupertinoBottomSheetDefaults
import com.slapps.cupertino.CupertinoSheetValue
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.PresentationDetent
import com.slapps.cupertino.PresentationStyle
import com.slapps.cupertino.rememberCupertinoBottomSheetScaffoldState
import com.slapps.cupertino.rememberCupertinoSheetState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val LocalHigAnimatedDismiss =
    staticCompositionLocalOf<
        (() -> Unit)?
    > {
        null
    }

@Composable
fun rememberHigAnimatedDismiss(
    fallback: () -> Unit
): () -> Unit =
    LocalHigAnimatedDismiss.current
        ?: fallback

private val LocalHigAnimatedCompletion =
    staticCompositionLocalOf<
        (((() -> Unit) -> Unit))?
    > {
        null
    }

@Composable
fun rememberHigAnimatedCompletion(
    fallbackDismiss: () -> Unit
): ((() -> Unit) -> Unit) =
    LocalHigAnimatedCompletion.current
        ?: { action ->
            action()
            fallbackDismiss()
        }

/*
 * Full-window presentation boundary for a sheet launched from inside
 * another scrollable sheet. Dialog owns only the window/constraints;
 * HigModalSheet still owns Cupertino motion, scrim, Back, swipe,
 * outside-tap and completion ordering.
 */
@Composable
fun HigModalSheetPortal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = {
            /*
             * Platform dismissal is disabled below. HigModalSheet owns
             * animated dismissal so parent cleanup cannot race motion.
             */
        },
        properties =
            DialogProperties(
                dismissOnBackPress =
                    false,
                dismissOnClickOutside =
                    false,
                usePlatformDefaultWidth =
                    false,
                decorFitsSystemWindows =
                    false
            )
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {
            HigModalSheet(
                onDismiss =
                    onDismiss,
                modifier =
                    modifier,
                accessibilityLabel =
                    accessibilityLabel,
                content =
                    content
            )
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun HigModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accessibilityLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Plan 3.3 CupertinoSheetState adapter:
    // Slanoss owns sheet motion, swipe and scrim interpolation.
    val coroutineScope =
        rememberCoroutineScope()

    var presentationStarted by
        remember {
            mutableStateOf(false)
        }

    var parentDismissDelivered by
        remember {
            mutableStateOf(false)
        }

    var completionInFlight by
        remember {
            mutableStateOf(false)
        }

    val sheetState =
        rememberCupertinoSheetState(
            initialValue =
                CupertinoSheetValue.Hidden,
            presentationStyle =
                PresentationStyle.Modal(
                    detents =
                        setOf(
                            PresentationDetent.Medium,
                            PresentationDetent.Large
                        ),
                    dismissOnClickOutside =
                        true
                )
        )

    val scaffoldState =
        rememberCupertinoBottomSheetScaffoldState(
            bottomSheetState =
                sheetState
        )

    fun deliverParentDismissOnce() {
        if (!parentDismissDelivered) {
            parentDismissDelivered = true
            onDismiss()
        }
    }

    fun requestAnimatedDismiss() {
        if (parentDismissDelivered) {
            return
        }

        coroutineScope.launch {
            runCatching {
                sheetState.hide()
            }
            deliverParentDismissOnce()
        }
    }

    fun requestAnimatedCompletion(
        action: () -> Unit
    ) {
        if (
            parentDismissDelivered ||
            completionInFlight
        ) {
            return
        }

        completionInFlight =
            true

        coroutineScope.launch {
            runCatching {
                sheetState.hide()
            }

            /*
             * Strict ordering:
             *   hide animation -> existing action -> parent cleanup.
             *
             * This prevents the Hidden observer from removing composition
             * before Save/Confirm logic runs.
             */
            action()

            completionInFlight =
                false

            deliverParentDismissOnce()
        }
    }

    LaunchedEffect(sheetState) {
        presentationStarted = true
        sheetState.show()
    }

    LaunchedEffect(
        sheetState,
        presentationStarted
    ) {
        snapshotFlow {
            sheetState.currentValue
        }
            .distinctUntilChanged()
            .collect { value ->
                if (
                    presentationStarted &&
                    !completionInFlight &&
                    value is CupertinoSheetValue.Hidden &&
                    sheetState.targetValue is CupertinoSheetValue.Hidden
                ) {
                    deliverParentDismissOnce()
                }
            }
    }

    BackHandler(
        enabled =
            !parentDismissDelivered
    ) {
        requestAnimatedDismiss()
    }

    CompositionLocalProvider(
        LocalHigAnimatedDismiss provides
            ::requestAnimatedDismiss,
        LocalHigAnimatedCompletion provides
            ::requestAnimatedCompletion
    ) {
        CupertinoBottomSheetScaffold(
            modifier =
                Modifier.fillMaxSize(),
            scaffoldState =
                scaffoldState,
            colors =
                CupertinoBottomSheetScaffoldDefaults.colors(
                    sheetContainerColor =
                        LiasThemeColors.secondaryBackground,
                    containerColor =
                        Color.Transparent,
                    contentColor =
                        LiasThemeColors.label,
                    scrimColor =
                        Color.Black.copy(
                            alpha = 0.40f
                        ),
                    scaledScaffoldBackgroundColor =
                        Color.Transparent
                ),
            sheetShape =
                RoundedCornerShape(
                    topStart =
                        HigSpec.SheetCorner,
                    topEnd =
                        HigSpec.SheetCorner
                ),
            sheetDragHandle = {
                CupertinoBottomSheetDefaults
                    .DragHandle()
            },
            sheetSwipeEnabled =
                true,
            applyContentScaling =
                false,
            sheetContent = {
                /*
                 * Slanoss needs a full-height measured surface for correct
                 * Medium/Large anchors. The user-visible content viewport,
                 * however, must match the active detent so navigation/IME
                 * insets land at the visible bottom rather than below it.
                 */
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                ) {
                    val sheetViewportFraction =
                        if (
                            sheetState.currentValue is
                                CupertinoSheetValue.Expanded ||
                            sheetState.targetValue is
                                CupertinoSheetValue.Expanded
                        ) {
                            1.0f
                        } else {
                            0.5f
                        }

                    Column(
                        modifier =
                            modifier
                                .align(
                                    Alignment.TopCenter
                                )
                                .fillMaxWidth()
                                .fillMaxHeight(
                                    sheetViewportFraction
                                )
                                .navigationBarsPadding()
                                .imePadding()
                                .then(
                                    if (
                                        accessibilityLabel.isNullOrBlank()
                                    ) {
                                        Modifier
                                    } else {
                                        Modifier.semantics {
                                            paneTitle =
                                                accessibilityLabel
                                        }
                                    }
                                )
                                .padding(
                                    bottom = 24.dp
                                )
                    ) {
                        content()
                    }
                }
            }
        ) {
            Spacer(
                modifier =
                    Modifier.fillMaxSize()
            )
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

    val animatedCancel =
        rememberHigAnimatedDismiss(
            fallback =
                onCancel
        )


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
                animatedCancel
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
