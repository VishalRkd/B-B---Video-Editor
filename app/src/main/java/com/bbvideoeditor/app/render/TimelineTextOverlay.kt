package com.bbvideoeditor.app.render

import android.graphics.Bitmap
import android.util.Pair
import androidx.core.graphics.createBitmap
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import com.bbvideoeditor.core.CanvasSettings
import com.bbvideoeditor.core.Track
import com.bbvideoeditor.core.TrackIndex
import com.bbvideoeditor.core.TextContent

/** One timed overlay per text track; only active/recent glyph bitmaps occupy the bounded cache. */
@UnstableApi
class TimelineTextOverlay(
    track: Track,
    private val canvas: CanvasSettings,
    private val outputHeight: Int,
    private val bitmaps: TextBitmapCache,
) : BitmapOverlay() {
    private val index = TrackIndex(track)
    private val ordered = track.clips.sortedBy { it.startUs }
    private val empty = createBitmap(1, 1)
    private var activeContent: TextContent? = null
    private var activeFrame: RasterizedText? = null

    private fun frameAt(presentationTimeUs: Long): RasterizedText? {
        val content = index.clipAt(presentationTimeUs)?.text
        if (content != activeContent) {
            activeFrame = content?.let { bitmaps.getForRender(it, canvas, outputHeight) }
            val next=ordered.binarySearch { it.startUs.compareTo(presentationTimeUs) }.let { if(it>=0)it+1 else -it-1 }
            ordered.getOrNull(next)?.text?.let{bitmaps.prefetch(it,canvas,outputHeight)}
            activeContent = content
        }
        return activeFrame
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap = frameAt(presentationTimeUs)?.bitmap ?: empty

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val clip = index.clipAt(presentationTimeUs)
        val transform = clip?.transform
        // Retain the active bitmap even if other tracks evict it from the shared LRU cache.
        val frame = frameAt(presentationTimeUs)
        // Raster dimensions are rounded up to whole pixels. Keep the geometry in canonical
        // coordinates so changing output resolution does not shift or resize the title.
        val scaleX = frame?.let { it.logicalWidth * outputHeight / 1080f / it.bitmap.width } ?: 1f
        val scaleY = frame?.let { it.logicalHeight * outputHeight / 1080f / it.bitmap.height } ?: 1f
        return object : OverlaySettings {
            override fun getAlphaScale() = transform?.opacity ?: 0f
            override fun getScale(): Pair<Float, Float> = Pair.create((transform?.scaleX ?: 1f) * scaleX, (transform?.scaleY ?: 1f) * scaleY)
            override fun getRotationDegrees() = transform?.rotationDegrees ?: 0f
            override fun getBackgroundFrameAnchor(): Pair<Float, Float> = Pair.create(transform?.translationX ?: 0f, transform?.translationY ?: 0f)
        }
    }

    override fun release() {
        try { super.release() } finally { activeContent = null; activeFrame = null; empty.recycle() }
    }
}
