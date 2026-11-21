package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryAddCheck
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.UiState
import com.example.data.model.UserProfile

@Composable
fun AccountHubScreen(
    userEmail: String?,
    fallbackDisplayName: String,
    fallbackPhotoUrl: String?,
    userProfileState: UiState<UserProfile?>,
    wishlistCount: Int,
    libraryCount: Int,
    updatesCount: Int,
    downloadsCount: Int,
    unreadNotifications: Int,
    onUpdateProfile: (displayName: String, bio: String, (Boolean) -> Unit) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenHelp: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (userProfileState as? UiState.Success)?.data
    val displayName = profile?.displayName?.ifBlank { fallbackDisplayName } ?: fallbackDisplayName
    val photoUrl = profile?.photoUrl?.ifBlank { fallbackPhotoUrl } ?: fallbackPhotoUrl
    val bio = profile?.bio ?: ""

    var showEditDialog by remember { mutableStateOf(false) }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.large
                        )
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = photoUrl,
                            contentDescription = "User avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "User avatar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = displayName, style = MaterialTheme.typography.titleLarge)
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (bio.isNotBlank()) {
                            Text(
                                text = bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit profile")
                    }
                }
            }
        }

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
                ) {
                    HubMenuItem(
                        icon = Icons.Rounded.LibraryAddCheck,
                        title = "App Library & Updates",
                        subtitle = if (updatesCount > 0) "$libraryCount apps • $updatesCount update(s) ready" else "$libraryCount apps tracked",
                        onClick = onOpenLibrary,
                        tag = "hub_library"
                    )
                    HorizontalDivider()
                    HubMenuItem(
                        icon = Icons.Rounded.Bookmark,
                        title = "Wishlist",
                        subtitle = "$wishlistCount saved app${if (wishlistCount == 1) "" else "s"}",
                        onClick = onOpenWishlist,
                        tag = "hub_wishlist"
                    )
                    HorizontalDivider()
                    HubMenuItem(
                        icon = Icons.Rounded.History,
                        title = "Download History",
                        subtitle = "$downloadsCount APK download${if (downloadsCount == 1) "" else "s"}",
                        onClick = onOpenDownloads,
                        tag = "hub_downloads"
                    )
                    HorizontalDivider()
                    HubMenuItem(
                        icon = Icons.Rounded.Notifications,
                        title = "Notifications",
                        subtitle = if (unreadNotifications > 0) "$unreadNotifications unread alert(s)" else "All caught up",
                        onClick = onOpenNotifications,
                        tag = "hub_notifications"
                    )
                    HorizontalDivider()
                    HubMenuItem(
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        title = "Help, Support & LK APK POST Guide",
                        subtitle = "FAQs, Firebase setup, and support tickets",
                        onClick = onOpenHelp,
                        tag = "hub_help"
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign out of LK Store")
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            initialName = displayName,
            initialBio = bio,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newBio, done ->
                onUpdateProfile(newName, newBio) { ok ->
                    done()
                    if (ok) showEditDialog = false
                }
            }
        )
    }
}

@Composable
private fun HubMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialBio: String,
    onDismiss: () -> Unit,
    onSave: (String, String, () -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var bio by remember { mutableStateOf(initialBio) }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 300) bio = it },
                    label = { Text("Bio (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    saving = true
                    onSave(name, bio) { saving = false }
                },
                enabled = name.isNotBlank() && !saving
            ) {
                Text(if (saving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
