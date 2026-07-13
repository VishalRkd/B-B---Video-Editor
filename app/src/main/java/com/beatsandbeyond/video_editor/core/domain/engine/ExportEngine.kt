package com.beatsandbeyond.video_editor.core.domain.engine

import com.beatsandbeyond.video_editor.core.domain.model.ExportConfig
import com.beatsandbeyond.video_editor.core.domain.model.ExportProgress
import com.beatsandbeyond.video_editor.core.domain.model.Project
import kotlinx.coroutines.flow.Flow

/**
 * Engine responsible for encoding and exporting the final video output.
 *
 * Export operations are long-running background tasks. Progress is reported
 * as a [Flow] of [ExportProgress] events, allowing the UI to show accurate
 * progress bars and handle completion or failure gracefully.
 *
 * Implementations will use MediaCodec with hardware encoding for performance,
 * and MediaMuxer to write the final container (MP4, MOV, etc.).
 *
 * The export process runs in a background service (WorkManager) in a future phase
 * to survive process death and allow background exports.
 *
 * Phase 1: Interface defined — no implementation yet.
 * Phase 2: Implement software-based export for basic video output.
 * Phase N: Hardware-accelerated export, background service, and cloud upload.
 */
interface ExportEngine {

    /**
     * Begins exporting the [project] using the given [config].
     *
     * @param project The project to export.
     * @param config  Export parameters (resolution, bitrate, format, output path).
     * @return A [Flow] that emits [ExportProgress] events from [ExportProgress.Started]
     *         to [ExportProgress.Completed] or [ExportProgress.Failed].
     */
    fun export(project: Project, config: ExportConfig): Flow<ExportProgress>

    /**
     * Cancels the running export operation for the specified [projectId].
     * Emits [ExportProgress.Cancelled] on the active [Flow].
     */
    fun cancel(projectId: String)
}
