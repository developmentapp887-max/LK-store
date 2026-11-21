package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.model.MarketplaceApp
import com.example.ui.theme.LkEmeraldPrimary
import com.example.ui.theme.LkIndigoTertiary
import com.example.ui.theme.LkStarAmber
import com.example.ui.theme.LkVerifiedGreen
import com.example.util.formatDownloadCount
import com.example.util.formatFileSize
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LkStoreTopBar(
    title: String?,
    showBack: Boolean,
    unreadNotifications: Int,
    userPhotoUrl: String?,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    TopAppBar(
        title = {
            if (title == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LK",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "LK STORE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Verified Marketplace",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (showBack) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag("top_bar_search_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.cd_search)
                )
            }
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.testTag("top_bar_notifications_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotifications > 0) {
                            Badge {
                                Text(if (unreadNotifications > 9) "9+" else unreadNotifications.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = stringResource(R.string.cd_notifications)
                    )
                }
            }
            IconButton(
                onClick = onAccountClick,
                modifier = Modifier.testTag("top_bar_account_button")
            ) {
                if (!userPhotoUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = userPhotoUrl,
                        contentDescription = stringResource(R.string.cd_account),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        error = {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = stringResource(R.string.cd_account)
                            )
                        }
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = stringResource(R.string.cd_account),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun AppIconView(
    iconUrl: String,
    title: String,
    size: Dp = 56.dp,
    verified: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fallbackBrush = rememberTitleBrush(title)
    val cornerRadius = (size.value * 0.24f).dp

    Box(modifier = modifier.size(size)) {
        if (iconUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = iconUrl,
                contentDescription = "$title icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(cornerRadius)
                    ),
                loading = {
                    MonogramIconBox(title = title, brush = fallbackBrush, cornerRadius = cornerRadius)
                },
                error = {
                    MonogramIconBox(title = title, brush = fallbackBrush, cornerRadius = cornerRadius)
                }
            )
        } else {
            MonogramIconBox(title = title, brush = fallbackBrush, cornerRadius = cornerRadius)
        }

        if (verified) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.34f).coerceIn(16f, 24f).dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = stringResource(R.string.cd_verified_badge),
                        tint = LkVerifiedGreen,
                        modifier = Modifier.fillMaxSize(0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonogramIconBox(
    title: String,
    brush: Brush,
    cornerRadius: Dp
) {
    val initials = title.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "LK" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun rememberTitleBrush(title: String): Brush {
    val palettes = listOf(
        listOf(LkEmeraldPrimary, Color(0xFF10B981)),
        listOf(LkIndigoTertiary, Color(0xFF0EA5E9)),
        listOf(Color(0xFF0F766E), Color(0xFF14B8A6)),
        listOf(Color(0xFF0369A1), Color(0xFF38BDF8)),
        listOf(Color(0xFF4338CA), Color(0xFF818CF8)),
        listOf(Color(0xFFB45309), Color(0xFFF59E0B))
    )
    val index = abs(title.hashCode()) % palettes.size
    return Brush.linearGradient(palettes[index])
}

@Composable
fun AppCompactCard(
    app: MarketplaceApp,
    hasUpdate: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(156.dp)
            .clickable(onClick = onClick)
            .testTag("app_compact_card_${app.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    MaterialTheme.shapes.medium
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                AppIconView(
                    iconUrl = app.iconUrl,
                    title = app.title,
                    size = 54.dp,
                    verified = app.verified || app.sha256.isNotBlank()
                )
                if (hasUpdate) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "UPDATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = app.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.developerName.ifBlank { app.category },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (app.ratingAverage > 0.0) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = LkStarAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f", app.ratingAverage),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatFileSize(app.fileSizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AppListRowCard(
    app: MarketplaceApp,
    isWishlisted: Boolean,
    hasUpdate: Boolean = false,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    onQuickDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("app_row_card_${app.id}"),
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
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppIconView(
                iconUrl = app.iconUrl,
                title = app.title,
                size = 60.dp,
                verified = app.verified || app.sha256.isNotBlank()
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = app.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (hasUpdate) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Update",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "${app.developerName} • ${app.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = LkStarAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (app.ratingAverage > 0.0) {
                                String.format(Locale.US, "%.1f", app.ratingAverage)
                            } else "New",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        text = "• v${app.versionName} • ${formatFileSize(app.fileSizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (app.downloadCount > 0L) {
                        Text(
                            text = "• ${formatDownloadCount(app.downloadCount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleWishlist,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("wishlist_toggle_${app.id}")
                ) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = stringResource(R.string.cd_wishlist),
                        tint = if (isWishlisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onQuickDownload,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("quick_download_${app.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (hasUpdate) "Update" else "APK",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun RatingStarsRow(
    rating: Double,
    starSize: Dp = 16.dp
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = if (i <= rating.toInt()) LkStarAmber else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

@Composable
fun VerifiedBadgeChip(
    text: String = "LK Verified APK",
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun LoadingStateView(
    message: String = "Syncing LK Store catalog...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_lk_empty_state),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(170.dp)
                .clip(MaterialTheme.shapes.large)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.testTag("empty_state_action_button")
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            text = "Unable to sync data",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
