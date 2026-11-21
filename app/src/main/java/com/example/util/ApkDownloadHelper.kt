package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.example.data.model.MarketplaceApp
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

object ApkDownloadHelper {

    fun isVerifiedHttpsUrl(url: String): Boolean {
        val parsed = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return false
        return parsed.scheme.equals("https", ignoreCase = true) && !parsed.host.isNullOrBlank()
    }

    fun startVerifiedApkDownload(context: Context, app: MarketplaceApp): Result<Long> {
        val rawUrl = app.apkUrl.trim()
        if (!isVerifiedHttpsUrl(rawUrl)) {
            return Result.failure(
                IllegalArgumentException("This listing does not have a valid HTTPS APK download URL.")
            )
        }
        val uri = Uri.parse(rawUrl)
        val sanitizedTitle = app.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "app" }
        val sanitizedVersion = app.versionName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "1.0" }
        val fileName = "${sanitizedTitle}_v${sanitizedVersion}.apk"

        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm != null) {
                val request = DownloadManager.Request(uri)
                    .setTitle("${app.title} v${app.versionName}")
                    .setDescription("Downloading verified APK from LK Store")
                    .setMimeType("application/vnd.android.package-archive")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                val downloadId = dm.enqueue(request)
                Result.success(downloadId)
            } else {
                openExternalUrl(context, rawUrl)
                Result.success(0L)
            }
        } catch (e: Exception) {
            try {
                openExternalUrl(context, rawUrl)
                Result.success(0L)
            } catch (openEx: Exception) {
                Result.failure(openEx)
            }
        }
    }

    fun openExternalUrl(context: Context, url: String) {
        val uri = Uri.parse(url.trim())
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun shareApp(context: Context, app: MarketplaceApp) {
        val text = buildString {
            append("Check out ${app.title} (v${app.versionName}) by ${app.developerName} on LK Store")
            if (app.packageName.isNotBlank()) append("\nPackage: ${app.packageName}")
            if (isVerifiedHttpsUrl(app.apkUrl)) append("\nDownload: ${app.apkUrl}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${app.title} on LK Store")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${app.title}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Unknown size"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

fun formatDownloadCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM+", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK+", count / 1_000.0)
        else -> "$count"
    }
}

fun formatTimestamp(timestamp: Timestamp?, pattern: String = "MMM d, yyyy"): String {
    val date = timestamp?.toDate() ?: return "Recently"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}
