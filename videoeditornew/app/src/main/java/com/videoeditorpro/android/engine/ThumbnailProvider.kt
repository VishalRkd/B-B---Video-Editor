package com.videoeditorpro.android.engine

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance, LRU-cached thumbnail extraction service.
 * Ensures the multi-clip filmstrip loads quickly without lag or memory leaks.
 */
object ThumbnailProvider {
    private const val TAG = "ThumbnailProvider"

    // 15% of available JVM heap for bitmap cache
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private fun buildCacheKey(path: String, timeUs: Long, width: Int, height: Int): String {
        return "${path}_${timeUs}_${width}x${height}"
    }

    /**
     * Retrieves or extracts a single video frame at [timeMs].
     */
    suspend fun getFrame(
        videoPath: String,
        timeMs: Int,
        targetWidth: Int = 120,
        targetHeight: Int = 120
    ): Bitmap? = withContext(Dispatchers.IO) {
        val timeUs = timeMs * 1000L
        val key = buildCacheKey(videoPath, timeUs, targetWidth, targetHeight)

        memoryCache.get(key)?.let { return@withContext it }

        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val frame = retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                targetWidth,
                targetHeight
            )
            if (frame != null) {
                memoryCache.put(key, frame)
            }
            frame
        } catch (e: Throwable) {
            Log.e(TAG, "Error extracting frame at $timeMs ms from $videoPath", e)
            null
        } finally {
            try {
                retriever?.release()
            } catch (ignored: Throwable) {}
        }
    }

    /**
     * Extracts an evenly-spaced list of thumbnail bitmaps across [startMs] to [endMs].
     */
    suspend fun getFilmstripThumbnails(
        videoPath: String,
        startMs: Int,
        endMs: Int,
        count: Int = 10,
        targetWidth: Int = 120,
        targetHeight: Int = 120
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val thumbnails = mutableListOf<Bitmap>()
        val spanMs = (endMs - startMs).coerceAtLeast(100)
        val stepMs = spanMs / count.coerceAtLeast(1)

        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)

            for (i in 0 until count) {
                val timeMs = startMs + (i * stepMs)
                val timeUs = timeMs * 1000L
                val key = buildCacheKey(videoPath, timeUs, targetWidth, targetHeight)

                val cached = memoryCache.get(key)
                if (cached != null) {
                    thumbnails.add(cached)
                } else {
                    val frame = retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        targetWidth,
                        targetHeight
                    )
                    if (frame != null) {
                        memoryCache.put(key, frame)
                        thumbnails.add(frame)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error generating filmstrip for $videoPath", e)
        } finally {
            try {
                retriever?.release()
            } catch (ignored: Throwable) {}
        }
        thumbnails
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}
