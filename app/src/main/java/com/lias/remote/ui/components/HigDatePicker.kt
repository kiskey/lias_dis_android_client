// ====================================================================
// File:
// app/src/main/java/com/lias/remote/ui/components/HigDatePicker.kt
//
// Purpose:
//   LIAS-owned adapter around Slanoss CupertinoDatePicker.
//
// Plan 3.1:
//   - Keeps raw CupertinoDatePicker usage out of screens.
//   - Defaults to Wheel mode for independent day/month/year columns.
//   - Preserves app-owned date conversion and schedule wire contracts.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.slapps.cupertino.CupertinoDatePicker
import com.slapps.cupertino.DatePickerStyle
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.rememberCupertinoDatePickerState

enum class HigDatePickerMode {
    Wheel,
    Pager
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun HigDatePicker(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    yearRange: IntRange,
    modifier: Modifier = Modifier,
    mode: HigDatePickerMode = HigDatePickerMode.Wheel
) {
    val latestOnDateSelected by
        rememberUpdatedState(
            onDateSelected
        )

    val pickerState =
        rememberCupertinoDatePickerState(
            initialSelectedDateMillis =
                selectedDateMillis,
            yearRange =
                yearRange
        )

    LaunchedEffect(
        pickerState.selectedDateMillis
    ) {
        latestOnDateSelected(
            pickerState.selectedDateMillis
        )
    }

    CupertinoDatePicker(
        state =
            pickerState,
        style =
            when (mode) {
                HigDatePickerMode.Wheel ->
                    DatePickerStyle.Wheel()

                HigDatePickerMode.Pager ->
                    DatePickerStyle.Pager()
            },
        modifier =
            modifier
    )
}
