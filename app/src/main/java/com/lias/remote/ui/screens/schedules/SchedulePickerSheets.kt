@file:OptIn(io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi::class)

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.components.HigButton
import com.lias.remote.ui.components.HigButtonStyle
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoDatePicker
import io.github.alexzhirkevich.cupertino.CupertinoTimePicker
import io.github.alexzhirkevich.cupertino.DatePickerStyle
import io.github.alexzhirkevich.cupertino.rememberCupertinoDatePickerState
import io.github.alexzhirkevich.cupertino.rememberCupertinoTimePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale

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
    val state = rememberCupertinoTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )

    HigModalSheet(onDismiss = onDismiss, accessibilityLabel = title) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HigSheetHeader(title = title, onCancel = onDismiss)
            CupertinoTimePicker(
                state = state,
                height = 190.dp,
                containerColor = LiasThemeColors.secondaryBackground,
                modifier = Modifier.fillMaxWidth()
            )
            HigButton(
                text = "Done",
                onClick = {
                    onConfirm(String.format(Locale.US, "%02d:%02d", state.hour, state.minute))
                },
                style = HigButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
        runCatching { LocalDate.parse(initialValue) }.getOrDefault(today)
    }
    val initialMillis = remember(initial) {
        initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val state = rememberCupertinoDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = (today.year - 2)..(today.year + 10)
    )

    HigModalSheet(onDismiss = onDismiss, accessibilityLabel = title) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HigSheetHeader(title = title, onCancel = onDismiss)
            CupertinoDatePicker(
                state = state,
                style = DatePickerStyle.Wheel(height = 190.dp),
                containerColor = LiasThemeColors.secondaryBackground,
                modifier = Modifier.fillMaxWidth()
            )
            HigButton(
                text = "Done",
                onClick = {
                    val date = Instant.ofEpochMilli(state.selectedDateMillis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                    onConfirm(date.toString())
                },
                style = HigButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
