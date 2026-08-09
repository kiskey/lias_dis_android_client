// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePickerSheets.kt
// Version: 28.2.1
//
// Focused Cupertino-style schedule picker presentation.
//
// - Keeps the v28.1 crash-safe LazyColumn + snap wheels.
// - Uses a full-window Dialog as a presentation portal so the picker is
//   never constrained by an individual schedule card.
// - Opaque focused background prevents other schedule windows from
//   visually competing with the active picker task.
// - Picker card animates upward from the bottom.
// - LIAS wire formats remain YYYY-MM-DD and HH:mm.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.theme.HigSpec
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val WHEEL_VISIBLE_ROWS = 5
private val WHEEL_ROW_HEIGHT = 44.dp
private val WHEEL_HEIGHT = WHEEL_ROW_HEIGHT * WHEEL_VISIBLE_ROWS

@Composable
fun ScheduleTimePickerSheet(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initial = remember(initialValue) {
        runCatching { LocalTime.parse(initialValue) }
            .getOrDefault(LocalTime.NOON)
    }

    var selectedHour by remember(initial.hour) {
        mutableIntStateOf(initial.hour)
    }

    var selectedMinute by remember(initial.minute) {
        mutableIntStateOf(initial.minute)
    }

    FocusedPickerDialog(
        title = title,
        onDismiss = onDismiss
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberWheel(
                values = (0..23).toList(),
                initialIndex = selectedHour,
                onSelected = { selectedHour = it },
                modifier = Modifier.weight(1f)
            )

            CupertinoText(
                text = ":",
                style = HigTypography.title1,
                color = LiasThemeColors.label,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            NumberWheel(
                values = (0..59).toList(),
                initialIndex = selectedMinute,
                onSelected = { selectedMinute = it },
                modifier = Modifier.weight(1f)
            )
        }

        CupertinoText(
            text = String.format(
                Locale.US,
                "%02d:%02d",
                selectedHour,
                selectedMinute
            ),
            style = HigTypography.headline,
            color = LiasThemeColors.secondaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        HigButton(
            text = "Done",
            onClick = {
                onConfirm(
                    String.format(
                        Locale.US,
                        "%02d:%02d",
                        selectedHour,
                        selectedMinute
                    )
                )
            },
            style = HigButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ScheduleDatePickerSheet(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val today = remember { LocalDate.now() }

    val initial = remember(initialValue) {
        runCatching { LocalDate.parse(initialValue) }
            .getOrDefault(today)
    }

    val firstYear = min(today.year - 2, initial.year - 1)
    val lastYear = max(today.year + 10, initial.year + 1)

    val startDate = remember(firstYear) {
        LocalDate.of(firstYear, 1, 1)
    }

    val endDate = remember(lastYear) {
        LocalDate.of(lastYear, 12, 31)
    }

    val dates = remember(startDate, endDate) {
        buildList {
            var cursor = startDate
            while (!cursor.isAfter(endDate)) {
                add(cursor)
                cursor = cursor.plusDays(1)
            }
        }
    }

    val initialIndex = remember(dates, initial) {
        dates.indexOf(initial).coerceAtLeast(0)
    }

    var selectedIndex by remember(initialIndex) {
        mutableIntStateOf(initialIndex)
    }

    val formatter = remember {
        DateTimeFormatter.ofPattern(
            "EEE, MMM d, yyyy",
            Locale.getDefault()
        )
    }

    FocusedPickerDialog(
        title = title,
        onDismiss = onDismiss
    ) {
        TextWheel(
            values = dates.map { it.format(formatter) },
            initialIndex = initialIndex,
            onSelectedIndex = { selectedIndex = it },
            modifier = Modifier.fillMaxWidth()
        )

        CupertinoText(
            text = dates[
                selectedIndex.coerceIn(dates.indices)
            ].toString(),
            style = HigTypography.headline,
            color = LiasThemeColors.secondaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        HigButton(
            text = "Done",
            onClick = {
                onConfirm(
                    dates[
                        selectedIndex.coerceIn(dates.indices)
                    ].toString()
                )
            },
            style = HigButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FocusedPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    var visible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LiasThemeColors.background)
                .windowInsetsPadding(WindowInsets.systemBars),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() +
                    slideInVertically { fullHeight -> fullHeight },
                exit = fadeOut() +
                    slideOutVertically { fullHeight -> fullHeight }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                topStart = HigSpec.SheetCorner,
                                topEnd = HigSpec.SheetCorner
                            )
                        )
                        .background(LiasThemeColors.secondaryBackground)
                        .padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HigSheetHeader(
                        title = title,
                        onCancel = onDismiss
                    )

                    content()
                }
            }
        }
    }
}

@Composable
private fun NumberWheel(
    values: List<Int>,
    initialIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    TextWheel(
        values = values.map {
            String.format(Locale.US, "%02d", it)
        },
        initialIndex = initialIndex.coerceIn(values.indices),
        onSelectedIndex = { index ->
            onSelected(
                values[
                    index.coerceIn(values.indices)
                ]
            )
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TextWheel(
    values: List<String>,
    initialIndex: Int,
    onSelectedIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return

    val safeInitial = initialIndex.coerceIn(values.indices)

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = safeInitial
    )

    val flingBehavior = rememberSnapFlingBehavior(
        lazyListState = listState
    )

    val selectedIndex by remember(listState, values) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo

            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                safeInitial
            } else {
                val center =
                    (
                        layoutInfo.viewportStartOffset +
                            layoutInfo.viewportEndOffset
                        ) / 2

                layoutInfo.visibleItemsInfo
                    .minByOrNull { item ->
                        abs(
                            (
                                item.offset +
                                    item.size / 2
                                ) - center
                        )
                    }
                    ?.index
                    ?.coerceIn(values.indices)
                    ?: safeInitial
            }
        }
    }

    LaunchedEffect(selectedIndex) {
        onSelectedIndex(selectedIndex)
    }

    Box(
        modifier = modifier.height(WHEEL_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WHEEL_ROW_HEIGHT)
                .padding(horizontal = 8.dp)
                .background(
                    color = LiasThemeColors.fill2,
                    shape = RoundedCornerShape(10.dp)
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(WHEEL_HEIGHT),
            contentPadding = PaddingValues(
                vertical =
                    (
                        WHEEL_HEIGHT -
                            WHEEL_ROW_HEIGHT
                        ) / 2
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = flingBehavior
        ) {
            items(
                count = values.size,
                key = { it }
            ) { index ->
                val isSelected = index == selectedIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WHEEL_ROW_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    CupertinoText(
                        text = values[index],
                        style = if (isSelected) {
                            HigTypography.headline
                        } else {
                            HigTypography.body
                        },
                        fontWeight = if (isSelected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                        color = if (isSelected) {
                            LiasThemeColors.label
                        } else {
                            LiasThemeColors.tertiaryLabel
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
