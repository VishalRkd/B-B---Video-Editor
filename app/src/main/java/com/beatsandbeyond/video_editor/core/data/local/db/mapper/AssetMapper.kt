package com.beatsandbeyond.video_editor.core.data.local.db.mapper

import com.beatsandbeyond.video_editor.core.data.local.db.entity.AssetEntity
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.MediaType

/**
 * Converts between [AssetEntity] (Room) and [Asset] (domain).
 */
object AssetMapper {

    fun toDomain(entity: AssetEntity): Asset = Asset(
        id = entity.id,
        mediaUri = entity.mediaUri,
        mediaType = MediaType.valueOf(entity.mediaType),
        displayName = entity.displayName,
        durationMs = entity.durationMs,
        width = entity.width,
        height = entity.height,
        sizeBytes = entity.sizeBytes,
        importedAt = entity.importedAt,
        isFavorite = entity.isFavorite,
        customName = entity.customName,
    )

    fun toEntity(domain: Asset, projectId: String? = null): AssetEntity = AssetEntity(
        id = domain.id,
        projectId = projectId,
        mediaUri = domain.mediaUri,
        mediaType = domain.mediaType.name,
        displayName = domain.displayName,
        durationMs = domain.durationMs,
        width = domain.width,
        height = domain.height,
        sizeBytes = domain.sizeBytes,
        importedAt = domain.importedAt,
        isFavorite = domain.isFavorite,
        customName = domain.customName,
    )

    fun toDomainList(entities: List<AssetEntity>): List<Asset> =
        entities.map { toDomain(it) }
}
