package com.beatsandbeyond.video_editor.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ExportConfig] default values and construction invariants.
 */
class ExportConfigTest {

    @Test
    fun `default config uses FHD resolution and MP4 format`() {
        val config = ExportConfig(outputPath = "/tmp/out.mp4")
        assertEquals(Resolution.FHD, config.resolution)
        assertEquals(ExportFormat.MP4, config.format)
        assertEquals(30.0f, config.frameRate)
        assertEquals(8_000_000, config.videoBitrateBps)
        assertEquals(192_000, config.audioBitrateBps)
    }

    @Test
    fun `config exposes requested output path`() {
        val path = "/storage/emulated/0/Movies/bb_export_1.mp4"
        val config = ExportConfig(outputPath = path, resolution = Resolution.HD)
        assertEquals(path, config.outputPath)
        assertEquals(Resolution.HD, config.resolution)
    }

    @Test
    fun `config supports custom bitrate and frame rate`() {
        val config = ExportConfig(
            outputPath = "/tmp/out.mp4",
            resolution = Resolution.PORTRAIT_9_16,
            frameRate = 60.0f,
            videoBitrateBps = 4_000_000,
            audioBitrateBps = 256_000,
            format = ExportFormat.MOV,
        )
        assertEquals(60.0f, config.frameRate)
        assertEquals(4_000_000, config.videoBitrateBps)
        assertEquals(256_000, config.audioBitrateBps)
        assertEquals(ExportFormat.MOV, config.format)
        assertTrue(config.videoBitrateBps > 0)
    }
}
