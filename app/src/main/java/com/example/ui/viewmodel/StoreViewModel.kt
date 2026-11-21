package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.MarketplaceRepository
import com.example.data.model.AppSortOption
import com.example.data.model.AppUpdateInfo
import com.example.data.model.DownloadHistoryEntry
import com.example.data.model.LibraryItem
import com.example.data.model.MarketplaceApp
import com.example.data.model.SupportCategory
import com.example.data.model.UiState
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.example.util.ApkDownloadHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StoreViewModel(
    private val repository: MarketplaceRepository,
    val currentUserId: String
) : ViewModel() {

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAppCategory = MutableStateFlow("All")
    val selectedAppCategory: StateFlow<String> = _selectedAppCategory.asStateFlow()

    private val _selectedGameGenre = MutableStateFlow("All")
    val selectedGameGenre: StateFlow<String> = _selectedGameGenre.asStateFlow()

    private val _sortOption = MutableStateFlow(AppSortOption.TOP_RATED)
    val sortOption: StateFlow<AppSortOption> = _sortOption.asStateFlow()

    private val _searchOnlyVerified = MutableStateFlow(false)
    val searchOnlyVerified: StateFlow<Boolean> = _searchOnlyVerified.asStateFlow()

    private val _searchMinRating = MutableStateFlow(0.0)
    val searchMinRating: StateFlow<Double> = _searchMinRating.asStateFlow()

    private val _searchTypeFilter = MutableStateFlow("All") // All, Apps, Games
    val searchTypeFilter: StateFlow<String> = _searchTypeFilter.asStateFlow()

    init {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && currentUser.uid == currentUserId) {
            viewModelScope.launch {
                repository.ensureUserProfile(
                    userId = currentUserId,
                    displayName = currentUser.displayName ?: "LK Store User",
                    photoUrl = currentUser.photoUrl?.toString()
                )
            }
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val appsState: StateFlow<UiState<List<MarketplaceApp>>> =
        repository.observeApprovedApps().toUiState("approved apps")

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiState("user profile")

    val wishlistState: StateFlow<UiState<List<WishlistItem>>> =
        repository.observeWishlist(currentUserId).toUiState("wishlist")

    val libraryState: StateFlow<UiState<List<LibraryItem>>> =
        repository.observeLibrary(currentUserId).toUiState("library")

    val downloadHistoryState: StateFlow<UiState<List<DownloadHistoryEntry>>> =
        repository.observeDownloadHistory(currentUserId).toUiState("download history")

    val notificationsState: StateFlow<UiState<List<UserNotification>>> =
        repository.observeNotifications(currentUserId).toUiState("notifications")

    val wishlistIds: StateFlow<Set<String>> = wishlistState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.map { it.appId }.toSet()
                else -> emptySet()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val unreadNotificationsCount: StateFlow<Int> = notificationsState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.count { !it.read }
                else -> 0
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    val availableUpdates: StateFlow<List<AppUpdateInfo>> = combine(appsState, libraryState) { apps, lib ->
        if (apps is UiState.Success && lib is UiState.Success) {
            val appMap = apps.data.associateBy { it.id }
            lib.data.mapNotNull { item ->
                val latest = appMap[item.appId]
                if (latest != null && latest.versionCode > item.versionCode) {
                    AppUpdateInfo(libraryItem = item, latestApp = latest)
                } else null
            }
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectAppCategory(category: String) {
        _selectedAppCategory.value = category
    }

    fun selectGameGenre(genre: String) {
        _selectedGameGenre.value = genre
    }

    fun selectSortOption(option: AppSortOption) {
        _sortOption.value = option
    }

    fun setSearchOnlyVerified(onlyVerified: Boolean) {
        _searchOnlyVerified.value = onlyVerified
    }

    fun setSearchMinRating(minRating: Double) {
        _searchMinRating.value = minRating
    }

    fun setSearchTypeFilter(type: String) {
        _searchTypeFilter.value = type
    }

    fun toggleWishlist(app: MarketplaceApp) {
        val currentlySaved = wishlistIds.value.contains(app.id)
        viewModelScope.launch {
            repository.toggleWishlist(currentUserId, app, currentlySaved)
                .onSuccess { saved ->
                    _snackbarMessages.tryEmit(
                        if (saved) "Added ${app.title} to Wishlist"
                        else "Removed ${app.title} from Wishlist"
                    )
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: "Could not update wishlist")
                }
        }
    }

    fun downloadVerifiedApk(context: Context, app: MarketplaceApp) {
        if (app.status != "approved") {
            _snackbarMessages.tryEmit("Only approved applications can be downloaded.")
            return
        }
        val downloadResult = ApkDownloadHelper.startVerifiedApkDownload(context, app)
        downloadResult.onSuccess {
            viewModelScope.launch {
                repository.recordVerifiedDownload(currentUserId, app)
                    .onSuccess {
                        _snackbarMessages.tryEmit("Downloading ${app.title} v${app.versionName}...")
                    }
                    .onFailure { err ->
                        _snackbarMessages.tryEmit(
                            err.localizedMessage ?: "Download started, but failed to log history."
                        )
                    }
            }
        }.onFailure { err ->
            _snackbarMessages.tryEmit(err.localizedMessage ?: "Invalid or unavailable APK link.")
        }
    }

    fun removeFromLibrary(appId: String, title: String) {
        viewModelScope.launch {
            repository.removeFromLibrary(currentUserId, appId)
                .onSuccess { _snackbarMessages.tryEmit("Removed $title from Library") }
                .onFailure { err -> _snackbarMessages.tryEmit(err.localizedMessage ?: "Failed to remove item") }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun markAllNotificationsRead() {
        val state = notificationsState.value
        if (state is UiState.Success) {
            val unreadIds = state.data.filter { !it.read }.map { it.id }
            if (unreadIds.isEmpty()) return
            viewModelScope.launch {
                repository.markAllNotificationsRead(currentUserId, unreadIds)
                    .onSuccess { _snackbarMessages.tryEmit("All notifications marked as read") }
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(currentUserId, notificationId)
        }
    }

    fun updateProfile(displayName: String, bio: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.updateUserProfile(currentUserId, displayName, bio)
                .onSuccess {
                    _snackbarMessages.tryEmit("Profile updated")
                    onComplete(true)
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: "Failed to update profile")
                    onComplete(false)
                }
        }
    }

    fun submitSupportTicket(
        userName: String,
        subject: String,
        category: SupportCategory,
        message: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            repository.submitSupportTicket(currentUserId, userName, subject, category, message)
                .onSuccess {
                    _snackbarMessages.tryEmit("Support request sent! Ticket saved.")
                    onComplete(true)
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: "Could not submit ticket")
                    onComplete(false)
                }
        }
    }

    fun emitMessage(message: String) {
        _snackbarMessages.tryEmit(message)
    }

    companion object {
        private const val TAG = "StoreVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun sortApps(apps: List<MarketplaceApp>, option: AppSortOption): List<MarketplaceApp> {
            return when (option) {
                AppSortOption.TOP_RATED -> apps.sortedByDescending { it.ratingAverage }
                AppSortOption.RECENTLY_UPDATED -> apps.sortedByDescending {
                    it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L
                }
                AppSortOption.MOST_DOWNLOADED -> apps.sortedByDescending { it.downloadCount }
                AppSortOption.NAME_ASC -> apps.sortedBy { it.title.lowercase() }
            }
        }
    }
}
