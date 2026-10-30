package com.bbvideoeditor.app.export

import android.content.Context
import com.bbvideoeditor.app.media.*
import androidx.media3.common.util.Clock
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import android.os.Build
import com.bbvideoeditor.app.render.OutputFramePipeline
import androidx.media3.effect.HardwareBufferJni
import android.media.MediaMetadataRetriever
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.*
import com.bbvideoeditor.app.render.CompositionCompiler
import com.bbvideoeditor.core.Project
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DecoderSelection(val name:String,val initializationFailures:Int)

data class RenderResult(val width: Int, val height: Int, val durationMs: Long, val bytes: Long, val codec: String, val videoFrames: Int, val audioChannels: Int, val actualFrameRate: Double? = null, val cadence: OutputCadence? = null, val decoderSelections:List<DecoderSelection> = emptyList())

/** Hardware-assisted render backend. Every edit is compiled by the same compiler as preview. */
@UnstableApi
@androidx.annotation.OptIn(androidx.media3.common.util.ExperimentalApi::class)
class Media3RenderEngine(context: Context, private val compiler: CompositionCompiler, private val synchronizedFrames: Boolean = true, private val preferredDecoderName: String? = null, private val decoderSelectorOverride:MediaCodecSelector? = null,
    private val preferSingleVisualInput: Boolean = false) {
    private val context = context.applicationContext

    suspend fun render(project:Project,settings:ExportSettings,output:File,
        onProgress:(Int?)->Unit={},onDetail:(String)->Unit={}):RenderResult {
        val lease=(context as? com.bbvideoeditor.app.EditorApplication)?.assets?.acquire()
        try{return renderOwned(project,settings,output,onProgress,onDetail)}
        catch(error:CancellationException){throw error}
        catch(error:MediaCompatibilityException){throw error}
        catch(error:Exception){
            val kind=when {
                error is SecurityException->MediaFailureKind.ACCESS
                error.message?.contains("ENOSPC")==true || error.message?.contains("No space left")==true->MediaFailureKind.STORAGE
                else->MediaFailureKind.UNKNOWN
            }
            val sources=project.timeline.clips.mapNotNull{project.media[it.mediaId]}.distinctBy{it.id}
            throw MediaCompatibilityException(PreviewFailure(kind,sources.singleOrNull()?.id,
                "Couldn't render this project. Check source access and available resources.",error.javaClass.simpleName,
                if(kind==MediaFailureKind.ACCESS)MediaRetry.RECHECK else MediaRetry.RESOURCES,sources.map{it.id}.toSet(),error))
        }finally{lease?.close()}
    }
    private suspend fun renderOwned(project: Project, settings: ExportSettings, output: File,
                       onProgress: (Int?) -> Unit, onDetail: (String) -> Unit): RenderResult {
        val (width, height) = settings.dimensions(project)
        val plan = compiler.compile(project, width, height, settings.frameRate,
            preferSingleVisualInput = preferSingleVisualInput || project.timeline.clips.any { it.speed != 1f })
        val pipeline = if (synchronizedFrames && project.timeline.clips.any { it.properties.isNotEmpty() } && project.media.values.none { it.colorTransfer==6 || it.colorTransfer==7 } && Build.VERSION.SDK_INT >= 28) OutputFramePipeline.create(context,plan.durationUs,1_000_000L/settings.frameRate,validateInputs=false) { _ -> } else null
        val selections=java.util.Collections.synchronizedList(mutableListOf<DecoderSelection>())
        val loader = withContext(Dispatchers.Default) {
            val selector = decoderSelectorOverride ?: MediaCodecSelector { mime, secure, tunneling ->
                MediaCodecSelector.DEFAULT.getDecoderInfos(mime, secure, tunneling)
                    .sortedBy { if (it.name == preferredDecoderName) 0 else 1 }.take(3)
            }
            val decoder = DefaultDecoderFactory.Builder(context).setEnableDecoderFallback(true).setMediaCodecSelector(selector)
                .setListener { name, failures -> selections.add(DecoderSelection(name,failures.size)) }.build()
            DefaultAssetLoaderFactory(context, decoder, Clock.DEFAULT, null)
        }
        val result = try { withContext(Dispatchers.Main.immediate) {
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
                            .apply { if (Build.VERSION.SDK_INT >= 28 && pipeline != null) {
                                setFrameProcessorFactory(pipeline.factory)
                                setNativeHardwareBufferHelpers(HardwareBufferJni.INSTANCE)
                            } }
                            .setAssetLoaderFactory(loader)
                            .setEncoderFactory(encoder)
                            .setVideoMimeType(settings.codec.mime)
                            .setAudioMimeType(MimeTypes.AUDIO_AAC)
                            .addListener(object : Transformer.Listener {
                                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                    if (continuation.isActive) continuation.resume(RenderResult(exportResult.width, exportResult.height, exportResult.approximateDurationMs, output.length(), exportResult.videoMimeType ?: settings.codec.mime, exportResult.videoFrameCount, exportResult.channelCount))
                                }
                                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                    if (continuation.isActive) continuation.resumeWithException(classifyFailure(project, exportException))
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
        } finally { pipeline?.close() }
        // Media3 normally encodes portrait frames sideways for codec compatibility and writes
        // MP4 rotation metadata. Report the displayed dimensions, including encoder fallbacks.
        return withContext(Dispatchers.IO) {
            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(output.absolutePath)
                val encodedWidth = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: result.width
                val encodedHeight = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: result.height
                val rotation = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                val cadence=inspectOutputCadence(output)
                val measured=if (rotation % 180 != 0) result.copy(width = encodedHeight, height = encodedWidth)
                    else result.copy(width = encodedWidth, height = encodedHeight)
                ExportCapabilities.recordObservedOutput(project,settings,measured.width,measured.height)
                measured.copy(actualFrameRate=cadence.averageFps,cadence=cadence,decoderSelections=synchronized(selections){selections.toList()})
            } finally { metadata.release() }
        }
    }
    private fun classifyFailure(project:Project,error:ExportException):MediaCompatibilityException {
        val sources=project.timeline.clips.mapNotNull{project.media[it.mediaId]}.distinctBy{it.id}
        val source=sources.singleOrNull()
        val code=error.errorCodeName
        val kind=when {
            generateSequence<Throwable>(error){it.cause}.any{it is SecurityException} || code.contains("PERMISSION") || code.contains("FILE_NOT_FOUND")->MediaFailureKind.ACCESS
            generateSequence<Throwable>(error){it.cause}.any{it.message?.contains("ENOSPC")==true || it.message?.contains("No space left")==true}->MediaFailureKind.STORAGE
            code.contains("DECODING_FORMAT_UNSUPPORTED")->MediaFailureKind.DECODER_INIT
            code.contains("DECOD")->MediaFailureKind.DECODE
            code.contains("ENCOD")->MediaFailureKind.ENCODER
            code.contains("IO")->MediaFailureKind.ACCESS
            else->MediaFailureKind.UNKNOWN
        }
        val retry=when(kind){MediaFailureKind.ACCESS->MediaRetry.RECHECK;MediaFailureKind.STORAGE,MediaFailureKind.UNKNOWN->MediaRetry.RESOURCES;else->MediaRetry.PREPARE}
        if(source!=null)return sourceFailure(source,kind,code,retry,error)
        return MediaCompatibilityException(PreviewFailure(kind,userMessage="Couldn't render this project. Check its source videos and available resources.",
            technicalReason=code,retry=retry,candidateSourceIds=sources.map{it.id}.toSet(),cause=error))
    }

}
