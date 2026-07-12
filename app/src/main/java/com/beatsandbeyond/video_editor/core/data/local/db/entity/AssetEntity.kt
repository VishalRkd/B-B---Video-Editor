package com.beatsandbeyond.video_editor.core.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisting [com.beatsandbeyond.video_editor.core.domain.model.Asset].
 *
 * An Asset record marks that the user has imported a specific device media item
 * into the app. It stores a [mediaUri] string (not the media itself) plus
 * app-specific metadata.
 *
 * [projectId] is nullable — in future phases, assets may be global (library) or
 * project-scoped. For Phase 1, all assets are global.
 */
@Entity(
    tableName = "assets",
    indices = [Index(value = ["projectId"])]
)
data class AssetEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val mediaUri: String,
    val mediaType: String,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val importedAt: Long,
    val isFavorite: Boolean,
    val customName: String?,
)
