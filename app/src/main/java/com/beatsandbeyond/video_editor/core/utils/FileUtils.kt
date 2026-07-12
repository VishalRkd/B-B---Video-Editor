package com.beatsandbeyond.video_editor.core.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

/**
 * Utility functions for file operations throughout B&B Video Editor.
 */
object FileUtils {

    /**
     * Returns a human-readable file size string.
     * e.g., 1048576 → "1.0 MB"
     */
    fun formatFileSize(sizeBytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            sizeBytes >= gb -> "%.1f GB".format(sizeBytes / gb)
            sizeBytes >= mb -> "%.1f MB".format(sizeBytes / mb)
            sizeBytes >= kb -> "%.1f KB".format(sizeBytes / kb)
            else -> "$sizeBytes B"
        }
    }

    /**
     * Returns a human-readable duration string from milliseconds.
     * e.g., 90000ms → "1:30"
     *       3661000ms → "1:01:01"
     */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /**
     * Derives the MIME type from a file extension.
     * Returns null if the extension is not recognized.
     */
    fun getMimeType(filePath: String): String? {
        val extension = filePath.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }

    /**
     * Returns the app's private export directory, creating it if it doesn't exist.
     */
    fun getExportDirectory(context: Context): File {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Returns the app's private thumbnail cache directory.
     */
    fun getThumbnailCacheDirectory(context: Context): File {
        val dir = File(context.cacheDir, "thumbnails")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Generates a unique export file path for a project.
     * e.g., /files/exports/export_1720800000000.mp4
     */
    fun generateExportPath(context: Context, extension: String = "mp4"): String {
        val dir = getExportDirectory(context)
        return File(dir, "export_${System.currentTimeMillis()}.$extension").absolutePath
    }
}
