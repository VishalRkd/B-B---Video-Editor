package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Defines filtering options for MediaStore queries.
 *
 * Used by [GetMediaItemsUseCase] and [MediaRepository] to scope the data returned
 * from device storage.
 *
 * @param mediaTypes Set of [MediaType] to include. Defaults to VIDEO and IMAGE.
 * @param sortOrder  How to order results. Defaults to newest first.
 * @param searchQuery Optional name filter. Empty string = no filter.
 */
data class MediaFilter(
    val mediaTypes: Set<MediaType> = setOf(MediaType.VIDEO, MediaType.IMAGE),
    val sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC,
    val searchQuery: String = "",
) {
    companion object {
        /** Convenience filter that shows all media types, newest first. */
        val ALL = MediaFilter()

        /** Videos only. */
        val VIDEOS_ONLY = MediaFilter(mediaTypes = setOf(MediaType.VIDEO))

        /** Images only. */
        val IMAGES_ONLY = MediaFilter(mediaTypes = setOf(MediaType.IMAGE))
    }
}

/**
 * Defines the sort order for media queries.
 */
enum class SortOrder {
    DATE_ADDED_DESC,
    DATE_ADDED_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    DURATION_DESC,
}
