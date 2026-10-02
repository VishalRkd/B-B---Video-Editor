package com.videoeditorpro.android.ui

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.videoeditorpro.android.engine.VideoEngineManager
import com.videoeditorpro.android.ui.components.TimelineView
import kotlinx.coroutines.launch
import java.io.File

class EditorActivity : ComponentActivity() {

    private var videoPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        videoPath = intent.getStringExtra("EXTRA_VIDEO_PATH") ?: ""
        if (videoPath.isEmpty() || !File(videoPath).exists()) {
            Toast.makeText(this, "Video file not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        VideoEngineManager.openVideo(videoPath)

        setContent {
            EditorScreen(
                videoPath = videoPath,
                onBack = { finish() },
                onExport = { showExportDialog() }
            )
        }
    }

    private fun showExportDialog() {
        val outDir = getExternalFilesDir(null) ?: filesDir
        val outFile = File(outDir, "Export_${System.currentTimeMillis()}.mp4")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Export Video")
        builder.setMessage("Ready to render with native VivaCut engine?")
        builder.setPositiveButton("Export (1080p)") { _, _ ->
            VideoEngineManager.export(
                outputPath = outFile.absolutePath,
                onProgress = { pct ->
                    // Progress callback
                },
                onComplete = { success, path ->
                    if (success) {
                        Toast.makeText(this, "Exported successfully: $path", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun onPause() {
        super.onPause()
        VideoEngineManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        VideoEngineManager.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    videoPath: String,
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableStateOf(VideoEngineManager.getDuration()) }
    var currentPositionMs by remember { mutableStateOf(0) }
    var trimStartMs by remember { mutableStateOf(0) }
    var trimEndMs by remember { mutableStateOf(durationMs.coerceAtLeast(1000)) }
    var thumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(videoPath) {
        thumbnails = VideoEngineManager.generateFilmstripThumbnails(videoPath, 10)
        durationMs = VideoEngineManager.getDuration()
        trimEndMs = durationMs
    }

    DisposableEffect(Unit) {
        VideoEngineManager.setProgressListener { cur, dur ->
            currentPositionMs = cur
            durationMs = dur
        }
        onDispose {
            VideoEngineManager.setProgressListener { _, _ -> }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor", fontSize = 18.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = onExport,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B5C)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export", fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121214))
            )
        },
        containerColor = Color(0xFF121214)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Video Preview SurfaceView Viewport
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
                                // Surface attached
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
                        .clickable {
                            if (isPlaying) {
                                VideoEngineManager.pause()
                                isPlaying = false
                            } else {
                                VideoEngineManager.play()
                                isPlaying = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Visual Timeline Component
            TimelineView(
                thumbnails = thumbnails,
                durationMs = durationMs,
                currentPositionMs = currentPositionMs,
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                onSeek = { targetMs ->
                    VideoEngineManager.seekTo(targetMs)
                    currentPositionMs = targetMs
                },
                onTrimChanged = { start, end ->
                    trimStartMs = start
                    trimEndMs = end
                    VideoEngineManager.trim(start, end)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Editing Tools Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A22))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ToolButton(icon = Icons.Default.ContentCut, label = "Split") {
                    // Split at playhead
                }
                ToolButton(icon = Icons.Default.Speed, label = "Speed") {
                    // Adjust speed
                }
                ToolButton(icon = Icons.Default.MusicNote, label = "Music") {
                    // Add audio
                }
                ToolButton(icon = Icons.Default.AutoFixHigh, label = "Effects") {
                    // Apply effect
                }
                ToolButton(icon = Icons.Default.TextFields, label = "Text") {
                    // Add text
                }
            }
        }
    }
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
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color(0xFFAAAAAA), fontSize = 11.sp)
    }
}
