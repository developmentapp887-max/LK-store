package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppUpdateInfo
import com.example.data.model.MarketplaceApp
import com.example.data.model.UiState
import com.example.ui.components.AppCompactCard
import com.example.ui.components.AppListRowCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView

@Composable
fun HomeScreen(
    appsState: UiState<List<MarketplaceApp>>,
    wishlistIds: Set<String>,
    availableUpdates: List<AppUpdateInfo>,
    onOpenApp: (String) -> Unit,
    onOpenAppsTab: () -> Unit,
    onOpenGamesTab: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenHelp: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleWishlist: (MarketplaceApp) -> Unit,
    onQuickDownload: (MarketplaceApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    when (appsState) {
        is UiState.Loading -> LoadingStateView(modifier = modifier)
        is UiState.Error -> ErrorStateView(message = appsState.message, modifier = modifier)
        is UiState.Success -> {
            val allApproved = appsState.data
            val featuredApps = allApproved.filter { it.featured }.ifEmpty { allApproved.take(6) }
            val topApps = allApproved.filter { !it.isGame }.sortedByDescending { it.ratingAverage }.take(8)
            val trendingGames = allApproved.filter { it.isGame }.sortedByDescending { it.downloadCount }.take(8)
            val updateAppIds = availableUpdates.map { it.latestApp.id }.toSet()
            val categories = allApproved.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    HeroBannerCard(
                        approvedCount = allApproved.size,
                        onExploreApps = onOpenAppsTab,
                        onExploreCategories = onOpenCategories,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Quick Navigation Shortcuts
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            AssistChip(
                                onClick = onOpenAppsTab,
                                label = { Text("Apps") },
                                leadingIcon = { Icon(Icons.Rounded.Apps, null, Modifier.size(18.dp)) },
                                modifier = Modifier.testTag("home_chip_apps")
                            )
                        }
                        item {
                            AssistChip(
                                onClick = onOpenGamesTab,
                                label = { Text("Games") },
                                leadingIcon = { Icon(Icons.Rounded.SportsEsports, null, Modifier.size(18.dp)) },
                                modifier = Modifier.testTag("home_chip_games")
                            )
                        }
                        item {
                            AssistChip(
                                onClick = onOpenCategories,
                                label = { Text("Categories") },
                                leadingIcon = { Icon(Icons.Rounded.Category, null, Modifier.size(18.dp)) },
                                modifier = Modifier.testTag("home_chip_categories")
                            )
                        }
                        item {
                            AssistChip(
                                onClick = onOpenWishlist,
                                label = { Text("Wishlist") },
                                leadingIcon = { Icon(Icons.Rounded.Bookmark, null, Modifier.size(18.dp)) },
                                modifier = Modifier.testTag("home_chip_wishlist")
                            )
                        }
                        item {
                            AssistChip(
                                onClick = onOpenDownloads,
                                label = { Text("Downloads") },
                                leadingIcon = { Icon(Icons.Rounded.History, null, Modifier.size(18.dp)) },
                                modifier = Modifier.testTag("home_chip_downloads")
                            )
                        }
                    }
                }

                // Updates Banner if library apps have newer versions
                if (availableUpdates.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable(onClick = onOpenLibrary)
                                .testTag("home_updates_banner"),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${availableUpdates.size} App Update${if (availableUpdates.size > 1) "s" else ""} Available",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Newer verified APK releases detected in your library.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Open library updates",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                if (allApproved.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = stringResource(R.string.empty_catalog_title),
                            subtitle = stringResource(R.string.empty_catalog_subtitle),
                            actionLabel = "Firebase & LK APK POST Setup Guide",
                            onAction = onOpenHelp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    if (categories.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Browse by Category",
                                actionLabel = "All",
                                onAction = onOpenCategories
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(categories) { category ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { onSelectCategory(category) }
                                    ) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (featuredApps.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Featured & Editor's Choice",
                                actionLabel = "Explore",
                                onAction = onOpenAppsTab
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(featuredApps, key = { it.id }) { app ->
                                    AppCompactCard(
                                        app = app,
                                        hasUpdate = updateAppIds.contains(app.id),
                                        onClick = { onOpenApp(app.id) }
                                    )
                                }
                            }
                        }
                    }

                    if (trendingGames.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Trending Android Games",
                                actionLabel = "All Games",
                                onAction = onOpenGamesTab
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(trendingGames, key = { it.id }) { game ->
                                    AppCompactCard(
                                        app = game,
                                        hasUpdate = updateAppIds.contains(game.id),
                                        onClick = { onOpenApp(game.id) }
                                    )
                                }
                            }
                        }
                    }

                    if (topApps.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Top Approved Apps",
                                actionLabel = "See All",
                                onAction = onOpenAppsTab
                            )
                        }
                        items(topApps, key = { it.id }) { app ->
                            AppListRowCard(
                                app = app,
                                isWishlisted = wishlistIds.contains(app.id),
                                hasUpdate = updateAppIds.contains(app.id),
                                onClick = { onOpenApp(app.id) },
                                onToggleWishlist = { onToggleWishlist(app) },
                                onQuickDownload = { onQuickDownload(app) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBannerCard(
    approvedCount: Int,
    onExploreApps: () -> Unit,
    onExploreCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(MaterialTheme.shapes.extraLarge)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_lk_hero_banner),
            contentDescription = "LK Store featured showcase",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xE6081513),
                            Color(0xB3081513),
                            Color(0x4D081513)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF6EE7B7),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$approvedCount Approved Releases",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Curated Android APKs,\nVerified by LK Store",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Direct verified downloads, live ratings, and instant update checks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onExploreApps,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34D399),
                        contentColor = Color(0xFF00211B)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("hero_browse_apps_button")
                ) {
                    Text("Browse Apps", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(
                    onClick = onExploreCategories,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("Categories")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        TextButton(onClick = onAction) {
            Text(actionLabel)
            Spacer(Modifier.width(2.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(16.dp))
        }
    }
}
