package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MarketplaceApp
import com.example.data.model.UiState
import com.example.ui.components.AppListRowCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView

@Composable
fun SearchScreen(
    appsState: UiState<List<MarketplaceApp>>,
    query: String,
    typeFilter: String,
    onlyVerified: Boolean,
    minRating: Double,
    wishlistIds: Set<String>,
    updateAppIds: Set<String>,
    onQueryChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onOnlyVerifiedChange: (Boolean) -> Unit,
    onMinRatingChange: (Double) -> Unit,
    onOpenApp: (String) -> Unit,
    onToggleWishlist: (MarketplaceApp) -> Unit,
    onQuickDownload: (MarketplaceApp) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search apps, games, developers, or package IDs...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_input")
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("All", "Apps", "Games")) { type ->
                FilterChip(
                    selected = typeFilter == type,
                    onClick = { onTypeFilterChange(type) },
                    label = { Text(type) },
                    modifier = Modifier.testTag("search_type_chip_${type.lowercase()}")
                )
            }
            item {
                FilterChip(
                    selected = onlyVerified,
                    onClick = { onOnlyVerifiedChange(!onlyVerified) },
                    label = { Text("SHA-256 Verified") },
                    leadingIcon = { Icon(Icons.Rounded.VerifiedUser, null) },
                    modifier = Modifier.testTag("search_verified_chip")
                )
            }
            item {
                FilterChip(
                    selected = minRating >= 4.0,
                    onClick = { onMinRatingChange(if (minRating >= 4.0) 0.0 else 4.0) },
                    label = { Text("4.0+ Stars") },
                    leadingIcon = { Icon(Icons.Rounded.Star, null) },
                    modifier = Modifier.testTag("search_rating_chip")
                )
            }
        }

        when (appsState) {
            is UiState.Loading -> LoadingStateView(modifier = Modifier.weight(1f))
            is UiState.Error -> ErrorStateView(message = appsState.message, modifier = Modifier.weight(1f))
            is UiState.Success -> {
                val trimmed = query.trim().lowercase()
                val results = appsState.data.filter { app ->
                    val matchesQuery = trimmed.isEmpty() ||
                        app.title.lowercase().contains(trimmed) ||
                        app.developerName.lowercase().contains(trimmed) ||
                        app.packageName.lowercase().contains(trimmed) ||
                        app.category.lowercase().contains(trimmed) ||
                        app.tags.any { it.lowercase().contains(trimmed) }
                    val matchesType = when (typeFilter) {
                        "Apps" -> !app.isGame
                        "Games" -> app.isGame
                        else -> true
                    }
                    val matchesVerified = !onlyVerified || app.verified || app.sha256.isNotBlank()
                    val matchesRating = app.ratingAverage >= minRating || minRating == 0.0
                    matchesQuery && matchesType && matchesVerified && matchesRating
                }

                if (results.isEmpty()) {
                    EmptyStateView(
                        title = if (trimmed.isEmpty()) "No matching approved apps" else "No results for \"$query\"",
                        subtitle = "Try another keyword or clear active search filters.",
                        actionLabel = if (query.isNotEmpty() || onlyVerified || minRating > 0.0 || typeFilter != "All") "Reset Filters" else null,
                        onAction = {
                            onQueryChange("")
                            onTypeFilterChange("All")
                            onOnlyVerifiedChange(false)
                            onMinRatingChange(0.0)
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            Text(
                                text = "${results.size} result${if (results.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(results, key = { it.id }) { app ->
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
