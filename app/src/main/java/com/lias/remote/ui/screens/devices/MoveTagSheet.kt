package com.lias.remote.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lias.remote.core.models.Device
import com.lias.remote.core.models.Tag
import com.lias.remote.ui.components.GroupedListCard
import com.lias.remote.ui.components.GroupedListRow
import com.lias.remote.ui.components.HigModalSheet
import com.lias.remote.ui.components.HigTextButton
import com.lias.remote.ui.theme.HigTypography
import com.lias.remote.ui.theme.LiasThemeColors
import io.github.alexzhirkevich.cupertino.CupertinoIcon
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Checkmark
import io.github.alexzhirkevich.cupertino.icons.outlined.Lock

@Composable
fun MoveTagSheet(
    device: Device,
    allTags: List<Tag>,
    onDismiss: () -> Unit,
    onConfirm: (tagIds: List<String>) -> Unit
) {
    val assignedTags = remember(device) {
        device.safeTags.ifEmpty { listOf("generic") }
    }

    val selectedTagIds = remember {
        mutableStateListOf<String>().apply { addAll(assignedTags) }
    }

    HigModalSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HigTextButton(text = "Cancel", onClick = onDismiss)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CupertinoText(
                        text = "Move Tag Group",
                        style = HigTypography.headline,
                        fontWeight = FontWeight.Bold
                    )
                    CupertinoText(
                        text = device.displayName,
                        style = HigTypography.caption,
                        color = LiasThemeColors.tertiaryLabel
                    )
                }
                HigTextButton(
                    text = "Done",
                    onClick = {
                        val finalTags = if (selectedTagIds.isEmpty()) listOf("generic") else selectedTagIds.toList()
                        onConfirm(finalTags)
                    }
                )
            }

            CupertinoText(
                text = "SELECT TAG GROUP",
                style = HigTypography.caption,
                color = LiasThemeColors.tertiaryLabel
            )

            GroupedListCard {
                allTags.forEachIndexed { index, tag ->
                    val isChecked = selectedTagIds.contains(tag.id)
                    val tagColor = try {
                        Color(android.graphics.Color.parseColor(tag.color))
                    } catch (_: Exception) {
                        Color.Gray
                    }

                    GroupedListRow(
                        primaryText = tag.name + if (tag.id == "infrastructure") " (Immune)" else "",
                        secondaryText = if (tag.builtin) "Built-in System Tag" else "Custom Tag",
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(tagColor)
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (tag.id == "infrastructure") {
                                    CupertinoIcon(
                                        imageVector = CupertinoIcons.Outlined.Lock,
                                        contentDescription = "Immune",
                                        tint = LiasThemeColors.tertiaryLabel,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                }
                                if (isChecked) {
                                    CupertinoIcon(
                                        imageVector = CupertinoIcons.Outlined.Checkmark,
                                        contentDescription = "Selected",
                                        tint = LiasThemeColors.blue
                                    )
                                }
                            }
                        },
                        showDivider = index < allTags.size - 1,
                        onClick = {
                            if (tag.id == "generic") {
                                selectedTagIds.clear()
                                selectedTagIds.add("generic")
                            } else {
                                selectedTagIds.remove("generic")
                                if (isChecked) {
                                    selectedTagIds.remove(tag.id)
                                    if (selectedTagIds.isEmpty()) {
                                        selectedTagIds.add("generic")
                                    }
                                } else {
                                    if (!selectedTagIds.contains(tag.id)) {
                                        selectedTagIds.add(tag.id)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
