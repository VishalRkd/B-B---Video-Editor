package com.bbvideoeditor.app.media

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.bbvideoeditor.core.MediaKind
import com.bbvideoeditor.core.MediaReference
import com.bbvideoeditor.core.Project
import com.bbvideoeditor.core.newId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext

data class ImportResult(val media: List<MediaReference>, val errors: List<String>)

/** Acquires durable read access and probes actual streams before adding any reference to a project. */
class MediaImporter(context: Context) {
    private val context = context.applicationContext
    private val resolver = this.context.contentResolver
    private val importsDirectory = File(this.context.filesDir, "imported_media")

    suspend fun importMedia(uris: List<Uri>): ImportResult {
        val copiedInThisBatch = mutableListOf<File>()
        try {
            return withContext(Dispatchers.IO) {
                val imported = mutableListOf<MediaReference>()
                val errors = mutableListOf<String>()
                for (uri in uris.distinct()) {
                    coroutineContext.ensureActive()
                    var copiedFile: File? = null
                    var name = uri.lastPathSegment ?: "Media"
                    try {
                        val info = displayInfo(uri)
                        name = info.first
                        val mime = resolver.getType(uri).orEmpty()
                        val durableUri = if (hasPersistentAccess(uri)) uri else {
                            importsDirectory.mkdirs()
                            val suffix = name.substringAfterLast('.', "").lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
                            copiedFile = File(importsDirectory, newId() + (suffix?.let { ".$it" } ?: ""))
                            copiedInThisBatch += copiedFile
                            copyCancellable(uri, copiedFile)
                            Uri.fromFile(copiedFile)
                        }
                        coroutineContext.ensureActive()
                        val media = if (mime.startsWith("image/") || looksLikeImage(name)) {
                            readImage(durableUri, name, mime, info.second)
                        } else {
                            readAudioVideo(durableUri, name, mime, info.second)
                        }
                        coroutineContext.ensureActive()
                        imported += media.copy(sizeBytes = if (media.sizeBytes > 0) media.sizeBytes else copiedFile?.length() ?: 0)
                    } catch (cancelled: CancellationException) {
                        copiedFile?.delete()
                        throw cancelled
                    } catch (error: Exception) {
                        copiedFile?.delete()
                        errors += "$name: ${error.message ?: "unsupported or damaged media"}"
                    }
                }
                ImportResult(imported, errors)
            }
        } catch (cancelled: CancellationException) {
            // Includes cancellation while dispatching the completed result back to the caller.
            withContext(NonCancellable + Dispatchers.IO) { copiedInThisBatch.forEach { it.delete() } }
            throw cancelled
        }
    }

    suspend fun unavailable(project: Project): List<MediaReference> = withContext(Dispatchers.IO) {
        project.media.values.filter { media ->
            coroutineContext.ensureActive()
            try {
                resolver.openAssetFileDescriptor(Uri.parse(media.uri), "r")?.use { false } ?: true
            } catch (_: Exception) {
                true
            }
        }
    }

