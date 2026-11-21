package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppUpdateInfo
import com.example.data.model.UiState
import com.example.data.model.UserNotification
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.util.formatTimestamp

@Composable
fun NotificationsScreen(
    notificationsState: UiState<List<UserNotification>>,
    availableUpdates: List<AppUpdateInfo>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onOpenApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (notificationsState) {
        is UiState.Loading -> LoadingStateView(modifier = modifier)
        is UiState.Error -> ErrorStateView(message = notificationsState.message, modifier = modifier)
        is UiState.Success -> {
            val items = notificationsState.data
            val hasUnread = items.any { !it.read }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Notifications", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = "Updates, verified download logs & security alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (hasUnread) {
                            TextButton(
                                onClick = onMarkAllRead,
                                modifier = Modifier.testTag("mark_all_notifications_read")
                            ) {
                                Icon(Icons.Rounded.DoneAll, null, Modifier.size(16.dp))
                                Text(" Mark all read")
                            }
                        }
                    }
                }

                if (availableUpdates.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending App Updates",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(availableUpdates, key = { "upd_${it.latestApp.id}" }) { update ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenApp(update.latestApp.id) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${update.latestApp.title} v${update.latestApp.versionName} available",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Installed: v${update.libraryItem.versionName} • Tap to review & update",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (items.isEmpty() && availableUpdates.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No notifications yet",
                            subtitle = "Download alerts, version updates, and account notices will appear here."
                        )
                    }
                } else {
                    items(items, key = { it.id }) { notif ->
                        val icon = when (notif.type) {
                            "update" -> Icons.Rounded.SystemUpdate
                            "download" -> Icons.Rounded.DownloadDone
                            "security" -> Icons.Rounded.Security
                            else -> Icons.Rounded.NotificationsNone
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!notif.read) onMarkRead(notif.id)
                                    if (notif.appId.isNotBlank()) onOpenApp(notif.appId)
                                },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (notif.read) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        MaterialTheme.shapes.medium
                                    )
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (notif.read) FontWeight.Medium else FontWeight.Bold
                                    )
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatTimestamp(notif.createdAt, "MMM d, yyyy • h:mm a"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteNotification(notif.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete notification")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
