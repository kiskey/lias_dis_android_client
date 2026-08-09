// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/SegmentedControl.kt
// Version: 28.0.0
//
// Purpose:
//   Cupertino-style mutually exclusive segmented control.
//
// Batch 21:
//   - 48dp touch height.
//   - Every segment exposes RadioButton semantics.
//   - Selected state is explicitly announced.
//   - Retains animated Cupertino thumb.
//   - Destructive final segment remains visually differentiated, but
//     color is no longer its only state signal.
//   - Handles larger text without horizontal clipping where practical.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {

    if (
        options.isEmpty()
    ) {
        return
    }

    val selectedIndex =
        options
            .indexOfFirst {
                it.equals(
                    selectedOption,
                    ignoreCase =
                        true
                )
            }
            .coerceAtLeast(
                0
            )

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(
                    min =
                        48.dp
                )
                .clip(
                    RoundedCornerShape(
                        HigSpec
                            .SegmentedControlCorner
                    )
                )
                .background(
                    LiasThemeColors.fill2
                )
                .border(
                    width = 0.5.dp,
                    color = LiasThemeColors.separator,
                    shape = RoundedCornerShape(HigSpec.SegmentedControlCorner)
                )
                .padding(
                    3.dp
                )
    ) {

        val segmentWidth =
            maxWidth /
                options.size

        val animatedOffset by
            animateDpAsState(
                targetValue =
                    segmentWidth *
                        selectedIndex,
                animationSpec =
                    spring(
                        dampingRatio =
                            0.82f,
                        stiffness =
                            400f
                    ),
                label =
                    "segmentThumb"
            )

        Box(
            modifier =
                Modifier
                    .offset(
                        x =
                            animatedOffset
                    )
                    .width(
                        segmentWidth
                    )
                    .fillMaxHeight()
                    .shadow(
                        elevation =
                            2.dp,
                        shape =
                            RoundedCornerShape(
                                HigSpec
                                    .SegmentedControlCorner -
                                    3.dp
                            )
                    )
                    .background(
                        color =
                            if (
                                isDestructive &&
                                selectedIndex ==
                                options.lastIndex
                            ) {
                                LiasThemeColors.red
                            } else {
                                LiasThemeColors
                                    .secondaryBackground
                            },
                        shape =
                            RoundedCornerShape(
                                HigSpec
                                    .SegmentedControlCorner -
                                    3.dp
                            )
                    )
        )

        Row(
            modifier =
                Modifier.fillMaxSize()
        ) {

            options.forEachIndexed {
                    index,
                    option ->

                val selected =
                    index ==
                        selectedIndex

                val destructiveSelection =
                    isDestructive &&
                        selected &&
                        index ==
                        options.lastIndex

                val textColor =
                    when {

                        destructiveSelection ->
                            Color.White

                        selected ->
                            LiasThemeColors.blue

                        else ->
                            LiasThemeColors
                                .secondaryLabel
                    }

                val interactionSource =
                    remember(
                        option
                    ) {
                        MutableInteractionSource()
                    }

                Box(
                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .fillMaxHeight()
                            .semantics(
                                mergeDescendants =
                                    true
                            ) {

                                role =
                                    Role.RadioButton

                                this.selected =
                                    selected

                                stateDescription =
                                    buildString {

                                        append(
                                            if (
                                                selected
                                            ) {
                                                "Selected"
                                            } else {
                                                "Not selected"
                                            }
                                        )

                                        if (
                                            isDestructive &&
                                            index ==
                                            options.lastIndex
                                        ) {
                                            append(
                                                ", destructive option"
                                            )
                                        }
                                    }
                            }
                            .clickable(
                                interactionSource =
                                    interactionSource,
                                indication =
                                    null
                            ) {

                                onOptionSelected(
                                    option
                                )
                            }
                            .padding(
                                horizontal =
                                    4.dp,
                                vertical =
                                    6.dp
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    CupertinoText(
                        text =
                            option,
                        color =
                            textColor,
                        style =
                            HigTypography
                                .subheadline,
                        fontWeight =
                            if (
                                selected
                            ) {
                                FontWeight
                                    .SemiBold
                            } else {
                                FontWeight
                                    .Normal
                            },
                        textAlign =
                            TextAlign.Center,
                        maxLines =
                            2,
                        overflow =
                            TextOverflow
                                .Ellipsis
                    )
                }
            }
        }
    }
}
