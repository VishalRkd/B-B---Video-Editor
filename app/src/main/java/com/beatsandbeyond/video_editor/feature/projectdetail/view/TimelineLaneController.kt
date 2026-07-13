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
import com.beatsandbeyond.video_editor.core.domain.model.Transition
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
    private val onPlusClick: (Long) -> Unit,
) {
    private var plusButtonView: View? = null
    private val viewMap = LinkedHashMap<String, View>()
    private val metadataMap = HashMap<String, ClipViewMetadata>()
    private val clipCache = HashMap<String, Clip>()
    private val transitionViews = mutableListOf<View>()
    var onTransitionClick: ((clipAId: String, clipBId: String, hasTransition: Boolean, transitionId: String?) -> Unit)? = null
    
    private var viewportScrollX = 0
    private var viewportWidth = 0

    /** 
     * Updates the viewport dimensions to enable thumbnail virtualization.
     * Call this from the fragment's scroll listener.
     */
    fun updateViewport(scrollX: Int, width: Int, isScrollingFast: Boolean = false) {
        if (viewportScrollX == scrollX && viewportWidth == width) return
        viewportScrollX = scrollX
        viewportWidth = width
        
        // Refresh thumbnails for all active views based on new viewport
        viewMap.forEach { (id, view) ->
            val clip = clipCache[id] ?: return@forEach
            val asset = getAsset(clip.assetId)
            updateThumbnailStrip(view, clip, asset, getPixelsPerMs(), isScrollingFast)
        }
    }

    /** Rebuilds the lane from the given track. */
    fun render(track: Track?, selectedClipIds: Set<String>, transitions: List<Transition> = emptyList()) {
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

        // 3. Render transitions (VIDEO track only)
        transitionViews.forEach { lane.removeView(it) }
        transitionViews.clear()

        if (trackType == TrackType.VIDEO && clips.size > 1) {
            val sortedClips = clips.sortedBy { it.timelinePositionMs }
            val pixelsPerMs = getPixelsPerMs()
            for (i in 0 until sortedClips.size - 1) {
                val clipA = sortedClips[i]
                val clipB = sortedClips[i + 1]
                val gap = clipB.timelinePositionMs - clipA.timelineEndMs
                if (kotlin.math.abs(gap) < 50) { // Adjacent clips (within 50ms tolerance)
                    val seamMs = clipA.timelineEndMs
                    val existingTransition = transitions.firstOrNull { 
                        (it.fromClipId == clipA.id && it.toClipId == clipB.id) ||
                        (it.fromClipId == clipB.id && it.toClipId == clipA.id)
                    }
                    val hasTransition = existingTransition != null

                    val size = (24 * lane.context.resources.displayMetrics.density).toInt()
                    val button = ImageView(lane.context).apply {
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        layoutParams = ConstraintLayout.LayoutParams(size, size).apply {
                            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                            marginStart = (seamMs * pixelsPerMs).toInt() - size / 2
                        }
                        
                        if (hasTransition) {
                            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                            imageTintList = ColorStateList.valueOf(0xFFFFAF3F.toInt()) // Amber-orange primary
                            background = ContextCompat.getDrawable(context, android.R.drawable.presence_online)
                            backgroundTintList = ColorStateList.valueOf(0xFF000000.toInt())
                        } else {
                            setImageResource(android.R.drawable.ic_input_add)
                            imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt()) // White plus
                            background = ContextCompat.getDrawable(context, android.R.drawable.presence_offline)
                            backgroundTintList = ColorStateList.valueOf(0xFF333333.toInt())
                        }

                        setOnClickListener {
                            onTransitionClick?.invoke(clipA.id, clipB.id, hasTransition, existingTransition?.id)
                        }
                    }
                    lane.addView(button)
                    transitionViews.add(button)
                }
            }
        }

        // 4. Render or update the Plus button at the end of the track (VIDEO only)
        if (trackType == TrackType.VIDEO) {
            val lastClip = clips.maxByOrNull { it.timelinePositionMs + it.durationOnTimelineMs }
            val plusPosMs = lastClip?.let { it.timelinePositionMs + it.durationOnTimelineMs } ?: 0L
            val pixelsPerMs = getPixelsPerMs()
            val plusMarginStart = (plusPosMs * pixelsPerMs).toInt() + (8 * lane.context.resources.displayMetrics.density).toInt()
            
            var plusView = plusButtonView
            if (plusView == null) {
                plusView = inflater.inflate(R.layout.item_timeline_plus_button, lane, false)
                lane.addView(plusView)
                plusButtonView = plusView
                plusView.setOnClickListener {
                    val currentClips = track?.clips ?: emptyList()
                    val currentLastClip = currentClips.maxByOrNull { it.timelinePositionMs + it.durationOnTimelineMs }
                    val currentPlusPosMs = currentLastClip?.let { it.timelinePositionMs + it.durationOnTimelineMs } ?: 0L
                    onPlusClick(currentPlusPosMs)
                }
            }
            
            val params = ConstraintLayout.LayoutParams(
                (40 * lane.context.resources.displayMetrics.density).toInt(),
                (40 * lane.context.resources.displayMetrics.density).toInt()
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = plusMarginStart
            }
            plusView.layoutParams = params
            plusView.visibility = View.VISIBLE
        } else {
            plusButtonView?.visibility = View.GONE
        }
    }

    /** Applies visual identity based on TrackType from the design system. */
    private fun applyTrackStyle(view: View) {
        val card = view.findViewById<MaterialCardView>(R.id.clipCard)
        val context = lane.context
        
        when (trackType) {
            TrackType.VIDEO -> {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_track_video_bg))
                card.strokeColor = ContextCompat.getColor(context, R.color.color_track_video_border)
                card.strokeWidth = 4
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
            TrackType.EFFECT -> {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_track_effect_bg))
                card.strokeColor = ContextCompat.getColor(context, R.color.color_track_effect_accent)
                card.strokeWidth = 2
            }
            TrackType.ADJUSTMENT -> {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_track_adjustment_bg))
                card.strokeColor = ContextCompat.getColor(context, R.color.color_track_adjustment_accent)
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
            pixelsPerMs = pixelsPerMs,
        )
        val current = metadataMap[clip.id]

        val needsLayout = current == null ||
                current.timelinePositionMs != newMeta.timelinePositionMs ||
                current.durationOnTimelineMs != newMeta.durationOnTimelineMs ||
                current.isSelected != newMeta.isSelected ||
                current.pixelsPerMs != newMeta.pixelsPerMs

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

            if (!isSelected) {
                view.setOnTouchListener(null)
            }
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

            // For TEXT, EFFECT, ADJUSTMENT, and OVERLAY mockups
            if (trackType == TrackType.TEXT) {
                nameView.visibility = View.GONE
            } else if (trackType == TrackType.EFFECT) {
                nameView.text = "Glow"
                nameView.visibility = View.VISIBLE
            } else if (trackType == TrackType.ADJUSTMENT) {
                nameView.text = "Color Grading"
                nameView.visibility = View.VISIBLE
            } else if (asset != null) {
                nameView.text = asset.displayName.substringBeforeLast(".")
                nameView.visibility = View.VISIBLE
            } else if (trackType == TrackType.AUDIO) {
                nameView.text = "Adventure.mp3"
                nameView.visibility = View.VISIBLE
            } else if (trackType == TrackType.OVERLAY) {
                nameView.text = "Mountains.png"
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
    private fun updateThumbnailStrip(view: View, clip: Clip, asset: Asset?, pixelsPerMs: Float, isScrollingFast: Boolean = false) {
        val stripContainer = view.findViewById<LinearLayout>(R.id.thumbnailStripContainer)
        val waveformView = view.findViewById<View>(R.id.waveformView)

        // Audio clips: show the waveform, hide the filmstrip.
        if (trackType == TrackType.AUDIO) {
            stripContainer.removeAllViews()
            stripContainer.isVisible = false
            drawAudioWaveform(view, clip)
            return
        }

        // Text, Effect, Adjustment clips: show a text preview badge instead of a filmstrip/waveform.
        if (trackType == TrackType.TEXT || trackType == TrackType.EFFECT || trackType == TrackType.ADJUSTMENT) {
            stripContainer.removeAllViews()
            stripContainer.isVisible = false
            waveformView.visibility = View.GONE
            val textPreview = view.findViewById<TextView?>(R.id.textPreview)
            
            if (trackType == TrackType.TEXT) {
                textPreview?.text = clip.textStyle?.text ?: "EXPLORE MORE"
            } else {
                textPreview?.text = ""
            }
            
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

        val density = lane.context.resources.displayMetrics.density
        val tileWidthPx = (48 * density).toInt() // visual filmstrip tiles in dp
        val clipWidthPx = (clip.durationOnTimelineMs * pixelsPerMs).toInt()
        val tileCount = max(1, ceil(clipWidthPx.toDouble() / tileWidthPx).toInt())
        val timeStepMs = if (tileCount > 1) clip.durationOnTimelineMs / tileCount else 0L

        // Recycle existing ImageViews, add new ones if needed
        val currentChildCount = stripContainer.childCount
        if (currentChildCount > tileCount) {
            stripContainer.removeViews(tileCount, currentChildCount - tileCount)
        }

        val timelinePadding = viewportWidth / 2

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

            // virtualization check: only load if tile is roughly visible in scrollview coordinates
            val tileStartPx = (clip.timelinePositionMs * pixelsPerMs).toInt() + (i * tileWidthPx)
            val tileStartInScroll = timelinePadding + tileStartPx
            val isVisible = viewportWidth == 0 || (tileStartInScroll + tileWidthPx >= viewportScrollX - tileWidthPx && 
                            tileStartInScroll <= viewportScrollX + viewportWidth + tileWidthPx)

            if (isVisible) {
                // Calculate frame time considering speed factor and trim
                val frameOffsetMs = (i * timeStepMs * clip.speedFactor).toLong()
                val frameTimeMs = clip.trimStartMs + frameOffsetMs
                
                val cachedBitmap = ThumbnailCache.get(asset.mediaUri, frameTimeMs)
                if (cachedBitmap != null) {
                    imageView.setImageBitmap(cachedBitmap)
                } else if (!isScrollingFast) {
                    imageView.load(android.net.Uri.parse(asset.mediaUri)) {
                        videoFrameMillis(frameTimeMs)
                        bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888)
                        crossfade(true)
                        placeholder(R.color.color_surface_variant)
                        error(R.color.color_error)
                        listener(
                            onSuccess = { _, result ->
                                if (result.drawable is android.graphics.drawable.BitmapDrawable) {
                                    ThumbnailCache.put(
                                        asset.mediaUri,
                                        frameTimeMs,
                                        (result.drawable as android.graphics.drawable.BitmapDrawable).bitmap
                                    )
                                }
                            }
                        )
                    }
                } else {
                    imageView.setImageDrawable(null)
                    imageView.setImageResource(R.color.color_surface_variant)
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

/**
 * Global LRU Cache for decoded video frames.
 * Key format: "assetUri:frameTimeMs"
 */
object ThumbnailCache {
    private val cache = android.util.LruCache<String, android.graphics.Bitmap>(200)

    fun get(assetUri: String, frameTimeMs: Long): android.graphics.Bitmap? {
        return cache.get("$assetUri:$frameTimeMs")
    }

    fun put(assetUri: String, frameTimeMs: Long, bitmap: android.graphics.Bitmap) {
        cache.put("$assetUri:$frameTimeMs", bitmap)
    }
}

