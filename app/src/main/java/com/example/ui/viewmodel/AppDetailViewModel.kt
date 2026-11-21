package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.MarketplaceRepository
import com.example.data.model.AppReview
import com.example.data.model.MarketplaceApp
import com.example.data.model.ReportReason
import com.example.data.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppDetailViewModel(
    private val repository: MarketplaceRepository,
    val appId: String,
    val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val appState: StateFlow<UiState<MarketplaceApp?>> = repository.observeApp(appId)
        .map<MarketplaceApp?, UiState<MarketplaceApp?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing app $appId", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to load app details"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val reviewsState: StateFlow<UiState<List<AppReview>>> = repository.observeReviews(appId)
        .map<List<AppReview>, UiState<List<AppReview>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing reviews for $appId", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to load reviews"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun submitReview(
        userName: String,
        userAvatarUrl: String?,
        versionName: String,
        rating: Int,
        comment: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            repository.submitReview(
                userId = currentUserId,
                userName = userName,
                userAvatarUrl = userAvatarUrl,
                appId = appId,
                versionName = versionName,
                rating = rating,
                comment = comment
            ).onSuccess {
                _events.tryEmit("Review published!")
                onComplete(true)
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to submit review")
                onComplete(false)
            }
        }
    }

    fun deleteMyReview() {
        viewModelScope.launch {
            repository.deleteReview(currentUserId, appId)
                .onSuccess { _events.tryEmit("Your review was deleted") }
                .onFailure { err -> _events.tryEmit(err.localizedMessage ?: "Failed to delete review") }
        }
    }

    fun submitReport(
        app: MarketplaceApp,
        reason: ReportReason,
        details: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            repository.submitReport(currentUserId, app, reason, details)
                .onSuccess {
                    _events.tryEmit("Report submitted for safety review.")
                    onComplete(true)
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Could not submit report")
                    onComplete(false)
                }
        }
    }

    private companion object {
        const val TAG = "AppDetailVM"
    }
}
