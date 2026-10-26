package com.bbvideoeditor.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.AtomicFile
import android.util.LruCache
import androidx.exifinterface.media.ExifInterface
import com.bbvideoeditor.core.MediaKind
import com.bbvideoeditor.core.MediaReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Shared bounded bitmap cache. Eviction never recycles bitmaps still being displayed by Compose. */
class ThumbnailCache(context: Context) : ThumbnailProvider {
    private val context = context.applicationContext
    private val directory = File(this.context.cacheDir, "timeline_thumbnails_v1")
    private val decodeSlots = Semaphore(if (Build.VERSION.SDK_INT >= 27) 2 else 1)
    private val locks = Array(16) { Mutex() }
    private val diskMutex = Mutex()
    private var lastDiskSweepMs = 0L
    private val memory = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 16L).coerceIn(4L * 1024 * 1024, 32L * 1024 * 1024).toInt(),
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    override suspend fun thumbnail(media: MediaReference, sourceUs: Long, width: Int, height: Int): Bitmap? {
        if (media.kind == MediaKind.AUDIO) return null
        val targetWidth = width.coerceIn(8, 512)
        val targetHeight = height.coerceIn(8, 512)
        // Thumbnail requests are deliberately quantized, unlike preview/player seeks.
        val timeUs = if (media.kind == MediaKind.IMAGE) 0L else
            (sourceUs.coerceIn(0, (media.durationUs - 1).coerceAtLeast(0)) / 250_000L) * 250_000L
        val key = cacheKey("${media.uri}|${media.sizeBytes}|${media.durationUs}|$timeUs|$targetWidth|$targetHeight")
        memory.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
                memory.get(key)?.let { return@withLock it }
                try {
                    directory.mkdirs()
                    val file = File(directory, "$key.png")
                    val fromDisk = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                    if (fromDisk != null) {
                        file.setLastModified(System.currentTimeMillis())
                        memory.put(key, fromDisk)
                        return@withLock fromDisk
                    }
                    val bitmap = decodeSlots.withPermit {
                        coroutineContext.ensureActive()
                        val source = if (media.kind == MediaKind.IMAGE) decodeImage(Uri.parse(media.uri), targetWidth, targetHeight)
                        else decodeVideo(Uri.parse(media.uri), timeUs, targetWidth, targetHeight)
                        source?.let { centerCrop(it, targetWidth, targetHeight) }
                    } ?: return@withLock null
                    coroutineContext.ensureActive()
                    memory.put(key, bitmap)
                    // Disk failures do not prevent a decoded thumbnail being displayed.
                    runCatching {
                        val atomic = AtomicFile(file)
                        val output = atomic.startWrite()
                        try {
                            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                            atomic.finishWrite(output)
                        } catch (error: Throwable) {
                            atomic.failWrite(output)
                            throw error
                        }
                    }
                    trimDiskIfNeeded()
                    bitmap
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    fun clearMemory() = memory.evictAll()

    private fun decodeVideo(uri: Uri, timeUs: Long, width: Int, height: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            if (Build.VERSION.SDK_INT >= 27) {
                retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, width, height)
            } else {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            }
        } finally {
            retriever.release()
        }
    }

    private fun decodeImage(uri: Uri, width: Int, height: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= width && bounds.outHeight / (sample * 2) >= height) sample *= 2
        // Also bound extremely narrow panoramas independently of their shorter dimension.
        while (max(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val source = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val exif = resolver.openInputStream(uri)?.use { runCatching { ExifInterface(it) }.getOrNull() }
        if (exif == null || (exif.rotationDegrees == 0 && !exif.isFlipped)) return source
        val matrix = Matrix().apply {
            if (exif.isFlipped) postScale(-1f, 1f)
            postRotate(exif.rotationDegrees.toFloat())
        }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated !== source) source.recycle()
        return rotated
    }

    private fun centerCrop(source: Bitmap, width: Int, height: Int): Bitmap {
        if (source.width == width && source.height == height) return source
        val scale = max(width.toFloat() / source.width, height.toFloat() / source.height)
        val cropWidth = (width / scale).roundToInt().coerceIn(1, source.width)
        val cropHeight = (height / scale).roundToInt().coerceIn(1, source.height)
        val left = (source.width - cropWidth) / 2
        val top = (source.height - cropHeight) / 2
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(source, Rect(left, top, left + cropWidth, top + cropHeight), Rect(0, 0, width, height), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        source.recycle()
        return output
    }

    private suspend fun trimDiskIfNeeded() = diskMutex.withLock {
        val now = SystemClock.elapsedRealtime()
        if (lastDiskSweepMs != 0L && now - lastDiskSweepMs < 60_000L) return@withLock
        lastDiskSweepMs = now
        val files = directory.listFiles { file -> file.extension == "png" }.orEmpty().sortedBy { it.lastModified() }
        var bytes = files.sumOf { it.length() }
        var count = files.size
        for (file in files) {
            if (bytes <= 96L * 1024 * 1024 && count <= 1500) break
            val length = file.length()
            if (file.delete()) { bytes -= length; count-- }
        }
    }
}

internal fun cacheKey(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
