package com.beatsandbeyond.video_editor.core.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting [com.beatsandbeyond.video_editor.core.domain.model.Project].
 *
 * The [timelineJson] column stores the serialized [Timeline] as a JSON string.
 * This is an intentional design decision for Phase 1:
 * - Avoids premature normalization of a rapidly evolving schema.
 * - Enables full Timeline read/write without multiple JOIN queries.
 *
 * Future phases will migrate to a normalized multi-table schema as the
 * Timeline structure stabilizes (see DECISIONS.md).
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val resolutionWidth: Int,
    val resolutionHeight: Int,
    val frameRate: Float,
    val timelineJson: String,
    val thumbnailUri: String?,
)
