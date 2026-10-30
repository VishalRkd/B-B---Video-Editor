package com.bbvideoeditor.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class MediaMetadataCompatibilityTest {
    @Test fun oldSourceJsonLoadsWithUnknownDecoderMetadata() {
        val media=Json.decodeFromString<MediaReference>("""{"uri":"file:/old.mp4","displayName":"old","kind":"VIDEO","durationUs":1000000,"width":1920,"height":1080,"frameRate":30.0}""")
        assertNull(media.codecProfile);assertNull(media.codecLevel);assertNull(media.bitDepth)
        assertNull(media.colorStandard);assertNull(media.colorRange);assertEquals(FrameRateProvenance.UNKNOWN,media.frameRateProvenance)
        assertEquals(30f,media.frameRate)
    }
    @Test fun optionalMetadataRoundTripsWithoutChangingOriginalTimeOrUri() {
        val media=MediaReference(uri="content://source/original",displayName="HEVC",kind=MediaKind.VIDEO,durationUs=12_000_000,
            width=3840,height=2160,rotationDegrees=90,codecMimeType="video/hevc",codecProfile=2,codecLevel=1024,
            bitDepth=null,colorTransfer=6,colorStandard=6,colorRange=2,frameRate=0f)
        val decoded=Json.decodeFromString<MediaReference>(Json.encodeToString(media))
        assertEquals(media,decoded);assertNull(decoded.bitDepth);assertEquals(2160,decoded.displayWidth);assertEquals(3840,decoded.displayHeight)
    }
}
