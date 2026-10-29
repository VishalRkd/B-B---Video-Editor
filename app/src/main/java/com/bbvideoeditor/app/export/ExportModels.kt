package com.bbvideoeditor.app.export

import android.media.MediaCodecList
import android.os.Build
import com.bbvideoeditor.core.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
enum class VideoCodec(val mime: String, val label: String) { AVC("video/avc", "H.264"), HEVC("video/hevc", "HEVC") }

@Serializable
data class ExportSettings(
    val resolution: Int = 1080,
    val frameRate: Int = 30,
    val bitrate: Int = 12_000_000,
    val codec: VideoCodec = VideoCodec.AVC,
    val documentUri: String? = null,
) {
    init { require(resolution in listOf(720, 1080, 2160)); require(frameRate in listOf(24, 25, 30, 50, 60)); require(bitrate in 1_000_000..120_000_000) }
    /** Resolution names refer to the short edge, preserving project aspect including portrait. */
    fun dimensions(project: Project): Pair<Int, Int> {
        val ratio = project.canvas.aspectRatio
        return if (ratio >= 1f) even(resolution * ratio) to resolution else resolution to even(resolution / ratio)
    }
    fun estimatedBytes(durationUs: Long): Long = ((bitrate + 192_000L) * (durationUs / 1_000_000.0) / 8).toLong()

    fun supportedBy(project: Project, capabilities: List<EncoderCapability>): Boolean {
        val (width, height) = dimensions(project)
        return capabilities.any { it.supported && it.codec == codec && it.width == width && it.height == height && it.frameRate == frameRate }
    }

    /** Change coupled settings together so a device cannot become stuck between disabled choices. */
    fun supportedFallback(project: Project, capabilities: List<EncoderCapability>): ExportSettings? {
        for (candidateCodec in (listOf(codec) + VideoCodec.entries).distinct()) {
            for (candidateResolution in listOf(resolution, 1080, 720, 2160).distinct()) {
                for (candidateFps in listOf(frameRate, 30, 25, 24, 50, 60).distinct()) {
                    val candidate = copy(codec = candidateCodec, resolution = candidateResolution, frameRate = candidateFps)
                    if (candidate.supportedBy(project, capabilities)) return candidate
                }
            }
        }
        return null
    }

    private fun even(value: Float): Int = ((value / 2).roundToInt() * 2).coerceAtLeast(2)
}

data class EncoderCapability(val codec: VideoCodec, val width: Int, val height: Int, val frameRate: Int, val supported: Boolean, val hardware: Boolean)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object ExportCapabilities {
    private val observedFallbacks=java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var context:android.content.Context?=null
    private var observationsLoaded=false
    private val observationLock=Any()
    /** No disk work during application startup. Probes/render completion load on workers. */
    internal fun configure(context:android.content.Context){this.context=context.applicationContext}
    private val observationPrefix get()="foundation-v1|${androidx.media3.common.MediaLibraryInfo.VERSION}|${Build.FINGERPRINT}|"
    private fun key(codec:VideoCodec,width:Int,height:Int,fps:Int)="$observationPrefix${codec.mime}|$width|$height|$fps"
    private fun loadObservations()=synchronized(observationLock) {
        if(observationsLoaded)return@synchronized
        val app=context ?: return@synchronized
        observedFallbacks+=app.getSharedPreferences("export_dimension_observations_v1",android.content.Context.MODE_PRIVATE)
            .getStringSet("fallbacks",emptySet()).orEmpty().filter{it.startsWith(observationPrefix)}
        observationsLoaded=true
    }
    internal fun recordObservedOutput(project:Project,settings:ExportSettings,width:Int,height:Int) {
        loadObservations()
        val requested=settings.dimensions(project)
        if(requested.first!=width || requested.second!=height) {
            observedFallbacks+=key(settings.codec,requested.first,requested.second,settings.frameRate)
            while(observedFallbacks.size>128){observedFallbacks.firstOrNull()?.let(observedFallbacks::remove) ?: break}
            context?.getSharedPreferences("export_dimension_observations_v1",android.content.Context.MODE_PRIVATE)?.edit()
                ?.putStringSet("fallbacks",observedFallbacks.toList().takeLast(128).toSet())?.apply()
        }
    }
    suspend fun inspect(project: Project): List<EncoderCapability> = withContext(Dispatchers.Default) {
        withContext(Dispatchers.IO){loadObservations()}
        val encoders = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { it.isEncoder }
        buildList {
            for (codec in VideoCodec.entries) for (resolution in listOf(720, 1080, 2160)) for (fps in listOf(24, 25, 30, 50, 60)) {
                val (width, height) = ExportSettings(resolution, fps, codec = codec).dimensions(project)
                val matching = encoders.filter { encoder ->
                    runCatching {
                        if (encoder.supportedTypes.none { it.equals(codec.mime, true) }) false
                        else encoder.getCapabilitiesForType(codec.mime).videoCapabilities?.let {
                            it.areSizeAndRateSupported(width, height, fps.toDouble()) || it.areSizeAndRateSupported(height, width, fps.toDouble())
                        } == true
                    }.getOrDefault(false)
                }
                add(EncoderCapability(codec, width, height, fps, matching.isNotEmpty() && key(codec,width,height,fps) !in observedFallbacks, matching.any {
                    if (Build.VERSION.SDK_INT >= 29) it.isHardwareAccelerated else !it.name.startsWith("OMX.google.") && !it.name.startsWith("c2.android.")
                }))
            }
        }
    }
}

@Serializable
enum class ExportPhase { IDLE, PREPARING, RUNNING, PUBLISHING, SUCCEEDED, CANCELLED, FAILED }

@Serializable
data class ExportState(
    val jobId: String? = null,
    val projectId: String? = null,
    val projectName: String = "",
    val phase: ExportPhase = ExportPhase.IDLE,
    val progress: Int? = null,
    val outputUri: String? = null,
    val outputBytes: Long = 0,
    val detail: String = "",
    val error: String? = null,
) {
    val active: Boolean get() = phase in listOf(ExportPhase.PREPARING, ExportPhase.RUNNING, ExportPhase.PUBLISHING)
}

@Serializable
data class ExportRequest(val jobId: String, val project: Project, val settings: ExportSettings)
