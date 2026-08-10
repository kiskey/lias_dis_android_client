// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigButton.kt
// Version: 21.0.0
//
// Purpose:
//   Unified Cupertino/HIG button primitive.
//
// Batch 21:
//   - 48dp minimum touch target.
//   - Text buttons receive the same accessible hit target.
//   - Supports scaled text without clipping into one line.
//   - Preserves Cupertino button press behavior.
//   - Destructive state is communicated through semantics as well as
//     color.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText

enum class HigButtonStyle {
    Primary,
    Secondary,
    Gray,
    Danger
}

@Composable
fun HigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HigButtonStyle = HigButtonStyle.Primary,
    enabled: Boolean = true
) {

    val colors =
        when (
            style
        ) {

            HigButtonStyle.Primary ->
                CupertinoButtonDefaults
                    .filledButtonColors(
                        containerColor =
                            LiasThemeColors.blue,
                        contentColor =
                            Color.White
                    )

            HigButtonStyle.Secondary ->
                CupertinoButtonDefaults
                    .tintedButtonColors(
                        containerColor =
                            LiasThemeColors.fill2,
                        contentColor =
                            LiasThemeColors.blue
                    )

            HigButtonStyle.Gray ->
                CupertinoButtonDefaults
                    .tintedButtonColors(
                        containerColor =
                            LiasThemeColors.fill2,
                        contentColor =
                            LiasThemeColors.label
                    )

            HigButtonStyle.Danger ->
                CupertinoButtonDefaults
                    .filledButtonColors(
                        containerColor =
                            LiasThemeColors.red,
                        contentColor =
                            Color.White
                    )
        }

    CupertinoButton(
        onClick =
            onClick,
        enabled =
            enabled,
        colors =
            colors,
        modifier =
            modifier
                .defaultMinSize(
                    minHeight =
                        48.dp
                )
                .clip(
                    RoundedCornerShape(
                        HigSpec.ButtonCorner
                    )
                )
                .semantics(
                    mergeDescendants =
                        true
                ) {

                    role =
                        Role.Button

                    if (
                        style ==
                        HigButtonStyle.Danger
                    ) {
                        stateDescription =
                            "Destructive action"
                    }
                }
    ) {

        CupertinoText(
            text =
                text,
            style =
                HigTypography.headline,
            fontWeight =
                FontWeight.SemiBold,
            textAlign =
                TextAlign.Center,
            maxLines =
                2,
            overflow =
                TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HigTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {

    val contentColor =
        if (
            isDestructive
        ) {
            LiasThemeColors.red
        } else {
            LiasThemeColors.blue
        }

    CupertinoButton(
        onClick =
            onClick,
        enabled =
            enabled,
        colors =
            CupertinoButtonDefaults
                .plainButtonColors(
                    contentColor =
                        contentColor
                ),
        modifier =
            modifier
                .defaultMinSize(
                    minWidth =
                        44.dp,
                    minHeight =
                        48.dp
                )
                .semantics(
                    mergeDescendants =
                        true
                ) {

                    role =
                        Role.Button

                    if (
                        isDestructive
                    ) {
                        stateDescription =
                            "Destructive action"
                    }
                }
    ) {

        CupertinoText(
            text =
                text,
            style =
                HigTypography.body,
            fontWeight =
                FontWeight.Normal,
            color =
                contentColor,
            maxLines =
                2,
            overflow =
                TextOverflow.Ellipsis,
            textAlign =
                TextAlign.Center
        )
    }
}
