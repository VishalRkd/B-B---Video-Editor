package com.beatsandbeyond.video_editor.core.domain.model

/**
 * The top-level container for all editing work in B&B Video Editor.
 *
 * A [Project] owns one [Timeline] and defines the output parameters:
 * resolution and frame rate. The timeline holds all tracks, clips, and effects.
 *
 * Projects are persisted in Room. The [timeline] is serialized as JSON in the
 * initial implementation and will be normalized to relational tables in a future phase.
 *
 * @param id           UUID generated at project creation.
 * @param name         User-visible project name.
 * @param createdAt    Unix timestamp (ms) of when the project was first created.
 * @param updatedAt    Unix timestamp (ms) of the last edit. Updated on every save.
 * @param timeline     The full editing timeline for this project.
 * @param resolution   Output video resolution.
 * @param frameRate    Output frame rate (e.g., 24.0, 30.0, 60.0 fps).
 * @param thumbnailUri URI of the project thumbnail image (generated from the first frame).
 */
data class Project(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val timeline: Timeline,
    val resolution: Resolution = Resolution.FHD,
    val frameRate: Float = 30.0f,
    val thumbnailUri: String? = null,
) {
    /** True if the project's timeline contains any clips. */
    val hasContent: Boolean get() = !timeline.isEmpty

    /** Total duration of the project's timeline in milliseconds. */
    val durationMs: Long get() = timeline.totalDurationMs
}
