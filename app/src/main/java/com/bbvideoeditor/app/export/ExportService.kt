package com.bbvideoeditor.app.export

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.util.UnstableApi
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.MainActivity
import com.bbvideoeditor.app.R
import kotlinx.coroutines.*
import java.io.File

@UnstableApi
class ExportService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val app get() = application as EditorApplication
    private var job: Job? = null
    private var currentJobId: String? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var timedOut = false

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "Video exports", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            if (intent.getStringExtra(EXTRA_JOB_ID) == currentJobId || currentJobId == null) job?.cancel()
            if (job == null) stopSelf()
            return START_NOT_STICKY
        }
        if (job?.isActive == true) return START_NOT_STICKY
        val id = intent?.getStringExtra(EXTRA_JOB_ID) ?: run { stopSelf(); return START_NOT_STICKY }
        currentJobId = id
        try {
            val notification = notification("Preparing export", null)
            if (Build.VERSION.SDK_INT >= 35) startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
            else if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else startForeground(NOTIFICATION, notification)
            wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Frame:Export").apply { acquire(6 * 60 * 60 * 1000L) }
        } catch (e: Exception) {
            app.exports.update(app.exports.state.value.copy(phase = ExportPhase.FAILED, error = e.message ?: "Background export could not start."))
            stopSelf(); return START_NOT_STICKY
        }
        job = scope.launch {
            var temporary: File? = null
            var terminal: ExportState? = null
            try {
                val request = app.exports.request(id)
                val state = ExportState(id, request.project.id, request.project.name, ExportPhase.PREPARING, detail = "Preparing media…")
                app.exports.update(state)
                temporary = File(app.exports.jobDirectory(id), "render.mp4")
                withContext(Dispatchers.IO) { temporary.delete() }
                val result = Media3RenderEngine(this@ExportService, app.compiler).render(request.project, request.settings, temporary,
                    onProgress = { progress ->
                        app.exports.update(app.exports.state.value.copy(phase = ExportPhase.RUNNING, progress = progress))
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification("Exporting ${request.project.name}", progress))
                    }, onDetail = { detail -> app.exports.update(app.exports.state.value.copy(detail = detail)) })
                app.exports.update(app.exports.state.value.copy(phase = ExportPhase.PUBLISHING, progress = 100, detail = "Saving video to your device…"))
                val output = MediaPublisher(this@ExportService).publish(temporary, request.project.name, request.settings, File(app.exports.jobDirectory(id), "pending_uri.txt"))
                terminal = app.exports.state.value.copy(phase = ExportPhase.SUCCEEDED, outputUri = output.toString(), outputBytes = result.bytes,
                    detail = "${result.width} × ${result.height} · ${request.settings.frameRate} FPS · ${if (result.codec == VideoCodec.HEVC.mime) "HEVC" else "H.264"} · AAC")
                getSystemService(NotificationManager::class.java).notify(COMPLETION_NOTIFICATION, notification("Video saved: ${request.project.name}", null, completed = true))
            } catch (e: CancellationException) {
                terminal = app.exports.state.value.copy(phase = if (timedOut) ExportPhase.FAILED else ExportPhase.CANCELLED, error = if (timedOut) "Android stopped this export after its background processing time limit." else null, detail = "Export cancelled.")
            } catch (e: Exception) {
                terminal = app.exports.state.value.copy(phase = ExportPhase.FAILED, error = e.message ?: "Export could not be completed.")
            } finally {
                withContext(NonCancellable) {
                    withContext(Dispatchers.IO) { temporary?.delete(); runCatching { File(app.exports.jobDirectory(id), "request.json").delete() } }
                    releaseWakeLock()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    terminal?.let(app.exports::update)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) { timedOut = true; job?.cancel(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() { job?.cancel(); scope.cancel(); releaseWakeLock(); super.onDestroy() }
    private fun releaseWakeLock() { wakeLock?.let { if (it.isHeld) it.release() }; wakeLock = null }

    private fun notification(text: String, progress: Int?, completed: Boolean = false): Notification {
        val launch = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Frame Studio").setContentText(text).setContentIntent(launch).setOnlyAlertOnce(true)
            .setOngoing(!completed).setAutoCancel(completed)
        if (!completed) {
            builder.setProgress(100, progress ?: 0, progress == null)
            val cancel = PendingIntent.getService(this, 1, Intent(this, ExportService::class.java).setAction(ACTION_CANCEL).putExtra(EXTRA_JOB_ID, currentJobId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, "Cancel", cancel)
        }
        return builder.build()
    }

    companion object {
        const val ACTION_START = "com.bbvideoeditor.EXPORT"
        const val ACTION_CANCEL = "com.bbvideoeditor.CANCEL_EXPORT"
        const val EXTRA_JOB_ID = "job_id"
        private const val CHANNEL = "video_exports"
        private const val NOTIFICATION = 51
        private const val COMPLETION_NOTIFICATION = 52
    }
}
