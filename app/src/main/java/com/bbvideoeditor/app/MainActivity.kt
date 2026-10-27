package com.bbvideoeditor.app

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bbvideoeditor.app.editor.EditorScreen
import com.bbvideoeditor.app.editor.EditorViewModel
import com.bbvideoeditor.app.editor.EmptyPreview
import com.bbvideoeditor.app.playback.PreviewEngine
import com.bbvideoeditor.app.playback.PreviewQuality
import com.bbvideoeditor.app.projects.ProjectsScreen
import com.bbvideoeditor.app.projects.WorkspaceViewModel
import com.bbvideoeditor.app.export.ExportScreen
import com.bbvideoeditor.app.ui.EditorTheme
import com.bbvideoeditor.app.ui.Ink
import com.bbvideoeditor.app.ui.Panel
import com.bbvideoeditor.core.TrackType

@UnstableApi
class MainActivity : ComponentActivity() {
    private var workspace: WorkspaceViewModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            EditorTheme {
                val model: WorkspaceViewModel = viewModel()
                workspace = model
                Workspace(model)
            }
        }
    }
    override fun onStop() { workspace?.flush(); super.onStop() }
}

@UnstableApi
@Composable
private fun Workspace(workspace: WorkspaceViewModel) {
    val state by workspace.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as EditorApplication
    val exportStatus by app.exports.state.collectAsStateWithLifecycle()
    var importTrack by rememberSaveable { mutableStateOf(TrackType.VIDEO) }
    var exportVisible by rememberSaveable { mutableStateOf(false) }
    val visualPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris -> workspace.importMedia(uris, importTrack) }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> workspace.importMedia(uris, TrackType.AUDIO) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val editor = state.editor
    BackHandler(editor != null) { if (exportVisible) exportVisible = false else workspace.closeEditor() }
    if (editor == null) {
        ProjectsScreen(state, { workspace.create(it) }, { workspace.open(it) }, { workspace.delete(it) })
    } else key(editor) {
        val editorState by editor.state.collectAsStateWithLifecycle()
        EditorSession(editor, app, state.importing, exportVisible || exportStatus.active,
            onBack = { workspace.closeEditor() },
            onImport = { type ->
                importTrack = type
                if (type == TrackType.AUDIO) audioPicker.launch(arrayOf("audio/*"))
                else visualPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            onExport = {
                editor.setPlaying(false); workspace.flush()
                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                exportVisible = true
            })
        if (exportVisible) ExportScreen(editorState.project, onDismiss = { exportVisible = false })
        LaunchedEffect(state.missingMedia) {
            if (state.missingMedia.isNotEmpty()) editor.message("Missing media: ${state.missingMedia.joinToString { it.displayName }}. Restore the files or delete the affected clips.")
        }
    }
    state.message?.let { message ->
        AlertDialog(onDismissRequest = workspace::dismissMessage, title = { Text("Project notice") }, text = { Text(message) }, confirmButton = { TextButton(onClick = workspace::dismissMessage) { Text("OK") } })
    }
}

@UnstableApi
@Composable
private fun EditorSession(editor: EditorViewModel, app: EditorApplication, importing: Boolean, exporting: Boolean,
                          onBack: () -> Unit, onImport: (TrackType) -> Unit, onExport: () -> Unit) {
    val preferences = remember(app) { app.getSharedPreferences("preview", android.content.Context.MODE_PRIVATE) }
    var quality by rememberSaveable {
        mutableStateOf(PreviewQuality.entries.firstOrNull { it.name == preferences.getString("quality", null) } ?: PreviewQuality.BALANCED)
    }
    var qualityMenu by remember { mutableStateOf(false) }
    val engine = remember(editor) {
        PreviewEngine(app, app.compiler, initialPositionUs = editor.state.value.positionUs, initialQuality = quality)
    }
    val state by editor.state.collectAsStateWithLifecycle()
    val playback by engine.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentlyExporting by rememberUpdatedState(exporting)
    LaunchedEffect(state.project) { engine.updateProject(state.project) }
    // Transport intent keeps Pause available while the requested playback is buffering.
    LaunchedEffect(playback.positionUs, playback.playWhenReady) { editor.updatePlayback(playback.positionUs, playback.playWhenReady) }
    LaunchedEffect(quality) { engine.setQuality(quality) }
    LaunchedEffect(exporting) {
        if (exporting || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) engine.suspendPlayback()
        else engine.resume()
    }
    DisposableEffect(engine, lifecycle) {
        editor.onSeek = engine::seek
        editor.onPlayingChanged = { playing ->
            if (playing && currentlyExporting) {
                editor.updatePlayback(editor.state.value.positionUs, false)
                editor.message("Preview is paused while the export finishes.")
            } else if (playing) engine.play() else engine.pause()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!currentlyExporting) engine.resume()
                Lifecycle.Event.ON_STOP -> {
                    engine.suspendPlayback()
                    editor.updatePlayback(engine.state.value.positionUs, false)
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && !exporting) engine.resume()
        onDispose {
            lifecycle.removeObserver(observer)
            editor.onSeek = {}; editor.onPlayingChanged = {}
            engine.close()
            editor.updatePlayback(engine.state.value.positionUs, false)
        }
    }
    EditorScreen(editor, app.thumbnails, importing, onBack, onImport, onExport, preview = { modifier ->
        Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
            if (state.project.durationUs == 0L) EmptyPreview(Modifier.fillMaxSize())
            else {
                AndroidView(factory = { context ->
                    PlayerView(context).apply { useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER); setShutterBackgroundColor(android.graphics.Color.BLACK) }
                }, update = { it.player = playback.player }, onRelease = { it.player = null }, modifier = Modifier.fillMaxSize())
                if (playback.buffering) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                playback.error?.let { error ->
                    Surface(color = Panel.copy(alpha = .96f), modifier = Modifier.padding(16.dp)) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error, fontSize = 12.sp)
                            TextButton(onClick = { engine.updateProject(state.project); engine.resume() }) { Text("Retry preview") }
                        }
                    }
                }
                Box(Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                    Surface(color = Panel.copy(alpha = .9f), shape = RoundedCornerShape(8.dp)) {
                        TextButton(onClick = { qualityMenu = true }) {
                            Text("Preview · ${quality.label}", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    DropdownMenu(expanded = qualityMenu, onDismissRequest = { qualityMenu = false }) {
                        PreviewQuality.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(option.label) }, onClick = {
                                quality = option
                                preferences.edit().putString("quality", option.name).apply()
                                qualityMenu = false
                            })
                        }
                        Text("Preview detail only. Export uses its own settings.", Modifier.padding(12.dp), fontSize = 11.sp)
                    }
                }
            }
        }
    }, waveforms = app.waveforms)
}
