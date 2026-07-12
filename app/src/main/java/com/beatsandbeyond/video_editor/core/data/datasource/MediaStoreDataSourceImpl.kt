package com.beatsandbeyond.video_editor.core.data.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.beatsandbeyond.video_editor.core.domain.model.MediaFilter
import com.beatsandbeyond.video_editor.core.domain.model.MediaItem
import com.beatsandbeyond.video_editor.core.domain.model.MediaType
import com.beatsandbeyond.video_editor.core.domain.model.SortOrder
import com.beatsandbeyond.video_editor.core.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Concrete implementation of [MediaStoreDataSource].
 *
 * Queries [MediaStore.Video.Media] and [MediaStore.Images.Media] using [ContentResolver].
 * Both queries are merged and sorted according to the [MediaFilter].
 *
 * This class contains all MediaStore cursor logic in one place, keeping the rest of
 * the codebase free of ContentResolver knowledge.
 */
class MediaStoreDataSourceImpl @Inject constructor(
    private val contentResolver: ContentResolver,
) : MediaStoreDataSource {

    companion object {
        private const val TAG = "MediaStoreDataSource"
    }

    override suspend fun queryMedia(filter: MediaFilter): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<MediaItem>()

            if (MediaType.VIDEO in filter.mediaTypes) {
                results.addAll(queryVideos(filter))
            }
            if (MediaType.IMAGE in filter.mediaTypes) {
                results.addAll(queryImages(filter))
            }

            results.sortedWith(buildComparator(filter.sortOrder))
        }

    override suspend fun queryMediaById(id: Long): MediaItem? =
        withContext(Dispatchers.IO) {
            // Try video first, then image
            queryVideoById(id) ?: queryImageById(id)
        }

    // ── Video queries ───────────────────────────────────────────────────────

    private fun queryVideos(filter: MediaFilter): List<MediaItem> {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA,
        )

        val selection = buildSelection(filter, isVideo = true)
        val selectionArgs = buildSelectionArgs(filter)

        return queryMediaStore(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection = projection,
            selection = selection,
            selectionArgs = selectionArgs,
            mediaType = MediaType.VIDEO,
            baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
    }

    private fun queryVideoById(id: Long): MediaItem? {
        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA,
        )
        return queryMediaStore(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection = projection,
            selection = "${MediaStore.Video.Media._ID} = ?",
            selectionArgs = arrayOf(id.toString()),
            mediaType = MediaType.VIDEO,
            baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).firstOrNull()
    }

    // ── Image queries ───────────────────────────────────────────────────────

    private fun queryImages(filter: MediaFilter): List<MediaItem> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATA,
        )

        val selection = buildSelection(filter, isVideo = false)
        val selectionArgs = buildSelectionArgs(filter)

        return queryMediaStore(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection = projection,
            selection = selection,
            selectionArgs = selectionArgs,
            mediaType = MediaType.IMAGE,
            baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        )
    }

    private fun queryImageById(id: Long): MediaItem? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATA,
        )
        return queryMediaStore(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection = projection,
            selection = "${MediaStore.Images.Media._ID} = ?",
            selectionArgs = arrayOf(id.toString()),
            mediaType = MediaType.IMAGE,
            baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        ).firstOrNull()
    }

    // ── Core cursor logic ───────────────────────────────────────────────────

    private fun queryMediaStore(
        uri: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        mediaType: MediaType,
        baseUri: Uri,
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null, // Sort order is applied post-query to merge video+image results
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val sizeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val dataColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val widthColumn = c.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightColumn = c.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val durationColumn = c.getColumnIndex(MediaStore.Video.Media.DURATION)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(baseUri, id)

                    items.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            name = c.getString(nameColumn) ?: "",
                            mediaType = mediaType,
                            mimeType = c.getString(mimeColumn) ?: "",
                            durationMs = if (durationColumn >= 0) c.getLong(durationColumn) else 0L,
                            width = if (widthColumn >= 0) c.getInt(widthColumn) else 0,
                            height = if (heightColumn >= 0) c.getInt(heightColumn) else 0,
                            sizeBytes = c.getLong(sizeColumn),
                            dateAddedMs = c.getLong(dateColumn) * 1000L, // MediaStore stores seconds
                            absolutePath = c.getString(dataColumn) ?: "",
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to query MediaStore: ${e.message}", e)
        } finally {
            cursor?.close()
        }

        return items
    }

    // ── Query builders ──────────────────────────────────────────────────────

    private fun buildSelection(filter: MediaFilter, isVideo: Boolean): String? {
        val nameColumn = if (isVideo) MediaStore.Video.Media.DISPLAY_NAME
        else MediaStore.Images.Media.DISPLAY_NAME

        return if (filter.searchQuery.isNotBlank()) {
            "$nameColumn LIKE ?"
        } else {
            null
        }
    }

    private fun buildSelectionArgs(filter: MediaFilter): Array<String>? {
        return if (filter.searchQuery.isNotBlank()) {
            arrayOf("%${filter.searchQuery}%")
        } else {
            null
        }
    }

    private fun buildComparator(sortOrder: SortOrder): Comparator<MediaItem> =
        when (sortOrder) {
            SortOrder.DATE_ADDED_DESC -> compareByDescending { it.dateAddedMs }
            SortOrder.DATE_ADDED_ASC -> compareBy { it.dateAddedMs }
            SortOrder.NAME_ASC -> compareBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> compareByDescending { it.name.lowercase() }
            SortOrder.SIZE_DESC -> compareByDescending { it.sizeBytes }
            SortOrder.DURATION_DESC -> compareByDescending { it.durationMs }
        }
}
