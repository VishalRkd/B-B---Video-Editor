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
import com.beatsandbeyond.video_editor.core.domain.model.trackTypeOf
import com.beatsandbeyond.video_editor.databinding.FragmentProjectDetailBinding
import com.beatsandbeyond.video_editor.feature.projectdetail.model.ProjectDetailUiState
import com.beatsandbeyond.video_editor.feature.projectdetail.view.TimelineLaneController
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

    // Per-lane controllers (O(1) view lookup, recycling) — one per track type
    private val laneControllers = mutableMapOf<com.beatsandbeyond.video_editor.core.domain.model.TrackType, TimelineLaneController>()
    private var isRenderingTracks = false
    private var lastRenderSignature: String? = null
    private var isGestureActive = false

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

        // During playback, position ticks fire ~30x/sec. Re-rendering the lanes every tick is
        // the main source of jank, so we only re-render when the project/selection/zoom actually
        // changed (guarded by renderTracks' internal signature check).
        renderTracksIfChanged(state)
    }

    /**
     * Re-renders the lanes only when something structural changed (project, selection, zoom),
     * not on every playback position tick.
     */
    private fun renderTracksIfChanged(state: ProjectDetailUiState.Content) {
        val signature = "${state.project.updatedAt}:${state.selectedClipId}:${state.selectedClipIds.joinToString()}:$pixelsPerMs"
        if (signature == lastRenderSignature) return
        lastRenderSignature = signature
        renderTracks(state)
    }

    private fun renderTracks(state: ProjectDetailUiState.Content) {
        if (isRenderingTracks) return
        isRenderingTracks = true
        try {
            // Update ruler configurations
            binding.timelineRuler.setDurationMs(state.totalDurationMs)
            binding.timelineRuler.setPixelsPerMs(pixelsPerMs)
            binding.timelineRuler.setViewportWidth(binding.timelineHorizontalScroll.width)

            // Empty timeline background sizing
            val totalTimelineWidth = (state.totalDurationMs * pixelsPerMs).toInt()
            binding.tracksContainer.layoutParams = binding.tracksContainer.layoutParams.apply {
                width = totalTimelineWidth
            }

            // Render every track lane through its controller (video/audio/text/overlay)
            state.project.timeline.tracks.forEach { track ->
                val controller = laneControllers.getOrPut(track.type) {
                    val lane = when (track.type) {
                        com.beatsandbeyond.video_editor.core.domain.model.TrackType.VIDEO -> binding.videoTrackLane
                        com.beatsandbeyond.video_editor.core.domain.model.TrackType.AUDIO -> binding.audioTrackLane
                        com.beatsandbeyond.video_editor.core.domain.model.TrackType.TEXT -> binding.textTrackLane
                        com.beatsandbeyond.video_editor.core.domain.model.TrackType.OVERLAY -> binding.overlayTrackLane
                    }
                    TimelineLaneController(
                        lane = lane,
                        inflater = layoutInflater,
                        getPixelsPerMs = { pixelsPerMs },
                        getAsset = { id -> state.assets[id] },
                        onClipClick = { clipId -> viewModel.selectClip(clipId) },
                        onClipLongClick = { clipId -> viewModel.toggleClipSelection(clipId) },
                        trackType = track.type,
                    )
                }
                controller.render(track, state.selectedClipIds)
            }

            // Rebind gestures for the selected clip only (selection changed → rebind)
            rebindSelectedClipGestures(state)

            // Context Toolbar toggle
            binding.contextToolbar.root.visibility = if (state.selectedClipId != null) View.VISIBLE else View.GONE
        } finally {
            isRenderingTracks = false
        }
    }

    /**
     * Rebinds move/trim gestures to the currently selected clip's view. Called only when
     * selection changes (not on every frame), so in-progress gestures are never interrupted.
     */
    private fun rebindSelectedClipGestures(state: ProjectDetailUiState.Content) {
        // Never rebind while a gesture is active — replacing the OnTouchListener mid-drag
        // resets its captured start values and causes crashes/jank.
        if (isGestureActive) return
        val selectedId = state.selectedClipId ?: return
        val clip = viewModel.getClip(selectedId) ?: return
        val trackType = state.project.timeline.trackTypeOf(selectedId) ?: return
        val controller = laneControllers[trackType] ?: return
        val clipView = controller.getView(selectedId) ?: return
        val leftHandle = clipView.findViewById<View>(R.id.leftTrimHandle)
        val rightHandle = clipView.findViewById<View>(R.id.rightTrimHandle)
        val assetDuration = viewModel.getAssetDurationMs(selectedId)
        setupTrimHandles(leftHandle, rightHandle, clip, assetDuration)
        setupMoveGesture(clipView, clip)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTrimHandles(leftHandle: View, rightHandle: View, clip: Clip, assetDurationMs: Long) {
        // Read live clip data at gesture start so bounds never go stale mid-drag.
        var startTouchX = 0f
        var startTrimStart = 0L
        var startTrimEnd = 0L

        leftHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val live = viewModel.getClip(clip.id)
                    startTouchX = event.rawX
                    startTrimStart = live?.trimStartMs ?: clip.trimStartMs
                    startTrimEnd = live?.trimEndMs ?: clip.trimEndMs
                    isGestureActive = true
                    binding.timelineHorizontalScroll.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val live = viewModel.getClip(clip.id) ?: return@setOnTouchListener true
                    val dx = event.rawX - startTouchX
                    val deltaMs = ((dx / pixelsPerMs) * live.speedFactor).toLong()
                    val newTrimStart = (startTrimStart + deltaMs).coerceIn(0L, startTrimEnd - 500L)
                    // Direct view manipulation — no StateFlow round-trip, no thumbnail reload.
                    laneControllers[stateTrackType(clip.id)]?.setClipBounds(
                        clip.id, live.timelinePositionMs, newTrimStart - live.trimStartMs + live.durationOnTimelineMs
                    )
                    // Lightweight commit for preview seek only (no full re-render).
                    viewModel.trimSelectedClip(newTrimStart, startTrimEnd, isFinal = false, ripple = isRippleEnabled)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val live = viewModel.getClip(clip.id) ?: return@setOnTouchListener true
                    val dx = event.rawX - startTouchX
                    val deltaMs = ((dx / pixelsPerMs) * live.speedFactor).toLong()
                    val newTrimStart = (startTrimStart + deltaMs).coerceIn(0L, startTrimEnd - 500L)
                    viewModel.trimSelectedClip(newTrimStart, startTrimEnd, isFinal = true, ripple = isRippleEnabled)
                    isGestureActive = false
                    binding.timelineHorizontalScroll.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }

        rightHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val live = viewModel.getClip(clip.id)
                    startTouchX = event.rawX
                    startTrimStart = live?.trimStartMs ?: clip.trimStartMs
                    startTrimEnd = live?.trimEndMs ?: clip.trimEndMs
                    isGestureActive = true
                    binding.timelineHorizontalScroll.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val live = viewModel.getClip(clip.id) ?: return@setOnTouchListener true
                    val dx = event.rawX - startTouchX
                    val deltaMs = ((dx / pixelsPerMs) * live.speedFactor).toLong()
                    val newTrimEnd = (startTrimEnd + deltaMs).coerceIn(startTrimStart + 500L, assetDurationMs)
                    laneControllers[stateTrackType(clip.id)]?.setClipBounds(
                        clip.id, live.timelinePositionMs, newTrimEnd - live.trimStartMs
                    )
                    viewModel.trimSelectedClip(startTrimStart, newTrimEnd, isFinal = false, ripple = isRippleEnabled)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val live = viewModel.getClip(clip.id) ?: return@setOnTouchListener true
                    val dx = event.rawX - startTouchX
                    val deltaMs = ((dx / pixelsPerMs) * live.speedFactor).toLong()
                    val newTrimEnd = (startTrimEnd + deltaMs).coerceIn(startTrimStart + 500L, assetDurationMs)
                    viewModel.trimSelectedClip(startTrimStart, newTrimEnd, isFinal = true, ripple = isRippleEnabled)
                    isGestureActive = false
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
                    val live = viewModel.getClip(clip.id)
                    startTouchX = event.rawX
                    startPositionVal = live?.timelinePositionMs ?: clip.timelinePositionMs
                    lastDeltaMs = 0L
                    isDragging = false
                    isGestureActive = true
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
                        val snappedTarget = snapPosition(
                            (startPositionVal + rawDeltaMs).coerceAtLeast(0L),
                            clip.id
                        )
                        val snappedDelta = snappedTarget - startPositionVal
                        val appliedDelta = snappedDelta - lastDeltaMs
                        if (appliedDelta != 0L) {
                            // Direct view manipulation for ALL selected clips (group drag).
                            val state = viewModel.uiState.value
                            if (state is ProjectDetailUiState.Content) {
                                state.selectedClipIds.forEach { id ->
                                    val c = viewModel.getClip(id) ?: return@forEach
                                    laneControllers[state.project.timeline.trackTypeOf(id)]?.setClipPosition(
                                        id, c.timelinePositionMs + appliedDelta
                                    )
                                }
                            }
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
                        viewModel.selectClip(null)
                    }
                    isGestureActive = false
                    binding.timelineHorizontalScroll.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }
    }

    /** Resolves the current track type for a clip from the live UI state. */
    private fun stateTrackType(clipId: String): com.beatsandbeyond.video_editor.core.domain.model.TrackType? {
        val state = viewModel.uiState.value
        return if (state is ProjectDetailUiState.Content) {
            state.project.timeline.trackTypeOf(clipId)
        } else null
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
        lastRenderSignature = null // force re-render at new zoom
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
