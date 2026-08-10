// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/HigField.kt
// Version: 29.0.0
//
// Purpose:
//   Unified LIAS form-field surface.
//
// Batch 29:
//   - 48dp+ interactive surface.
//   - Preserves cursor selection and IME composition for long text.
//   - Keeps the caret visible while single-line content scrolls.
//   - Supports semantic keyboards and multiline editing.
//   - Read-only/clickable fields expose Button semantics.
//   - Editable fields retain native text-field semantics.
//   - Labels remain visible instead of relying on placeholder text.
//   - Larger text may wrap naturally.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun HigField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    visualTransformation:
        VisualTransformation =
        VisualTransformation.None,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    HigConfiguredField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        visualTransformation = visualTransformation,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
internal fun HigConfiguredField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    visualTransformation:
        VisualTransformation =
        VisualTransformation.None,
    keyboardOptions:
        KeyboardOptions =
        KeyboardOptions.Default,
    keyboardActions:
        KeyboardActions =
        KeyboardActions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 5,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {

    val interactiveModifier =
        if (
            onClick !=
            null
        ) {

            Modifier
                .semantics(
                    mergeDescendants =
                        true
                ) {

                    role =
                        Role.Button

                    contentDescription =
                        label

                    stateDescription =
                        value
                            .ifBlank {
                                placeholder
                            }
                            .ifBlank {
                                "No value selected"
                            }
                }
                .clickable(
                    enabled =
                        enabled,
                    onClick =
                        onClick
                )

        } else {
            Modifier
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(
                    min =
                        56.dp
                )
                .clip(
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .background(
                    LiasThemeColors
                        .tertiaryBackground
                )
                .then(
                    interactiveModifier
                )
                .padding(
                    horizontal =
                        14.dp,
                    vertical =
                        8.dp
                )
    ) {

        CupertinoText(
            text =
                label.uppercase(),
            style =
                HigTypography.caption,
            color =
                LiasThemeColors
                    .tertiaryLabel
        )

        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )

        CursorSafeTextField(
            value =
                value,
            onValueChange =
                onValueChange,
            placeholder = {

                if (
                    placeholder
                        .isNotBlank()
                ) {

                    CupertinoText(
                        text =
                            placeholder,
                        style =
                            HigTypography.body,
                        color =
                            LiasThemeColors
                                .tertiaryLabel
                    )
                }
            },
            enabled =
                enabled &&
                    onClick == null,
            visualTransformation =
                visualTransformation,
            keyboardOptions =
                keyboardOptions,
            keyboardActions =
                keyboardActions,
            singleLine =
                singleLine,
            minLines =
                minLines,
            maxLines =
                maxLines,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min =
                            44.dp
                    )
        )
    }
}

/**
 * A controlled text editor that retains selection and IME composition while
 * the screen continues to own the String value. BasicTextField keeps the
 * caret visible when a single-line value scrolls horizontally.
 */
@Composable
internal fun CursorSafeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 5
) {
    var editorValue by
        remember {
            mutableStateOf(
                TextFieldValue(
                    text = value,
                    selection = TextRange(value.length)
                )
            )
        }

    LaunchedEffect(value) {
        if (value != editorValue.text) {
            editorValue = reconcileEditorValue(editorValue, value)
        }
    }

    BasicTextField(
        value = editorValue,
        onValueChange = { updated ->
            editorValue = updated
            if (updated.text != value) {
                onValueChange(updated.text)
            }
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle =
            HigTypography.body.copy(
                color =
                    if (enabled) {
                        LiasThemeColors.label
                    } else {
                        LiasThemeColors.tertiaryLabel
                    }
            ),
        cursorBrush = SolidColor(LiasThemeColors.blue),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        decorationBox = { innerTextField ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment =
                    if (singleLine) {
                        Alignment.CenterStart
                    } else {
                        Alignment.TopStart
                    }
            ) {
                if (editorValue.text.isEmpty() && placeholder != null) {
                    placeholder()
                }
                innerTextField()
            }
        }
    )
}

internal fun reconcileEditorValue(
    current: TextFieldValue,
    externalText: String
): TextFieldValue {
    if (current.text == externalText) {
        return current
    }

    val length = externalText.length
    return current.copy(
        text = externalText,
        selection =
            TextRange(
                current.selection.start.coerceIn(0, length),
                current.selection.end.coerceIn(0, length)
            ),
        composition = null
    )
}
