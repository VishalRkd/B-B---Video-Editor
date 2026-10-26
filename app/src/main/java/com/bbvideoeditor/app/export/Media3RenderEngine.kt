package com.bbvideoeditor.app.export

import android.content.Context
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.*
import com.bbvideoeditor.app.render.CompositionCompiler
import com.bbvideoeditor.core.Project
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class RenderResult(val width: Int, val height: Int, val durationMs: Long, val bytes: Long, val codec: String, val videoFrames: Int, val audioChannels: Int)

/** Hardware-assisted render backend. Every edit is compiled by the same compiler as preview. */
@UnstableApi
class Media3RenderEngine(context: Context, private val compiler: CompositionCompiler) {
    private val context = context.applicationContext

    suspend fun render(project: Project, settings: ExportSettings, output: File,
                       onProgress: (Int?) -> Unit = {}, onDetail: (String) -> Unit = {}): RenderResult {
        val (width, height) = settings.dimensions(project)
        val plan = compiler.compile(project, width, height, settings.frameRate)
        return withContext(Dispatchers.Main.immediate) {
            coroutineScope {
                var transformer: Transformer? = null
                var polling: Job? = null
                try {
                    suspendCancellableCoroutine { continuation ->
                        val encoder = DefaultEncoderFactory.Builder(context)
                            .setEnableFallback(true)
                            .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(settings.bitrate).setiFrameIntervalSeconds(1f).build())
                            .setRequestedAudioEncoderSettings(AudioEncoderSettings.Builder().setBitrate(192_000).build())
                            .build()
                        val instance = Transformer.Builder(context)
                            .setEncoderFactory(encoder)
                            .setVideoMimeType(settings.codec.mime)
                            .setAudioMimeType(MimeTypes.AUDIO_AAC)
                            .addListener(object : Transformer.Listener {
                                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                    if (continuation.isActive) continuation.resume(RenderResult(exportResult.width, exportResult.height, exportResult.approximateDurationMs, output.length(), exportResult.videoMimeType ?: settings.codec.mime, exportResult.videoFrameCount, exportResult.channelCount))
                                }
                                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                    if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Export failed (${exportException.errorCodeName}): ${exportException.cause?.message ?: exportException.message}", exportException))
                                }
                                override fun onFallbackApplied(composition: Composition, originalTransformationRequest: TransformationRequest, fallbackTransformationRequest: TransformationRequest) {
                                    onDetail("The device selected a compatible encoder configuration. Actual output details will be shown when finished.")
                                }
                            }).build()
                        transformer = instance
                        continuation.invokeOnCancellation { /* Cancelled on the player thread in finally below. */ }
                        instance.start(plan.composition, output.absolutePath)
                        polling = launch {
                            val holder = ProgressHolder()
                            while (isActive) {
                                onProgress(if (instance.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) holder.progress else null)
                                delay(400)
                            }
                        }
                    }
                } finally { polling?.cancel(); transformer?.cancel() }
            }
        }
    }
}
