package com.lias.remote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoSlider
import com.slapps.cupertino.CupertinoText

@Composable
fun MinutePickerSheet(
    targetLabel: String,
    onConfirm: (minutes: Int) -> Unit,
    onDismiss: () -> Unit,
    quickPicks: List<Int> = listOf(15, 30, 60, 120)
) {
    var selectedMinutes by remember { mutableFloatStateOf(30f) }

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HigSheetHeader(title = "Extend Access", onCancel = onDismiss)

            CupertinoText(
                text = targetLabel,
                style = HigTypography.title2,
                fontWeight = FontWeight.SemiBold,
                color = LiasThemeColors.label
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPicks.forEach { pick ->
                    val isSelected = selectedMinutes.toInt() == pick
                    HigButton(
                        text = "${pick}m",
                        onClick = { selectedMinutes = pick.toFloat() },
                        style = if (isSelected) HigButtonStyle.Primary else HigButtonStyle.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CupertinoText(
                    text = "${selectedMinutes.toInt()} minutes",
                    style = HigTypography.title1,
                    fontWeight = FontWeight.Bold
                )
                CupertinoSlider(
                    value = selectedMinutes,
                    onValueChange = { selectedMinutes = it },
                    valueRange = 1f..120f,
                    steps = 118,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            HigButton(
                text = "Allow for ${selectedMinutes.toInt()} Minutes",
                onClick = { onConfirm(selectedMinutes.toInt()) },
                style = HigButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
