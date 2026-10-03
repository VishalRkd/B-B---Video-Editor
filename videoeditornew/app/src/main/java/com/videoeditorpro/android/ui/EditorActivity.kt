package com.videoeditorpro.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.videoeditorpro.android.domain.state.ActiveTool
import com.videoeditorpro.android.engine.VideoEngineManager
import com.videoeditorpro.android.ui.components.TimelineView
import com.videoeditorpro.android.ui.components.tools.ExportProgressDialog
import com.videoeditorpro.android.ui.components.tools.FilterBottomSheet
import com.videoeditorpro.android.ui.components.tools.SpeedBottomSheet
import com.videoeditorpro.android.ui.components.tools.VolumeBottomSheet
import com.videoeditorpro.android.ui.viewmodel.EditorViewModel
import java.io.File
import java.io.FileOutputStream

class EditorActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()
    private var videoPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        videoPath = intent.getStringExtra("EXTRA_VIDEO_PATH") ?: ""
        if (videoPath.isEmpty() || !File(videoPath).exists()) {
            Toast.makeText(this, "Video file not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.loadVideo(videoPath, this)

        setContent {
            EditorScreen(
                viewModel = viewModel,
                videoPath = videoPath,
                onBack = { finish() }
            )
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pause()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    videoPath: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var thumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    // Audio file picker launcher
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val copiedPath = copyUriToInternalStorage(context, uri)
            if (copiedPath != null) {
                // Add audio track to engine
                VideoEngineManager.addAudioTrack(copiedPath, File(copiedPath).nameWithoutExtension)
                Toast.makeText(context, "Audio track added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load thumbnails when videoPath or project changes
    LaunchedEffect(videoPath, uiState.project.videoClips.size) {
        thumbnails = VideoEngineManager.thumbnailProvider.getFilmstripThumbnails(
            videoPath = videoPath,
            startMs = 0,
            endMs = uiState.totalDurationMs.coerceAtLeast(1000),
            count = 10
        )
    }

    // Status toasts from commands
    LaunchedEffect(uiState.statusToast) {
        uiState.statusToast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.project.name,
                        fontSize = 17.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Undo Button
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) Color.White else Color(0xFF555566)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) Color.White else Color(0xFF555566)
                        )
                    }

                    // Export Button
                    Button(
                        onClick = {
                            val outDir = context.getExternalFilesDir(null) ?: context.filesDir
                            val outFile = File(outDir, "Export_${System.currentTimeMillis()}.mp4")
                            viewModel.exportVideo(outFile.absolutePath, context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B5C)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Export", fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF14141A))
            )
        },
        containerColor = Color(0xFF121216)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Video Preview Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            VideoEngineManager.attachSurface(this) {
                                // Surface ready
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Play / Pause Overlay Button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000))
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Density-Independent Visual Timeline
            val selectedClip = uiState.selectedClip
            TimelineView(
                thumbnails = thumbnails,
                durationMs = uiState.totalDurationMs,
                currentPositionMs = uiState.currentPlayheadMs,
                trimStartMs = selectedClip?.trimStartMs ?: 0,
                trimEndMs = selectedClip?.trimEndMs ?: uiState.totalDurationMs.coerceAtLeast(1000),
                onSeek = { targetMs -> viewModel.seekTo(targetMs) },
                onTrimChanged = { start, end -> viewModel.trimSelectedClip(start, end) },
                clips = uiState.project.videoClips,
                selectedClipIndex = uiState.selectedClipIndex,
                onSelectClipIndex = { index ->
                    uiState.project.videoClips.getOrNull(index)?.let {
                        viewModel.selectClip(it.id)
                    }
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )

            // Editing Tools Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A24))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ToolButton(icon = Icons.Default.ContentCut, label = "Split") {
                    viewModel.splitAtPlayhead()
                }

                ToolButton(icon = Icons.Default.Speed, label = "Speed") {
                    viewModel.setActiveTool(ActiveTool.SPEED)
                }

                ToolButton(icon = Icons.AutoMirrored.Filled.VolumeUp, label = "Volume") {
                    viewModel.setActiveTool(ActiveTool.TRIM)
                }

                ToolButton(icon = Icons.Default.MusicNote, label = "Music") {
                    audioPicker.launch("audio/*")
                }

                ToolButton(icon = Icons.Default.AutoFixHigh, label = "Filters") {
                    viewModel.setActiveTool(ActiveTool.FILTERS)
                }
            }
        }
    }

    // --- Active Tool Bottom Sheets & Modals ---

    when (uiState.activeTool) {
        ActiveTool.SPEED -> {
            SpeedBottomSheet(
                currentSpeed = uiState.selectedClip?.speed ?: 1.0f,
                onSpeedSelected = { speed -> viewModel.setSpeed(speed) },
                onDismiss = { viewModel.setActiveTool(ActiveTool.NONE) }
            )
        }

        ActiveTool.TRIM -> {
            VolumeBottomSheet(
                currentVolume = uiState.selectedClip?.volume ?: 100,
                isMuted = uiState.selectedClip?.isMuted ?: false,
                onVolumeChanged = { vol, muted -> viewModel.setVolume(vol, muted) },
                onDismiss = { viewModel.setActiveTool(ActiveTool.NONE) }
            )
        }

        ActiveTool.FILTERS -> {
            FilterBottomSheet(
                currentFilter = uiState.selectedClip?.filter ?: com.videoeditorpro.android.domain.model.FilterItem.NONE,
                onFilterChanged = { filter -> viewModel.setFilter(filter) },
                onDismiss = { viewModel.setActiveTool(ActiveTool.NONE) }
            )
        }

        else -> {}
    }

    // Export Progress & Status Dialog
    ExportProgressDialog(
        progressPct = uiState.exportProgress,
        successPath = uiState.exportSuccessPath,
        errorMessage = uiState.errorMessage,
        onDismiss = { viewModel.dismissExportDialog() }
    )
}

@Composable
fun ToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color(0xFFAAAAAA), fontSize = 11.sp)
    }
}

/**
 * Copies a content URI into app internal storage to enable direct C++ JNI file access.
 */
private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val fileName = "audio_${System.currentTimeMillis()}"
        val destFile = File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        null
    }
}
