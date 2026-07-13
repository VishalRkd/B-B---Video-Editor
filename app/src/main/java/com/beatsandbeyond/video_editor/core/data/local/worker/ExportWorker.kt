package com.beatsandbeyond.video_editor.core.data.local.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.WorkManager
import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.ExportEngine
import com.beatsandbeyond.video_editor.core.domain.model.ExportConfig
import com.beatsandbeyond.video_editor.core.domain.model.ExportFormat
import com.beatsandbeyond.video_editor.core.domain.model.ExportProgress
import com.beatsandbeyond.video_editor.core.domain.model.Resolution
import com.beatsandbeyond.video_editor.core.domain.usecase.project.GetProjectByIdUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * A WorkManager worker that runs video export operations in the background.
 * Promotes itself to a foreground service with a progress notification to prevent process death.
 */
class ExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "export_channel"
        private const val NOTIFICATION_ID = 42

        const val KEY_PROJECT_ID = "PROJECT_ID"
        const val KEY_OUTPUT_PATH = "OUTPUT_PATH"
        const val KEY_RESOLUTION_WIDTH = "RESOLUTION_WIDTH"
        const val KEY_RESOLUTION_HEIGHT = "RESOLUTION_HEIGHT"
        const val KEY_FRAME_RATE = "FRAME_RATE"
        const val KEY_VIDEO_BITRATE = "VIDEO_BITRATE"
        const val KEY_AUDIO_BITRATE = "AUDIO_BITRATE"
        const val KEY_FORMAT = "FORMAT"

        const val KEY_PROGRESS = "PROGRESS"
        const val KEY_PROGRESS_FRACTION = "PROGRESS_FRACTION"
        const val KEY_FRAME_MS = "FRAME_MS"
        const val KEY_STATE = "STATE"
        const val KEY_COMPLETED_PATH = "COMPLETED_PATH"
        const val KEY_ERROR = "ERROR"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ExportWorkerEntryPoint {
        fun getProjectByIdUseCase(): GetProjectByIdUseCase
        fun exportEngine(): ExportEngine
    }

    override suspend fun doWork(): Result {
        val projectId = inputData.getString(KEY_PROJECT_ID) ?: return Result.failure()
        val outputPath = inputData.getString(KEY_OUTPUT_PATH) ?: return Result.failure()
        val width = inputData.getInt(KEY_RESOLUTION_WIDTH, 1920)
        val height = inputData.getInt(KEY_RESOLUTION_HEIGHT, 1080)
        val frameRate = inputData.getFloat(KEY_FRAME_RATE, 30.0f)
        val videoBitrate = inputData.getInt(KEY_VIDEO_BITRATE, 8_000_000)
        val audioBitrate = inputData.getInt(KEY_AUDIO_BITRATE, 192_000)
        val formatName = inputData.getString(KEY_FORMAT) ?: "MP4"

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ExportWorkerEntryPoint::class.java
        )
        val getProjectByIdUseCase = entryPoint.getProjectByIdUseCase()
        val exportEngine = entryPoint.exportEngine()

        val projectResult = getProjectByIdUseCase(projectId)
        if (projectResult !is AppResult.Success) {
            return Result.failure(workDataOf(KEY_ERROR to "Project not found"))
        }
        val project = projectResult.data

        val config = ExportConfig(
            outputPath = outputPath,
            resolution = Resolution(width, height),
            frameRate = frameRate,
            videoBitrateBps = videoBitrate,
            audioBitrateBps = audioBitrate,
            format = try { ExportFormat.valueOf(formatName) } catch (e: Exception) { ExportFormat.MP4 }
        )

        createNotificationChannel()
        setForeground(createForegroundInfo(0))

        return try {
            var exportSuccess = false
            var errorMsg: String? = null

            exportEngine.export(project, config).collect { progress ->
                when (progress) {
                    is ExportProgress.Started -> {
                        setProgress(workDataOf(KEY_STATE to "STARTED", KEY_PROGRESS to 0))
                    }
                    is ExportProgress.InProgress -> {
                        val percent = (progress.progressFraction * 100).toInt().coerceIn(0, 100)
                        try {
                            setForeground(createForegroundInfo(percent))
                        } catch (e: Exception) {
                            // ignore foreground promotion crashes on background workers
                        }
                        setProgress(workDataOf(
                            KEY_STATE to "IN_PROGRESS",
                            KEY_PROGRESS to percent,
                            KEY_PROGRESS_FRACTION to progress.progressFraction,
                            KEY_FRAME_MS to progress.currentFrameMs
                        ))
                    }
                    is ExportProgress.Completed -> {
                        exportSuccess = true
                    }
                    is ExportProgress.Failed -> {
                        errorMsg = progress.cause.message ?: "Export failed"
                    }
                    is ExportProgress.Cancelled -> {
                        errorMsg = "Export cancelled"
                    }
                }
            }

            if (exportSuccess) {
                showSuccessNotification(outputPath)
                Result.success(workDataOf(
                    KEY_STATE to "COMPLETED",
                    KEY_COMPLETED_PATH to outputPath
                ))
            } else {
                showFailureNotification(errorMsg ?: "Export failed")
                Result.failure(workDataOf(KEY_ERROR to (errorMsg ?: "Export failed")))
            }
        } catch (e: Exception) {
            showFailureNotification(e.message ?: "Unknown error")
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        } finally {
            if (isStopped) {
                exportEngine.cancel(projectId)
            }
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("B&B Video Editor")
            .setContentText("Exporting video... $progress%")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", intent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Export Progress",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showSuccessNotification(outputPath: String) {
        try {
            val authority = "${applicationContext.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(applicationContext, authority, java.io.File(outputPath))
            
            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val pendingIntent = android.app.PendingIntent.getActivity(
                applicationContext,
                0,
                playIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val successNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Export completed")
                .setContentText("Tap to play your exported video")
                .setSmallIcon(android.R.drawable.ic_menu_save)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
                
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID + 1, successNotification)
        } catch (e: Exception) {
            com.beatsandbeyond.video_editor.core.utils.Logger.e("ExportWorker", "Failed to show success notification", e)
        }
    }

    private fun showFailureNotification(errorMsg: String) {
        try {
            val launchIntent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)?.apply {
                putExtra("export_error", errorMsg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val pendingIntent = android.app.PendingIntent.getActivity(
                applicationContext,
                0,
                launchIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val failureNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Export failed")
                .setContentText("Tap to view error: $errorMsg")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
                
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID + 2, failureNotification)
        } catch (e: Exception) {
            com.beatsandbeyond.video_editor.core.utils.Logger.e("ExportWorker", "Failed to show failure notification", e)
        }
    }
}
