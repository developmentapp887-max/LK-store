package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppUpdateInfo
import com.example.data.model.DownloadHistoryEntry
import com.example.data.model.LibraryItem
import com.example.data.model.MarketplaceApp
import com.example.data.model.UiState
import com.example.data.model.WishlistItem
import com.example.ui.components.AppIconView
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.LkVerifiedGreen
import com.example.util.formatFileSize
import com.example.util.formatTimestamp

@Composable
fun LibraryScreen(
    libraryState: UiState<List<LibraryItem>>,
    appsState: UiState<List<MarketplaceApp>>,
    availableUpdates: List<AppUpdateInfo>,
    onOpenApp: (String) -> Unit,
    onUpdateApp: (MarketplaceApp) -> Unit,
    onRemoveFromLibrary: (appId: String, title: String) -> Unit,
    onBrowseStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUpdatesOnly by remember { mutableStateOf(false) }

    when (libraryState) {
        is UiState.Loading -> LoadingStateView(modifier = modifier)
        is UiState.Error -> ErrorStateView(message = libraryState.message, modifier = modifier)
        is UiState.Success -> {
            val allLibrary = libraryState.data
            val approvedMap = ((appsState as? UiState.Success)?.data ?: emptyList()).associateBy { it.id }
            val updateIds = availableUpdates.map { it.latestApp.id }.toSet()
            val displayed = if (showUpdatesOnly) {
                allLibrary.filter { updateIds.contains(it.appId) }
            } else {
                allLibrary
            }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your App Library", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Track downloaded apps and check for newer verified releases.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !showUpdatesOnly,
                                onClick = { showUpdatesOnly = false },
                                label = { Text("All (${allLibrary.size})") },
                                modifier = Modifier.testTag("library_all_chip")
                            )
                            FilterChip(
                                selected = showUpdatesOnly,
                                onClick = { showUpdatesOnly = true },
                                label = { Text("Updates (${availableUpdates.size})") },
                                leadingIcon = { Icon(Icons.Rounded.SystemUpdate, null, Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("library_updates_chip")
                            )
                        }
                    }
                }

                if (displayed.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = if (showUpdatesOnly) "All library apps are up to date" else "Your library is empty",
                            subtitle = if (showUpdatesOnly) {
                                "No newer versions are pending for your downloaded apps."
                            } else {
                                "Download any approved APK from LK Store to track it and receive update alerts."
                            },
                            actionLabel = if (!showUpdatesOnly) "Explore Store" else null,
                            onAction = if (!showUpdatesOnly) onBrowseStore else null
                        )
                    }
                } else {
                    items(displayed, key = { it.id }) { item ->
                        val latest = approvedMap[item.appId]
                        val hasUpdate = latest != null && latest.versionCode > item.versionCode
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenApp(item.appId) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AppIconView(
                                    iconUrl = item.iconUrl,
                                    title = item.appTitle,
                                    size = 54.dp,
                                    verified = item.sha256.isNotBlank()
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.appTitle, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${item.developerName} • Installed v${item.versionName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (latest != null && latest.versionCode > item.versionCode) {
                                        Text(
                                            text = "Update ready: v${latest.versionName} (${formatFileSize(latest.fileSizeBytes)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "Updated ${formatTimestamp(item.updatedAt ?: item.createdAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (latest != null && latest.versionCode > item.versionCode) {
                                    FilledTonalButton(
                                        onClick = { onUpdateApp(latest) },
                                        modifier = Modifier.testTag("library_update_${item.appId}")
                                    ) {
                                        Text("Update")
                                    }
                                }
                                IconButton(
                                    onClick = { onRemoveFromLibrary(item.appId, item.appTitle) }
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove from library")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WishlistScreen(
    wishlistState: UiState<List<WishlistItem>>,
    appsState: UiState<List<MarketplaceApp>>,
    onOpenApp: (String) -> Unit,
    onRemoveWishlist: (MarketplaceApp) -> Unit,
    onRemoveWishlistFallback: (WishlistItem) -> Unit,
    onBrowseStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (wishlistState) {
        is UiState.Loading -> LoadingStateView(modifier = modifier)
        is UiState.Error -> ErrorStateView(message = wishlistState.message, modifier = modifier)
        is UiState.Success -> {
            val items = wishlistState.data
            val approvedMap = ((appsState as? UiState.Success)?.data ?: emptyList()).associateBy { it.id }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Saved Wishlist", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "${items.size} saved application${if (items.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (items.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Your wishlist is empty",
                            subtitle = "Bookmark apps and games from the marketplace to revisit or download them later.",
                            actionLabel = "Browse Marketplace",
                            onAction = onBrowseStore
                        )
                    }
                } else {
                    items(items, key = { it.id }) { wish ->
                        val app = approvedMap[wish.appId]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenApp(wish.appId) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AppIconView(
                                    iconUrl = wish.iconUrl,
                                    title = wish.appTitle,
                                    size = 54.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(wish.appTitle, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${wish.developerName} • ${wish.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (app != null) {
                                        Text(
                                            text = "v${app.versionName} • ${formatFileSize(app.fileSizeBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        if (app != null) onRemoveWishlist(app)
                                        else onRemoveWishlistFallback(wish)
                                    }
                                ) {
                                    Icon(Icons.Rounded.BookmarkRemove, contentDescription = "Remove from wishlist")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadHistoryScreen(
    historyState: UiState<List<DownloadHistoryEntry>>,
    appsState: UiState<List<MarketplaceApp>>,
    onOpenApp: (String) -> Unit,
    onRedownload: (MarketplaceApp) -> Unit,
    modifier: Modifier = Modifier
) {
    when (historyState) {
        is UiState.Loading -> LoadingStateView(modifier = modifier)
        is UiState.Error -> ErrorStateView(message = historyState.message, modifier = modifier)
        is UiState.Success -> {
            val entries = historyState.data
            val approvedMap = ((appsState as? UiState.Success)?.data ?: emptyList()).associateBy { it.id }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Download History", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Chronological log of verified APK downloads initiated from your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (entries.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No downloads recorded yet",
                            subtitle = "Verified APK downloads you start in LK Store are logged here with version and checksum info."
                        )
                    }
                } else {
                    items(entries, key = { it.id }) { entry ->
                        val liveApp = approvedMap[entry.appId]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenApp(entry.appId) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AppIconView(
                                    iconUrl = entry.iconUrl,
                                    title = entry.appTitle,
                                    size = 50.dp,
                                    verified = entry.verified
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(entry.appTitle, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "v${entry.versionName} • ${formatFileSize(entry.fileSizeBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (entry.verified) {
                                            Icon(
                                                imageVector = Icons.Rounded.VerifiedUser,
                                                contentDescription = null,
                                                tint = LkVerifiedGreen,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = formatTimestamp(entry.downloadedAt, "MMM d, yyyy • h:mm a"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (liveApp != null) {
                                    IconButton(onClick = { onRedownload(liveApp) }) {
                                        Icon(Icons.Rounded.Download, contentDescription = "Re-download APK")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
