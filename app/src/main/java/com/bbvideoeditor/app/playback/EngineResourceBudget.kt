package com.bbvideoeditor.app.playback

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import com.bbvideoeditor.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Budgets cover app-owned allocations, not an assertion about exact driver/Media3 GPU memory. */
data class EngineResourceBudget(val thumbnailBytes:Int, val textBytes:Int, val waveformBytes:Int,
    val thumbnailWorkers:Int, val decoderWorkingBytes:Long, val maximumVideoInputs:Int,val proxyDiskBytes:Long=2L*1024*1024*1024) {
    companion object {
        fun inspect(context:Context):EngineResourceBudget {
            val manager=context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val heap=minOf(Runtime.getRuntime().maxMemory(),manager.memoryClass*1024L*1024)
            val low=manager.isLowRamDevice || !android.os.Process.is64Bit()
            return EngineResourceBudget((heap/16).coerceIn(4L shl 20,32L shl 20).toInt(),
                (heap/12).coerceIn(4L shl 20,32L shl 20).toInt(),(heap/128).coerceIn(512L shl 10,4L shl 20).toInt(),
                if(low || Build.VERSION.SDK_INT<27)1 else 2,
                (heap/2).coerceIn(48L shl 20,256L shl 20),if(low)2 else 4)
        }
    }
}
enum class PreviewAdmissionKind { ORIGINAL, REDUCED_OUTPUT, OPTIMIZED_MEDIA_REQUIRED, UNSUPPORTED }
data class PreviewAdmission(val kind:PreviewAdmissionKind, val quality:PreviewQuality,
    val declaredVideoInputs:Int, val peakActiveVideos:Int, val estimatedWorkingBytes:Long,
    val reasons:List<String>) {
    val admitted get()=kind==PreviewAdmissionKind.ORIGINAL || kind==PreviewAdmissionKind.REDUCED_OUTPUT
}

/** Source buffers are charged at SOURCE resolution. Lower output quality never disguises 4K
 * decoder pressure. maxSupportedInstances is deliberately not treated as an admission proof. */
class PreviewResourcePolicy(private val context:Context, private val budget:EngineResourceBudget) {
    private val failed=linkedSetOf<String>()
    @Synchronized fun recordFailure(project:Project) {
        project.media.values.filter{it.kind==MediaKind.VIDEO}.forEach{failed+=signature(it)}
        while(failed.size>128)failed.remove(failed.first())
    }
    suspend fun assess(plan:RenderPlan,requested:PreviewQuality):PreviewAdmission=withContext(Dispatchers.Default) {
        val clips=plan.visualClips.filter{plan.project.media[it.mediaId]?.kind==MediaKind.VIDEO}
        val media=clips.mapNotNull{plan.project.media[it.mediaId]}.distinctBy{it.id}
        val problems=mutableListOf<String>()
        val codecs=MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filterNot{it.isEncoder}
        for(source in media) {
            coroutineContext.ensureActive()
            val mime=source.codecMimeType.ifBlank { withContext(Dispatchers.IO){codecMime(source)} }
            val supported=codecs.any { codec -> runCatching {
                codec.supportedTypes.any{it.equals(mime,true)} && codec.getCapabilitiesForType(mime).videoCapabilities?.let {
                    it.areSizeAndRateSupported(source.width,source.height,source.frameRate.takeIf{it>0}?.toDouble() ?: 30.0)
                }==true
            }.getOrDefault(false) }
            if(!supported)problems+="A source format (${source.width}×${source.height}, $mime) is outside reported decoder capabilities."
        }
        val events=clips.flatMap{listOf(it.startUs to 1,it.endUs to -1)}.sortedWith(compareBy<Pair<Long,Int>>{it.first}.thenBy{it.second})
        var active=0;var peak=0
        events.forEach{active+=it.second;peak=maxOf(peak,active)}
        val declared=plan.visuals.count{track->track.track.clips.any{plan.project.media[it.mediaId]?.kind==MediaKind.VIDEO}}
        val sourceBytes=plan.visuals.sumOf{track->track.track.clips.mapNotNull{plan.project.media[it.mediaId]}
            .maxOfOrNull{it.width.toLong()*it.height*(if(it.kind==MediaKind.VIDEO)16 else 8)} ?: 0L} // four RGBA-equivalent surfaces/references, before output textures
        val (width,height)=requested.dimensions(plan.project.canvas)
        val working=sourceBytes+width.toLong()*height*4*(plan.visuals.size+3)
        val hdr=media.any { it.colorTransfer==6 || it.colorTransfer==7 }
        val knownFailure=synchronized(this@PreviewResourcePolicy){media.any{signature(it) in failed}}
        when {
            plan.visuals.size>budget.maximumVideoInputs ->PreviewAdmission(PreviewAdmissionKind.UNSUPPORTED,requested,declared,peak,working,
                listOf("This device's preview policy admits ${budget.maximumVideoInputs} visual inputs; this composition needs ${plan.visuals.size}. No layer was omitted."))
            problems.isNotEmpty()->PreviewAdmission(PreviewAdmissionKind.UNSUPPORTED,requested,declared,peak,working,problems)
            hdr || declared>budget.maximumVideoInputs || sourceBytes>budget.decoderWorkingBytes || knownFailure ->
                PreviewAdmission(PreviewAdmissionKind.OPTIMIZED_MEDIA_REQUIRED,PreviewQuality.FAST,declared,peak,working,
                    listOf("Original-resolution preview exceeds this device's conservative decoder budget. Optimized preview media is required; all layers are retained."))
            working>budget.decoderWorkingBytes ->PreviewAdmission(PreviewAdmissionKind.REDUCED_OUTPUT,PreviewQuality.FAST,declared,peak,working,
                listOf("Preview output was reduced to limit texture memory. Original source decoding and export are unchanged."))
            else->PreviewAdmission(PreviewAdmissionKind.ORIGINAL,requested,declared,peak,working,emptyList())
        }
    }
    private fun signature(media:MediaReference)="${media.mimeType}:${media.width}:${media.height}:${media.frameRate}"
    private fun codecMime(media:MediaReference):String {
        val extractor=MediaExtractor()
        return try {
            extractor.setDataSource(context,Uri.parse(media.uri),null)
            (0 until extractor.trackCount).map{extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME).orEmpty()}.firstOrNull{it.startsWith("video/")} ?: media.mimeType
        } finally{extractor.release()}
    }
}
