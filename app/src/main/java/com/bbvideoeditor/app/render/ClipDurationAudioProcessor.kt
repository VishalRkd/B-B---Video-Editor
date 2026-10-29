package com.bbvideoeditor.app.render

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.roundToLong

/** Normalize each post-speed PCM item to its document duration. Time-stretch/decoder tails
 * must not shift the next clip or gap in a concatenated audio sequence. Padding is streamed;
 * this processor never retains a whole clip. Export uses complete-item normalization; preview
 * uses timestamped partial streams that can end early during a seek and must not be padded. */
@UnstableApi
class ClipDurationAudioProcessor(private val durationUs:Long):BaseAudioProcessor() {
    init{require(durationUs>0)}
    private var remainingFrames=0L
    private var ended=false
    override fun onConfigure(format:AudioProcessor.AudioFormat):AudioProcessor.AudioFormat {
        if(format.encoding!=C.ENCODING_PCM_16BIT)throw AudioProcessor.UnhandledAudioFormatException(format)
        return format
    }
    override fun onFlush(metadata:AudioProcessor.StreamMetadata) {
        remainingFrames=((durationUs-metadata.positionOffsetUs).coerceAtLeast(0).toDouble()*inputAudioFormat.sampleRate/1_000_000).roundToLong()
        ended=false
    }
    override fun queueInput(input:ByteBuffer) {
        val frames=minOf(remainingFrames,(input.remaining()/inputAudioFormat.bytesPerFrame).toLong())
        val bytes=(frames*inputAudioFormat.bytesPerFrame).toInt()
        if(bytes>0) {
            val originalLimit=input.limit()
            input.limit(input.position()+bytes)
            replaceOutputBuffer(bytes).put(input).flip()
            input.limit(originalLimit)
            remainingFrames-=frames
        }
        input.position(input.limit()) // Discard only this item's excess tail.
    }
    override fun onQueueEndOfStream(){ended=true}
    override fun getOutput():ByteBuffer {
        val buffered=super.getOutput()
        if(buffered.hasRemaining() || !ended || remainingFrames==0L)return buffered
        val frames=minOf(remainingFrames,4096)
        val bytes=(frames*inputAudioFormat.bytesPerFrame).toInt()
        val output=replaceOutputBuffer(bytes).apply{limit(bytes)}
        while(output.hasRemaining())output.put(0)
        output.flip();remainingFrames-=frames
        return super.getOutput()
    }
    override fun isEnded()=super.isEnded() && remainingFrames==0L
    override fun onReset(){remainingFrames=0;ended=false}
}
