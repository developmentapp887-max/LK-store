package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.SupportCategory

private data class FaqItem(
    val question: String,
    val answer: String
)

private val HelpFaqs = listOf(
    FaqItem(
        "Which applications appear in LK Store?",
        "LK Store queries Cloud Firestore collection 'apps' filtered strictly by status == 'approved'. Pending, rejected, or suspended submissions from LK APK POST are never shown publicly."
    ),
    FaqItem(
        "How are APK downloads verified?",
        "Every download checks that the listing status is 'approved' and the APK URL uses HTTPS. Downloads are queued through Android DownloadManager, and any SHA-256 checksum published by the developer is displayed on the App Details screen."
    ),
    FaqItem(
        "How do app updates work?",
        "Whenever you download an app from LK Store, its versionCode is recorded in your personal library (/users/{uid}/library). If a newer approved versionCode is published to Firestore, LK Store automatically surfaces an Update badge."
    ),
    FaqItem(
        "How do I connect LK Store to my LK APK POST Firebase project?",
        "Replace app/google-services.json and update R.string.firestore_database_id with the target Firebase project and database used by LK APK POST, and deploy firestore.rules to ensure approved apps and user collections stay aligned."
    )
)

@Composable
fun HelpSupportScreen(
    userName: String,
    onSubmitTicket: (subject: String, category: SupportCategory, message: String, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedIndex by remember { mutableStateOf<Int?>(0) }
    var selectedCategory by remember { mutableStateOf(SupportCategory.DOWNLOADS) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Help & Support", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Marketplace safety, APK verification, LK APK POST sync & support tickets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // LK APK POST Integration Info Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "LK APK POST Firestore Integration",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Collection: /apps/{appId} (requires status: \"approved\", title, packageName, developerId, developerName, category, appType, description, apkUrl, versionName, versionCode, fileSizeBytes, createdAt, updatedAt). Reviews are stored under /apps/{appId}/reviews/{userId}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text("Frequently Asked Questions", style = MaterialTheme.typography.titleMedium)
        }

        items(HelpFaqs.size) { index ->
            val faq = HelpFaqs[index]
            val expanded = expandedIndex == index
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedIndex = if (expanded) null else index },
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
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = faq.question,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        Text(
                            text = faq.answer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // Contact Support Form
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
                    Text("Contact Support", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Submitting as $userName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SupportCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { if (it.length <= 150) subject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("support_subject_input")
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { if (it.length <= 2000) message = it },
                        label = { Text("Describe how we can help") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("support_message_input")
                    )

                    Button(
                        onClick = {
                            submitting = true
                            onSubmitTicket(subject, selectedCategory, message) { ok ->
                                submitting = false
                                if (ok) {
                                    subject = ""
                                    message = ""
                                }
                            }
                        },
                        enabled = subject.isNotBlank() && message.isNotBlank() && !submitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_support_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (submitting) "Sending..." else "Submit Support Ticket")
                    }
                }
            }
        }
    }
}
