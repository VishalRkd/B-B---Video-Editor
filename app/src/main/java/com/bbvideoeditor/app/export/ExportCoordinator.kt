package com.bbvideoeditor.app.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.AtomicFile
import androidx.core.content.ContextCompat
import com.bbvideoeditor.core.Project
import com.bbvideoeditor.core.newId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Application-owned job status survives navigation; snapshots isolate a running export from edits. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExportCoordinator(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(ExportState())
    val state = mutableState.asStateFlow()
    private val ioMutex = Mutex()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private var startJob: Job? = null
    private val initialized = scope.async {
        withContext(Dispatchers.IO) {
            val previous = runCatching { json.decodeFromString<ExportState>(AtomicFile(statusFile()).openRead().bufferedReader().use { it.readText() }) }.getOrNull()
            // No service survives process death. Remove partial private renders and pending rows.
            File(context.filesDir, "exports").listFiles().orEmpty().filter { it.isDirectory }.forEach { directory ->
                File(directory, "render.mp4").delete()
                val journal = File(directory, "pending_uri.txt")
                if (journal.exists()) {
                    runCatching {
                        val uri = Uri.parse(journal.readText())
                        if (Build.VERSION.SDK_INT >= 29) {
                            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.IS_PENDING), null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst() && cursor.getInt(0) == 1) context.contentResolver.delete(uri, null, null)
                            }
                        }
                    }
                    journal.delete()
                }
                File(directory, "request.json").delete()
                directory.delete()
            }
            previous
        }?.let { saved ->
            mutableState.value = if (saved.active) saved.copy(phase = ExportPhase.FAILED, error = "The previous export was interrupted. Open its project and export again.") else saved
        }
    }

    fun start(project: Project, settings: ExportSettings) {
        if (state.value.active || startJob?.isActive == true) return
        startJob = scope.launch {
            initialized.await()
            if (state.value.active) return@launch
            val request = ExportRequest(newId(), project.validate(), settings)
            update(ExportState(request.jobId, project.id, project.name, ExportPhase.PREPARING, detail = "Preparing the render pipeline…"))
            try {
                withContext(Dispatchers.IO) {
                    val directory = jobDirectory(request.jobId).apply { mkdirs() }
                    writeAtomic(File(directory, "request.json"), json.encodeToString(request))
                    val estimated = settings.estimatedBytes(project.durationUs)
                    require(directory.usableSpace > estimated * 2 + 32_000_000) { "Not enough free storage. Free approximately ${estimated * 2 / 1_000_000} MB and try again." }
                }
                ContextCompat.startForegroundService(context, Intent(context, ExportService::class.java).setAction(ExportService.ACTION_START).putExtra(ExportService.EXTRA_JOB_ID, request.jobId))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                update(state.value.copy(phase = ExportPhase.FAILED, error = e.message ?: "Could not start export."))
            }
        }
    }

    fun cancel() {
        if (!state.value.active) return
        if (startJob?.isActive == true) {
            startJob?.cancel()
            update(state.value.copy(phase = ExportPhase.CANCELLED, detail = "Export cancelled."))
        }
        context.startService(Intent(context, ExportService::class.java).setAction(ExportService.ACTION_CANCEL).putExtra(ExportService.EXTRA_JOB_ID, state.value.jobId))
    }

    fun update(value: ExportState) {
        mutableState.value = value
        if (value.phase != ExportPhase.RUNNING || value.progress == null) scope.launch(Dispatchers.IO) {
            ioMutex.withLock { writeAtomic(statusFile(), json.encodeToString(mutableState.value)) }
        }
    }

    suspend fun request(jobId: String): ExportRequest = withContext(Dispatchers.IO) {
        json.decodeFromString<ExportRequest>(File(jobDirectory(jobId), "request.json").readText()).also { require(it.jobId == jobId); it.project.validate() }
    }

    fun jobDirectory(id: String): File {
        require(java.util.UUID.fromString(id).toString() == id)
        return File(context.filesDir, "exports/$id")
    }
    private fun statusFile(): File = File(context.filesDir, "exports/status.json").also { it.parentFile?.mkdirs() }
    private fun writeAtomic(file: File, text: String) {
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try { output.write(text.toByteArray()); atomic.finishWrite(output) } catch (e: Throwable) { atomic.failWrite(output); throw e }
    }
}
