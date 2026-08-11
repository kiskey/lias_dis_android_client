// ====================================================================
// File: app/src/main/java/com/lias/remote/ui/screens/devices/UserAssignmentSheet.kt
// Version: 7.0.0
//
// Purpose:
//   Assign an existing LIAS user to a device or create a new user.
//
// Backend alignment:
//   POST /api/v1/users accepts User.
//   If ID is empty, LIAS generates the ID.
//
// Therefore this client intentionally submits:
//       User(id = "", name = ...)
// rather than manufacturing an Android-side identifier.
// ====================================================================

package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.User
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigConfiguredField
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.rememberHigAnimatedCompletion
import com.lias.remote.ui.components.HigSheetHeader
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import com.slapps.cupertino.CupertinoText

@Composable
fun UserAssignmentSheet(
    users: List<User>,
    assignedUserId: String?,
    onDismiss: () -> Unit,
    onSelectUser: (String) -> Unit,
    onCreateUser: (User) -> Unit
) {
    var showCreateUser by
        remember {
            mutableStateOf(false)
        }

    var newUserName by
        remember {
            mutableStateOf("")
        }

    HigModalSheet(
        onDismiss = onDismiss
    ) {
        val animatedComplete =
            rememberHigAnimatedCompletion(
                fallbackDismiss =
                    onDismiss
            )

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
                title =
                    "Assigned User",
                onCancel =
                    onDismiss,
                trailingAction = {
                    HigTextButton(
                        text =
                            if (showCreateUser) {
                                "Done"
                            } else {
                                "New User"
                            },
                        onClick = {
                            showCreateUser =
                                !showCreateUser

                            if (!showCreateUser) {
                                newUserName = ""
                            }
                        }
                    )
                }
            )

            if (users.isEmpty()) {
                CupertinoText(
                    text =
                        "No users have been created yet.",
                    style =
                        HigTypography.body,
                    color =
                        LiasThemeColors.secondaryLabel
                )
            } else {
                GroupedListCard {

                    users.forEachIndexed { index, user ->

                        val selected =
                            user.id ==
                                assignedUserId

                        GroupedListRow(
                            primaryText =
                                user.name.ifBlank {
                                    "Unnamed User"
                                },
                            secondaryText =
                                if (selected) {
                                    "Assigned to this device"
                                } else {
                                    null
                                },
                            trailingContent = {
                                if (selected) {
                                    CupertinoText(
                                        text = "✓",
                                        style =
                                            HigTypography.headline,
                                        fontWeight =
                                            FontWeight.Bold,
                                        color =
                                            LiasThemeColors.blue
                                    )
                                }
                            },
                            showDivider =
                                index <
                                    users.lastIndex,
                            onClick = {
                                if (!selected) {
                                    animatedComplete {
                                        onSelectUser(
                                            user.id
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (showCreateUser) {

                CupertinoText(
                    text =
                        "NEW USER",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.tertiaryLabel
                )

                HigConfiguredField(
                    value =
                        newUserName,
                    onValueChange = {
                        newUserName = it
                    },
                    label =
                        "Name",
                    placeholder =
                        "e.g. Alex",
                    keyboardOptions =
                        KeyboardOptions(
                            imeAction = ImeAction.Done
                        )
                )

                HigTextButton(
                    text =
                        "Create User",
                    onClick = {
                        val name =
                            newUserName.trim()

                        if (name.isNotBlank()) {
                            /*
                             * Empty ID is deliberate.
                             * LIAS owns user-ID generation.
                             */
                            onCreateUser(
                                User(
                                    id = "",
                                    name = name
                                )
                            )

                            newUserName = ""
                            showCreateUser = false
                        }
                    }
                )

                CupertinoText(
                    text =
                        "After the user is created, select them from the list above.",
                    style =
                        HigTypography.caption,
                    color =
                        LiasThemeColors.tertiaryLabel
                )
            }
        }
    }
}
