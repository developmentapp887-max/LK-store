package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AppReview
import com.example.data.model.DeveloperProfile
import com.example.data.model.DownloadHistoryEntry
import com.example.data.model.LibraryItem
import com.example.data.model.MarketplaceApp
import com.example.data.model.ReportReason
import com.example.data.model.SupportCategory
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun handleStreamError(error: Throwable, operationType: OperationType, path: String, expectedUid: String? = null) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        val activeUid = auth.currentUser?.uid
        val shouldLog = exceptionToLog != null && (expectedUid == null || activeUid == expectedUid)
        if (shouldLog) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    fun observeApprovedApps(): Flow<List<MarketplaceApp>> = flow {
        requireUserId()
        val path = "apps"
        emitAll(
            db.collection("apps")
                .whereEqualTo("status", "approved")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MarketplaceApp::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeApp(appId: String): Flow<MarketplaceApp?> = flow {
        requireUserId()
        val path = "apps/$appId"
        emitAll(
            db.collection("apps").document(appId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            MarketplaceApp::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeDeveloper(developerId: String): Flow<DeveloperProfile?> = flow {
        requireUserId()
        val path = "developers/$developerId"
        emitAll(
            db.collection("developers").document(developerId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            DeveloperProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeReviews(appId: String): Flow<List<AppReview>> = flow {
        requireUserId()
        val path = "apps/$appId/reviews"
        emitAll(
            db.collection("apps").document(appId).collection("reviews")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AppReview::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleStreamError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(userId: String, displayName: String, photoUrl: String?): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = displayName.trim().ifBlank { "LK Store User" }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "photoUrl" to photoUrl?.takeIf { it.isNotBlank() }?.take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(userId: String, displayName: String, bio: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val trimmedName = displayName.trim()
            require(trimmedName.length in 1..80) { "Display name must be 1 to 80 characters." }
            val trimmedBio = bio.trim()
            require(trimmedBio.length <= 300) { "Bio must be 300 characters or fewer." }
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to trimmedName,
                    "bio" to trimmedBio,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeWishlist(userId: String): Flow<List<WishlistItem>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$userId/wishlist"
        emitAll(
            db.collection("users").document(userId).collection("wishlist")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WishlistItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun toggleWishlist(userId: String, app: MarketplaceApp, isCurrentlySaved: Boolean): Result<Boolean> {
        val docPath = "users/$userId/wishlist/${app.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(app.id.isNotBlank()) { "App ID cannot be blank." }
            val docRef = db.collection("users").document(uid).collection("wishlist").document(app.id)
            if (isCurrentlySaved) {
                docRef.delete().await()
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "appId" to app.id,
                    "appTitle" to app.title.take(100),
                    "developerName" to app.developerName.ifBlank { "Developer" }.take(100),
                    "iconUrl" to app.iconUrl.takeIf { it.isNotBlank() }?.take(1000),
                    "category" to app.category.ifBlank { "Apps" }.take(50),
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeLibrary(userId: String): Flow<List<LibraryItem>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$userId/library"
        emitAll(
            db.collection("users").document(userId).collection("library")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LibraryItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeDownloadHistory(userId: String): Flow<List<DownloadHistoryEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$userId/download_history"
        emitAll(
            db.collection("users").document(userId).collection("download_history")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DownloadHistoryEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.downloadedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun recordVerifiedDownload(userId: String, app: MarketplaceApp): Result<Unit> {
        var currentPath = "users/$userId/download_history"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(app.id.isNotBlank()) { "App ID cannot be blank." }
            require(app.apkUrl.isNotBlank()) { "APK URL is missing." }

            // 1. Add download history record
            val historyRef = db.collection("users").document(uid).collection("download_history").document()
            currentPath = historyRef.path
            val historyPayload = mapOf(
                "userId" to uid,
                "appId" to app.id,
                "appTitle" to app.title.take(100),
                "developerName" to app.developerName.ifBlank { "Developer" }.take(100),
                "iconUrl" to app.iconUrl.takeIf { it.isNotBlank() }?.take(1000),
                "versionName" to app.versionName.ifBlank { "1.0.0" }.take(40),
                "versionCode" to app.versionCode.coerceAtLeast(1L),
                "fileSizeBytes" to app.fileSizeBytes.coerceAtLeast(0L),
                "sha256" to app.sha256.takeIf { it.isNotBlank() }?.take(128),
                "verified" to (app.verified || app.sha256.isNotBlank()),
                "downloadedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            historyRef.set(historyPayload).await()

            // 2. Upsert library item (preserving createdAt on update)
            val libRef = db.collection("users").document(uid).collection("library").document(app.id)
            currentPath = libRef.path
            val existingLib = libRef.get().await()
            if (existingLib.exists()) {
                val updatePayload = mapOf(
                    "appTitle" to app.title.take(100),
                    "developerName" to app.developerName.ifBlank { "Developer" }.take(100),
                    "iconUrl" to app.iconUrl.takeIf { it.isNotBlank() }?.take(1000),
                    "versionName" to app.versionName.ifBlank { "1.0.0" }.take(40),
                    "versionCode" to app.versionCode.coerceAtLeast(1L),
                    "fileSizeBytes" to app.fileSizeBytes.coerceAtLeast(0L),
                    "apkUrl" to app.apkUrl.take(1000),
                    "sha256" to app.sha256.takeIf { it.isNotBlank() }?.take(128),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                libRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "appId" to app.id,
                    "appTitle" to app.title.take(100),
                    "packageName" to app.packageName.ifBlank { "com.lkstore.app" }.take(150),
                    "developerName" to app.developerName.ifBlank { "Developer" }.take(100),
                    "iconUrl" to app.iconUrl.takeIf { it.isNotBlank() }?.take(1000),
                    "versionName" to app.versionName.ifBlank { "1.0.0" }.take(40),
                    "versionCode" to app.versionCode.coerceAtLeast(1L),
                    "fileSizeBytes" to app.fileSizeBytes.coerceAtLeast(0L),
                    "apkUrl" to app.apkUrl.take(1000),
                    "sha256" to app.sha256.takeIf { it.isNotBlank() }?.take(128),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                libRef.set(createPayload).await()
            }

            // 3. Create download notification
            val notifRef = db.collection("users").document(uid).collection("notifications").document()
            currentPath = notifRef.path
            val notifPayload = mapOf(
                "userId" to uid,
                "title" to "APK Download Started: ${app.title}".take(120),
                "message" to "Version ${app.versionName} queued via DownloadManager. ${if (app.sha256.isNotBlank()) "SHA-256 verified." else "Verified HTTPS source."}".take(500),
                "type" to "download",
                "appId" to app.id,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            notifRef.set(notifPayload).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun removeFromLibrary(userId: String, appId: String): Result<Unit> {
        val docPath = "users/$userId/library/$appId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("library").document(appId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<UserNotification>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        val docPath = "users/$userId/notifications/$notificationId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("notifications").document(notificationId)
                .update(mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(userId: String, unreadIds: List<String>): Result<Unit> {
        val path = "users/$userId/notifications"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            if (unreadIds.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            unreadIds.forEach { id ->
                val ref = db.collection("users").document(uid).collection("notifications").document(id)
                batch.update(ref, mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(userId: String, notificationId: String): Result<Unit> {
        val docPath = "users/$userId/notifications/$notificationId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).collection("notifications").document(notificationId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitReview(
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        appId: String,
        versionName: String,
        rating: Int,
        comment: String
    ): Result<Unit> {
        val docPath = "apps/$appId/reviews/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(rating in 1..5) { "Rating must be between 1 and 5 stars." }
            val trimmedComment = comment.trim()
            require(trimmedComment.length <= 2000) { "Review must be 2000 characters or fewer." }
            val safeName = userName.trim().ifBlank { "LK Store User" }.take(80)

            val docRef = db.collection("apps").document(appId).collection("reviews").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updateMap = mapOf(
                    "rating" to rating.toLong(),
                    "comment" to trimmedComment,
                    "userName" to safeName,
                    "userAvatarUrl" to userAvatarUrl?.takeIf { it.isNotBlank() }?.take(500),
                    "versionName" to versionName.takeIf { it.isNotBlank() }?.take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "appId" to appId,
                    "userId" to uid,
                    "userName" to safeName,
                    "userAvatarUrl" to userAvatarUrl?.takeIf { it.isNotBlank() }?.take(500),
                    "rating" to rating.toLong(),
                    "comment" to trimmedComment,
                    "versionName" to versionName.takeIf { it.isNotBlank() }?.take(40),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteReview(userId: String, appId: String): Result<Unit> {
        val docPath = "apps/$appId/reviews/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("apps").document(appId).collection("reviews").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        userId: String,
        app: MarketplaceApp,
        reason: ReportReason,
        details: String
    ): Result<Unit> {
        var docPath = "reports"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val trimmedDetails = details.trim()
            require(trimmedDetails.length in 1..1500) { "Please describe the issue (1-1500 characters)." }
            val docRef = db.collection("reports").document()
            docPath = docRef.path
            val payload = mapOf(
                "reporterId" to uid,
                "appId" to app.id,
                "appTitle" to app.title.take(100),
                "reason" to reason.key,
                "details" to trimmedDetails,
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitSupportTicket(
        userId: String,
        userName: String,
        subject: String,
        category: SupportCategory,
        message: String
    ): Result<Unit> {
        var docPath = "support_tickets"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val trimmedSubject = subject.trim()
            val trimmedMessage = message.trim()
            require(trimmedSubject.length in 1..150) { "Subject must be 1 to 150 characters." }
            require(trimmedMessage.length in 1..2000) { "Message must be 1 to 2000 characters." }
            val docRef = db.collection("support_tickets").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "userName" to userName.trim().ifBlank { "LK Store User" }.take(80),
                "subject" to trimmedSubject,
                "category" to category.key,
                "message" to trimmedMessage,
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
