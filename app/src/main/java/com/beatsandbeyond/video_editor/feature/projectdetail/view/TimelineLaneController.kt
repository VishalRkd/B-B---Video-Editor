package com.beatsandbeyond.video_editor.feature.projectdetail.view

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.constraintlayout.widget.ConstraintLayout
import coil.load
import coil.request.videoFrameMillis
import com.beatsandbeyond.video_editor.R
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import com.beatsandbeyond.video_editor.core.domain.model.VideoFilter
import com.google.android.material.card.MaterialCardView
import kotlin.math.*
import androidx.core.view.isVisible

/**
 * Advanced Timeline Lane Controller.
 * Handles clip rendering, view recycling, and high-performance thumbnail tiling.
 */
class TimelineLaneController(
    private val lane: ViewGroup,
    private val inflater: LayoutInflater,
    private val getPixelsPerMs: () -> Float,
    private val getAsset: (String) -> Asset?,
    private val onClipClick: (String) -> Unit,
    private val onClipLongClick: (String) -> Unit,
    val trackType: TrackType,
) {
    private val viewMap = LinkedHashMap<String, View>()
    private val metadataMap = HashMap<String, ClipViewMetadata>()
    private val clipCache = HashMap<String, Clip>()
    
    private var viewportScrollX = 0
    private var viewportWidth = 0

    /** 
     * Updates the viewport dimensions to enable thumbnail virtualization.
     * Call this from the fragment's scroll listener.
     */
    fun updateViewport(scrollX: Int, width: Int) {
        if (viewportScrollX == scrollX && viewportWidth == width) return
        viewportScrollX = scrollX
        viewportWidth = width
        
        // Refresh thumbnails for all active views based on new viewport
        viewMap.forEach { (id, view) ->
            val clip = clipCache[id] ?: return@forEach
            val asset = getAsset(clip.assetId)
            updateThumbnailStrip(view, clip, asset, getPixelsPerMs())
        }
    }

    /** Rebuilds the lane from the given track. */
    fun render(track: Track?, selectedClipIds: Set<String>) {
        val clips = track?.clips ?: emptyList()
        val clipIds = clips.map { it.id }.toSet()

        // 1. Remove views whose clip no longer exists
        val toRemove = viewMap.keys.filter { it !in clipIds }
        toRemove.forEach { id ->
            viewMap.remove(id)?.let { lane.removeView(it) }
            metadataMap.remove(id)
            clipCache.remove(id)
        }

        // 2. Add or update existing views
        clips.forEach { clip ->
            clipCache[clip.id] = clip
            var view = viewMap[clip.id]
            if (view == null) {
                view = inflater.inflate(R.layout.item_timeline_clip_editable, lane, false)
                lane.addView(view)
                viewMap[clip.id] = view
                applyTrackStyle(view)
            }
            updateView(view, clip, selectedClipIds)
        }
    }

    /** Applies visual identity based on TrackType from the design system. */
    private fun applyTrackStyle(view: View) {
        val card = view.findViewById<MaterialCardView>(R.id.clipCard)
        val context = lane.context
        
        when (trackType) {
            TrackType.VIDEO -> {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_surface_variant))
            }
            TrackType.AUDIO -> {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_track_audio_bg))
                card.strokeColor = ContextCompat.getColor(context, R.color.color_track_audio_waveform)
                card.strokeWidth = 2
            }
            TrackType.TEXT -> {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_track_text_bg))
                card.strokeColor = ContextCompat.getColor(context, R.color.color_track_text_accent)
                card.strokeWidth = 2
            }
            TrackType.OVERLAY -> {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_track_overlay_bg))
                card.strokeColor = ContextCompat.getColor(context, R.color.color_track_overlay_accent)
                card.strokeWidth = 2
            }
        }
    }

    /** Updates a single view's layout + thumbnails with high-performance recycling. */
    private fun updateView(view: View, clip: Clip, selectedClipIds: Set<String>) {
        val pixelsPerMs = getPixelsPerMs()
        val asset = getAsset(clip.assetId)
        val isSelected = clip.id in selectedClipIds

        val newMeta = ClipViewMetadata(
            clipId = clip.id,
            timelinePositionMs = clip.timelinePositionMs,
            trimStartMs = clip.trimStartMs,
            trimEndMs = clip.trimEndMs,
            durationOnTimelineMs = clip.durationOnTimelineMs,
            isSelected = isSelected,
            assetUri = asset?.mediaUri,
        )
        val current = metadataMap[clip.id]

        val needsLayout = current == null ||
                current.timelinePositionMs != newMeta.timelinePositionMs ||
                current.durationOnTimelineMs != newMeta.durationOnTimelineMs ||
                current.isSelected != newMeta.isSelected

        val needsThumbnails = current == null ||
                current.clipId != newMeta.clipId ||
                current.trimStartMs != newMeta.trimStartMs ||
                current.trimEndMs != newMeta.trimEndMs ||
                current.durationOnTimelineMs != newMeta.durationOnTimelineMs ||
                current.assetUri != newMeta.assetUri

        if (needsLayout) {
            val params = ConstraintLayout.LayoutParams(
                (clip.durationOnTimelineMs * pixelsPerMs).toInt().coerceAtLeast(10),
                ViewGroup.LayoutParams.MATCH_PARENT,
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = (clip.timelinePositionMs * pixelsPerMs).toInt()
            }
            view.layoutParams = params
            
            val borderView = view.findViewById<View>(R.id.activeBorder)
            val leftHandle = view.findViewById<View>(R.id.leftTrimHandle)
            val rightHandle = view.findViewById<View>(R.id.rightTrimHandle)
            borderView.visibility = if (isSelected) View.VISIBLE else View.GONE
            leftHandle.visibility = if (isSelected) View.VISIBLE else View.GONE
            rightHandle.visibility = if (isSelected) View.VISIBLE else View.GONE
        }

        if (needsThumbnails) {
            updateThumbnailStrip(view, clip, asset, pixelsPerMs)

            val nameView = view.findViewById<TextView>(R.id.clipName)
            val durationView = view.findViewById<TextView>(R.id.clipDurationText)
            val filterBadge = view.findViewById<TextView>(R.id.filterBadge)

            // Filter badge: visible only when a non-None filter is applied.
            val filter = clip.filter ?: VideoFilter.None
            if (filter != VideoFilter.None) {
                filterBadge.text = filter.displayName
                filterBadge.visibility = View.VISIBLE
            } else {
                filterBadge.visibility = View.GONE
            }

            // For TEXT clips the name/duration are less relevant; the text preview
            // (set in updateThumbnailStrip) carries the content. Keep name hidden.
            if (trackType == TrackType.TEXT) {
                nameView.visibility = View.GONE
            } else if (asset != null) {
                nameView.text = asset.displayName.substringBeforeLast(".")
                nameView.visibility = View.VISIBLE
            } else {
                nameView.text = "Missing Asset"
                nameView.visibility = View.VISIBLE
            }

            val displaySecs = clip.durationOnTimelineMs / 1000f
            durationView.text = if (clip.speedFactor != 1.0f) {
                "%.1fs (%.1fx)".format(displaySecs, clip.speedFactor)
            } else {
                "%.1fs".format(displaySecs)
            }
        }

        view.tag = newMeta
        metadataMap[clip.id] = newMeta

        if (view.getTag(R.id.clip_interaction_bound) != true) {
            view.setOnClickListener { onClipClick(clip.id) }
            view.setOnLongClickListener { onClipLongClick(clip.id); true }
            view.setTag(R.id.clip_interaction_bound, true)
        }
    }

    private var waveforms: Map<String, FloatArray> = emptyMap()

    fun updateWaveforms(waveforms: Map<String, FloatArray>) {
        this.waveforms = waveforms
        // Re-render audio clips that have new waveform data
        if (trackType == TrackType.AUDIO) {
            viewMap.forEach { (clipId, view) ->
                clipCache[clipId]?.let { clip ->
                    drawAudioWaveform(view, clip)
                }
            }
        }
    }

    /** Draws the waveform for audio clips using real or placeholder data. */
    private fun drawAudioWaveform(view: View, clip: Clip) {
        val waveformView = view.findViewById<WaveformView>(R.id.waveformView)
        waveformView.visibility = View.VISIBLE
        
        val data = waveforms[clip.assetId]
        waveformView.setAmplitudes(data)
    }

    /**
     * Renders the clip's visual content: a filmstrip of video frames for VIDEO/OVERLAY
     * clips, or a synthesized waveform for AUDIO clips. TEXT clips show a text preview.
     * Recycles ImageViews and virtualizes off-screen tiles to prevent jank.
     */
    private fun updateThumbnailStrip(view: View, clip: Clip, asset: Asset?, pixelsPerMs: Float) {
        val stripContainer = view.findViewById<LinearLayout>(R.id.thumbnailStripContainer)
        val waveformView = view.findViewById<View>(R.id.waveformView)

        // Audio clips: show the waveform, hide the filmstrip.
        if (trackType == TrackType.AUDIO) {
            stripContainer.removeAllViews()
            stripContainer.isVisible = false
            drawAudioWaveform(view, clip)
            return
        }

        // Text clips: show a text preview badge instead of a filmstrip/waveform.
        if (trackType == TrackType.TEXT) {
            stripContainer.removeAllViews()
            stripContainer.isVisible = false
            waveformView.visibility = View.GONE
            val textPreview = view.findViewById<TextView?>(R.id.textPreview)
            textPreview?.text = clip.textStyle?.text ?: "Text"
            textPreview?.visibility = View.VISIBLE
            return
        }

        // Video / Overlay: filmstrip of frames.
        waveformView.visibility = View.GONE
        stripContainer.isVisible = true
        if (asset == null) {
            stripContainer.removeAllViews()
            return
        }

        val clipWidthPx = (clip.durationOnTimelineMs * pixelsPerMs).toInt()
        // Denser tiles (~32dp) so the strip reads like a filmstrip.
        val tileWidthPx = 32
        val tileCount = max(1, ceil(clipWidthPx.toDouble() / tileWidthPx).toInt())
        val timeStepMs = if (tileCount > 1) clip.durationOnTimelineMs / tileCount else 0L

        // Recycle existing ImageViews, add new ones if needed
        val currentChildCount = stripContainer.childCount
        if (currentChildCount > tileCount) {
            stripContainer.removeViews(tileCount, currentChildCount - tileCount)
        }

        for (i in 0 until tileCount) {
            val imageView = if (i < stripContainer.childCount) {
                stripContainer.getChildAt(i) as ImageView
            } else {
                ImageView(lane.context).apply {
                    layoutParams = LinearLayout.LayoutParams(tileWidthPx, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    stripContainer.addView(this)
                }
            }

            // virtualization check: only load if tile is roughly visible
            val tileStartPx = (clip.timelinePositionMs * pixelsPerMs).toInt() + (i * tileWidthPx)
            val isVisible = viewportWidth == 0 || (tileStartPx + tileWidthPx >= viewportScrollX - tileWidthPx && 
                            tileStartPx <= viewportScrollX + viewportWidth + tileWidthPx)

            if (isVisible) {
                // Calculate frame time considering speed factor and trim
                val frameOffsetMs = (i * timeStepMs * clip.speedFactor).toLong()
                val frameTimeMs = clip.trimStartMs + frameOffsetMs
                
                imageView.load(android.net.Uri.parse(asset.mediaUri)) {
                    videoFrameMillis(frameTimeMs)
                    crossfade(true)
                    placeholder(R.color.color_surface_variant)
                    error(R.color.color_error)
                    // Optimization: don't reload if it's the same URI and time
                    data(asset.mediaUri)
                }
            } else {
                // Clear any pending image load by setting drawable to null
                imageView.setImageDrawable(null)
                imageView.setImageResource(R.color.color_surface_variant)
            }
        }
    }

    fun setClipPosition(clipId: String, timelinePositionMs: Long) {
        val view = viewMap[clipId] ?: return
        val params = view.layoutParams as? ConstraintLayout.LayoutParams ?: return
        params.marginStart = (timelinePositionMs * getPixelsPerMs()).toInt()
        view.layoutParams = params
        metadataMap[clipId] = metadataMap[clipId]?.copy(timelinePositionMs = timelinePositionMs) ?: return
    }

    fun setClipBounds(clipId: String, timelinePositionMs: Long, durationOnTimelineMs: Long) {
        val view = viewMap[clipId] ?: return
        val params = view.layoutParams as? ConstraintLayout.LayoutParams ?: return
        params.width = (durationOnTimelineMs * getPixelsPerMs()).toInt().coerceAtLeast(10)
        params.marginStart = (timelinePositionMs * getPixelsPerMs()).toInt()
        view.layoutParams = params
        metadataMap[clipId] = metadataMap[clipId]?.copy(
            timelinePositionMs = timelinePositionMs,
            durationOnTimelineMs = durationOnTimelineMs,
        ) ?: return
    }

    fun getView(clipId: String): View? = viewMap[clipId]

    fun clear() {
        viewMap.values.forEach { lane.removeView(it) }
        viewMap.clear()
        metadataMap.clear()
    }
}

