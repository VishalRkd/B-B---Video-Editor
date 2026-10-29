package com.bbvideoeditor.app.render

import android.content.Context
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.video.HardwareBufferFrame
import androidx.media3.common.video.AsyncFrame
import androidx.media3.common.video.Frame
import androidx.media3.common.video.FrameProcessor
import androidx.media3.common.video.FrameWriter
import androidx.media3.common.video.SyncFenceWrapper
import androidx.media3.effect.DefaultGlFrameProcessor
import androidx.media3.transformer.Composition
import java.util.concurrent.atomic.AtomicBoolean

/** Public Media3 packet pipeline. Media3 owns native interop, GL contexts, pools and teardown.
 * A submission acknowledges writer acceptance of a buffer AND its fence, not display scanout.
 * Source identity is checked before processing; a stale prewarmed item is never acknowledged or
 * displayed as the requested edit. The controller can recover it on the same player. */
@androidx.annotation.OptIn(androidx.media3.common.util.ExperimentalApi::class, androidx.media3.common.util.UnstableApi::class)
internal class OutputFramePipeline private constructor(
    context:Context,
    private val terminalTimeUs:Long?,
    private val frameDurationUs:Long,
    private val validateInputs:Boolean,
    private val onInputMismatch:(FrameInputMismatch)->Unit,
    private val onDelivered:(FrameSubmission)->Unit,
) : AutoCloseable {
    @Volatile var expectedRevision:Long?=null
    private val closed=AtomicBoolean(false)
    private val processors=mutableListOf<FrameProcessor>()
    val factory=FrameProcessor.Factory { output,listenerExecutor,listener ->
        // Each installed processor owns its receipts. Two structural revisions may emit the
        // same timestamp during teardown; an old writer must never consume a new stamp.
        val stamps=LinkedHashMap<Long,FrameSubmission>()
        val observing=object:FrameWriter by output {
            private var lastTimeUs=Long.MIN_VALUE
            private var ended=false
            override fun signalEndOfStream() {
                if(terminalTimeUs==null){output.signalEndOfStream();return}
                // Per-item EOS must not terminate an encoder before the composition ends.
                if(!ended && lastTimeUs>=terminalTimeUs-frameDurationUs-1_000) {
                    ended=true;output.signalEndOfStream()
                }
            }
            override fun queueInputFrame(frame:Frame,fence:SyncFenceWrapper?) {
                val time=frame.contentTimeUs
                val stamp=synchronized(stamps){stamps.remove(time)}
                // The GL processor creates a new output-buffer frame and does not propagate
                // the packet's wall-clock release metadata. Preserve it explicitly: otherwise
                // SurfaceHolderFrameWriter displays early-processed frames immediately, ahead
                // of the audio/player clock (especially visible in opacity animation).
                val scheduled=if(frame is HardwareBufferFrame && stamp?.displayTimeNs!=null) {
                    frame.buildUpon().setMetadata(frame.metadata.toMutableMap().apply {
                        put(Frame.KEY_DISPLAY_TIME_NS,stamp.displayTimeNs)
                    }).build()
                }else frame
                output.queueInputFrame(scheduled,fence)
                lastTimeUs=time
                onDelivered(stamp ?: FrameSubmission(time,null,null))
            }
        }
        val forwarding=object:FrameProcessor.Listener by listener {
            override fun onFrameProcessed(frame:Frame,fence:SyncFenceWrapper?) {
                listener.onFrameProcessed((frame as? LegacySdrFrame)?.original ?: frame,fence)
            }
        }
        val formats=LinkedHashMap<Format,Format>()
        val delegate=DefaultGlFrameProcessor.Factory.Builder(context.applicationContext)
            .setWorkingColorSpace(DefaultGlFrameProcessor.BT709_SRGB)
            .build().create(observing,listenerExecutor,forwarding)
        synchronized(processors){processors+=delegate}
        object:FrameProcessor by delegate {
            override fun close() {
                try{delegate.close()}finally{
                    synchronized(stamps){stamps.clear()}
                    synchronized(processors){processors.remove(delegate)}
                }
            }
            override fun queue(frames:List<AsyncFrame>):Boolean {
                val primary=frames.first().frame
                val time=primary.contentTimeUs
                val graph=primary.metadata[DefaultGlFrameProcessor.KEY_COMPOSITOR_SETTINGS] as? FrameGraphIdentity
                val revision=graph?.binding?.revision
                val stale=expectedRevision?.let{it!=revision}==true
                var mismatch:FrameInputMismatch?=null
                if(validateInputs && graph!=null && !stale) {
                    val composition=primary.metadata[Composition.KEY_COMPOSITION] as? Composition
                    val present=mutableSetOf<Int>()
                    for(input in frames) {
                        val sequence=input.frame.metadata[DefaultGlFrameProcessor.KEY_COMPOSITION_SEQUENCE_INDEX] as? Int ?: continue
                        present+=sequence
                        val expected=graph.binding.plan.visuals.getOrNull(sequence-1)?.clipAt(time) ?: continue
                        val item=input.frame.metadata[Composition.KEY_COMPOSITION_ITEM_INDEX] as? Int ?: -1
                        val actual=composition?.sequences?.getOrNull(sequence)?.editedMediaItems?.getOrNull(item)?.mediaItem?.mediaId
                        if(actual!=expected.id)mismatch=FrameInputMismatch(time,revision,sequence,item)
                        val media=graph.binding.plan.project.media[expected.mediaId]
                        if(media?.kind==com.bbvideoeditor.core.MediaKind.VIDEO) {
                            val period=(1_000_000.0/(media.frameRate.takeIf{it>0} ?: 30f)/expected.speed).toLong()+2_000
                            if(kotlin.math.abs(input.frame.contentTimeUs-time)>period)
                                mismatch=FrameInputMismatch(time,revision,sequence,item)
                        }
                    }
                    graph.binding.plan.visuals.forEachIndexed { index,track ->
                        if(track.clipAt(time)!=null && index+1 !in present)mismatch=FrameInputMismatch(time,revision,index+1,-1)
                    }
                }
                if(stale || mismatch!=null) {
                    // No GPU read occurred. Transfer the acquire fence back as the release fence.
                    // Media3's listener executor serializes these callbacks after queue returns,
                    // when its in-flight ownership map has registered the accepted input frames.
                    listenerExecutor.execute {
                        frames.forEach{listener.onFrameProcessed(it.frame,it.acquireFence)}
                        listener.onWakeup()
                    }
                    mismatch?.let(onInputMismatch)
                    return true
                }
                val stamp=FrameSubmission(time,revision,primary.metadata[DefaultGlFrameProcessor.KEY_FRAME_DISCONTINUITY_NUMBER] as? Int,
                    (primary.metadata[Frame.KEY_DISPLAY_TIME_NS] as? Number)?.toLong()?.takeIf{it>0})
                synchronized(stamps){stamps[time]=stamp;while(stamps.size>128)stamps.remove(stamps.keys.first())}
                // Preserve the editor's established SDR RGB convention. The Android external
                // sampler already applies the buffer's YUV matrix/range; the old pipeline treated
                // resulting SDR RGB as the common sRGB working space, without a second primaries
                // conversion. HDR retains its real metadata and uses Media3 tone mapping.
                val compatible=frames.map { input ->
                    val frame=input.frame
                    val color=frame.format.colorInfo
                    if(frame is HardwareBufferFrame && color?.colorSpace==C.COLOR_SPACE_BT601) {
                        val format=formats.getOrPut(frame.format) {
                            frame.format.buildUpon().setColorInfo(color.buildUpon().setColorSpace(C.COLOR_SPACE_BT709).build()).build()
                        }
                        while(formats.size>32)formats.remove(formats.keys.first())
                        AsyncFrame(LegacySdrFrame(frame,format),input.acquireFence)
                    }else input
                }
                val accepted=delegate.queue(compatible)
                if(!accepted)synchronized(stamps){if(stamps[time]===stamp)stamps.remove(time)}
                return accepted
            }
        }
    }
    override fun close() {
        if(!closed.compareAndSet(false,true))return
        synchronized(processors){processors.forEach{it.close()};processors.clear()}
    }
    companion object {
        @RequiresApi(28)
        suspend fun create(context:Context,terminalTimeUs:Long?=null,frameDurationUs:Long=33_333,
            validateInputs:Boolean=true,onInputMismatch:(FrameInputMismatch)->Unit={},
            onDelivered:(FrameSubmission)->Unit)=OutputFramePipeline(context,terminalTimeUs,frameDurationUs,
                validateInputs,onInputMismatch,onDelivered)
    }
}
internal data class FrameSubmission(val timeUs:Long,val revision:Long?,val discontinuity:Int?,val displayTimeNs:Long?=null)
internal data class FrameInputMismatch(val timeUs:Long,val revision:Long?,val sequence:Int,val item:Int)

@androidx.annotation.OptIn(androidx.media3.common.util.ExperimentalApi::class,androidx.media3.common.util.UnstableApi::class)
private class LegacySdrFrame(val original:HardwareBufferFrame,private val compatible:Format):HardwareBufferFrame by original {
    override fun getFormat()=compatible
}
