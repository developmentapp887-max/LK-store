package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSortOption
import com.example.data.model.MarketplaceApp
import com.example.data.model.UiState
import com.example.ui.components.AppListRowCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.viewmodel.StoreViewModel

private val DefaultAppCategories = listOf(
    "All", "Productivity", "Tools", "Social", "Media", "Education", "Finance", "Health", "Photography"
)

private val DefaultGameGenres = listOf(
    "All", "Action", "Puzzle", "Arcade", "Strategy", "Indie", "Racing"
)

@Composable
fun AppsScreen(
    appsState: UiState<List<MarketplaceApp>>,
    selectedCategory: String,
    sortOption: AppSortOption,
    wishlistIds: Set<String>,
    updateAppIds: Set<String>,
    onSelectCategory: (String) -> Unit,
    onSelectSort: (AppSortOption) -> Unit,
    onOpenApp: (String) -> Unit,
    onToggleWishlist: (MarketplaceApp) -> Unit,
    onQuickDownload: (MarketplaceApp) -> Unit,
    modifier: Modifier = Modifier
) {
    when (appsState) {
        is UiState.Loading -> LoadingStateView(modifier = modifier)
        is UiState.Error -> ErrorStateView(message = appsState.message, modifier = modifier)
        is UiState.Success -> {
            val nonGames = appsState.data.filter { !it.isGame }
            val dynamicCategories = (DefaultAppCategories + nonGames.map { it.category }.filter { it.isNotBlank() }).distinct()
            val filtered = nonGames.filter {
                selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)
            }
            val sorted = StoreViewModel.sortApps(filtered, sortOption)

            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val columns = when {
                    maxWidth >= 900.dp -> 2
                    else -> 1
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(columns) }) {
                        CatalogFilterHeader(
                            title = "Approved Android Apps",
                            subtitle = "${sorted.size} verified application${if (sorted.size == 1) "" else "s"}",
                            categories = dynamicCategories,
                            selectedCategory = selectedCategory,
                            sortOption = sortOption,
                            onSelectCategory = onSelectCategory,
                            onSelectSort = onSelectSort
                        )
                    }

                    if (sorted.isEmpty()) {
                        item(span = { GridItemSpan(columns) }) {
                            EmptyStateView(
                                title = "No approved apps in \"$selectedCategory\"",
                                subtitle = "Switch category filters or check back after new releases are approved.",
                                actionLabel = if (selectedCategory != "All") "Show All Apps" else null,
                                onAction = if (selectedCategory != "All") {
                                    { onSelectCategory("All") }
                                } else null
                            )
                        }
                    } else {
                        items(sorted, key = { it.id }) { app ->
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
}

@Composable
fun GamesScreen(
    appsState: UiState<List<MarketplaceApp>>,
    selectedGenre: String,
    sortOption: AppSortOption,
    wishlistIds: Set<String>,
    updateAppIds: Set<String>,
    onSelectGenre: (String) -> Unit,
    onSelectSort: (AppSortOption) -> Unit,
    onOpenApp: (String) -> Unit,
    onToggleWishlist: (MarketplaceApp) -> Unit,
    onQuickDownload: (MarketplaceApp) -> Unit,
    modifier: Modifier = Modifier
) {
    when (appsState) {
        is UiState.Loading -> LoadingStateView(modifier = modifier)
        is UiState.Error -> ErrorStateView(message = appsState.message, modifier = modifier)
        is UiState.Success -> {
            val games = appsState.data.filter { it.isGame }
            val filtered = games.filter { game ->
                selectedGenre == "All" ||
                    game.category.equals(selectedGenre, ignoreCase = true) ||
                    game.tags.any { it.equals(selectedGenre, ignoreCase = true) }
            }
            val sorted = StoreViewModel.sortApps(filtered, sortOption)

            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val columns = if (maxWidth >= 900.dp) 2 else 1
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(columns) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(155.dp)
                                    .clip(MaterialTheme.shapes.large)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_lk_games_banner),
                                    contentDescription = "LK Store Games Arena banner",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xD90B1220), Color(0x400B1220))
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "LK GAMES ARENA",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF34D399)
                                    )
                                    Text(
                                        text = "Verified Android & Indie Games",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            CatalogFilterHeader(
                                title = "Game Genres",
                                subtitle = "${sorted.size} approved game${if (sorted.size == 1) "" else "s"}",
                                categories = DefaultGameGenres,
                                selectedCategory = selectedGenre,
                                sortOption = sortOption,
                                onSelectCategory = onSelectGenre,
                                onSelectSort = onSelectSort
                            )
                        }
                    }

                    if (sorted.isEmpty()) {
                        item(span = { GridItemSpan(columns) }) {
                            EmptyStateView(
                                title = "No approved games in \"$selectedGenre\"",
                                subtitle = "Games with publication status 'approved' in Firestore appear here automatically.",
                                actionLabel = if (selectedGenre != "All") "Show All Games" else null,
                                onAction = if (selectedGenre != "All") {
                                    { onSelectGenre("All") }
                                } else null
                            )
                        }
                    } else {
                        items(sorted, key = { it.id }) { game ->
                            AppListRowCard(
                                app = game,
                                isWishlisted = wishlistIds.contains(game.id),
                                hasUpdate = updateAppIds.contains(game.id),
                                onClick = { onOpenApp(game.id) },
                                onToggleWishlist = { onToggleWishlist(game) },
                                onQuickDownload = { onQuickDownload(game) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogFilterHeader(
    title: String,
    subtitle: String,
    categories: List<String>,
    selectedCategory: String,
    sortOption: AppSortOption,
    onSelectCategory: (String) -> Unit,
    onSelectSort: (AppSortOption) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                OutlinedButton(
                    onClick = { sortMenuExpanded = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("catalog_sort_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sortOption.label, style = MaterialTheme.typography.labelMedium)
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    AppSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onSelectSort(option)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory.equals(category, ignoreCase = true),
                    onClick = { onSelectCategory(category) },
                    label = { Text(category) },
                    modifier = Modifier.testTag("category_chip_${category.lowercase()}")
                )
            }
        }
    }
}

private data class CategoryTileData(
    val name: String,
    val icon: ImageVector,
    val description: String
)

private val CategoryDirectory = listOf(
    CategoryTileData("Productivity", Icons.Rounded.WorkOutline, "Notes, docs, calendars & focus"),
    CategoryTileData("Tools", Icons.Rounded.Build, "Utilities, file managers & dev kits"),
    CategoryTileData("Games", Icons.Rounded.SportsEsports, "Action, puzzle, arcade & indie"),
    CategoryTileData("Social", Icons.AutoMirrored.Rounded.Chat, "Messaging & community spaces"),
    CategoryTileData("Media", Icons.Rounded.MusicNote, "Audio, video & streaming players"),
    CategoryTileData("Education", Icons.Rounded.School, "Languages, coding & flashcards"),
    CategoryTileData("Finance", Icons.Rounded.Paid, "Budgets, wallets & trackers"),
    CategoryTileData("Health", Icons.Rounded.FavoriteBorder, "Fitness, wellness & nutrition"),
    CategoryTileData("Photography", Icons.Rounded.CameraAlt, "Camera tools & photo editors")
)

@Composable
fun CategoriesScreen(
    appsState: UiState<List<MarketplaceApp>>,
    onCategorySelected: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val apps = (appsState as? UiState.Success)?.data ?: emptyList()

    val extraCategories = apps.map { it.category }
        .filter { cat -> cat.isNotBlank() && CategoryDirectory.none { it.name.equals(cat, true) } }
        .distinct()
        .map { CategoryTileData(it, Icons.Rounded.Apps, "Approved apps in $it") }

    val allCategories = CategoryDirectory + extraCategories

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 840.dp -> 3
            maxWidth >= 560.dp -> 2
            else -> 2
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(columns) }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Explore Categories",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Tap a category to filter approved apps and games in LK Store.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(allCategories, key = { it.name }) { cat ->
                val count = if (cat.name.equals("Games", ignoreCase = true)) {
                    apps.count { it.isGame }
                } else {
                    apps.count { it.category.equals(cat.name, ignoreCase = true) }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCategorySelected(cat.name, cat.name.equals("Games", ignoreCase = true))
                        }
                        .testTag("category_card_${cat.name.lowercase()}"),
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.name,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = cat.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
