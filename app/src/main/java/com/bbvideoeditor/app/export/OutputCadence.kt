package com.bbvideoeditor.app.export

import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.ensureActive
import java.io.File
import kotlin.coroutines.coroutineContext

data class OutputCadence(val frameCount:Long,val averageFps:Double?,val minimumDeltaUs:Long?,val maximumDeltaUs:Long?,val reordered:Boolean=false)

/** Inspect muxed VIDEO sample timestamps, never requested settings or wall render speed.
 * Constant memory even for long exports; VFR is reported as an average plus cadence bounds. */
internal suspend fun inspectOutputCadence(file:File):OutputCadence {
    val extractor=MediaExtractor()
    try {
        extractor.setDataSource(file.path)
        val index=(0 until extractor.trackCount).firstOrNull{extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/")==true}
            ?: return OutputCadence(0,null,null,null)
        extractor.selectTrack(index)
        var count=0L;var first=Long.MAX_VALUE;var last=Long.MIN_VALUE;var previous=0L;var minimum=Long.MAX_VALUE;var maximum=0L;var reordered=false
        while(extractor.sampleTime>=0) {
            if(count%1024==0L)coroutineContext.ensureActive()
            val time=extractor.sampleTime
            first=minOf(first,time);last=maxOf(last,time)
            if(count>0L) {
                if(time<previous)reordered=true
                if(time>previous){minimum=minOf(minimum,time-previous);maximum=maxOf(maximum,time-previous)}
            }
            previous=time;count++
            if(!extractor.advance())break
        }
        return OutputCadence(count,if(count>1 && last>first)(count-1)*1_000_000.0/(last-first) else null,
            minimum.takeIf{it!=Long.MAX_VALUE && !reordered},maximum.takeIf{it>0 && !reordered},reordered)
    }finally{extractor.release()}
}
