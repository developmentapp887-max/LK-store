package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.model.AppReview
import com.example.data.model.MarketplaceApp
import com.example.data.model.ReportReason
import com.example.data.model.UiState
import com.example.ui.components.AppIconView
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.RatingStarsRow
import com.example.ui.components.VerifiedBadgeChip
import com.example.ui.theme.LkStarAmber
import com.example.util.ApkDownloadHelper
import com.example.util.formatDownloadCount
import com.example.util.formatFileSize
import com.example.util.formatTimestamp
import java.util.Locale

@Composable
fun AppDetailScreen(
    appState: UiState<MarketplaceApp?>,
    reviewsState: UiState<List<AppReview>>,
    isWishlisted: Boolean,
    installedVersionCode: Long?,
    onDeveloperClick: (developerId: String, developerName: String) -> Unit,
    onOpenAllReviews: (appId: String) -> Unit,
    onToggleWishlist: (MarketplaceApp) -> Unit,
    onDownloadApk: (MarketplaceApp) -> Unit,
    onSubmitReport: (MarketplaceApp, ReportReason, String, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var zoomedScreenshot by remember { mutableStateOf<String?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }

    when (appState) {
        is UiState.Loading -> LoadingStateView(modifier = modifier)
        is UiState.Error -> ErrorStateView(message = appState.message, modifier = modifier)
        is UiState.Success -> {
            val app = appState.data
            if (app == null || app.status != "approved") {
                EmptyStateView(
                    title = "Application unavailable",
                    subtitle = "This application is either unpublished or not yet approved for public listing.",
                    modifier = modifier
                )
                return
            }

            val reviews = (reviewsState as? UiState.Success)?.data ?: emptyList()
            val computedAverage = if (reviews.isNotEmpty()) {
                reviews.map { it.rating.toDouble() }.average()
            } else {
                app.ratingAverage
            }
            val computedCount = if (reviews.isNotEmpty()) reviews.size.toLong() else app.ratingCount
            val hasUpdate = installedVersionCode != null && app.versionCode > installedVersionCode
            val hasValidUrl = ApkDownloadHelper.isVerifiedHttpsUrl(app.apkUrl)

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconView(
                                iconUrl = app.iconUrl,
                                title = app.title,
                                size = 82.dp,
                                verified = app.verified || app.sha256.isNotBlank()
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = app.title,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = app.developerName.ifBlank { "Verified Publisher" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable {
                                            onDeveloperClick(
                                                app.developerId.ifBlank { app.developerName },
                                                app.developerName
                                            )
                                        }
                                        .testTag("app_detail_developer_link")
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = app.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (app.verified || app.sha256.isNotBlank()) {
                                        VerifiedBadgeChip()
                                    }
                                }
                            }
                        }

                        // Metrics Strip
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                MetricColumn(
                                    value = if (computedAverage > 0.0) {
                                        String.format(Locale.US, "%.1f ★", computedAverage)
                                    } else "New",
                                    label = "$computedCount review${if (computedCount == 1L) "" else "s"}"
                                )
                                MetricColumn(
                                    value = formatFileSize(app.fileSizeBytes),
                                    label = "APK Size"
                                )
                                MetricColumn(
                                    value = "v${app.versionName}",
                                    label = "Version"
                                )
                                MetricColumn(
                                    value = formatDownloadCount(app.downloadCount),
                                    label = "Downloads"
                                )
                            }
                        }

                        // Primary Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onDownloadApk(app) },
                                enabled = hasValidUrl,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("download_apk_button")
                            ) {
                                Icon(
                                    imageVector = if (hasUpdate) Icons.Rounded.SystemUpdate else Icons.Rounded.Download,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        !hasValidUrl -> "APK Link Unavailable"
                                        hasUpdate -> "Update Verified APK"
                                        else -> stringResource(R.string.download_verified_apk)
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = { onToggleWishlist(app) },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("detail_wishlist_button")
                            ) {
                                Icon(
                                    imageVector = if (isWishlisted) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = stringResource(R.string.cd_wishlist)
                                )
                            }

                            IconButton(
                                onClick = { ApkDownloadHelper.shareApp(context, app) },
                                modifier = Modifier.testTag("detail_share_button")
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "Share app")
                            }
                        }
                    }
                }

                // Screenshots Gallery
                if (app.screenshots.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Screenshots",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(app.screenshots) { url ->
                                    SubcomposeAsyncImage(
                                        model = url,
                                        contentDescription = stringResource(R.string.cd_screenshot),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(175.dp)
                                            .height(310.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                MaterialTheme.shapes.medium
                                            )
                                            .clickable { zoomedScreenshot = url }
                                    )
                                }
                            }
                        }
                    }
                }

                // Description & What's New
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.large
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("About this app", style = MaterialTheme.typography.titleMedium)
                            if (app.summary.isNotBlank()) {
                                Text(
                                    text = app.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = app.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (app.changelog.isNotBlank()) {
                                HorizontalDivider()
                                Text("What's New in v${app.versionName}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = app.changelog,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Release & Verification Metadata
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.large
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Release & Integrity Details", style = MaterialTheme.typography.titleMedium)
                            }
                            InfoRow("Package ID", app.packageName)
                            InfoRow("Version", "${app.versionName} (Build ${app.versionCode})")
                            InfoRow("File Size", formatFileSize(app.fileSizeBytes))
                            InfoRow("Last Updated", formatTimestamp(app.updatedAt ?: app.createdAt))
                            InfoRow("Minimum Android", "API ${app.minSdk}+")
                            if (app.sha256.isNotBlank()) {
                                InfoRow("SHA-256", app.sha256)
                            }
                        }
                    }
                }

                // Ratings & Reviews Preview
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { onOpenAllReviews(app.id) }
                            .testTag("detail_reviews_card"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.large
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ratings & Reviews", style = MaterialTheme.typography.titleMedium)
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "See all reviews")
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = if (computedAverage > 0.0) {
                                        String.format(Locale.US, "%.1f", computedAverage)
                                    } else "—",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    RatingStarsRow(rating = computedAverage, starSize = 18.dp)
                                    Text(
                                        text = "$computedCount total rating${if (computedCount == 1L) "" else "s"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            reviews.take(2).forEach { rev ->
                                HorizontalDivider()
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(rev.userName, style = MaterialTheme.typography.titleSmall)
                                        RatingStarsRow(rating = rev.rating.toDouble(), starSize = 14.dp)
                                    }
                                    if (rev.comment.isNotBlank()) {
                                        Text(
                                            text = rev.comment,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { onOpenAllReviews(app.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("write_or_view_reviews_button")
                            ) {
                                Icon(Icons.Rounded.RateReview, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Rate & Read All Reviews")
                            }
                        }
                    }
                }

                // Report Inappropriate App
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.testTag("report_app_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.report_app),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (zoomedScreenshot != null) {
                Dialog(
                    onDismissRequest = { zoomedScreenshot = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.92f))
                            .clickable { zoomedScreenshot = null },
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = zoomedScreenshot,
                            contentDescription = "Zoomed screenshot",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        )
                        IconButton(
                            onClick = { zoomedScreenshot = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(20.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }

            if (showReportDialog) {
                ReportAppDialog(
                    appTitle = app.title,
                    onDismiss = { showReportDialog = false },
                    onSubmit = { reason, details, done ->
                        onSubmitReport(app, reason, details) { success ->
                            done()
                            if (success) showReportDialog = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun ReportAppDialog(
    appTitle: String,
    onDismiss: () -> Unit,
    onSubmit: (ReportReason, String, () -> Unit) -> Unit
) {
    var selectedReason by remember { mutableStateOf(ReportReason.MALWARE) }
    var details by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $appTitle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select the policy or safety issue:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ReportReason.entries.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(reason.label, style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 1500) details = it },
                    label = { Text("Details (required)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_details_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitting = true
                    onSubmit(selectedReason, details) { submitting = false }
                },
                enabled = details.isNotBlank() && !submitting,
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text(if (submitting) "Sending..." else "Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
