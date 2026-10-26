package com.bbvideoeditor.app.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbvideoeditor.app.media.ThumbnailProvider
import com.bbvideoeditor.app.media.WaveformRepository
import com.bbvideoeditor.app.ui.*
import com.bbvideoeditor.core.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    editor: EditorViewModel,
    thumbnails: ThumbnailProvider? = null,
    importing: Boolean = false,
    onBack: () -> Unit,
    onImport: (TrackType) -> Unit,
    onExport: (() -> Unit)? = null,
    preview: @Composable (Modifier) -> Unit = { EmptyPreview(it) },
    waveforms: WaveformRepository? = null,
) {
    val state by editor.state.collectAsStateWithLifecycle()
    val selected = state.selectedClipId?.let { state.project.findClip(it) }
    val track = selected?.let { state.project.findTrackForClip(it.id) }
    var showProperties by remember { mutableStateOf(false) }
    var showCanvas by remember { mutableStateOf(false) }
    var showTracks by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); editor.message(null) } }

    Scaffold(containerColor = Ink, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to projects") }
                TextButton(onClick = { showRename = true }, modifier = Modifier.weight(1f)) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(state.project.name, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text("${state.project.canvas.width} × ${state.project.canvas.height}  ·  ${state.project.canvas.frameRate} FPS", color = Muted, fontSize = 10.sp)
                    }
                }
                FilledTonalButton(onClick = { onExport?.invoke() }, enabled = onExport != null && state.project.durationUs > 0,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Accent, contentColor = Ink), contentPadding = PaddingValues(horizontal = 14.dp), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.NorthEast, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Export", fontSize = 12.sp)
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black).padding(10.dp), contentAlignment = Alignment.Center) {
                preview(Modifier.fillMaxSize())
                if (importing) Surface(color = Panel.copy(alpha = .95f), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp)); Text("Preparing media…", fontSize = 13.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(formatTime(state.positionUs, true), color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("/ ${formatTime(state.project.durationUs, true)}", color = Muted, fontSize = 10.sp)
                }
                IconButton(onClick = { editor.seek(state.positionUs - state.project.canvas.frameDurationUs); editor.endScrub() }, enabled = state.positionUs > 0) { Icon(Icons.Default.SkipPrevious, "Previous frame", Modifier.size(20.dp)) }
                FilledIconButton(onClick = { editor.setPlaying(!state.playing) }, enabled = state.project.durationUs > 0,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Accent, contentColor = Ink)) {
                    Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.playing) "Pause" else "Play")
                }
                IconButton(onClick = { editor.seek(state.positionUs + state.project.canvas.frameDurationUs); editor.endScrub() }, enabled = state.positionUs < state.project.durationUs) { Icon(Icons.Default.SkipNext, "Next frame", Modifier.size(20.dp)) }
                Spacer(Modifier.weight(.3f))
                IconButton(onClick = editor::undo, enabled = state.canUndo, modifier = Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo", Modifier.size(19.dp)) }
                IconButton(onClick = editor::redo, enabled = state.canRedo, modifier = Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.Redo, "Redo", Modifier.size(19.dp)) }
            }
            TimelineView(state, editor, thumbnails, Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 315.dp).weight(.85f), waveforms)
            HorizontalDivider(color = PanelRaised)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Tool("Media", Icons.Default.Add, !importing) { onImport(TrackType.VIDEO) }
                Tool("Audio", Icons.Default.MusicNote, !importing) { onImport(TrackType.AUDIO) }
                Tool("Overlay", Icons.Default.Layers, !importing) { onImport(TrackType.OVERLAY) }
                Tool("Split", Icons.Default.ContentCut, selected != null && track?.locked == false && state.positionUs > selected.startUs && state.positionUs < selected.endUs) { selected?.let { editor.execute(SplitClip(it.id, state.positionUs)) } }
                Tool("Delete", Icons.Default.DeleteOutline, selected != null && track?.locked == false) { selected?.let { editor.execute(DeleteClip(it.id)) } }
                Tool("Duplicate", Icons.Default.ContentCopy, selected != null && track?.locked == false) { selected?.let { editor.execute(DuplicateClip(it.id)) } }
                Tool("Adjust", Icons.Default.Tune, selected != null && track?.locked == false) { showProperties = true }
                Tool("Earlier", Icons.Default.ChevronLeft, selected != null && track?.locked == false) {
                    if (track != null && selected != null) editor.execute(ReorderClip(selected.id, (track.clips.sortedBy { it.startUs }.indexOfFirst { it.id == selected.id } - 1).coerceAtLeast(0)))
                }
                Tool("Later", Icons.Default.ChevronRight, selected != null && track?.locked == false) {
                    if (track != null && selected != null) editor.execute(ReorderClip(selected.id, (track.clips.sortedBy { it.startUs }.indexOfFirst { it.id == selected.id } + 1).coerceAtMost(track.clips.lastIndex)))
                }
                Tool("Tracks", Icons.Default.ViewAgenda) { showTracks = true }
                Tool("Canvas", Icons.Default.AspectRatio) { showCanvas = true }
            }
        }
    }
    if (showProperties && selected != null) ClipProperties(selected, state.project, onDismiss = { showProperties = false }, onApply = { speed, volume, transform ->
        editor.execute(UpdateClip(selected.id, speed = speed, volume = volume, transform = transform)); showProperties = false
    }, onMove = { target -> editor.execute(MoveClip(selected.id, target.id, selected.startUs)); showProperties = false })
    if (showCanvas) {
        AlertDialog(onDismissRequest = { showCanvas = false }, title = { Text("Canvas format") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Landscape 16:9" to (1920 to 1080), "Portrait 9:16" to (1080 to 1920), "Square 1:1" to (1080 to 1080), "Portrait 4:5" to (1080 to 1350)).forEach { (label, dimensions) ->
                    TextButton(onClick = { editor.execute(SetCanvas(CanvasSettings(dimensions.first, dimensions.second, state.project.canvas.frameRate))); showCanvas = false }) { Text(label) }
                }
            }
        }, confirmButton = { TextButton(onClick = { showCanvas = false }) { Text("Close") } })
    }
    if (showTracks) {
        AlertDialog(onDismissRequest = { showTracks = false }, title = { Text("Tracks") }, text = {
            Column {
                state.project.timeline.tracks.forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.name, Modifier.weight(1f))
                        IconButton(onClick = { editor.execute(SetTrackMuted(t.id, !t.muted)) }) { Icon(if (t.muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, "Toggle mute") }
                        IconButton(onClick = { editor.execute(SetTrackLocked(t.id, !t.locked)) }) { Icon(if (t.locked) Icons.Default.Lock else Icons.Default.LockOpen, "Toggle lock") }
                    }
                }
                Row {
                    TextButton(onClick = { editor.execute(AddTrack(Track(name = "Overlay ${state.project.timeline.tracks.count { it.type == TrackType.OVERLAY } + 1}", type = TrackType.OVERLAY))) }) { Text("+ Overlay") }
                    TextButton(onClick = { editor.execute(AddTrack(Track(name = "Audio ${state.project.timeline.tracks.count { it.type == TrackType.AUDIO } + 1}", type = TrackType.AUDIO))) }) { Text("+ Audio") }
                }
            }
        }, confirmButton = { TextButton(onClick = { showTracks = false }) { Text("Done") } })
    }
    if (showRename) {
        var name by remember { mutableStateOf(state.project.name) }
        AlertDialog(onDismissRequest = { showRename = false }, title = { Text("Project name") }, text = { OutlinedTextField(name, { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { editor.execute(RenameProject(name.trim())); showRename = false }, enabled = name.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } })
    }
}

