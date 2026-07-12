package com.beatsandbeyond.video_editor.core.utils

import android.util.Log

/**
 * Centralized logging wrapper for B&B Video Editor.
 *
 * Provides a consistent API for logging that:
 * - Truncates tag names that exceed Android's 23-character limit
 * - Can be upgraded to Timber or remote logging (Crashlytics) without changing call sites
 * - Allows build-flavor-based log suppression in release builds (future)
 *
 * Usage:
 * ```
 * Logger.d(TAG, "Media loaded: ${items.size} items")
 * Logger.e(TAG, "Failed to query MediaStore", exception)
 * ```
 */
object Logger {

    private const val MAX_TAG_LENGTH = 23

    fun v(tag: String, message: String) {
        Log.v(sanitizeTag(tag), message)
    }

    fun d(tag: String, message: String) {
        Log.d(sanitizeTag(tag), message)
    }

    fun i(tag: String, message: String) {
        Log.i(sanitizeTag(tag), message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(sanitizeTag(tag), message, throwable)
        } else {
            Log.w(sanitizeTag(tag), message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(sanitizeTag(tag), message, throwable)
        } else {
            Log.e(sanitizeTag(tag), message)
        }
    }

    private fun sanitizeTag(tag: String): String =
        if (tag.length <= MAX_TAG_LENGTH) tag else tag.substring(0, MAX_TAG_LENGTH)
}
