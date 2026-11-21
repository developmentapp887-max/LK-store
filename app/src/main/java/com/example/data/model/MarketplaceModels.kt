package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class MarketplaceApp(
    @DocumentId val id: String = "",
    val title: String = "",
    val packageName: String = "",
    val developerId: String = "",
    val developerName: String = "",
    val category: String = "",
    val appType: String = "app",
    val summary: String = "",
    val description: String = "",
    val changelog: String = "",
    val iconUrl: String = "",
    val screenshots: List<String> = emptyList(),
    val apkUrl: String = "",
    val sha256: String = "",
    val versionName: String = "1.0.0",
    val versionCode: Long = 1L,
    val fileSizeBytes: Long = 0L,
    val minSdk: Long = 24L,
    val status: String = "approved",
    val featured: Boolean = false,
    val verified: Boolean = false,
    val downloadCount: Long = 0L,
    val ratingAverage: Double = 0.0,
    val ratingCount: Long = 0L,
    val tags: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isGame: Boolean
        get() = appType.equals("game", ignoreCase = true) || category.equals("Games", ignoreCase = true)
}

@Keep
@IgnoreExtraProperties
data class DeveloperProfile(
    @DocumentId val id: String = "",
    val developerId: String = "",
    val name: String = "",
    val bio: String = "",
    val website: String = "",
    val verified: Boolean = false,
    val logoUrl: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppReview(
    @DocumentId val id: String = "",
    val appId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val versionName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appId: String = "",
    val appTitle: String = "",
    val developerName: String = "",
    val iconUrl: String = "",
    val category: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LibraryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appId: String = "",
    val appTitle: String = "",
    val packageName: String = "",
    val developerName: String = "",
    val iconUrl: String = "",
    val versionName: String = "",
    val versionCode: Long = 1L,
    val fileSizeBytes: Long = 0L,
    val apkUrl: String = "",
    val sha256: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DownloadHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appId: String = "",
    val appTitle: String = "",
    val developerName: String = "",
    val iconUrl: String = "",
    val versionName: String = "",
    val versionCode: Long = 1L,
    val fileSizeBytes: Long = 0L,
    val sha256: String = "",
    val verified: Boolean = false,
    @ServerTimestamp val downloadedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "system",
    val appId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AppUpdateInfo(
    val libraryItem: LibraryItem,
    val latestApp: MarketplaceApp
)

enum class ReportReason(val key: String, val label: String) {
    MALWARE("malware", "Malware or harmful APK"),
    SPAM("spam", "Spam or misleading metadata"),
    COPYRIGHT("copyright", "Copyright or trademark violation"),
    INAPPROPRIATE("inappropriate", "Inappropriate content"),
    BROKEN_LINK("broken_link", "Broken or unverified APK link"),
    OTHER("other", "Other policy concern")
}

enum class SupportCategory(val key: String, val label: String) {
    DOWNLOADS("downloads", "APK Downloads & Verification"),
    ACCOUNT("account", "Account & Google Sign-In"),
    DEVELOPER("developer", "Developer & LK APK POST"),
    SECURITY("security", "Security & Safety"),
    OTHER("other", "General Feedback")
}

enum class AppSortOption(val label: String) {
    TOP_RATED("Top Rated"),
    RECENTLY_UPDATED("Recently Updated"),
    MOST_DOWNLOADED("Most Downloaded"),
    NAME_ASC("Name (A-Z)")
}
