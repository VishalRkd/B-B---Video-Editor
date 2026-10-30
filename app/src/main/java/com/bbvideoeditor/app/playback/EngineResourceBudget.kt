package com.bbvideoeditor.app.playback

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.bbvideoeditor.core.*

/** Budgets cover app-owned allocations, not an assertion about exact driver/Media3 GPU memory. */
data class EngineResourceBudget(val thumbnailBytes:Int, val textBytes:Int, val waveformBytes:Int,
    val thumbnailWorkers:Int, val decoderWorkingBytes:Long, val maximumVideoInputs:Int,val proxyDiskBytes:Long=2L*1024*1024*1024, val maximumActiveVideos:Int=maximumVideoInputs) {
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
    val reasons:List<String>, val requiredMediaIds:Set<String> = emptySet(), val graphInputs:Int = 0) {
    val admitted get()=kind==PreviewAdmissionKind.ORIGINAL || kind==PreviewAdmissionKind.REDUCED_OUTPUT
}

/** Static graph slots and peak videos are different metrics. Source buffers remain charged at
 * source resolution, conservatively per retained sequence. Codec evidence is owned by the service. */
class PreviewResourcePolicy(private val budget:EngineResourceBudget) {
    constructor(context:Context, budget:EngineResourceBudget):this(budget)
    fun assess(plan:RenderPlan,requested:PreviewQuality,selected:Map<String,MediaReference> = emptyMap()):PreviewAdmission {
        fun media(clip:Clip)=selected[clip.mediaId] ?: plan.project.media[clip.mediaId]
        val clips=plan.visualClips.filter{media(it)?.kind==MediaKind.VIDEO}
        val events=clips.flatMap{listOf(it.startUs to 1,it.endUs to -1)}.sortedWith(compareBy<Pair<Long,Int>>{it.first}.thenBy{it.second})
        var active=0;var peak=0
        events.forEach{active+=it.second;peak=maxOf(peak,active)}
        val declared=plan.visuals.count{track->track.track.clips.any{media(it)?.kind==MediaKind.VIDEO}}
        val sources=plan.visualClips.mapNotNull(::media).distinctBy{it.id}
        fun cost(source:MediaReference)=source.width.toLong()*source.height*(if(source.kind==MediaKind.VIDEO)16 else 8)
        val sourceBytes=plan.visuals.sumOf{track->track.track.clips.mapNotNull(::media).maxOfOrNull(::cost) ?: 0L}
        fun working(quality:PreviewQuality):Long {
            val (width,height)=quality.dimensions(plan.project.canvas)
            return sourceBytes+width.toLong()*height*4*(plan.visuals.size+3)
        }
        if(plan.visuals.size>budget.maximumVideoInputs || peak>budget.maximumActiveVideos)
            return PreviewAdmission(PreviewAdmissionKind.UNSUPPORTED,requested,declared,peak,working(requested),
                listOf("This composition exceeds the device's safe simultaneous preview inputs. No layer was omitted."),graphInputs=plan.visuals.size)
        val fastOutput=working(PreviewQuality.FAST)-sourceBytes
        val share=((budget.decoderWorkingBytes-fastOutput).coerceAtLeast(0)/maxOf(1,plan.visuals.size))
        val required=sources.filter { it.colorTransfer in listOf(6,7) || (working(PreviewQuality.FAST)>budget.decoderWorkingBytes && cost(it)>share) }.map{it.id}.toSet()
        return when {
            required.isNotEmpty()->PreviewAdmission(PreviewAdmissionKind.OPTIMIZED_MEDIA_REQUIRED,PreviewQuality.FAST,declared,peak,working(requested),
                listOf("Preparing resource-appropriate editing media; original audio and export remain unchanged."),required,plan.visuals.size)
            working(requested)>budget.decoderWorkingBytes->PreviewAdmission(PreviewAdmissionKind.REDUCED_OUTPUT,PreviewQuality.FAST,declared,peak,working(PreviewQuality.FAST),
                listOf("Preview output was reduced to limit texture memory."),graphInputs=plan.visuals.size)
            else->PreviewAdmission(PreviewAdmissionKind.ORIGINAL,requested,declared,peak,working(requested),emptyList(),graphInputs=plan.visuals.size)
        }
    }
}
