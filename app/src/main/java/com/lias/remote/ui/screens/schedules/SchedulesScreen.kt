// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/schedules/SchedulesScreen.kt
// Version: 16.0.0
//
// Purpose:
//   Dependency-aware LIAS schedule management.
//
// Batch 16:
//   - Exact policy usage is displayed.
//   - Referenced schedules open a dependency sheet.
//   - Delete is disabled until all references are removed.
//   - Removes false "defaults to open" wording.
//   - Editing/copying behavior from Batch 8 retained.
// ====================================================================

package com.lias.remote.ui.screens.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Schedule
import com.lias.remote.core.util.ConfigurationSafety
import com.lias.remote.core.util.ScheduleFormatting
import com.lias.remote.repositories.SyncState
import com.lias.remote.ui.LiasViewModel
import com.lias.remote.ui.components.DestructiveBiometricAuth
import com.lias.remote.ui.components.findFragmentActivity
import com.lias.remote.ui.components.requiresProtectedDelete
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.HigLargeTitleScaffold
import com.lias.remote.ui.components.HigSwipeRow
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.components.ListSectionHeader
import com.lias.remote.ui.components.MiniWeekStrip
import com.lias.remote.ui.components.PillTone
import com.lias.remote.ui.components.ScheduleDeleteSheet
import com.lias.remote.ui.components.ScreenStateTone
import com.lias.remote.ui.components.ScreenStateView
import com.lias.remote.ui.components.StaleDataNotice
import com.lias.remote.ui.components.StatusPill
import com.lias.remote.ui.components.SwipeAction
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Pencil
import io.github.alexzhirkevich.cupertino.icons.outlined.Trash

