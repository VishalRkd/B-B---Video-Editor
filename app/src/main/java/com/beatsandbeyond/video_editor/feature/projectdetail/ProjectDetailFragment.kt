package com.beatsandbeyond.video_editor.feature.projectdetail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import coil.request.videoFrameMillis
import com.beatsandbeyond.video_editor.R
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.PlaybackState
import com.beatsandbeyond.video_editor.databinding.FragmentProjectDetailBinding
import com.beatsandbeyond.video_editor.feature.projectdetail.model.ProjectDetailUiState
import com.beatsandbeyond.video_editor.ui.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.appcompat.widget.PopupMenu
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@AndroidEntryPoint
class ProjectDetailFragment : BaseFragment<FragmentProjectDetailBinding>() {

    private val viewModel: ProjectDetailViewModel by viewModels()
    private val args: ProjectDetailFragmentArgs by navArgs()
    private var pixelsPerMs = 0.15f // 150 pixels per second (zoomable)
    private val minPixelsPerMs = 0.05f
    private val maxPixelsPerMs = 0.6f
    private var isUserScrolling = false
    private var timelinePadding = 0
    private var isMagnetEnabled = false
    private var isRippleEnabled = false
    private val snapThresholdMs = 150L // snap within 150ms

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentProjectDetailBinding =
        FragmentProjectDetailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupSystemBarInsets()
        setupSurface()
        setupTimelineScroll()
        setupPlayPauseButton()
        setupTrackLabels()
        setupContextToolbarActions()
        setupFrameControls()
        observeUiState()
        viewModel.loadProject(args.projectId)
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnUndo.setOnClickListener {
            viewModel.undo()
        }
        binding.btnRedo.setOnClickListener {
            viewModel.redo()
        }
        binding.btnResolution.setOnClickListener {
            Toast.makeText(context, "Resolution settings: 1080P", Toast.LENGTH_SHORT).show()
        }
        binding.btnExport.setOnClickListener {
            Toast.makeText(context, "Exporting project...", Toast.LENGTH_SHORT).show()
        }
        binding.btnMore.setOnClickListener {
            Toast.makeText(context, "More options", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.topHeaderBar) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = statusBars.top
            view.layoutParams = params
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomBar) { view, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                navigationBars.bottom + resources.getDimensionPixelSize(R.dimen.spacing_md)
            )
            insets
        }
    }

    private fun setupSurface() {
        binding.previewSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                viewModel.onSurfaceReady(holder.surface)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                viewModel.onSurfaceDestroyed()
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTimelineScroll() {
        // Setup initial timeline padding start/end equal to half the screen width to center playhead at 0ms
        binding.timelineHorizontalScroll.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            val newWidth = right - left
            val oldWidth = oldRight - oldLeft
            if (newWidth != oldWidth && newWidth > 0) {
                timelinePadding = newWidth / 2
                val contentContainer = binding.timelineRuler.parent as LinearLayout
                contentContainer.setPadding(timelinePadding, 0, timelinePadding, 0)
                binding.timelineRuler.setViewportWidth(newWidth)
            }
        }

        binding.timelineHorizontalScroll.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isUserScrolling = true
                    // Pause playback immediately on timeline touch
                    val state = viewModel.uiState.value
                    if (state is ProjectDetailUiState.Content && state.isPlaying) {
                        viewModel.togglePlayPause()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isUserScrolling = false
                    val scrollX = binding.timelineHorizontalScroll.scrollX
                    val timeMs = (scrollX / pixelsPerMs).toLong()
                    viewModel.seekTo(timeMs)
                }
            }
            false
        }

        binding.timelineHorizontalScroll.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            val timeMs = (scrollX / pixelsPerMs).toLong()
            binding.timelineRuler.setScrollOffsetMs(timeMs)
            binding.playheadView.setPlayheadTime(timeMs)

            // Update overlay timecode
            val state = viewModel.uiState.value
            if (state is ProjectDetailUiState.Content) {
                binding.txtOverlayTimecode.text = "${formatTime(timeMs)} / ${formatTime(state.totalDurationMs)}"
            }

            // Seek ExoPlayer preview in real-time when scrubbing paused timeline
            if (state is ProjectDetailUiState.Content && !state.isPlaying && isUserScrolling) {
                viewModel.seekTo(timeMs)
            }
        }
    }

    private fun setupPlayPauseButton() {
        binding.btnPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
        }
        binding.btnOverlayPlay.setOnClickListener {
            viewModel.togglePlayPause()
        }
    }

    private fun setupTrackLabels() {
        binding.labelVideo.root.setBackgroundColor(android.graphics.Color.parseColor("#1A191C"))
        binding.labelVideo.trackIcon.setImageResource(R.drawable.ic_track_video)
        binding.labelVideo.trackTitle.text = getString(R.string.filter_videos)
        binding.labelVideo.btnMute.visibility = View.GONE
        binding.labelVideo.btnLock.visibility = View.GONE

        binding.labelAudio.root.setBackgroundColor(android.graphics.Color.parseColor("#0D2220"))
        binding.labelAudio.trackIcon.setImageResource(R.drawable.ic_track_audio)
        binding.labelAudio.trackTitle.text = "Audio"
        binding.labelAudio.btnMute.visibility = View.GONE
        binding.labelAudio.btnLock.visibility = View.GONE

        binding.labelText.root.setBackgroundColor(android.graphics.Color.parseColor("#221C14"))
        binding.labelText.trackIcon.setImageResource(R.drawable.ic_track_text)
        binding.labelText.trackTitle.text = "Text"
        binding.labelText.btnMute.visibility = View.GONE
        binding.labelText.btnLock.visibility = View.GONE

        binding.labelOverlay.root.setBackgroundColor(android.graphics.Color.parseColor("#1C1422"))
        binding.labelOverlay.trackIcon.setImageResource(R.drawable.ic_track_overlay)
        binding.labelOverlay.trackTitle.text = "Overlay"
        binding.labelOverlay.btnMute.visibility = View.GONE
        binding.labelOverlay.btnLock.visibility = View.GONE
    }

    private fun setupContextToolbarActions() {
        binding.contextToolbar.btnSplit.setOnClickListener {
            viewModel.splitAtPlayhead()
        }
        binding.contextToolbar.btnSpeed.setOnClickListener { view ->
            showSpeedPickerMenu(view)
        }
        binding.contextToolbar.btnDelete.setOnClickListener {
            showDeleteClipConfirmation()
        }
        binding.contextToolbar.btnTrim.setOnClickListener {
            // Trim handles are directly draggable on the clip card.
        }
        binding.contextToolbar.btnVolume.setOnClickListener {
            Toast.makeText(context, "Volume adjustment", Toast.LENGTH_SHORT).show()
        }
        binding.contextToolbar.btnFilter.setOnClickListener {
            Toast.makeText(context, "Filters", Toast.LENGTH_SHORT).show()
        }
        binding.contextToolbar.btnAdjust.setOnClickListener {
            Toast.makeText(context, "Color adjustments", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFrameControls() {
        // Seek frame-by-frame (+/- 33ms for 30fps simulation)
        binding.btnFrameNext.setOnClickListener {
            val state = viewModel.uiState.value
            if (state is ProjectDetailUiState.Content) {
                viewModel.seekTo((state.currentPositionMs + 33L).coerceAtMost(state.totalDurationMs))
            }
        }
        binding.btnFramePrev.setOnClickListener {
            val state = viewModel.uiState.value
            if (state is ProjectDetailUiState.Content) {
                viewModel.seekTo((state.currentPositionMs - 33L).coerceAtLeast(0L))
            }
        }
        binding.btnSkipPrev.setOnClickListener {
            viewModel.seekTo(0L)
        }
        binding.btnSkipNext.setOnClickListener {
            val state = viewModel.uiState.value
            if (state is ProjectDetailUiState.Content) {
                viewModel.seekTo(state.totalDurationMs)
            }
        }
        binding.btnMagnet.setOnClickListener {
            isMagnetEnabled = !isMagnetEnabled
            binding.btnMagnet.alpha = if (isMagnetEnabled) 1.0f else 0.4f
            binding.btnMagnet.setColorFilter(
                if (isMagnetEnabled) resources.getColor(R.color.color_primary, null)
                else resources.getColor(R.color.white, null)
            )
            Toast.makeText(
                context,
                if (isMagnetEnabled) "Magnetic Snap ON" else "Magnetic Snap OFF",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.btnKeyframe.setOnClickListener {
            // Zoom in / out toggle (reuse keyframe icon as zoom control)
            zoomTimeline(if (pixelsPerMs < maxPixelsPerMs) 1.5f else 1.0f / 1.5f)
        }
        binding.btnExpand.setOnClickListener {
            // Toggle ripple trim mode
            isRippleEnabled = !isRippleEnabled
            binding.btnExpand.alpha = if (isRippleEnabled) 1.0f else 0.4f
            binding.btnExpand.setColorFilter(
                if (isRippleEnabled) resources.getColor(R.color.color_primary, null)
                else resources.getColor(R.color.white, null)
            )
            Toast.makeText(
                context,
                if (isRippleEnabled) "Ripple Trim ON" else "Ripple Trim OFF",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.btnMidAdd.setOnClickListener {
            Toast.makeText(context, "Add media clip", Toast.LENGTH_SHORT).show()
        }
        binding.btnOverlayGrid.setOnClickListener {
            Toast.makeText(context, "Grid overlay options", Toast.LENGTH_SHORT).show()
        }
        binding.btnOverlayCrop.setOnClickListener {
            Toast.makeText(context, "Crop options", Toast.LENGTH_SHORT).show()
        }
        binding.btnOverlayAspect.setOnClickListener {
            Toast.makeText(context, "Aspect ratio settings", Toast.LENGTH_SHORT).show()
        }
        binding.btnOverlayExpand.setOnClickListener {
            Toast.makeText(context, "Fullscreen player", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSpeedPickerMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_speed_options, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            val speed = when (item.itemId) {
                R.id.speed_0_25 -> 0.25f
                R.id.speed_0_5 -> 0.5f
                R.id.speed_1 -> 1.0f
                R.id.speed_1_5 -> 1.5f
                R.id.speed_2 -> 2.0f
                R.id.speed_4 -> 4.0f
                else -> 1.0f
            }
            viewModel.changeSelectedClipSpeed(speed)
            true
        }
        popup.show()
    }

    private fun showDeleteClipConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_clip_title)
            .setMessage(R.string.dialog_delete_clip_message)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewModel.deleteSelectedClip()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    // ── State observation ─────────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: ProjectDetailUiState) {
        when (state) {
            is ProjectDetailUiState.Loading -> showLoading()
            is ProjectDetailUiState.Content -> showContent(state)
            is ProjectDetailUiState.Error -> showError(state.message)
        }
    }

    private fun showLoading() {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.previewSurface.visibility = View.INVISIBLE
        binding.bottomBar.visibility = View.GONE
        binding.errorMessage.visibility = View.GONE
    }

    private fun showContent(state: ProjectDetailUiState.Content) {
        binding.loadingIndicator.visibility = View.GONE
        binding.previewSurface.visibility = View.VISIBLE
        binding.bottomBar.visibility = View.VISIBLE
        binding.errorMessage.visibility = View.GONE

        // Undo/Redo button enablement
        binding.btnUndo.isEnabled = state.canUndo
        binding.btnUndo.alpha = if (state.canUndo) 1.0f else 0.4f
        binding.btnRedo.isEnabled = state.canRedo
        binding.btnRedo.alpha = if (state.canRedo) 1.0f else 0.4f

        // Play/Pause icon
        val playPauseIcon = if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.btnPlayPause.setIconResource(playPauseIcon)
        binding.btnOverlayPlay.setImageResource(playPauseIcon)

        // Buffering Indicator
        binding.bufferingIndicator.visibility =
            if (state.playbackState is PlaybackState.Buffering) View.VISIBLE else View.GONE

        // Update timecode text badge
        binding.txtOverlayTimecode.text = "${formatTime(state.currentPositionMs)} / ${formatTime(state.totalDurationMs)}"

        // Sync scroll view position from ExoPlayer progress ticks (only when user is not scrubbing)
        if (!isUserScrolling) {
            val targetScrollX = (state.currentPositionMs * pixelsPerMs).toInt()
            if (state.isPlaying) {
                binding.timelineHorizontalScroll.scrollTo(targetScrollX, 0)
            }
            binding.playheadView.setPlayheadTime(state.currentPositionMs)
        }

        renderTracks(state)
    }

    private fun renderTracks(state: ProjectDetailUiState.Content) {
        // Update ruler configurations
        binding.timelineRuler.setDurationMs(state.totalDurationMs)
        binding.timelineRuler.setPixelsPerMs(pixelsPerMs)
        binding.timelineRuler.setViewportWidth(binding.timelineHorizontalScroll.width)

        // Clear other lanes (audio, text, overlay remain empty in Phase 3)
        binding.audioTrackLane.removeAllViews()
        binding.textTrackLane.removeAllViews()
        binding.overlayTrackLane.removeAllViews()

        // Empty timeline background sizing
        val totalTimelineWidth = (state.totalDurationMs * pixelsPerMs).toInt()
        binding.tracksContainer.layoutParams = binding.tracksContainer.layoutParams.apply {
            width = totalTimelineWidth
        }

        // Render video clips with recycling
        val videoTrack = state.project.timeline.primaryVideoTrack
        val clips = videoTrack?.clips ?: emptyList()
        val clipIds = clips.map { it.id }.toSet()

        // 1. Remove old/deleted clip views
        val viewsToRemove = mutableListOf<View>()
        for (i in 0 until binding.videoTrackLane.childCount) {
            val child = binding.videoTrackLane.getChildAt(i)
            val tagId = (child.tag as? ClipViewMetadata)?.clipId
            if (tagId == null || tagId !in clipIds) {
                viewsToRemove.add(child)
            }
        }
        viewsToRemove.forEach { binding.videoTrackLane.removeView(it) }

        // 2. Add or update clip views
        clips.forEach { clip ->
            var clipView = (0 until binding.videoTrackLane.childCount)
                .map { binding.videoTrackLane.getChildAt(it) }
                .firstOrNull { (it.tag as? ClipViewMetadata)?.clipId == clip.id }

            if (clipView == null) {
                clipView = layoutInflater.inflate(R.layout.item_timeline_clip_editable, binding.videoTrackLane, false)
                binding.videoTrackLane.addView(clipView)
            }

            val asset = state.assets[clip.assetId]
            val isSelected = clip.id == state.selectedClipId
            val newMetadata = ClipViewMetadata(
                clipId = clip.id,
                timelinePositionMs = clip.timelinePositionMs,
                trimStartMs = clip.trimStartMs,
                trimEndMs = clip.trimEndMs,
                durationOnTimelineMs = clip.durationOnTimelineMs,
                isSelected = isSelected,
                assetUri = asset?.mediaUri
            )

            val currentMetadata = clipView.tag as? ClipViewMetadata
            val needsLayoutUpdate = currentMetadata == null ||
                    currentMetadata.clipId != newMetadata.clipId ||
                    currentMetadata.timelinePositionMs != newMetadata.timelinePositionMs ||
                    currentMetadata.durationOnTimelineMs != newMetadata.durationOnTimelineMs ||
                    currentMetadata.isSelected != newMetadata.isSelected

            val needsThumbnailsUpdate = currentMetadata == null ||
                    currentMetadata.clipId != newMetadata.clipId ||
                    currentMetadata.trimStartMs != newMetadata.trimStartMs ||
                    currentMetadata.trimEndMs != newMetadata.trimEndMs ||
                    currentMetadata.durationOnTimelineMs != newMetadata.durationOnTimelineMs ||
                    currentMetadata.assetUri != newMetadata.assetUri

            // Setup layout params
            if (needsLayoutUpdate) {
                val params = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                    (clip.durationOnTimelineMs * pixelsPerMs).toInt(),
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    marginStart = (clip.timelinePositionMs * pixelsPerMs).toInt()
                }
                clipView.layoutParams = params
            }

            val stripContainer = clipView.findViewById<android.widget.LinearLayout>(R.id.thumbnailStripContainer)
            val nameView = clipView.findViewById<TextView>(R.id.clipName)
            val durationView = clipView.findViewById<TextView>(R.id.clipDurationText)
            val borderView = clipView.findViewById<View>(R.id.activeBorder)
            val leftHandle = clipView.findViewById<View>(R.id.leftTrimHandle)
            val rightHandle = clipView.findViewById<View>(R.id.rightTrimHandle)

            // Load thumbnails if needed
            if (needsThumbnailsUpdate) {
                if (asset != null) {
                    val clipWidthPx = (clip.durationOnTimelineMs * pixelsPerMs).toInt()
                    val tileWidthPx = resources.getDimensionPixelSize(R.dimen.timeline_track_height)
                    val tileCount = Math.max(1, Math.ceil(clipWidthPx.toDouble() / tileWidthPx).toInt())
                    val timeStepMs = clip.durationOnTimelineMs / tileCount
                    
                    stripContainer.removeAllViews()
                    for (i in 0 until tileCount) {
                        val imageView = ImageView(context).apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(tileWidthPx, ViewGroup.LayoutParams.MATCH_PARENT)
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

            // Bind click and touch gestures — only rebind on identity/selection change,
            // NOT on every trim/move update (which would reset the gesture mid-drag)
            val needsGestureRebind = currentMetadata == null ||
                    currentMetadata.clipId != newMetadata.clipId ||
                    currentMetadata.isSelected != newMetadata.isSelected

            if (needsGestureRebind) {
                val isInGroup = state.selectedClipIds.contains(clip.id)
                borderView.visibility = if (isInGroup) View.VISIBLE else View.GONE
                leftHandle.visibility = if (isSelected) View.VISIBLE else View.GONE
                rightHandle.visibility = if (isSelected) View.VISIBLE else View.GONE

                if (!isSelected) {
                    clipView.setOnClickListener {
                        // Tap selects; tap again with magnet-like multi-select via long press
                        viewModel.selectClip(clip.id)
                    }
                    clipView.setOnLongClickListener {
                        viewModel.toggleClipSelection(clip.id)
                        true
                    }
                    clipView.setOnTouchListener(null)
                    leftHandle.setOnTouchListener(null)
                    rightHandle.setOnTouchListener(null)
                } else {
                    clipView.setOnClickListener(null)
                    clipView.setOnLongClickListener(null)
                    setupTrimHandles(leftHandle, rightHandle, clip, asset?.durationMs ?: 0L)
                    setupMoveGesture(clipView, clip)
                }
            }

            clipView.tag = newMetadata
        }

        // Context Toolbar toggle
        binding.contextToolbar.root.visibility = if (state.selectedClipId != null) View.VISIBLE else View.GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTrimHandles(leftHandle: View, rightHandle: View, clip: Clip, assetDurationMs: Long) {
        var startTouchX = 0f
        var startTrimVal = 0L

        leftHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchX = event.rawX
                    startTrimVal = clip.trimStartMs
                    binding.timelineHorizontalScroll.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startTouchX
                    val deltaMs = ((dx / pixelsPerMs) * clip.speedFactor).toLong()
                    val newTrimStart = (startTrimVal + deltaMs).coerceIn(0L, clip.trimEndMs - 500L)
                    viewModel.trimSelectedClip(newTrimStart, clip.trimEndMs, isFinal = false, ripple = isRippleEnabled)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = event.rawX - startTouchX
                    val deltaMs = ((dx / pixelsPerMs) * clip.speedFactor).toLong()
                    val newTrimStart = (startTrimVal + deltaMs).coerceIn(0L, clip.trimEndMs - 500L)
                    viewModel.trimSelectedClip(newTrimStart, clip.trimEndMs, isFinal = true, ripple = isRippleEnabled)
                    binding.timelineHorizontalScroll.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }

        rightHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchX = event.rawX
                    startTrimVal = clip.trimEndMs
                    binding.timelineHorizontalScroll.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startTouchX
                    val deltaMs = ((dx / pixelsPerMs) * clip.speedFactor).toLong()
                    val newTrimEnd = (startTrimVal + deltaMs).coerceIn(clip.trimStartMs + 500L, assetDurationMs)
                    viewModel.trimSelectedClip(clip.trimStartMs, newTrimEnd, isFinal = false, ripple = isRippleEnabled)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = event.rawX - startTouchX
                    val deltaMs = ((dx / pixelsPerMs) * clip.speedFactor).toLong()
                    val newTrimEnd = (startTrimVal + deltaMs).coerceIn(clip.trimStartMs + 500L, assetDurationMs)
                    viewModel.trimSelectedClip(clip.trimStartMs, newTrimEnd, isFinal = true, ripple = isRippleEnabled)
                    binding.timelineHorizontalScroll.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMoveGesture(clipView: View, clip: Clip) {
        var startTouchX = 0f
        var startPositionVal = 0L
        var lastDeltaMs = 0L
        val touchSlop = ViewConfiguration.get(clipView.context).scaledTouchSlop
        var isDragging = false

        clipView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchX = event.rawX
                    startPositionVal = clip.timelinePositionMs
                    lastDeltaMs = 0L
                    isDragging = false
                    binding.timelineHorizontalScroll.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startTouchX
                    if (Math.abs(dx) > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val rawDeltaMs = (dx / pixelsPerMs).toLong()
                        // Snap the absolute target position when magnet is on
                        val snappedTarget = snapPosition(
                            (startPositionVal + rawDeltaMs).coerceAtLeast(0L),
                            clip.id
                        )
                        val snappedDelta = snappedTarget - startPositionVal
                        val appliedDelta = snappedDelta - lastDeltaMs
                        if (appliedDelta != 0L) {
                            viewModel.moveSelectedClips(appliedDelta, isFinal = false)
                            lastDeltaMs = snappedDelta
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        if (lastDeltaMs != 0L) {
                            viewModel.moveSelectedClips(lastDeltaMs, isFinal = true)
                        }
                    } else if (event.action == MotionEvent.ACTION_UP) {
                        // Small touch movement with no drag = click gesture to deselect
                        viewModel.selectClip(null)
                    }
                    binding.timelineHorizontalScroll.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }
    }

    private fun showError(message: String) {
        binding.loadingIndicator.visibility = View.GONE
        binding.previewSurface.visibility = View.INVISIBLE
        binding.bottomBar.visibility = View.GONE
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = message
    }

    private fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%02d:%02d".format(mins, secs)
    }

    private fun zoomTimeline(factor: Float) {
        val newZoom = (pixelsPerMs * factor).coerceIn(minPixelsPerMs, maxPixelsPerMs)
        pixelsPerMs = newZoom
        val state = viewModel.uiState.value
        if (state is ProjectDetailUiState.Content) {
            renderTracks(state)
        }
    }

    /**
     * Snaps a candidate timeline position to nearby clip edges or the playhead
     * when magnetic snapping is enabled. Returns the (possibly) adjusted position.
     */
    private fun snapPosition(candidateMs: Long, excludeClipId: String?): Long {
        if (!isMagnetEnabled) return candidateMs
        val state = viewModel.uiState.value
        if (state !is ProjectDetailUiState.Content) return candidateMs

        val snapTargets = mutableListOf<Long>()
        state.project.timeline.primaryVideoTrack?.clips?.forEach { clip ->
            if (clip.id != excludeClipId) {
                snapTargets.add(clip.timelinePositionMs)
                snapTargets.add(clip.timelineEndMs)
            }
        }
        snapTargets.add(state.currentPositionMs)

        var best = candidateMs
        var bestDist = snapThresholdMs
        for (target in snapTargets) {
            val dist = kotlin.math.abs(target - candidateMs)
            if (dist < bestDist) {
                bestDist = dist
                best = target
            }
        }
        return best
    }
}

/**
 * Metadata cache stored on a timeline clip view's tag to enable performance-optimal view recycling
 * and prevent redundant film strip thumbnail loads.
 */
data class ClipViewMetadata(
    val clipId: String,
    val timelinePositionMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val durationOnTimelineMs: Long,
    val isSelected: Boolean,
    val assetUri: String?
)
