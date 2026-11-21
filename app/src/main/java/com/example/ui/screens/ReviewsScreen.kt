package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppReview
import com.example.data.model.MarketplaceApp
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.RatingStarsRow
import com.example.ui.theme.LkStarAmber
import com.example.util.formatTimestamp
import java.util.Locale

@Composable
fun ReviewsScreen(
    appState: UiState<MarketplaceApp?>,
    reviewsState: UiState<List<AppReview>>,
    currentUserId: String,
    currentUserName: String,
    currentUserAvatar: String?,
    onSubmitReview: (versionName: String, rating: Int, comment: String, (Boolean) -> Unit) -> Unit,
    onDeleteMyReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = (appState as? UiState.Success)?.data

    when (reviewsState) {
        is UiState.Loading -> LoadingStateView(message = "Loading reviews...", modifier = modifier)
        is UiState.Error -> ErrorStateView(message = reviewsState.message, modifier = modifier)
        is UiState.Success -> {
            val reviews = reviewsState.data
            val myReview = reviews.firstOrNull { it.userId == currentUserId }
            var rating by remember(myReview?.id) { mutableIntStateOf(myReview?.rating?.toInt() ?: 5) }
            var comment by remember(myReview?.id) { mutableStateOf(myReview?.comment ?: "") }
            var submitting by remember { mutableStateOf(false) }

            LaunchedEffect(myReview) {
                if (myReview != null) {
                    rating = myReview.rating.toInt().coerceIn(1, 5)
                    comment = myReview.comment
                }
            }

            val average = if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average() else 0.0

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Rating Breakdown Summary
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.large
                                )
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (reviews.isNotEmpty()) String.format(Locale.US, "%.1f", average) else "—",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                RatingStarsRow(rating = average, starSize = 15.dp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${reviews.size} review${if (reviews.size == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (star in 5 downTo 1) {
                                    val count = reviews.count { it.rating.toInt() == star }
                                    val ratio = if (reviews.isNotEmpty()) count.toFloat() / reviews.size else 0f
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("$star", style = MaterialTheme.typography.labelSmall)
                                        LinearProgressIndicator(
                                            progress = { ratio },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Write / Edit Review Card
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
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (myReview != null) "Update Your Review" else "Write a Review",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (myReview != null) {
                                    IconButton(
                                        onClick = onDeleteMyReview,
                                        modifier = Modifier.testTag("delete_my_review_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete my review",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (i in 1..5) {
                                    IconButton(
                                        onClick = { rating = i },
                                        modifier = Modifier.testTag("rate_star_$i")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = "Rate $i stars",
                                            tint = if (i <= rating) LkStarAmber else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = comment,
                                onValueChange = { if (it.length <= 2000) comment = it },
                                label = { Text("Share your experience with ${app?.title ?: "this app"}") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("review_comment_input")
                            )

                            Button(
                                onClick = {
                                    submitting = true
                                    onSubmitReview(
                                        app?.versionName ?: "",
                                        rating,
                                        comment
                                    ) { submitting = false }
                                },
                                enabled = !submitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_review_button")
                            ) {
                                Text(
                                    when {
                                        submitting -> "Publishing..."
                                        myReview != null -> "Update Review"
                                        else -> "Publish Review"
                                    }
                                )
                            }
                        }
                    }
                }

                if (reviews.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No reviews yet",
                            subtitle = "Be the first user to rate and review ${app?.title ?: "this application"}."
                        )
                    }
                } else {
                    items(reviews, key = { it.id }) { rev ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        MaterialTheme.shapes.medium
                                    )
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (rev.userId == currentUserId) "${rev.userName} (You)" else rev.userName,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = formatTimestamp(rev.updatedAt ?: rev.createdAt) +
                                                if (rev.versionName.isNotBlank()) " • v${rev.versionName}" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RatingStarsRow(rating = rev.rating.toDouble(), starSize = 15.dp)
                                }
                                if (rev.comment.isNotBlank()) {
                                    Text(
                                        text = rev.comment,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
