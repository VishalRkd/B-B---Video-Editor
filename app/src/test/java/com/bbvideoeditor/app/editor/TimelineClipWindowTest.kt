package com.bbvideoeditor.app.editor

import com.bbvideoeditor.core.*
import org.junit.Assert.*
import org.junit.Test

class TimelineClipWindowTest {
    @Test fun ordinaryClipKeepsItsDimensionsAcrossSubpixelViewportMovement() {
        val clip = Clip(startUs = 10_000_000, sourceRange = TimeRange(0, 1_500_000), text = TextContent("caption"))
        val first = timelineClipWindow(clip, TimelineViewport(10_000_000, 144.0), 900.0)
        repeat(120) { assertEquals(first, timelineClipWindow(clip, TimelineViewport(10_000_000 + it * 1_000L, 144.0), 900.0)) }
        assertEquals(clip.startUs, first.startUs); assertEquals(clip.endUs, first.endUs)
    }
    @Test fun hoursLongClipHasBoundedLayoutAtMaximumZoomAndStableBucketOrigin() {
        val clip = Clip(startUs = 0, sourceRange = TimeRange(0, 7_200_000_000), text = TextContent("long"))
        val viewport = TimelineViewport(3_000_000_000, TimelineViewport.MAX_ZOOM)
        val window = timelineClipWindow(clip, viewport, 900.0)
        assertTrue((window.endUs - window.startUs) / 1_000_000.0 * viewport.pixelsPerSecond <= 2_700.0)
        assertEquals(window, timelineClipWindow(clip, viewport.copy(centerUs = viewport.centerUs + 100), 900.0))
        assertTrue(window.startUs <= viewport.xToTime(0.0, 900.0))
        assertTrue(window.endUs >= viewport.xToTime(900.0, 900.0))
    }
    @Test fun segmentClampsToRealClipEdgesWithoutChangingSourceMapping() {
        val clip = Clip(startUs = 5_000_000, sourceRange = TimeRange(200_000, 20_000_000), text = TextContent("long"))
        val left = timelineClipWindow(clip, TimelineViewport(5_100_000, 2400.0), 900.0)
        assertEquals(clip.startUs, left.startUs)
        val right = timelineClipWindow(clip, TimelineViewport(clip.endUs - 10_000, 2400.0), 900.0)
        assertEquals(clip.endUs, right.endUs)
        assertEquals(300_000, clip.sourceTimeAt(5_100_000))
    }
}
