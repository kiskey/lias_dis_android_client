// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/components/DependencyDeleteSheets.kt
// Version: 16.0.0
//
// Purpose:
//   Dependency-aware destructive-action presentation.
//
// Design:
//   - Do not present an enabled destructive button for an operation
//     the repository will reject.
//   - Explain exactly which policies/devices must be changed first.
//   - Never imply cascading behavior the LIAS backend does not provide.
// ====================================================================

package com.lias.remote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.util.ScheduleDependencyImpact
import com.lias.remote.core.util.TagDependencyImpact
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoText

@Composable
fun ScheduleDeleteSheet(
    impact: ScheduleDependencyImpact,
    authError: String? = null,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    HigModalSheet(
        onDismiss = onDismiss
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 16.dp
                    )
                    .verticalScroll(
                        rememberScrollState()
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            HigSheetHeader(
                title = "Delete Schedule",
                onCancel = onDismiss
            )

            CupertinoText(
                text =
                    impact.schedule.name.ifBlank {
                        "Unnamed Schedule"
                    },
                style = HigTypography.title2,
                fontWeight = FontWeight.Bold,
                color = LiasThemeColors.label
            )

            if (
                impact.hasDependencies
            ) {

                DependencyWarning(
                    title = "Schedule Is Still In Use",
                    message =
                        buildString {
                            append(
                                impact.referencingPolicies.size
                            )

                            append(
                                if (
                                    impact.referencingPolicies.size == 1
                                ) {
                                    " rule still references this schedule. "
                                } else {
                                    " rules still reference this schedule. "
                                }
                            )

                            append(
                                "Deleting it directly would leave dangling references and LIAS would fail those schedule bundles closed to Block."
                            )
                        }
                )

                CupertinoText(
                    text = "RULES USING THIS SCHEDULE",
                    style = HigTypography.caption,
                    color = LiasThemeColors.tertiaryLabel
                )

                GroupedListCard {

                    impact.referencingPolicies
                        .forEachIndexed { index, policy ->

                            GroupedListRow(
                                primaryText =
                                    policy.name.ifBlank {
                                        policy.id
                                    },
                                secondaryText =
                                    buildString {

                                        append(
                                            when (
                                                policy.type.lowercase()
                                            ) {
                                                "device" ->
                                                    "Device rule"

                                                "tag" ->
                                                    "Tag rule"

                                                "global" ->
                                                    "Global rule"

                                                else ->
                                                    "Rule"
                                            }
                                        )

                                        if (
                                            policy.targetID.isNotBlank()
                                        ) {
                                            append(" · ")
                                            append(policy.targetID)
                                        }
                                    },
                                showDivider =
                                    index <
                                        impact.referencingPolicies.lastIndex
                            )
                        }
                }

                CupertinoText(
                    text =
                        "Edit the rules above and remove this schedule first. Delete becomes available after no policy references remain.",
                    style = HigTypography.caption,
                    color = LiasThemeColors.secondaryLabel
                )

                HigButton(
                    text = "Cannot Delete While In Use",
                    onClick = {},
                    enabled = false,
                    style = HigButtonStyle.Gray,
                    modifier = Modifier.fillMaxWidth()
                )

            } else {

                CupertinoText(
                    text =
                        "This schedule is not referenced by any rule. Deleting it will permanently remove its time windows from LIAS.",
                    style = HigTypography.body,
                    color = LiasThemeColors.secondaryLabel
                )

                authError
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        message ->

                        DependencyWarning(
                            title =
                                "Biometric Verification Required",
                            message =
                                message
                        )
                    }

                HigButton(
                    text = "Delete Schedule",
                    onClick = onDelete,
                    style = HigButtonStyle.Danger,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TagDeleteSheet(
    impact: TagDependencyImpact,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    HigModalSheet(
        onDismiss = onDismiss
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 16.dp
                    )
                    .verticalScroll(
                        rememberScrollState()
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            HigSheetHeader(
                title = "Delete Tag",
                onCancel = onDismiss
            )

            CupertinoText(
                text =
                    impact.tag.name.ifBlank {
                        impact.tag.id
                    },
                style = HigTypography.title2,
                fontWeight = FontWeight.Bold,
                color = LiasThemeColors.label
            )

            when {

                impact.isInfrastructure -> {

                    DependencyWarning(
                        title = "Protected Infrastructure",
                        message =
                            "Infrastructure is a super-immutable system classification and cannot be deleted."
                    )
                }

                impact.isBuiltIn -> {

                    DependencyWarning(
                        title = "Built-In Tag",
                        message =
                            "Built-in LIAS system tags cannot be deleted."
                    )
                }

                impact.hasDependencies -> {

                    DependencyWarning(
                        title = "Tag Is Still In Use",
                        message =
                            "LIAS does not cascade tag deletion across devices and rules. Remove all dependencies before deleting this tag."
                    )

                    if (
                        impact.assignedDevices.isNotEmpty()
                    ) {

                        CupertinoText(
                            text = "ASSIGNED DEVICES",
                            style = HigTypography.caption,
                            color = LiasThemeColors.tertiaryLabel
                        )

                        GroupedListCard {

                            impact.assignedDevices
                                .forEachIndexed { index, device ->

                                    GroupedListRow(
                                        primaryText =
                                            device.displayName,
                                        secondaryText =
                                            device.currentIP.ifBlank {
                                                device.pdid
                                            },
                                        showDivider =
                                            index <
                                                impact.assignedDevices.lastIndex
                                    )
                                }
                        }
                    }

                    if (
                        impact.targetingPolicies.isNotEmpty()
                    ) {

                        CupertinoText(
                            text = "RULES TARGETING THIS TAG",
                            style = HigTypography.caption,
                            color = LiasThemeColors.tertiaryLabel
                        )

                        GroupedListCard {

                            impact.targetingPolicies
                                .forEachIndexed { index, policy ->

                                    GroupedListRow(
                                        primaryText =
                                            policy.name.ifBlank {
                                                policy.id
                                            },
                                        secondaryText =
                                            buildString {
                                                append(
                                                    policy.action
                                                        .replaceFirstChar {
                                                            it.uppercase()
                                                        }
                                                )

                                                append(
                                                    " · Priority "
                                                )

                                                append(
                                                    policy.priority
                                                )
                                            },
                                        showDivider =
                                            index <
                                                impact.targetingPolicies.lastIndex
                                    )
                                }
                        }
                    }

                    HigButton(
                        text = "Cannot Delete While In Use",
                        onClick = {},
                        enabled = false,
                        style = HigButtonStyle.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {

                    CupertinoText(
                        text =
                            "No devices or rules depend on this tag. It can be removed safely.",
                        style = HigTypography.body,
                        color = LiasThemeColors.secondaryLabel
                    )

                    HigButton(
                        text = "Delete Tag",
                        onClick = onDelete,
                        style = HigButtonStyle.Danger,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DependencyWarning(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                4.dp
            )
    ) {

        CupertinoText(
            text = title,
            style = HigTypography.headline,
            fontWeight = FontWeight.SemiBold,
            color = LiasThemeColors.red
        )

        CupertinoText(
            text = message,
            style = HigTypography.subheadline,
            color = LiasThemeColors.secondaryLabel
        )
    }
}
