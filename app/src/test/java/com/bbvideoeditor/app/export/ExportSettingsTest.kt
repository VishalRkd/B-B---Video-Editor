package com.bbvideoeditor.app.export

import com.bbvideoeditor.core.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportSettingsTest {
    private val project = Project(name = "Export capability test")

    @Test fun supportedSelectionPreservesQualityAndDocumentDestination() {
        val settings = ExportSettings(frameRate = 60, bitrate = 24_000_000, documentUri = "content://output/movie")
        val capabilities = listOf(EncoderCapability(VideoCodec.AVC, 1920, 1080, 60, true, true))
        assertEquals(settings, settings.supportedFallback(project, capabilities))
    }

    @Test fun olderDeviceCanChangeBothResolutionAndFrameRateAtOnce() {
        val settings = ExportSettings(frameRate = 60)
        val capabilities = listOf(EncoderCapability(VideoCodec.AVC, 1280, 720, 30, true, false))
        assertEquals(settings.copy(resolution = 720, frameRate = 30), settings.supportedFallback(project, capabilities))
    }

    @Test fun unavailableHevcFallsBackToAnAdvertisedAvcConfiguration() {
        val settings = ExportSettings(codec = VideoCodec.HEVC)
        val capabilities = listOf(
            EncoderCapability(VideoCodec.HEVC, 1920, 1080, 30, false, false),
            EncoderCapability(VideoCodec.AVC, 1920, 1080, 30, true, true),
        )
        assertEquals(settings.copy(codec = VideoCodec.AVC), settings.supportedFallback(project, capabilities))
    }

    @Test fun absentEncodersDoNotInventASupportedConfiguration() {
        assertNull(ExportSettings().supportedFallback(project, emptyList()))
    }
}
