package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Represents a visual transition between two adjacent [Clip]s on the same [Track].
 *
 * Transitions are stored as metadata — the actual rendering is performed by [EffectsEngine]
 * or [RenderEngine] at preview/export time.
 *
 * @param id          Unique identifier.
 * @param type        The visual style of the transition.
 * @param durationMs  Duration of the transition overlap between the two clips.
 * @param fromClipId  ID of the outgoing clip.
 * @param toClipId    ID of the incoming clip.
 */
data class Transition(
    val id: String,
    val type: TransitionType,
    val durationMs: Long,
    val fromClipId: String,
    val toClipId: String,
)

/**
 * Defines the visual style of a [Transition].
 * Future: each enum value maps to a concrete shader or animation in [EffectsEngine].
 */
enum class TransitionType {
    CROSSFADE,
    CUT,
    DISSOLVE,
    FADE_IN,
    FADE_OUT,
    WIPE_LEFT,
    WIPE_RIGHT,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    ZOOM_IN,
    ZOOM_OUT,
    FLASH,
}
