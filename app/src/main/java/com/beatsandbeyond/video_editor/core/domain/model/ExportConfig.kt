package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Configuration for an export operation performed by [ExportEngine].
 *
 * @param outputPath      Absolute path where the exported file will be written.
 * @param resolution      Output video resolution.
 * @param frameRate       Output frame rate.
 * @param videoBitrateBps Target video bitrate in bits per second.
 * @param audioBitrateBps Target audio bitrate in bits per second.
 * @param format          Output container format.
 */
data class ExportConfig(
    val outputPath: String,
    val resolution: Resolution = Resolution.FHD,
    val frameRate: Float = 30.0f,
    val videoBitrateBps: Int = 8_000_000,
    val audioBitrateBps: Int = 192_000,
    val format: ExportFormat = ExportFormat.MP4,
)

enum class ExportFormat {
    MP4,
    MOV,
    WEBM,
}