@Composable
fun SchedulesScreen(
    viewModel: LiasViewModel
) {

    val state by
        viewModel.state
            .collectAsState()

    val scrollState =
        rememberLazyListState()

    val hostActivity =
        LocalContext.current
            .findFragmentActivity()

    var showEditor by
        remember {
            mutableStateOf(false)
        }

    var editingSchedule by
        remember {
            mutableStateOf<Schedule?>(null)
        }

    var scheduleToDelete by
        remember {
            mutableStateOf<Schedule?>(null)
        }

    var scheduleDeleteAuthError by
        remember {
            mutableStateOf<String?>(null)
        }

    HigLargeTitleScaffold(
        title = "Schedules",
        scrollState = scrollState,
        navTrailing = {

            HigTextButton(
                text = "＋",
                onClick = {
                    editingSchedule = null
                    showEditor = true
                }
            )
        }
    ) { padding ->

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {

            when (
                val sync =
                    state.syncState
            ) {

                SyncState.Idle,
                SyncState.Loading -> {

                    if (
                        !state.isInitialLoaded
                    ) {

                        item {

                            ScreenStateView(
                                title = "Loading Schedules",
                                message =
                                    "Synchronizing schedule definitions from LIAS."
                            )
                        }

                        return@LazyColumn
                    }
                }

                is SyncState.Failed -> {

                    item {

                        ScreenStateView(
                            title =
                                "Unable to Load Schedules",
                            message =
                                sync.message,
                            actionText =
                                "Try Again",
                            onAction =
                                viewModel::refresh,
                            tone =
                                ScreenStateTone.ERROR
                        )
                    }

                    return@LazyColumn
                }

                is SyncState.Stale -> {

                    item {

                        StaleDataNotice(
                            message =
                                sync.message,
                            onRefresh =
                                viewModel::refresh
                        )
                    }
                }

                is SyncState.Ready ->
                    Unit
            }

            if (
                state.schedules.isEmpty()
            ) {

                item {

                    ScreenStateView(
                        title = "No Schedules",
                        message =
                            "Create reusable time windows, then attach them to rules.",
                        actionText =
                            "Create Schedule",
                        onAction = {
                            editingSchedule = null
                            showEditor = true
                        }
                    )
                }

                return@LazyColumn
            }

            item {

                ListSectionHeader(
                    "${state.schedules.size} Configured"
                )
            }

            items(
                items =
                    state.schedules,
                key = {
                    it.id
                }
            ) { schedule ->

                val impact =
                    remember(
                        schedule,
                        state.policies
                    ) {

                        ConfigurationSafety
                            .scheduleImpact(
                                schedule =
                                    schedule,
                                policies =
                                    state.policies
                            )
                    }

                GroupedListCard(
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 4.dp
                        )
                ) {

                    HigSwipeRow(
                        leadingAction =
                            SwipeAction(
                                icon =
                                    CupertinoIcons
                                        .Outlined
                                        .Pencil,
                                color =
                                    LiasThemeColors.blue,
                                onTrigger = {
                                    editingSchedule =
                                        schedule

                                    showEditor =
                                        true
                                }
                            ),
                        trailingAction =
                            SwipeAction(
                                icon =
                                    CupertinoIcons
                                        .Outlined
                                        .Trash,
                                color =
                                    if (
                                        impact.canDeleteSafely
                                    ) {
                                        LiasThemeColors.red
                                    } else {
                                        LiasThemeColors.orange
                                    },
                                onTrigger = {
                                    scheduleDeleteAuthError =
                                        null

                                    scheduleToDelete =
                                        schedule
                                }
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    14.dp
                                )
                        ) {

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment =
                                    Alignment.Top
                            ) {

                                Column(
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                ) {

                                    CupertinoText(
                                        text =
                                            schedule.name.ifBlank {
                                                "Unnamed Schedule"
                                            },
                                        style =
                                            HigTypography.headline,
                                        fontWeight =
                                            FontWeight.SemiBold,
                                        color =
                                            LiasThemeColors.label
                                    )

                                    CupertinoText(
                                        text =
                                            schedule.timezone.ifBlank {
                                                "Invalid timezone"
                                            },
                                        style =
                                            HigTypography.caption,
                                        color =
                                            LiasThemeColors.tertiaryLabel
                                    )
                                }

                                StatusPill(
                                    text =
                                        ScheduleFormatting
                                            .modeTitle(
                                                schedule.mode
                                            ),
                                    tone =
                                        if (
                                            schedule.mode.equals(
                                                "whitelist",
                                                true
                                            )
                                        ) {
                                            PillTone.ALLOWED
                                        } else {
                                            PillTone.BLOCKED
                                        }
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )

                            CupertinoText(
                                text =
                                    ScheduleFormatting
                                        .modeExplanation(
                                            schedule.mode
                                        ),
                                style =
                                    HigTypography.subheadline,
                                color =
                                    LiasThemeColors.secondaryLabel
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )

                            MiniWeekStrip(
                                schedules =
                                    listOf(
                                        schedule
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )

                            schedule.safeRules
                                .take(
                                    3
                                )
                                .forEach { rule ->

                                    CupertinoText(
                                        text =
                                            ScheduleFormatting
                                                .ruleSummary(
                                                    rule
                                                ),
                                        style =
                                            HigTypography.caption,
                                        color =
                                            LiasThemeColors.secondaryLabel,
                                        modifier =
                                            Modifier.padding(
                                                bottom = 3.dp
                                            )
                                    )
                                }

                            if (
                                schedule.safeRules.size >
                                3
                            ) {

                                CupertinoText(
                                    text =
                                        "+${schedule.safeRules.size - 3} more windows",
                                    style =
                                        HigTypography.caption,
                                    color =
                                        LiasThemeColors.tertiaryLabel
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )

                            CupertinoText(
                                text =
                                    impact.summary,
                                style =
                                    HigTypography.caption,
                                color =
                                    if (
                                        impact.hasDependencies
                                    ) {
                                        LiasThemeColors.orange
                                    } else {
                                        LiasThemeColors.tertiaryLabel
                                    }
                            )

                            if (
                                impact.referencingPolicies
                                    .isNotEmpty()
                            ) {

                                CupertinoText(
                                    text =
                                        impact.referencingPolicies
                                            .joinToString(
                                                separator = " · "
                                            ) {
                                                it.name.ifBlank {
                                                    it.id
                                                }
                                            },
                                    style =
                                        HigTypography.caption,
                                    color =
                                        LiasThemeColors.secondaryLabel,
                                    modifier =
                                        Modifier.padding(
                                            top = 3.dp
                                        )
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.End
                            ) {

                                HigTextButton(
                                    text = "Copy",
                                    onClick = {

                                        editingSchedule =
                                            schedule.copy(
                                                id = "",
                                                name =
                                                    "Copy of ${schedule.name}"
                                            )

                                        showEditor =
                                            true
                                    }
                                )

                                HigTextButton(
                                    text = "Edit",
                                    onClick = {

                                        editingSchedule =
                                            schedule

                                        showEditor =
                                            true
                                    }
                                )

                                HigTextButton(
                                    text =
                                        if (
                                            impact.canDeleteSafely
                                        ) {
                                            "Delete"
                                        } else {
                                            "Dependencies"
                                        },
                                    onClick = {
                                        scheduleDeleteAuthError =
                                            null

                                        scheduleToDelete =
                                            schedule
                                    },
                                    isDestructive =
                                        impact.canDeleteSafely
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (
        showEditor
    ) {

        ScheduleEditorSheet(
            initialSchedule =
                editingSchedule,
            onDismiss = {

                showEditor =
                    false

                editingSchedule =
                    null
            },
            onSave = { schedule ->

                viewModel.saveSchedule(
                    schedule
                )

                showEditor =
                    false

                editingSchedule =
                    null
            }
        )
    }

    scheduleToDelete
        ?.let { schedule ->

            val impact =
                ConfigurationSafety
                    .scheduleImpact(
                        schedule =
                            schedule,
                        policies =
                            state.policies
                    )

            ScheduleDeleteSheet(
                impact =
                    impact,
                authError =
                    scheduleDeleteAuthError,
                onDismiss = {
                    scheduleDeleteAuthError =
                        null

                    scheduleToDelete =
                        null
                },
                onDelete = {

                    if (
                        !requiresProtectedDelete(
                            schedule.id
                        )
                    ) {

                        scheduleDeleteAuthError =
                            "Only saved schedules can be deleted."

                    } else {

                        DestructiveBiometricAuth.authenticate(
                            activity =
                                hostActivity,
                            objectLabel =
                                "schedule “${schedule.name}”",
                            onAuthenticated = {

                                viewModel.deleteSchedule(
                                    schedule.id
                                )

                                scheduleDeleteAuthError =
                                    null

                                scheduleToDelete =
                                    null
                            },
                            onUnavailable = {
                                message ->

                                scheduleDeleteAuthError =
                                    message
                            }
                        )
                    }
                }
            )
        }
}
