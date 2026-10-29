package com.bbvideoeditor.app.render

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ClipDurationAudioProcessorTest {
    private val format=AudioProcessor.AudioFormat(48_000,2,C.ENCODING_PCM_16BIT)
    @Test fun excessTailIsConsumedWithoutMovingTheNextClip() {
        val processor=ClipDurationAudioProcessor(100_000)
        processor.configure(format);processor.flush()
        val input=ByteBuffer.allocateDirect(6000*4).order(ByteOrder.nativeOrder())
        repeat(12000){input.putShort(1234)};input.flip()
        processor.queueInput(input)
        assertFalse(input.hasRemaining())
        val output=processor.output
        assertEquals(4800*4,output.remaining());assertEquals(1234,output.short.toInt())
        output.position(output.limit());processor.queueEndOfStream()
        assertEquals(0,processor.output.remaining());assertTrue(processor.isEnded)
    }
    @Test fun shortInputPadsInBoundedChunksAndFlushHonorsSeekOffset() {
        val processor=ClipDurationAudioProcessor(1_000_000)
        processor.configure(format);processor.flush(AudioProcessor.StreamMetadata(500_000))
        processor.queueInput(ByteBuffer.allocateDirect(4800*4))
        assertEquals(4800*4,processor.output.remaining())
        processor.queueEndOfStream()
        var bytes=0
        while(!processor.isEnded) {
            val output=processor.output
            assertTrue(output.remaining()<=4096*4)
            bytes+=output.remaining()
            while(output.hasRemaining())assertEquals(0,output.get().toInt())
        }
        assertEquals((24000-4800)*4,bytes)
        processor.flush();processor.queueEndOfStream()
        var resetBytes=0
        while(!processor.isEnded){val output=processor.output;resetBytes+=output.remaining();output.position(output.limit())}
        assertEquals(48_000*4,resetBytes)
    }
}
