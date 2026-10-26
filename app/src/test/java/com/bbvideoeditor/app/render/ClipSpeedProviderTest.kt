package com.bbvideoeditor.app.render

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import com.bbvideoeditor.core.Clip
import com.bbvideoeditor.core.TimeRange
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.floor

@UnstableApi
class ClipSpeedProviderTest {
    @Test fun normalSpeedAndCorrectedSplitsMatchTheProjectExactly() {
        for (speed in listOf(.1f, .33f, .5f, 1f, 1.5f, 3f, 8f)) {
            for (duration in listOf(17L, 333_333L, 2_000_001L, 3_600_000_000L)) {
                val natural = floor(duration / speed.toDouble()).toLong()
                val bound = ceil(1.0 / speed).toLong()
                for (offset in -bound..bound) {
                    val expected = natural + offset
                    if (expected <= 0) continue
                    val clip = Clip(mediaId = "asset", startUs = 0, sourceRange = TimeRange(0, duration),
                        speed = speed, durationOverrideUs = expected)
                    val provider = ClipSpeedProvider(clip)
                    var sourceTimeUs = 0L
                    var outputDuration = 0.0
                    while (sourceTimeUs < duration) {
                        val next = provider.getNextSpeedChangeTimeUs(sourceTimeUs)
                        val end = if (next == C.TIME_UNSET) duration else minOf(next, duration)
                        outputDuration += (end - sourceTimeUs) / provider.getSpeed(sourceTimeUs).toDouble()
                        sourceTimeUs = end
                    }
                    assertEquals("speed=$speed duration=$duration offset=$offset", expected, floor(outputDuration).toLong())
                }
            }
        }
    }
}
