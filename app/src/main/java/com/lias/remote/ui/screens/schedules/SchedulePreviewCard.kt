// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulePreviewCard.kt
// Version: 18.0.0
//
// Purpose:
//   Human-readable live schedule preview.
//
// This intentionally does not pretend calendar ranges are a permanent
// weekly projection. Recurring windows get a weekly summary; calendar
// windows display their explicit dates.
//
// Server remains authoritative for persisted conflict validation.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.schedule.ScheduleDraft
import com.lias.remote.core.schedule.ScheduleRuleScope
import com.lias.remote.core.schedule.ScheduleSemantics
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun SchedulePreviewCard(
    draft: ScheduleDraft
) {

    GroupedListCard(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        14.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    CupertinoText(
                        text =
                            "Preview",
                        style =
                            HigTypography.headline,
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            LiasThemeColors.label
                    )

                    CupertinoText(
                        text =
                            draft.timezone
                                .ifBlank {
                                    "Timezone not set"
                                },
                        style =
                            HigTypography.caption,
                        color =
                            LiasThemeColors.tertiaryLabel
                    )
                }

                StatusPill(
                    text =
                        ScheduleSemantics
                            .modeTitle(
                                draft.mode
                            ),
                    tone =
                        if (
                            ScheduleSemantics
                                .normalizeMode(
                                    draft.mode
                                ) ==
                            "whitelist"
                        ) {
                            PillTone.ALLOWED
                        } else {
                            PillTone.BLOCKED
                        }
                )
            }

            CupertinoText(
                text =
                    ScheduleSemantics
                        .modeExplanation(
                            draft.mode
                        ),
                style =
                    HigTypography.subheadline,
                color =
                    LiasThemeColors.secondaryLabel
            )

            if (
                draft.rules
                    .isEmpty()
            ) {

                CupertinoText(
                    text =
                        "No time windows.",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.orange
                )

                return@Column
            }

            draft.rules
                .forEachIndexed {
                        index,
                        rule ->

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(
                                2.dp
                            )
                    ) {

                        CupertinoText(
                            text =
                                "Window ${index + 1} · ${
                                    ScheduleSemantics
                                        .windowAction(
                                            draft.mode
                                        )
                                }",
                            style =
                                HigTypography.subheadline,
                            fontWeight =
                                FontWeight.SemiBold,
                            color =
                                LiasThemeColors.label
                        )

                        CupertinoText(
                            text =
                                ScheduleSemantics
                                    .ruleSummary(
                                        rule
                                    ),
                            style =
                                HigTypography.caption,
                            color =
                                LiasThemeColors.secondaryLabel
                        )

                        if (
                            rule.scope ==
                            ScheduleRuleScope.CALENDAR
                        ) {

                            CupertinoText(
                                text =
                                    "Calendar range · end date inclusive",
                                style =
                                    HigTypography.caption,
                                color =
                                    LiasThemeColors.tertiaryLabel
                            )
                        }
                    }
                }

            CupertinoText(
                text =
                    if (
                        ScheduleSemantics
                            .normalizeMode(
                                draft.mode
                            ) ==
                        "whitelist"
                    ) {
                        "Outside every matching window → Block"
                    } else {
                        "Outside every matching window → Allow"
                    },
                style =
                    HigTypography.caption,
                color =
                    LiasThemeColors.tertiaryLabel
            )
        }
    }
}