@Composable
private fun Tool(label: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(5.dp), modifier = Modifier.width(60.dp).height(58.dp), shape = RoundedCornerShape(10.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, label, Modifier.size(21.dp), tint = if (enabled) Color.White else Muted.copy(alpha = .3f))
            Text(label, fontSize = 10.sp, color = if (enabled) Muted else Muted.copy(alpha = .3f))
        }
    }
}

@Composable
fun EmptyPreview(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Movie, null, Modifier.size(42.dp), tint = Muted.copy(alpha = .45f))
        Spacer(Modifier.height(14.dp))
        Text("Your next story starts here", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text("Add videos or photos to begin editing", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ClipProperties(clip: Clip, project: Project, onDismiss: () -> Unit, onApply: (Float, Float, Transform) -> Unit, onMove: (Track) -> Unit) {
    var speed by remember(clip.id) { mutableFloatStateOf(clip.speed) }
    var volume by remember(clip.id) { mutableFloatStateOf(clip.volume) }
    var scale by remember(clip.id) { mutableFloatStateOf(clip.transform.scaleX) }
    var rotation by remember(clip.id) { mutableFloatStateOf(clip.transform.rotationDegrees) }
    var x by remember(clip.id) { mutableFloatStateOf(clip.transform.translationX) }
    var y by remember(clip.id) { mutableFloatStateOf(clip.transform.translationY) }
    var opacity by remember(clip.id) { mutableFloatStateOf(clip.transform.opacity) }
    val media = project.media[clip.mediaId]
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Clip adjustments") }, text = {
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            PropertySlider("Speed", "%.2f×".format(speed), speed, .25f..4f) { speed = (it * 20).roundToInt() / 20f }
            PropertySlider("Volume", "${(volume * 100).roundToInt()}%", volume, 0f..2f) { volume = it }
            if (media?.kind != MediaKind.AUDIO) {
                PropertySlider("Scale", "%.2f×".format(scale), scale, .1f..3f) { scale = it }
                PropertySlider("Rotation", "${rotation.roundToInt()}°", rotation, -180f..180f) { rotation = it }
                PropertySlider("Horizontal", "%.2f".format(x), x, -1f..1f) { x = it }
                PropertySlider("Vertical", "%.2f".format(y), y, -1f..1f) { y = it }
                PropertySlider("Opacity", "${(opacity * 100).roundToInt()}%", opacity, 0f..1f) { opacity = it }
            }
            Text("Move to track", color = Muted, fontSize = 12.sp)
            project.timeline.tracks.filter { it.id != project.findTrackForClip(clip.id)?.id && !it.locked && media != null && it.type.accepts(media.kind) }.forEach { track ->
                TextButton(onClick = { onMove(track) }) { Text(track.name) }
            }
        }
    }, confirmButton = { TextButton(onClick = { onApply(speed, volume, clip.transform.copy(scaleX = scale, scaleY = scale, rotationDegrees = rotation, translationX = x, translationY = y, opacity = opacity)) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun PropertySlider(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row { Text(label, Modifier.weight(1f), fontSize = 12.sp); Text(valueLabel, color = Accent, fontSize = 12.sp) }
    Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range)
}
