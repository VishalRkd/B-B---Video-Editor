package com.bbvideoeditor.app.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/** Publishes only complete MP4s, removing pending entries when a copy fails or is cancelled. */
class MediaPublisher(private val context: Context) {
    suspend fun publish(file: File, name: String, settings: ExportSettings, pendingJournal: File? = null): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val safeName = name.replace(Regex("[^\\p{L}\\p{N} _.-]"), "_").take(70).ifBlank { "Frame" }
        val displayName="${safeName}_${java.util.UUID.randomUUID()}.mp4"
        val uri = if (Build.VERSION.SDK_INT >= 29) {
            val collection=MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            PublicationJournal.reserve(pendingJournal,displayName,collection,context)
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Frame")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            requireNotNull(resolver.insert(collection, values)) { "Could not create the output video." }
        } else Uri.parse(requireNotNull(settings.documentUri) { "Choose an output location before exporting." })
        try {
            PublicationJournal.writeUri(pendingJournal,uri)
            resolver.openOutputStream(uri, "w").use { output ->
                requireNotNull(output) { "The output location is unavailable." }
                file.inputStream().use { input ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            coroutineContext.ensureActive()
            if (Build.VERSION.SDK_INT >= 29) {
                check(resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null) == 1) { "Could not publish the finished video." }
            }
            PublicationJournal.clear(pendingJournal)
            uri
        } catch (e: Throwable) { runCatching { resolver.delete(uri, null, null) }; PublicationJournal.clear(pendingJournal); throw e }
    }
}