    private fun hasPersistentAccess(uri: Uri): Boolean {
        if (uri.scheme == "file") {
            val file = File(uri.path ?: return false)
            return file.canonicalPath.startsWith(context.filesDir.canonicalPath + File.separator)
        }
        if (uri.scheme != "content") return false
        if (resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }) return true
        return try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
        } catch (_: SecurityException) { false }
          catch (_: UnsupportedOperationException) { false }
    }

    private suspend fun copyCancellable(uri: Uri, output: File) {
        val input = resolver.openInputStream(uri) ?: throw IOException("Cannot read this media file")
        input.use { source ->
            output.outputStream().use { destination ->
                val buffer = ByteArray(128 * 1024)
                while (true) {
                    coroutineContext.ensureActive()
                    val count = source.read(buffer)
                    if (count < 0) break
                    destination.write(buffer, 0, count)
                }
                destination.fd.sync()
            }
        }
    }

    private fun displayInfo(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported media"
        var size = 0L
        if (uri.scheme == "file") {
            val file = File(uri.path ?: throw IOException("Invalid file URI"))
            return file.name to file.length()
        }
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameColumn >= 0 && !cursor.isNull(nameColumn)) name = cursor.getString(nameColumn)
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn).coerceAtLeast(0)
            }
        }
        return name to size
    }

    private fun readImage(uri: Uri, name: String, mime: String, size: Long): MediaReference {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val imageStream = resolver.openInputStream(uri) ?: throw IOException("Cannot read image")
        imageStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported or damaged image" }
        val rotation = resolver.openInputStream(uri)?.use { input ->
            runCatching { ExifInterface(input).rotationDegrees }.getOrDefault(0)
        } ?: 0
        return MediaReference(
            uri = uri.toString(), displayName = name, kind = MediaKind.IMAGE,
            durationUs = DEFAULT_IMAGE_DURATION_US, width = bounds.outWidth, height = bounds.outHeight,
            rotationDegrees = rotation, mimeType = bounds.outMimeType ?: mime, sizeBytes = size,
        )
    }

    private fun readAudioVideo(uri: Uri, name: String, mime: String, size: Long): MediaReference {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            var video: MediaFormat? = null
            var audio: MediaFormat? = null
            var durationUs = 0L
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val type = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (type.startsWith("video/") && video == null) video = format
                if (type.startsWith("audio/") && audio == null) audio = format
                if (type.startsWith("video/") || type.startsWith("audio/")) {
                    durationUs = maxOf(durationUs, format.longOrZero(MediaFormat.KEY_DURATION))
                }
            }
            require(video != null || audio != null) { "No supported video or audio stream" }
            var rotation = video?.intOrZero(MediaFormat.KEY_ROTATION) ?: 0
            var frameRate = video?.numberOrZero(MediaFormat.KEY_FRAME_RATE) ?: 0f
            var width = video?.intOrZero(MediaFormat.KEY_WIDTH) ?: 0
            var height = video?.intOrZero(MediaFormat.KEY_HEIGHT) ?: 0
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                if (durationUs == 0L) durationUs = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) * 1_000L
                if (video != null) {
                    rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: rotation
                    if (frameRate <= 0f) frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 0f
                    if (width == 0) width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    if (height == 0) height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                }
            } catch (_: RuntimeException) {
                // Extractor metadata is sufficient; optional retriever metadata can fail on some codecs.
            } finally {
                retriever.release()
            }
            require(durationUs > 0L) { "Media has no readable duration" }
            if (video != null) require(width > 0 && height > 0) { "Video dimensions are unavailable" }
            return MediaReference(
                uri = uri.toString(), displayName = name, kind = if (video != null) MediaKind.VIDEO else MediaKind.AUDIO,
                durationUs = durationUs, width = width, height = height,
                rotationDegrees = rotation, frameRate = frameRate.takeIf { it.isFinite() && it > 0 } ?: 0f,
                hasAudio = audio != null, mimeType = mime.ifEmpty { (video ?: audio)?.getString(MediaFormat.KEY_MIME).orEmpty() },
                sizeBytes = size,
                codecMimeType = video?.getString(MediaFormat.KEY_MIME).orEmpty(),
                colorTransfer = video?.intOrZero(MediaFormat.KEY_COLOR_TRANSFER) ?: 0,
                audioChannels = audio?.intOrZero(MediaFormat.KEY_CHANNEL_COUNT) ?: 0,
                audioSampleRate = audio?.intOrZero(MediaFormat.KEY_SAMPLE_RATE) ?: 0,
            )
        } finally {
            extractor.release()
        }
    }

    private fun MediaFormat.longOrZero(key: String): Long = runCatching { getLong(key) }.getOrDefault(0L)
    private fun MediaFormat.intOrZero(key: String): Int = runCatching { getInteger(key) }.getOrDefault(0)
    private fun MediaFormat.numberOrZero(key: String): Float = runCatching { getInteger(key).toFloat() }
        .getOrElse { runCatching { getFloat(key) }.getOrDefault(0f) }
    private fun looksLikeImage(name: String) = name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "avif", "bmp", "gif")

    companion object { const val DEFAULT_IMAGE_DURATION_US = 5_000_000L }
}
