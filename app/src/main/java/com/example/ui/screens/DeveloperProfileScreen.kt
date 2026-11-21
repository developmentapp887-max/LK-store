package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DeveloperProfile
import com.example.data.model.MarketplaceApp
import com.example.data.model.UiState
import com.example.ui.components.AppListRowCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.theme.LkVerifiedGreen
import com.example.util.ApkDownloadHelper
import java.util.Locale

@Composable
fun DeveloperProfileScreen(
    developerId: String,
    developerName: String,
    developerProfile: DeveloperProfile?,
    appsState: UiState<List<MarketplaceApp>>,
    wishlistIds: Set<String>,
    updateAppIds: Set<String>,
    onOpenApp: (String) -> Unit,
    onToggleWishlist: (MarketplaceApp) -> Unit,
    onQuickDownload: (MarketplaceApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    when (appsState) {
        is UiState.Loading -> LoadingStateView(modifier = modifier)
        is UiState.Error -> ErrorStateView(message = appsState.message, modifier = modifier)
        is UiState.Success -> {
            val devApps = appsState.data.filter {
                it.developerId.equals(developerId, ignoreCase = true) ||
                    it.developerName.equals(developerName, ignoreCase = true)
            }
            val displayName = developerProfile?.name?.ifBlank { developerName } ?: developerName.ifBlank { "Developer" }
            val avgRating = devApps.map { it.ratingAverage }.filter { it > 0.0 }.average().takeIf { !it.isNaN() } ?: 0.0
            val totalDownloads = devApps.sumOf { it.downloadCount }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.large
                                )
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Code,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                        if (developerProfile?.verified == true || devApps.any { it.verified }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Verified,
                                                contentDescription = "Verified Developer",
                                                tint = LkVerifiedGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${devApps.size} approved release${if (devApps.size == 1) "" else "s"} • $totalDownloads downloads",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (avgRating > 0.0) {
                                        Text(
                                            text = "Catalog Rating: ${String.format(Locale.US, "%.1f ★", avgRating)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            val devBio = developerProfile?.bio.orEmpty()
                            if (devBio.isNotBlank()) {
                                Text(
                                    text = devBio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val website = developerProfile?.website.orEmpty()
                            if (ApkDownloadHelper.isVerifiedHttpsUrl(website)) {
                                OutlinedButton(
                                    onClick = { ApkDownloadHelper.openExternalUrl(context, website) }
                                ) {
                                    Icon(Icons.Rounded.Language, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Developer Website")
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Published Applications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (devApps.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No approved apps for $displayName",
                            subtitle = "Approved releases published by this developer will appear here."
                        )
                    }
                } else {
                    items(devApps, key = { it.id }) { app ->
                        AppListRowCard(
                            app = app,
                            isWishlisted = wishlistIds.contains(app.id),
                            hasUpdate = updateAppIds.contains(app.id),
                            onClick = { onOpenApp(app.id) },
                            onToggleWishlist = { onToggleWishlist(app) },
                            onQuickDownload = { onQuickDownload(app) }
                        )
                    }
                }
            }
        }
    }
}
