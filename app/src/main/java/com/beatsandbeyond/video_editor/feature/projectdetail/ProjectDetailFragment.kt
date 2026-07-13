package com.beatsandbeyond.video_editor.feature.projectdetail

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.load
import coil.request.videoFrameMillis
import com.beatsandbeyond.video_editor.R
import com.beatsandbeyond.video_editor.core.domain.model.Clip
import com.beatsandbeyond.video_editor.core.domain.model.ExportConfig
import com.beatsandbeyond.video_editor.core.domain.model.ExportFormat
import com.beatsandbeyond.video_editor.core.domain.model.ExportProgress
import com.beatsandbeyond.video_editor.core.domain.model.PlaybackState
import com.beatsandbeyond.video_editor.core.domain.model.Resolution
import com.beatsandbeyond.video_editor.core.domain.model.TextStyle
import com.beatsandbeyond.video_editor.core.domain.model.TrackType
import com.beatsandbeyond.video_editor.core.domain.model.VideoFilter
import com.beatsandbeyond.video_editor.core.domain.model.trackTypeOf
import com.beatsandbeyond.video_editor.databinding.FragmentProjectDetailBinding
import com.beatsandbeyond.video_editor.feature.projectdetail.bottomsheet.AssetPickerBottomSheet
import com.beatsandbeyond.video_editor.feature.projectdetail.bottomsheet.FilterPickerBottomSheet
import com.beatsandbeyond.video_editor.feature.projectdetail.model.ProjectDetailUiState
import com.beatsandbeyond.video_editor.feature.projectdetail.view.TimelineLaneController
import com.beatsandbeyond.video_editor.ui.base.BaseFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.appcompat.widget.PopupMenu
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import java.io.File

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
    private var pendingImportPositionMs: Long? = null

    private lateinit var scaleDetector: ScaleGestureDetector
    private var lastScrollTime = 0L
    private var lastScrollX = 0
    private var isScrollingFast = false
    private var cachedSnapTargets: List<Long> = emptyList()
    private val scrollStopRunnable = Runnable {
        isScrollingFast = false
        val scrollX = binding.timelineHorizontalScroll.scrollX
        val viewportWidth = binding.timelineHorizontalScroll.width
        laneControllers.values.forEach { it.updateViewport(scrollX, viewportWidth, isScrollingFast = false) }
    }
    private val scrollHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                com.beatsandbeyond.video_editor.core.utils.Logger.w("ProjectDetailFragment", "Failed to take persistable URI permission for video", e)
            }
            viewModel.importMedia(uri, com.beatsandbeyond.video_editor.core.domain.model.MediaType.VIDEO, pendingImportPositionMs)
        }
        pendingImportPositionMs = null
    }

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                com.beatsandbeyond.video_editor.core.utils.Logger.w("ProjectDetailFragment", "Failed to take persistable URI permission for audio", e)
            }
            viewModel.importMedia(uri, com.beatsandbeyond.video_editor.core.domain.model.MediaType.AUDIO, pendingImportPositionMs)
        }
        pendingImportPositionMs = null
    }

    private val pickOverlayLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                com.beatsandbeyond.video_editor.core.utils.Logger.w("ProjectDetailFragment", "Failed to take persistable URI permission for overlay", e)
            }
            viewModel.importMedia(uri, com.beatsandbeyond.video_editor.core.domain.model.MediaType.IMAGE, pendingImportPositionMs)
        }
        pendingImportPositionMs = null
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentProjectDetailBinding =
        FragmentProjectDetailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scaleDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (isGestureActive) return false
                isScrollingFast = true // Treat scaling as rapid scroll to throttle Coil loads
                zoomTimeline(detector.scaleFactor, detector.focusX)
                return true
            }
        })

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
            Toast.makeText(context, getString(R.string.toast_resolution), Toast.LENGTH_SHORT).show()
        }
        binding.btnExport.setOnClickListener {
            showExportConfigSheet()
        }
        binding.btnMore.setOnClickListener {
            Toast.makeText(context, getString(R.string.toast_more_options), Toast.LENGTH_SHORT).show()
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

                // Trigger viewport update for all controllers on layout change
                laneControllers.values.forEach { it.updateViewport(binding.timelineHorizontalScroll.scrollX, newWidth) }
            }
        }

        binding.timelineHorizontalScroll.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            if (scaleDetector.isInProgress) {
                return@setOnTouchListener true
            }

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
                    
                    scrollHandler.post(scrollStopRunnable)
                }
            }
            false
        }

        binding.timelineHorizontalScroll.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            scrollHandler.removeCallbacks(scrollStopRunnable)
            
            val currentTime = System.currentTimeMillis()
            val dt = currentTime - lastScrollTime
            if (dt > 0) {
                val dx = Math.abs(scrollX - lastScrollX)
                val velocity = dx.toFloat() / dt
                isScrollingFast = velocity > 1.5f
            }
            lastScrollTime = currentTime
            lastScrollX = scrollX

            // Update viewport for virtualization
            val viewportWidth = binding.timelineHorizontalScroll.width
            laneControllers.values.forEach { it.updateViewport(scrollX, viewportWidth, isScrollingFast) }

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
            
            scrollHandler.postDelayed(scrollStopRunnable, 150)
        }
    }

    private fun setupPlayPauseButton() {
        binding.btnPlayPauseCenter.setOnClickListener {
            viewModel.togglePlayPause()
        }
        binding.btnPlayLeft.setOnClickListener {
            viewModel.togglePlayPause()
        }
        binding.btnOverlayPlay.setOnClickListener {
            viewModel.togglePlayPause()
        }
    }

    private fun setupTrackLabels() {
        binding.labelVideo.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_track_video_bg))
        binding.labelVideo.trackIcon.setImageResource(R.drawable.ic_track_video)
        binding.labelVideo.trackTitle.text = getString(R.string.filter_videos)
        binding.labelVideo.btnMute.visibility = View.GONE
        binding.labelVideo.btnLock.visibility = View.GONE
        binding.labelVideo.btnPlus.setOnClickListener {
            pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }

        binding.labelAudio.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_track_audio_bg))
        binding.labelAudio.trackIcon.setImageResource(R.drawable.ic_track_audio)
        binding.labelAudio.trackTitle.text = "Audio"
        binding.labelAudio.btnMute.visibility = View.GONE
        binding.labelAudio.btnLock.visibility = View.GONE
        binding.labelAudio.btnPlus.setOnClickListener {
            pickAudioLauncher.launch("audio/*")
        }

        binding.labelText.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_track_text_bg))
        binding.labelText.trackIcon.setImageResource(R.drawable.ic_track_text)
        binding.labelText.trackTitle.text = "Text"
        binding.labelText.btnMute.visibility = View.GONE
        binding.labelText.btnLock.visibility = View.GONE
        binding.labelText.btnPlus.setOnClickListener {
            viewModel.addDefaultTextAtPlayhead()
        }

        binding.labelOverlay.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_track_overlay_bg))
        binding.labelOverlay.trackIcon.setImageResource(R.drawable.ic_track_overlay)
        binding.labelOverlay.trackTitle.text = "Overlay"
        binding.labelOverlay.btnMute.visibility = View.GONE
        binding.labelOverlay.btnLock.visibility = View.GONE
        binding.labelOverlay.btnPlus.setOnClickListener {
            pickOverlayLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }

        binding.labelEffect.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_track_effect_bg))
        binding.labelEffect.trackIcon.setImageResource(R.drawable.ic_filter)
        binding.labelEffect.trackTitle.text = "Effect"
        binding.labelEffect.btnMute.visibility = View.GONE
        binding.labelEffect.btnLock.visibility = View.GONE
        binding.labelEffect.btnPlus.setOnClickListener {
            viewModel.addDefaultEffectAtPlayhead()
        }

        binding.labelAdjustment.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_track_adjustment_bg))
        binding.labelAdjustment.trackIcon.setImageResource(R.drawable.ic_adjust)
        binding.labelAdjustment.trackTitle.text = "Adjust"
        binding.labelAdjustment.btnMute.visibility = View.GONE
        binding.labelAdjustment.btnLock.visibility = View.GONE
        binding.labelAdjustment.btnPlus.setOnClickListener {
            viewModel.addDefaultAdjustmentAtPlayhead()
        }
    }

    private fun setupContextToolbarActions() {
        binding.contextToolbar.btnSplit.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(requireContext(), view)
            popup.menuInflater.inflate(R.menu.menu_split_options, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_split -> viewModel.splitAtPlayhead()
                    R.id.action_trim_left -> viewModel.trimLeftToPlayhead()
                    R.id.action_trim_right -> viewModel.trimRightToPlayhead()
                }
                true
            }
            popup.show()
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
            showVolumeDialog()
        }
        binding.contextToolbar.btnFilter.setOnClickListener {
            showFilterPickerSheet()
        }
        binding.contextToolbar.btnAdjust.setOnClickListener {
            showFilterPickerSheet(initialTab = FilterPickerBottomSheet.Tab.ADJUST)
        }
    }

    private fun setupFrameControls() {
        // Setup top menu item actions
        binding.btnMore.setOnClickListener {
            Toast.makeText(context, getString(R.string.toast_more_options), Toast.LENGTH_SHORT).show()
        }
        binding.btnOverlayGrid.setOnClickListener {
            Toast.makeText(context, getString(R.string.toast_grid_options), Toast.LENGTH_SHORT).show()
        }
        binding.btnOverlayCrop.setOnClickListener {
            Toast.makeText(context, getString(R.string.toast_crop_options), Toast.LENGTH_SHORT).show()
        }
        binding.btnOverlayAspect.setOnClickListener {
            Toast.makeText(context, getString(R.string.toast_aspect_ratio), Toast.LENGTH_SHORT).show()
        }
        binding.btnOverlayExpand.setOnClickListener {
            Toast.makeText(context, getString(R.string.toast_fullscreen), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSpeedPickerMenu(view: View) {
        val clip = viewModel.getSelectedClip() ?: return
        val sheet = com.beatsandbeyond.video_editor.feature.projectdetail.bottomsheet.SpeedBottomSheet.newInstance(clip.speedFactor)
        sheet.onSpeedSelected = { speed ->
            viewModel.changeSelectedClipSpeed(speed)
        }
        sheet.show(childFragmentManager, com.beatsandbeyond.video_editor.feature.projectdetail.bottomsheet.SpeedBottomSheet::class.java.simpleName)
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

    // ── Phase 4: Volume dialog ───────────────────────────────────────────────

    private fun showVolumeDialog() {
        val clip = viewModel.getSelectedClip() ?: return
        val sheet = com.beatsandbeyond.video_editor.feature.projectdetail.bottomsheet.AudioOptionsBottomSheet.newInstance(clip.volume)
        sheet.onVolumeSelected = { volume ->
            viewModel.setSelectedClipVolume(volume)
        }
        sheet.show(childFragmentManager, com.beatsandbeyond.video_editor.feature.projectdetail.bottomsheet.AudioOptionsBottomSheet::class.java.simpleName)
    }

    // ── Add-media menu (audio / text / overlay) ─────────────────────────────

    private fun showAddMediaMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_add_media, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.add_audio -> {
                    showAssetPicker(AssetPickerBottomSheet.Mode.AUDIO)
                    true
                }
                R.id.add_text -> {
                    showAddTextDialog()
                    true
                }
                R.id.add_overlay -> {
                    showAssetPicker(AssetPickerBottomSheet.Mode.OVERLAY)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showAssetPicker(mode: AssetPickerBottomSheet.Mode) {
        val content = viewModel.uiState.value as? ProjectDetailUiState.Content
        val assetIds = content?.assets?.values?.map { it.id }.orEmpty()
        if (assetIds.isEmpty()) {
            Toast.makeText(context, getString(R.string.toast_no_assets), Toast.LENGTH_SHORT).show()
            return
        }
        val sheet = AssetPickerBottomSheet.newInstance(mode, assetIds)
        sheet.assetResolver = { id -> viewModel.getAsset(id) }
        sheet.onAssetPicked = { assetId ->
            viewModel.getAsset(assetId)?.let { asset ->
                when (mode) {
                    AssetPickerBottomSheet.Mode.AUDIO -> viewModel.addAudioClip(asset)
                    AssetPickerBottomSheet.Mode.OVERLAY -> viewModel.addOverlayClip(asset)
                }
            }
        }
        sheet.show(childFragmentManager, AssetPickerBottomSheet::class.java.simpleName)
    }

    private fun showAddTextDialog() {
        val editText = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.hint_text_enter)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.spacing_md),
                resources.getDimensionPixelSize(R.dimen.spacing_md),
                resources.getDimensionPixelSize(R.dimen.spacing_md),
                resources.getDimensionPixelSize(R.dimen.spacing_md),
            )
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_add_text_title)
            .setView(editText)
            .setPositiveButton(R.string.btn_add) { _, _ ->
                val text = editText.text?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    viewModel.addTextClip(
                        TextStyle(text = text, fontSize = 48f, color = 0xFFFFFFFF.toInt())
                    )
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    // ── Phase 5: Filter picker ──────────────────────────────────────────────

    private fun showFilterPickerSheet(initialTab: FilterPickerBottomSheet.Tab = FilterPickerBottomSheet.Tab.FILTER) {
        val clip = viewModel.getSelectedClip() ?: return
        val sheet = FilterPickerBottomSheet.newInstance(clip.id, clip.filter, initialTab)
        sheet.onFilterSelected = { filter ->
            viewModel.applyFilterToSelectedClip(filter)
        }
        sheet.show(childFragmentManager, FilterPickerBottomSheet::class.java.simpleName)
    }

    private fun showExportConfigSheet() {
        val resolutions = arrayOf(
            Resolution.FHD,
            Resolution.HD,
            Resolution.SQUARE,
            Resolution.PORTRAIT_9_16,
            Resolution.UHD_4K,
        )
        val qualityLabels = arrayOf(
            getString(R.string.export_quality_high),
            getString(R.string.export_quality_medium),
            getString(R.string.export_quality_low),
        )
        val qualityBitrates = intArrayOf(8_000_000, 4_000_000, 2_000_000)
        var selectedResolution = Resolution.FHD
        var selectedQuality = 0

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.export_config_title)
            .setSingleChoiceItems(qualityLabels, selectedQuality) { _, which -> selectedQuality = which }
            .setPositiveButton(R.string.btn_export) { _, _ ->
                val config = ExportConfig(
                    outputPath = createExportOutputPath(),
                    resolution = selectedResolution,
                    frameRate = 30.0f,
                    videoBitrateBps = qualityBitrates[selectedQuality],
                    audioBitrateBps = 192_000,
                    format = ExportFormat.MP4,
                )
                viewModel.startExport(config)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        // Resolution is chosen via a secondary control row.
        val resolutionNames = resolutions.map { it.toString() }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.export_config_title)
            .setSingleChoiceItems(resolutionNames, resolutions.indexOf(selectedResolution)) { d, which ->
                selectedResolution = resolutions[which]
                d.dismiss()
                showExportConfigSheetWithSelection(selectedResolution)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        dialog.dismiss()
    }

    private fun showExportConfigSheetWithSelection(resolution: Resolution) {
        val qualityLabels = arrayOf(
            getString(R.string.export_quality_high),
            getString(R.string.export_quality_medium),
            getString(R.string.export_quality_low),
        )
        val qualityBitrates = intArrayOf(8_000_000, 4_000_000, 2_000_000)
        var selectedQuality = 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.export_config_title)
            .setMessage(getString(R.string.export_resolution_format, resolution.toString()))
            .setSingleChoiceItems(qualityLabels, selectedQuality) { _, which -> selectedQuality = which }
            .setPositiveButton(R.string.btn_export) { _, _ ->
                val config = ExportConfig(
                    outputPath = createExportOutputPath(),
                    resolution = resolution,
                    frameRate = 30.0f,
                    videoBitrateBps = qualityBitrates[selectedQuality],
                    audioBitrateBps = 192_000,
                    format = ExportFormat.MP4,
                )
                viewModel.startExport(config)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun createExportOutputPath(): String {
        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: requireContext().filesDir
        val fileName = "bb_export_${System.currentTimeMillis()}.mp4"
        return File(dir, fileName).absolutePath
    }

    private var exportProgressDialog: AlertDialog? = null

    private fun renderExportProgress(progress: ExportProgress) {
        when (progress) {
            is ExportProgress.Started -> {
                exportProgressDialog?.dismiss()
                exportProgressDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.export_progress_title)
                    .setMessage(R.string.export_progress_preparing)
                    .setNegativeButton(R.string.btn_cancel) { _, _ -> viewModel.cancelExport() }
                    .setCancelable(false)
                    .create()
                exportProgressDialog?.show()
            }
            is ExportProgress.InProgress -> {
                if (exportProgressDialog?.isShowing != true) {
                    exportProgressDialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.export_progress_title)
                        .setNegativeButton(R.string.btn_cancel) { _, _ -> viewModel.cancelExport() }
                        .setCancelable(false)
                        .create()
                    exportProgressDialog?.show()
                }
                exportProgressDialog?.setMessage(
                    getString(R.string.export_progress_percent, (progress.progressFraction * 100).toInt()),
                )
            }
            is ExportProgress.Completed -> {
                exportProgressDialog?.dismiss()
                exportProgressDialog = null
                Snackbar.make(
                    binding.root,
                    getString(R.string.export_completed, progress.outputPath),
                    Snackbar.LENGTH_LONG,
                ).show()
            }
            is ExportProgress.Failed -> {
                exportProgressDialog?.dismiss()
                exportProgressDialog = null
                Snackbar.make(
                    binding.root,
                    getString(R.string.export_failed, progress.cause.message ?: getString(R.string.export_error_unknown)),
                    Snackbar.LENGTH_LONG,
                ).show()
            }
            is ExportProgress.Cancelled -> {
                exportProgressDialog?.dismiss()
                exportProgressDialog = null
                Snackbar.make(binding.root, R.string.export_cancelled, Snackbar.LENGTH_SHORT).show()
            }
        }
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
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.waveforms.collect { waveforms ->
                    laneControllers[TrackType.AUDIO]?.updateWaveforms(waveforms)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exportProgress.collect { progress ->
                    renderExportProgress(progress)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeClipFilter.collect { filter ->
                    applyFilterToPreviewOverlay(filter)
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

        // Loading Overlay visibility during media import
        binding.loadingOverlay.visibility = if (state.isImporting) View.VISIBLE else View.GONE

        // Undo/Redo button enablement
        binding.btnUndo.isEnabled = state.canUndo
        binding.btnUndo.alpha = if (state.canUndo) 1.0f else 0.4f
        binding.btnRedo.isEnabled = state.canRedo
        binding.btnRedo.alpha = if (state.canRedo) 1.0f else 0.4f

        // Play/Pause icon
        val playPauseIcon = if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        binding.btnPlayPauseCenter.setImageResource(playPauseIcon)
        binding.btnPlayLeft.setImageResource(playPauseIcon)
        binding.btnOverlayPlay.setImageResource(playPauseIcon)

        // Buffering Indicator
        binding.bufferingIndicator.visibility =
            if (state.playbackState is PlaybackState.Buffering) View.VISIBLE else View.GONE

        // Update timecode text badge
        binding.txtOverlayTimecode.text = "${formatTime(state.currentPositionMs)} / ${formatTime(state.totalDurationMs)}"
        binding.txtTimecodeCurrent.text = "${formatTime(state.currentPositionMs)} "
        binding.txtTimecodeTotal.text = "/ ${formatTime(state.totalDurationMs)}"

        // Sync scroll + playhead from playback position (only when the user isn't scrubbing).
        // Done on every position tick — playing OR paused — so seeking while paused also
        // scrolls the timeline and updates the (centered) playhead timecode.
        if (!isUserScrolling) {
            val targetScrollX = (state.currentPositionMs * pixelsPerMs).toInt()
            binding.timelineHorizontalScroll.scrollTo(targetScrollX, 0)
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
     *
     * HAND-TRACED DRAG LIFECYCLE:
     * 1. ACTION_DOWN:
     *    - sets isGestureActive = true.
     *    - caches basePositions, groupMinDeltaMs, groupMaxDeltaMs, and cachedSnapTargets.
     * 2. ACTION_MOVE:
     *    - directly translates view marginStart / width (smooth 60fps).
     *    - calls viewModel.moveSelectedClips/trimSelectedClip(isFinal=false), updating ViewModel state in-memory.
     *    - position ticks from ExoPlayer emit new UiState.Content:
     *        - calls renderTracksIfChanged().
     *        - isGestureActive is true -> returns early (stops layout reconstructs / OnTouchListener resets).
     * 3. ACTION_UP / ACTION_CANCEL:
     *    - sets isGestureActive = false.
     *    - calls viewModel.moveSelectedClips/trimSelectedClip(isFinal=true), which commits to Room and emits final state.
     *    - renderTracksIfChanged() runs because isGestureActive is now false, rebuilding to match Room.
     */
    private fun renderTracksIfChanged(state: ProjectDetailUiState.Content) {
        if (isGestureActive) return
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
                        com.beatsandbeyond.video_editor.core.domain.model.TrackType.EFFECT -> binding.effectTrackLane
                        com.beatsandbeyond.video_editor.core.domain.model.TrackType.ADJUSTMENT -> binding.adjustmentTrackLane
                    }
                    TimelineLaneController(
                        lane = lane,
                        inflater = layoutInflater,
                        getPixelsPerMs = { pixelsPerMs },
                        getAsset = { id -> state.assets[id] },
                        onClipClick = { clipId -> viewModel.selectClip(clipId) },
                        onClipLongClick = { clipId -> viewModel.toggleClipSelection(clipId) },
                        trackType = track.type,
                        onPlusClick = { positionMs ->
                            pendingImportPositionMs = positionMs
                            when (track.type) {
                                com.beatsandbeyond.video_editor.core.domain.model.TrackType.VIDEO -> {
                                    pickVideoLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VideoOnly))
                                }
                                com.beatsandbeyond.video_editor.core.domain.model.TrackType.AUDIO -> {
                                    pickAudioLauncher.launch("audio/*")
                                }
                                com.beatsandbeyond.video_editor.core.domain.model.TrackType.OVERLAY -> {
                                    pickOverlayLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                }
                                else -> {}
                            }
                        }
                    ).apply {
                        updateViewport(binding.timelineHorizontalScroll.scrollX, binding.timelineHorizontalScroll.width)
                        onTransitionClick = { clipAId, clipBId, hasTransition, transitionId ->
                            if (hasTransition && transitionId != null) {
                                viewModel.removeTransition(transitionId)
                            } else {
                                val newTransition = com.beatsandbeyond.video_editor.core.domain.model.Transition(
                                    id = java.util.UUID.randomUUID().toString(),
                                    type = com.beatsandbeyond.video_editor.core.domain.model.TransitionType.CROSSFADE,
                                    durationMs = 1000L,
                                    fromClipId = clipAId,
                                    toClipId = clipBId
                                )
                                viewModel.addTransition(newTransition)
                                android.widget.Toast.makeText(
                                    context,
                                    "Transition created. Preview & export rendering coming soon!",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
                controller.render(track, state.selectedClipIds, state.project.timeline.transitions)
            }

            // Rebind gestures for the selected clip only (selection changed → rebind)
            rebindSelectedClipGestures(state)

            // Context Toolbar toggle
            val selectedId = state.selectedClipId
            if (selectedId != null) {
                binding.contextToolbar.root.visibility = View.VISIBLE
                val trackType = state.project.timeline.trackTypeOf(selectedId)
                binding.contextToolbar.btnSpeed.visibility = if (trackType == TrackType.VIDEO) View.VISIBLE else View.GONE
            } else {
                binding.contextToolbar.root.visibility = View.INVISIBLE
            }
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
                    val maxStart = (startTrimEnd - 500L).coerceAtLeast(0L)
                    val newTrimStart = (startTrimStart + deltaMs).coerceIn(0L, maxStart)
                    // New timeline duration = (new window) / speed. Trimming the left edge
                    // INWARD must SHRINK the clip, not grow it.
                    val newDuration = ((startTrimEnd - newTrimStart) / live.speedFactor).toLong()
                    // Direct view manipulation — no StateFlow round-trip, no thumbnail reload.
                    laneControllers[stateTrackType(clip.id)]?.setClipBounds(
                        clip.id, live.timelinePositionMs, newDuration
                    )
                    // Lightweight commit for preview seek only (no full re-render).
                    viewModel.trimSelectedClip(newTrimStart, startTrimEnd, isFinal = false, ripple = isRippleEnabled)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val live = viewModel.getClip(clip.id) ?: return@setOnTouchListener true
                    val dx = event.rawX - startTouchX
                    val deltaMs = ((dx / pixelsPerMs) * live.speedFactor).toLong()
                    val maxStart = (startTrimEnd - 500L).coerceAtLeast(0L)
                    val newTrimStart = (startTrimStart + deltaMs).coerceIn(0L, maxStart)
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
                    val minEnd = startTrimStart + 500L
                    val maxEnd = if (assetDurationMs > 0) assetDurationMs else Long.MAX_VALUE
                    val newTrimEnd = (startTrimEnd + deltaMs).coerceIn(minEnd, maxEnd)
                    // New timeline duration = (new window) / speed.
                    val newDuration = ((newTrimEnd - startTrimStart) / live.speedFactor).toLong()
                    laneControllers[stateTrackType(clip.id)]?.setClipBounds(
                        clip.id, live.timelinePositionMs, newDuration
                    )
                    viewModel.trimSelectedClip(startTrimStart, newTrimEnd, isFinal = false, ripple = isRippleEnabled)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val live = viewModel.getClip(clip.id) ?: return@setOnTouchListener true
                    val dx = event.rawX - startTouchX
                    val deltaMs = ((dx / pixelsPerMs) * live.speedFactor).toLong()
                    val minEnd = startTrimStart + 500L
                    val maxEnd = if (assetDurationMs > 0) assetDurationMs else Long.MAX_VALUE
                    val newTrimEnd = (startTrimEnd + deltaMs).coerceIn(minEnd, maxEnd)
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
        // Base timeline positions of every selected clip, captured ONCE at ACTION_DOWN.
        // We drive the view from these absolute anchors (base + snappedDelta) instead of
        // re-reading getClip() mid-drag — because moveSelectedClips(isFinal=false) mutates
        // currentProject, so a re-read would return the already-moved position and
        // double-apply the delta, making clips jump away from the finger.
        var basePositions: Map<String, Long> = emptyMap()
        var groupMinDeltaMs = Long.MIN_VALUE
        var groupMaxDeltaMs = Long.MAX_VALUE
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
                    val state = viewModel.uiState.value
                    if (state is ProjectDetailUiState.Content) {
                        basePositions = state.selectedClipIds.associateWith { id ->
                            viewModel.getClip(id)?.timelinePositionMs ?: 0L
                        }

                        // Compute groupMinDeltaMs and groupMaxDeltaMs based on stationary clips on their tracks
                        var minD = Long.MIN_VALUE
                        var maxD = Long.MAX_VALUE
                        state.selectedClipIds.forEach { id ->
                            val c = viewModel.getClip(id) ?: return@forEach
                            val base = basePositions[id] ?: 0L
                            val trackType = state.project.timeline.trackTypeOf(id) ?: return@forEach
                            val track = state.project.timeline.tracks.firstOrNull { it.type == trackType } ?: return@forEach
                            val stationary = track.clips.filter { it.id !in state.selectedClipIds }
                            
                            var clipMinD = -base
                            var clipMaxD = Long.MAX_VALUE
                            
                            stationary.forEach { other ->
                                if (other.timelineEndMs <= base) {
                                    clipMinD = kotlin.math.max(clipMinD, other.timelineEndMs - base)
                                }
                                if (other.timelinePositionMs >= base + c.durationOnTimelineMs) {
                                    clipMaxD = kotlin.math.min(clipMaxD, other.timelinePositionMs - c.durationOnTimelineMs - base)
                                }
                            }
                            minD = kotlin.math.max(minD, clipMinD)
                            maxD = kotlin.math.min(maxD, clipMaxD)
                        }
                        groupMinDeltaMs = minD
                        groupMaxDeltaMs = maxD
                    } else {
                        basePositions = emptyMap()
                        groupMinDeltaMs = Long.MIN_VALUE
                        groupMaxDeltaMs = Long.MAX_VALUE
                    }

                    // Build and cache snap targets across all lanes once per gesture
                    if (state is ProjectDetailUiState.Content && isMagnetEnabled) {
                        val targets = mutableListOf<Long>()
                        state.project.timeline.tracks.forEach { track ->
                            track.clips.forEach { clip ->
                                if (clip.id !in state.selectedClipIds) {
                                    targets.add(clip.timelinePositionMs)
                                    targets.add(clip.timelineEndMs)
                                }
                            }
                        }
                        targets.add(state.currentPositionMs) // playhead
                        cachedSnapTargets = targets
                    } else {
                        cachedSnapTargets = emptyList()
                    }
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
                        updateSnapIndicator(snappedTarget, (startPositionVal + rawDeltaMs))
                        val snappedDelta = snappedTarget - startPositionVal
                        val clampedDelta = snappedDelta.coerceIn(groupMinDeltaMs, groupMaxDeltaMs)
                        val appliedDelta = clampedDelta - lastDeltaMs
                        if (appliedDelta != 0L) {
                            // Direct view manipulation for ALL selected clips (group drag).
                            // Drive from captured base positions + absolute snapped target.
                            val state = viewModel.uiState.value
                            if (state is ProjectDetailUiState.Content) {
                                state.selectedClipIds.forEach { id ->
                                    val base = basePositions[id] ?: return@forEach
                                    laneControllers[state.project.timeline.trackTypeOf(id)]?.setClipPosition(
                                        id, base + clampedDelta
                                    )
                                }
                            }
                            viewModel.moveSelectedClips(appliedDelta, isFinal = false)
                            lastDeltaMs = clampedDelta
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.snapIndicator.visibility = View.GONE
                    if (isDragging) {
                        if (lastDeltaMs != 0L) {
                            viewModel.moveSelectedClips(lastDeltaMs, isFinal = true)
                        }
                    } else if (event.action == MotionEvent.ACTION_UP) {
                        viewModel.selectClip(null)
                    }
                    isGestureActive = false
                    basePositions = emptyMap()
                    groupMinDeltaMs = Long.MIN_VALUE
                    groupMaxDeltaMs = Long.MAX_VALUE
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

    private fun zoomTimeline(factor: Float, focalX: Float) {
        val scrollX = binding.timelineHorizontalScroll.scrollX
        val timeMs = (scrollX + focalX - timelinePadding) / pixelsPerMs
        
        val newZoom = (pixelsPerMs * factor).coerceIn(minPixelsPerMs, maxPixelsPerMs)
        if (newZoom == pixelsPerMs) return
        pixelsPerMs = newZoom
        
        binding.timelineRuler.setPixelsPerMs(pixelsPerMs)
        
        val state = viewModel.uiState.value
        if (state is ProjectDetailUiState.Content) {
            val totalTimelineWidth = (state.totalDurationMs * pixelsPerMs).toInt()
            binding.tracksContainer.layoutParams = binding.tracksContainer.layoutParams.apply {
                width = totalTimelineWidth
            }
            binding.timelineRuler.setDurationMs(state.totalDurationMs)
            binding.timelineRuler.setViewportWidth(binding.timelineHorizontalScroll.width)
            
            renderTracks(state)
        }
        
        val newScrollX = (timeMs * pixelsPerMs - focalX + timelinePadding).toInt().coerceAtLeast(0)
        binding.timelineHorizontalScroll.scrollTo(newScrollX, 0)
    }

    /**
     * Snaps a candidate timeline position to nearby clip edges or the playhead
     * when magnetic snapping is enabled. Returns the (possibly) adjusted position.
     */
    private fun snapPosition(candidateMs: Long, excludeClipId: String?): Long {
        if (!isMagnetEnabled) return candidateMs
        if (cachedSnapTargets.isEmpty()) return candidateMs

        var best = candidateMs
        var bestDist = snapThresholdMs
        for (target in cachedSnapTargets) {
            val dist = kotlin.math.abs(target - candidateMs)
            if (dist < bestDist) {
                bestDist = dist
                best = target
            }
        }
        return best
    }

    private fun updateSnapIndicator(snappedMs: Long, rawMs: Long) {
        val isSnapped = kotlin.math.abs(snappedMs - rawMs) < snapThresholdMs && isMagnetEnabled
        if (isSnapped) {
            val scrollX = binding.timelineHorizontalScroll.scrollX
            val snappedX = (snappedMs * pixelsPerMs).toInt()
            val labelWidth = resources.getDimensionPixelSize(R.dimen.timeline_track_label_width)
            val relativeX = labelWidth + (snappedX - scrollX + timelinePadding)
            
            binding.snapIndicator.translationX = relativeX.toFloat()
            binding.snapIndicator.visibility = View.VISIBLE
        } else {
            binding.snapIndicator.visibility = View.GONE
        }
    }

    private fun applyFilterToPreviewOverlay(filter: VideoFilter) {
        val overlay = binding.filterPreviewOverlay
        when (filter) {
            is VideoFilter.None -> {
                overlay.visibility = View.GONE
                overlay.background = null
            }
            is VideoFilter.Vintage -> {
                overlay.visibility = View.VISIBLE
                overlay.setBackgroundColor(android.graphics.Color.argb((filter.intensity * 60).toInt().coerceIn(0, 255), 158, 118, 59))
            }
            is VideoFilter.Vignette -> {
                overlay.visibility = View.VISIBLE
                val colors = intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.argb((filter.radius * 200).toInt().coerceIn(0, 255), 0, 0, 0))
                val gd = android.graphics.drawable.GradientDrawable().apply {
                    setGradientType(android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT)
                    setColors(colors)
                    gradientRadius = Math.max(overlay.width, overlay.height).toFloat() * 0.7f
                }
                overlay.background = gd
            }
            is VideoFilter.Brightness -> {
                overlay.visibility = View.VISIBLE
                val value = filter.value
                if (value < 0) {
                    overlay.setBackgroundColor(android.graphics.Color.argb((Math.abs(value) * 150).toInt().coerceIn(0, 255), 0, 0, 0))
                } else {
                    overlay.setBackgroundColor(android.graphics.Color.argb((value * 120).toInt().coerceIn(0, 255), 255, 255, 255))
                }
            }
            is VideoFilter.Contrast -> {
                overlay.visibility = View.VISIBLE
                val value = filter.value
                if (value > 1f) {
                    overlay.setBackgroundColor(android.graphics.Color.argb(((value - 1f) * 30).toInt().coerceIn(0, 255), 255, 255, 255))
                } else {
                    overlay.setBackgroundColor(android.graphics.Color.argb(((1f - value) * 45).toInt().coerceIn(0, 255), 128, 128, 128))
                }
            }
            is VideoFilter.Saturation -> {
                overlay.visibility = View.VISIBLE
                val value = filter.value
                if (value < 1f) {
                    overlay.setBackgroundColor(android.graphics.Color.argb(((1f - value) * 80).toInt().coerceIn(0, 255), 128, 128, 128))
                } else {
                    overlay.setBackgroundColor(android.graphics.Color.argb(((value - 1f) * 20).toInt().coerceIn(0, 255), 255, 128, 0))
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pause()
    }

    override fun onDestroyView() {
        viewModel.pause()
        viewModel.onSurfaceDestroyed()
        super.onDestroyView()
    }
}
