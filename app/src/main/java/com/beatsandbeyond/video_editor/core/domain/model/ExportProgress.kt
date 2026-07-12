package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Represents progress updates emitted by [ExportEngine] during an export operation.
 */
sealed class ExportProgress {

    /** Export has started. */
    data object Started : ExportProgress()

    /**
     * Export is in progress.
     * @param progressFraction A value from 0.0 to 1.0.
     * @param currentFrameMs   The timeline position of the frame currently being encoded.
     */
    data class InProgress(
        val progressFraction: Float,
        val currentFrameMs: Long,
    ) : ExportProgress()

    /**
     * Export completed successfully.
     * @param outputPath The path of the exported file.
     * @param durationMs Total time taken for the export in milliseconds.
     */
    data class Completed(
        val outputPath: String,
        val durationMs: Long,
    ) : ExportProgress()

    /**
     * Export failed.
     * @param cause The exception that caused the failure.
     */
    data class Failed(val cause: Throwable) : ExportProgress()

    /** Export was cancelled by the user. */
    data object Cancelled : ExportProgress()
}
