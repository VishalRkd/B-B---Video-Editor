package com.beatsandbeyond.video_editor.feature.projectdetail.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import coil.load
import coil.request.videoFrameMillis
import com.beatsandbeyond.video_editor.R
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.Track
import com.beatsandbeyond.video_editor.core.domain.model.TrackType

/**
 * Owns the clip views for a single timeline lane (video / audio / text / overlay).
 *
 * Design goals (smoothness + scalability):
 * - O(1) clipId -> View lookup via an internal [viewMap] (no per-frame child scans).
 * - View recycling: existing views are reused; only layout/thumbnails update when dirty.
 * - Thumbnails are reloaded ONLY when trim/duration/asset actually change — never during a
 *   drag, because drags update [marginStart] directly without going through [render].
 * - [render] is cheap: it diffs against cached [ClipViewMetadata] and skips unchanged views.
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

    /** Rebuilds the lane from the given track. Call when the project/selection/zoom changes. */
    fun render(track: Track?, selectedClipIds: Set<String>) {
        val clips = track?.clips ?: emptyList()
        val clipIds = clips.map { it.id }.toSet()

        // 1. Remove views whose clip no longer exists
        val toRemove = viewMap.keys.filter { it !in clipIds }
        toRemove.forEach { id ->
            viewMap.remove(id)?.let { lane.removeView(it) }
            metadataMap.remove(id)
        }

        // 2. Add or update
        clips.forEach { clip ->
            var view = viewMap[clip.id]
            if (view == null) {
                view = inflater.inflate(R.layout.item_timeline_clip_editable, lane, false)
                lane.addView(view)
                viewMap[clip.id] = view
            }
            updateView(view, clip, selectedClipIds)
        }
    }

    /** Updates a single view's layout + thumbnails, diffing against cached metadata. */
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
                (clip.durationOnTimelineMs * pixelsPerMs).toInt(),
                ViewGroup.LayoutParams.MATCH_PARENT,
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = (clip.timelinePositionMs * pixelsPerMs).toInt()
            }
            view.layoutParams = params
        }

        val borderView = view.findViewById<View>(R.id.activeBorder)
        val leftHandle = view.findViewById<View>(R.id.leftTrimHandle)
        val rightHandle = view.findViewById<View>(R.id.rightTrimHandle)
        borderView.visibility = if (isSelected) View.VISIBLE else View.GONE
        leftHandle.visibility = if (isSelected) View.VISIBLE else View.GONE
        rightHandle.visibility = if (isSelected) View.VISIBLE else View.GONE

        if (needsThumbnails) {
            val stripContainer = view.findViewById<LinearLayout>(R.id.thumbnailStripContainer)
            val nameView = view.findViewById<TextView>(R.id.clipName)
            val durationView = view.findViewById<TextView>(R.id.clipDurationText)
            if (asset != null) {
                val clipWidthPx = (clip.durationOnTimelineMs * pixelsPerMs).toInt()
                val tileWidthPx = lane.resources.getDimensionPixelSize(R.dimen.timeline_track_height)
                val tileCount = kotlin.math.max(1, kotlin.math.ceil(clipWidthPx.toDouble() / tileWidthPx).toInt())
                val timeStepMs = clip.durationOnTimelineMs / tileCount
                stripContainer.removeAllViews()
                for (i in 0 until tileCount) {
                    val imageView = ImageView(lane.context).apply {
                        layoutParams = LinearLayout.LayoutParams(tileWidthPx, ViewGroup.LayoutParams.MATCH_PARENT)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    val frameTimeMs = clip.trimStartMs + (i * timeStepMs)
                    imageView.load(android.net.Uri.parse(asset.mediaUri)) {
                        videoFrameMillis(frameTimeMs)
                        crossfade(true)
                        placeholder(R.color.color_surface_variant)
                    }
                    stripContainer.addView(imageView)
                }
                nameView.text = asset.displayName.substringBeforeLast(".")
            } else {
                nameView.text = "Unknown Clip"
            }
            durationView.text = "%.1fs".format(clip.durationOnTimelineMs / 1000f)
        }

        view.tag = newMeta
        metadataMap[clip.id] = newMeta

        // Bind selection interactions once per view (cheap; survives layout updates).
        if (view.getTag(R.id.clip_interaction_bound) != true) {
            view.setOnClickListener { onClipClick(clip.id) }
            view.setOnLongClickListener { onClipLongClick(clip.id); true }
            view.setTag(R.id.clip_interaction_bound, true)
        }
    }

    /** Directly moves a clip view during a drag (no re-render, no thumbnail reload). */
    fun setClipPosition(clipId: String, timelinePositionMs: Long) {
        val view = viewMap[clipId] ?: return
        val params = view.layoutParams as? ConstraintLayout.LayoutParams ?: return
        params.marginStart = (timelinePositionMs * getPixelsPerMs()).toInt()
        view.layoutParams = params
        val old = metadataMap[clipId]
        if (old != null) {
            metadataMap[clipId] = old.copy(timelinePositionMs = timelinePositionMs)
        }
    }

    /** Directly resizes a clip view during a trim (no re-render, no thumbnail reload). */
    fun setClipBounds(clipId: String, timelinePositionMs: Long, durationOnTimelineMs: Long) {
        val view = viewMap[clipId] ?: return
        val params = view.layoutParams as? ConstraintLayout.LayoutParams ?: return
        params.width = (durationOnTimelineMs * getPixelsPerMs()).toInt()
        params.marginStart = (timelinePositionMs * getPixelsPerMs()).toInt()
        view.layoutParams = params
        val old = metadataMap[clipId]
        if (old != null) {
            metadataMap[clipId] = old.copy(
                timelinePositionMs = timelinePositionMs,
                durationOnTimelineMs = durationOnTimelineMs,
            )
        }
    }

    fun getView(clipId: String): View? = viewMap[clipId]

    fun clear() {
        viewMap.values.forEach { lane.removeView(it) }
        viewMap.clear()
        metadataMap.clear()
    }
}
