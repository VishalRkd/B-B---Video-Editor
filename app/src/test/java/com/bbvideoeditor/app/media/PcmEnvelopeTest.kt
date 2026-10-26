package com.bbvideoeditor.app.media

import android.media.AudioFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmEnvelopeTest {
    @Test fun stereoFramesShareOneTimestampAndKeepTheLouderChannel() {
        val pcm = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        listOf(0, 16384, 0, -32768, 8192, 0, 0, 0).forEach { pcm.putShort(it.toShort()) }
        pcm.flip()
        val envelope = FloatArray(4)
        val frames = consumePcm(pcm, AudioFormat.ENCODING_PCM_16BIT, 2, 4, 0, 1_000_000, envelope)
        assertEquals(4, frames)
        assertEquals(0.5f, envelope[0], 0.0001f)
        assertEquals(1f, envelope[1], 0.0001f)
        assertEquals(0.25f, envelope[2], 0.0001f)
        assertEquals(0f, envelope[3], 0.0001f)
    }

    @Test fun decoderTimestampPlacesSamplesInSourceTimeBuckets() {
        val pcm = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putShort(16384).putShort(8192)
        pcm.flip()
        val envelope = FloatArray(4)
        consumePcm(pcm, AudioFormat.ENCODING_PCM_16BIT, 1, 4, 500_000, 1_000_000, envelope)
        assertEquals(0f, envelope[0], 0f)
        assertEquals(0f, envelope[1], 0f)
        assertEquals(0.5f, envelope[2], 0.0001f)
        assertEquals(0.25f, envelope[3], 0.0001f)
    }

    @Test fun floatPcmSkipsInvalidValuesAndClampsClipping() {
        val pcm = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
            .putFloat(Float.NaN).putFloat(-2f).putFloat(0.25f).putFloat(Float.POSITIVE_INFINITY)
        pcm.flip()
        val envelope = FloatArray(4)
        consumePcm(pcm, AudioFormat.ENCODING_PCM_FLOAT, 1, 4, 0, 1_000_000, envelope)
        assertEquals(0f, envelope[0], 0f)
        assertEquals(1f, envelope[1], 0f)
        assertEquals(0.25f, envelope[2], 0f)
        assertEquals(0f, envelope[3], 0f)
    }

    @Test fun samplesOutsideTheMediaDurationDoNotBleedIntoTheEdges() {
        val pcm = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        repeat(4) { pcm.putShort(16384) }
        pcm.flip()
        val envelope = FloatArray(2)
        consumePcm(pcm, AudioFormat.ENCODING_PCM_16BIT, 1, 2, -500_000, 1_000_000, envelope)
        assertEquals(0.5f, envelope[0], 0f)
        assertEquals(0.5f, envelope[1], 0f)
        assertTrue(envelope.all { it.isFinite() })
    }

    @Test fun packed24BitPcmIsSignExtendedCorrectly() {
        val pcm = ByteBuffer.wrap(byteArrayOf(0, 0, 0x80.toByte(), 0, 0, 0x40)).order(ByteOrder.LITTLE_ENDIAN)
        val envelope = FloatArray(2)
        consumePcm(pcm, AudioFormat.ENCODING_PCM_24BIT_PACKED, 1, 2, 0, 1_000_000, envelope)
        assertEquals(1f, envelope[0], 0f)
        assertEquals(0.5f, envelope[1], 0f)
    }
}
