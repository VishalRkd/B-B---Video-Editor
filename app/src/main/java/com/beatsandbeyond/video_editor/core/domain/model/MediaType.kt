package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Classifies the type of a media item.
 * Used throughout the app to route media to the correct engine and UI treatment.
 */
enum class MediaType {
    /** A video file (mp4, mov, mkv, etc.) */
    VIDEO,
    /** A still image file (jpg, png, heic, etc.) */
    IMAGE,
    /** An audio-only file (mp3, aac, wav, etc.) */
    AUDIO,
}
