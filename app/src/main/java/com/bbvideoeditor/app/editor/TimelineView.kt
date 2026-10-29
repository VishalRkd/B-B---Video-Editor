package com.bbvideoeditor.app.editor

import android.graphics.Paint
import android.os.SystemClock
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbvideoeditor.app.media.ThumbnailProvider
import com.bbvideoeditor.app.media.WaveformRepository
import com.bbvideoeditor.app.ui.*
import com.bbvideoeditor.core.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.*

private val TrackHeight = 76.dp
private val LabelWidth = 70.dp
private val RulerHeight = 30.dp

fun formatTime(us: Long, precise: Boolean = false): String {
    val ms = us.coerceAtLeast(0) / 1_000
    val seconds = ms / 1_000
    val base = if (seconds >= 3600) "%d:%02d:%02d".format(java.util.Locale.ROOT, seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%02d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
    return if (precise) "$base.%03d".format(java.util.Locale.ROOT, ms % 1_000) else base
}

private enum class EditKind { MOVE, TRIM_START, TRIM_END }
private data class PointerEdit(
    val kind: EditKind,
    val project: Project,
    val clip: Clip,
    val trackId: String,
    val down: Offset,
    val initialCenterUs: Long,
    val snapIndex: SnapIndex,
)
private class TimelineMotion {
    var fling: Job? = null
    var snapTargetUs by mutableStateOf<Long?>(null)
    var draggingClipId by mutableStateOf<String?>(null)
}

@Composable
fun TimelineView(editor: EditorViewModel, thumbnails: ThumbnailProvider?, modifier: Modifier = Modifier,
    waveforms: WaveformRepository? = null) {
    val document by editor.document.collectAsStateWithLifecycle()
    val transport by editor.transport.collectAsStateWithLifecycle()
    TimelineView(document.withTransport(transport), editor, thumbnails, modifier, waveforms)
}

/**
 * Input is owned by this stable parent, rather than a clip that can disappear or change tracks.
 * The real command is previewed throughout a drag, then committed as one undo step on release.
 */
@Composable
fun TimelineView(
    state: EditorUiState,
    editor: EditorViewModel,
    thumbnails: ThumbnailProvider?,
    modifier: Modifier = Modifier,
    waveforms: WaveformRepository? = null,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val rows = rememberLazyListState()
    val motion = remember { TimelineMotion() }
    var bodyWidthPx by remember { mutableIntStateOf(1) }
    var pixelsPerSecond by remember { mutableDoubleStateOf(with(density) { 72.dp.toPx().toDouble() }) }
    val labelPx = with(density) { LabelWidth.toPx() }
    val rulerPx = with(density) { RulerHeight.toPx() }
    val edgePx = with(density) { 44.dp.toPx() }
    val handlePx = with(density) { 18.dp.toPx() }
    val snapPx = with(density) { 9.dp.toPx().toDouble() }
    val contentWidthPx = (bodyWidthPx - labelPx).toDouble().coerceAtLeast(1.0)
    val currentScale by rememberUpdatedState(pixelsPerSecond)
    val currentContentWidth by rememberUpdatedState(contentWidthPx)
    val viewport = TimelineViewport(state.positionUs, pixelsPerSecond)
    val derived = remember { TimelineDerivedIndexes() }
    val indexes = remember(state.project.timeline.tracks) { derived.update(state.project.timeline.tracks) }
    val currentIndexes by rememberUpdatedState(indexes)

    LaunchedEffect(state.selectedClipId) {
        val index = state.project.timeline.tracks.indexOfFirst { track -> track.clips.any { it.id == state.selectedClipId } }
        if (index >= 0 && rows.layoutInfo.visibleItemsInfo.none { it.index == index }) rows.animateScrollToItem(index)
    }

    DisposableEffect(editor) {
        onDispose {
            motion.fling?.cancel()
            editor.cancelGesture()
            editor.endScrub()
        }
    }

    Column(modifier.background(Panel)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("TIMELINE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("Pinch to zoom · Hold to move", color = Muted, fontSize = 10.sp)
        }
        Box(Modifier.fillMaxWidth().weight(1f).testTag("timeline").clipToBounds().onSizeChanged { bodyWidthPx = it.width }
            .pointerInput(editor, rows, labelPx, rulerPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    motion.fling?.cancel()
                    motion.fling = null
                    editor.endScrub()
                    // The controls in the fixed label column retain their own tap and scroll handling.
                    if (down.position.x < labelPx) return@awaitEachGesture
                    val initialProject = editor.state.value.project
                    val initialViewport = TimelineViewport(editor.state.value.positionUs, currentScale)
                    val downTrack = trackAt(rows, initialProject, down.position.y - rulerPx)
                    val downTime = initialViewport.xToTime((down.position.x - labelPx).toDouble(), currentContentWidth)
                    val hitClip = downTrack?.let { currentIndexes[it.id]?.clipAt(downTime) }
                    val handle = hitClip?.takeIf { it.id == editor.state.value.selectedClipId && downTrack?.locked == false }?.let { clip ->
                        val leftX = initialViewport.timeToX(clip.startUs, currentContentWidth) + labelPx
                        val rightX = initialViewport.timeToX(clip.endUs, currentContentWidth) + labelPx
                        when {
                            leftX >= labelPx && abs(down.position.x - leftX) <= handlePx -> EditKind.TRIM_START
                            rightX <= labelPx + currentContentWidth && abs(down.position.x - rightX) <= handlePx -> EditKind.TRIM_END
                            else -> null
                        }
                    }
                    var edit: PointerEdit? = null
                    var position = down.position
                    var moved = false
                    var horizontal = false
                    var pinching = false
                    var vertical = false
                    var allReleased = false
                    var lastTick = SystemClock.uptimeMillis()
                    val velocity = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
                    val holdDeadline = down.uptimeMillis + viewConfiguration.longPressTimeoutMillis

                    fun startEdit(kind: EditKind) {
                        val clip = hitClip ?: return
                        val track = downTrack ?: return
                        if (track.locked) return
                        editor.select(clip.id)
                        editor.beginGesture()
                        edit = PointerEdit(kind, initialProject, clip, track.id, down.position, initialViewport.centerUs, SnapIndex(initialProject, clip.id))
                        motion.draggingClipId = clip.id
                        if (kind == EditKind.MOVE) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (handle != null) startEdit(handle)

                    fun updateEdit(centerUs: Long = editor.state.value.positionUs) {
                        val active = edit ?: return
                        val deltaUs = ((position.x - active.down.x) / currentScale * MICROS_PER_SECOND + centerUs - active.initialCenterUs).roundToLong()
                        val toleranceUs = (snapPx / currentScale * MICROS_PER_SECOND).roundToLong()
                        val targets = active.snapIndex
                        val result = when (active.kind) {
                            EditKind.MOVE -> {
                                val target = trackAt(rows, editor.state.value.project, position.y - rulerPx)
                                    ?: editor.state.value.project.findTrackForClip(active.clip.id)
                                    ?: return
                                if (target.locked || !target.type.accepts(active.clip, active.project.media)) return
                                val raw = (active.clip.startUs + deltaUs).coerceAtLeast(0)
                                val snap = targets.snapMove(raw, active.clip.durationUs, toleranceUs, centerUs)
                                val otherClips = active.project.findTrack(target.id)?.clips.orEmpty().filterNot { it.id == active.clip.id }
                                val collision = otherClips.any { snap.timeUs < it.endUs && snap.timeUs + active.clip.durationUs > it.startUs }
                                val command: EditorCommand = if (collision && target.id == active.trackId && target.type == TrackType.VIDEO) {
                                    val center = raw + active.clip.durationUs / 2
                                    val insertion = otherClips.count { center > it.startUs + it.durationUs / 2 }
                                    ReorderClip(active.clip.id, insertion)
                                } else MoveClip(active.clip.id, target.id, snap.timeUs)
                                command to snap.targetUs
                            }
                            EditKind.TRIM_START, EditKind.TRIM_END -> trimCommand(active, deltaUs, targets, toleranceUs, centerUs)
                        }
                        editor.previewGesture(result.first)
                        motion.snapTargetUs = result.second
                    }

                    try {
                        while (!allReleased && !vertical) {
                            val timeout = if (edit != null) 16L
                            else if (!moved && hitClip != null && downTrack?.locked == false) (holdDeadline - SystemClock.uptimeMillis()).coerceAtLeast(1)
                            else Long.MAX_VALUE
                            // Once captured, consume before the LazyColumn sees vertical edit movement.
                            val pass = if (edit != null || pinching || horizontal) PointerEventPass.Initial else PointerEventPass.Main
                            val event = if (timeout == Long.MAX_VALUE) awaitPointerEvent(pass)
                            else withTimeoutOrNull(timeout) { awaitPointerEvent(pass) }
                            val now = SystemClock.uptimeMillis()
                            if (event == null) {
                                if (edit == null && !moved && now >= holdDeadline) {
                                    startEdit(EditKind.MOVE)
                                } else if (edit != null) {
                                    val edgeVelocity = when {
                                        position.x < labelPx + edgePx -> -((labelPx + edgePx - position.x) / edgePx).coerceIn(0f, 1f)
                                        position.x > labelPx + currentContentWidth - edgePx -> ((position.x - labelPx - currentContentWidth + edgePx) / edgePx).toFloat().coerceIn(0f, 1f)
                                        else -> 0f
                                    }
                                    if (edgeVelocity != 0f) {
                                        val elapsed = (now - lastTick).coerceIn(1L, 48L)
                                        val scrollUs = (edgeVelocity * edgePx * 9 * elapsed / 1000.0 / currentScale * MICROS_PER_SECOND).roundToLong()
                                        val center = (editor.state.value.positionUs + scrollUs).coerceAtLeast(0)
                                        // Preview first so extending a clip also extends the allowable seek range.
                                        updateEdit(center)
                                        editor.seek(center)
                                    }
                                }
                                lastTick = now
                                continue
                            }
                            lastTick = now
                            val pressed = event.changes.filter { it.pressed }
                            val primary = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
                            position = primary.position
                            allReleased = pressed.isEmpty()
                            velocity.addPosition(primary.uptimeMillis, primary.position)
                            if (event.changes.count { it.pressed || it.previousPressed } > 1 && pressed.size > 1) {
                                if (edit != null) { editor.cancelGesture(); edit = null; motion.draggingClipId = null; motion.snapTargetUs = null }
                                pinching = true
                                moved = true
                                val focal = event.calculateCentroid(useCurrent = false)
                                val old = TimelineViewport(editor.state.value.positionUs, currentScale)
                                val next = old.zoom(event.calculateZoom().toDouble(), (focal.x - labelPx).toDouble(), currentContentWidth)
                                pixelsPerSecond = next.pixelsPerSecond
                                editor.seek(next.pan(event.calculatePan().x.toDouble()).centerUs)
                                event.changes.forEach { it.consume() }
                            } else if (edit != null) {
                                if (!allReleased) updateEdit()
                                event.changes.forEach { it.consume() }
                            } else if (!pinching && !allReleased) {
                                val delta = primary.position - down.position
                                if (!moved && delta.getDistance() >= viewConfiguration.touchSlop) {
                                    moved = true
                                    horizontal = abs(delta.x) > abs(delta.y)
                                    vertical = !horizontal
                                }
                                if (horizontal && !primary.isConsumed) {
                                    val movement = primary.position.x - primary.previousPosition.x
                                    editor.seek(TimelineViewport(editor.state.value.positionUs, currentScale).pan(movement.toDouble()).centerUs)
                                    primary.consume()
                                } else if (primary.isConsumed) vertical = true
                            } else if (pinching && !allReleased) {
                                // Continue one-finger panning after one finger lifts from a pinch.
                                editor.seek(TimelineViewport(editor.state.value.positionUs, currentScale).pan((primary.position.x - primary.previousPosition.x).toDouble()).centerUs)
                                primary.consume()
                            }
                        }
                        if (edit != null) {
                            editor.finishGesture()
                        } else if (!moved && allReleased) {
                            if (hitClip != null) editor.select(hitClip.id)
                            else { editor.select(null); editor.seek(downTime) }
                        } else if (horizontal && !pinching && allReleased) {
                            val speed = velocity.calculateVelocity().x
                            if (abs(speed) > 100f) {
                                motion.fling = scope.launch {
                                    var previous = 0f
                                    try {
                                        AnimationState(initialValue = 0f, initialVelocity = speed)
                                            .animateDecay(exponentialDecay(frictionMultiplier = 1.8f)) {
                                                val delta = value - previous
                                                previous = value
                                                val before = editor.state.value.positionUs
                                                editor.seek(TimelineViewport(before, currentScale).pan(delta.toDouble()).centerUs)
                                                if (editor.state.value.positionUs == before && abs(delta) > .1f) cancelAnimation()
                                            }
                                    } finally { editor.endScrub() }
                                }
                            }
                        }
                    } finally {
                        if (!allReleased && edit != null) editor.cancelGesture()
                        motion.draggingClipId = null
                        motion.snapTargetUs = null
                        if (motion.fling?.isActive != true) editor.endScrub()
                    }
                }
            }) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().height(RulerHeight)) {
                    Box(Modifier.width(LabelWidth).fillMaxHeight().background(Ink), contentAlignment = Alignment.Center) {
                        Text("TRACKS", color = Muted, fontSize = 8.sp, letterSpacing = 1.sp)
                    }
                    TimelineRuler(viewport, Modifier.weight(1f).fillMaxHeight())
                }
                LazyColumn(Modifier.fillMaxSize(), state = rows, contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(state.project.timeline.tracks, key = { it.id }) { track ->
                        Row(Modifier.fillMaxWidth().height(TrackHeight)) {
                            TrackLabel(track, editor)
                            TimelineTrack(track, state.project, state.selectedClipId, viewport, contentWidthPx,
                                indexes.getValue(track.id), thumbnails, waveforms, editor, Modifier.weight(1f), motion.draggingClipId)
                        }
                    }
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                motion.snapTargetUs?.let { target ->
                    val x = labelPx + viewport.timeToX(target, contentWidthPx).toFloat()
                    if (x in labelPx..size.width) drawLine(Accent.copy(alpha = .45f), Offset(x, rulerPx), Offset(x, size.height), 1.dp.toPx())
                }
                val x = labelPx + contentWidthPx.toFloat() / 2
                drawLine(Accent, Offset(x, 0f), Offset(x, size.height), 1.5.dp.toPx())
                drawRoundRect(Accent, Offset(x - 5.dp.toPx(), 0f), Size(10.dp.toPx(), 10.dp.toPx()), CornerRadius(2.dp.toPx()))
            }
        }
    }
}

private fun trackAt(rows: LazyListState, project: Project, y: Float): Track? {
    if (y < 0) return null
    val row = rows.layoutInfo.visibleItemsInfo.firstOrNull { y >= it.offset && y < it.offset + it.size } ?: return null
    return project.timeline.tracks.getOrNull(row.index)
}

private fun trimCommand(active: PointerEdit, deltaUs: Long, targets: SnapIndex, toleranceUs: Long, playheadUs: Long): Pair<EditorCommand, Long?> {
    val clip = active.clip
    val track = active.project.findTrack(active.trackId)!!
    val previousEnd = track.clips.filter { it.id != clip.id && it.endUs <= clip.startUs }.maxOfOrNull { it.endUs } ?: 0L
    val nextStart = track.clips.filter { it.id != clip.id && it.startUs >= clip.endUs }.minOfOrNull { it.startUs }
    val minimum = min(clip.sourceRange.durationUs, max(1L, ceil(MICROS_PER_SECOND.toDouble() / active.project.canvas.frameRate * clip.speed).toLong()))
    return if (active.kind == EditKind.TRIM_START) {
        val desired = targets.snap((clip.startUs + deltaUs).coerceAtLeast(0), toleranceUs, playheadUs)
        if (clip.text != null) {
            // Native text has no source-file boundary: its left edge can extend into empty time.
            val start = desired.timeUs.coerceIn(previousEnd, clip.endUs - minimum)
            val sourceStart = (clip.sourceRange.startUs + start - clip.startUs).coerceAtLeast(0)
            return TrimClip(clip.id, TimeRange(sourceStart, clip.endUs - start), start) to
                desired.targetUs?.takeIf { abs(it - start) <= 12 }
        }
        val earliestSource = max(0L, clip.sourceRange.endUs - ((clip.endUs - previousEnd) * clip.speed.toDouble()).toLong())
        val latestSource = (clip.sourceRange.endUs - minimum).coerceAtLeast(earliestSource)
        val newSourceStart = (clip.sourceRange.startUs + (desired.timeUs - clip.startUs) * clip.speed.toDouble()).roundToLong().coerceIn(earliestSource, latestSource)
        val range = TimeRange(newSourceStart, clip.sourceRange.endUs - newSourceStart)
        val newStart = clip.endUs - (range.durationUs / clip.speed.toDouble()).toLong()
        TrimClip(clip.id, range, newStart.coerceAtLeast(previousEnd)) to desired.targetUs?.takeIf { abs(it - newStart) <= 12 }
    } else {
        val desired = targets.snap((clip.endUs + deltaUs).coerceAtLeast(clip.startUs), toleranceUs, playheadUs)
        val source = active.project.media[clip.mediaId]
        val mediaEnd = if (clip.text != null || source?.kind == MediaKind.IMAGE) Long.MAX_VALUE / 4 else requireNotNull(source).durationUs
        val trackLimit = nextStart?.let { clip.sourceRange.startUs + ((it - clip.startUs) * clip.speed.toDouble())
            .coerceAtMost((Long.MAX_VALUE - clip.sourceRange.startUs).toDouble()).toLong() } ?: mediaEnd
        val maximum = min(mediaEnd, trackLimit).coerceAtLeast(clip.sourceRange.startUs + minimum)
        val newEnd = (clip.sourceRange.startUs + (desired.timeUs - clip.startUs) * clip.speed.toDouble()).roundToLong()
            .coerceIn(clip.sourceRange.startUs + minimum, maximum)
        TrimClip(clip.id, TimeRange(clip.sourceRange.startUs, newEnd - clip.sourceRange.startUs), clip.startUs) to desired.targetUs
    }
}

@Composable
private fun TrackLabel(track: Track, editor: EditorViewModel) {
    Column(Modifier.width(LabelWidth).fillMaxHeight().background(Ink).padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Text(track.name, color = Muted, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row {
            if (track.type != TrackType.TEXT) IconButton(onClick = { editor.execute(SetTrackMuted(track.id, !track.muted)) }, enabled = !track.locked, modifier = Modifier.size(28.dp)) {
                Icon(if (track.muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, if (track.muted) "Unmute ${track.name}" else "Mute ${track.name}", Modifier.size(14.dp), tint = if (track.muted) Accent else Muted)
            }
            IconButton(onClick = { editor.execute(SetTrackLocked(track.id, !track.locked)) }, modifier = Modifier.size(28.dp)) {
                Icon(if (track.locked) Icons.Default.Lock else Icons.Default.LockOpen, if (track.locked) "Unlock ${track.name}" else "Lock ${track.name}", Modifier.size(13.dp), tint = if (track.locked) Accent else Muted)
            }
        }
    }
}

@Composable
private fun TimelineRuler(viewport: TimelineViewport, modifier: Modifier) {
    val density = LocalDensity.current
    val paint = remember(density) { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(151, 158, 172); textSize = with(density) { 9.sp.toPx() } } }
    Canvas(modifier.background(Ink).clipToBounds()) {
        val stepUs = rulerStepUs(viewport.pixelsPerSecond, 64.dp.toPx().toDouble())
        val left = viewport.xToTime(0.0, size.width.toDouble())
        val right = viewport.xToTime(size.width.toDouble(), size.width.toDouble())
        rulerTicks((left - stepUs).coerceAtLeast(0), right, stepUs).take(200).forEach { tick ->
            val x = viewport.timeToX(tick, size.width.toDouble()).toFloat()
            drawLine(Muted, Offset(x, size.height - 6.dp.toPx()), Offset(x, size.height), 1f)
            val label = if (stepUs < MICROS_PER_SECOND) "%.2fs".format(java.util.Locale.ROOT, tick / 1_000_000.0) else formatTime(tick)
            drawContext.canvas.nativeCanvas.drawText(label, x + 4.dp.toPx(), 13.dp.toPx(), paint)
            for (minor in 1..3) {
                val mx = x + (stepUs.toDouble() / MICROS_PER_SECOND * viewport.pixelsPerSecond * minor / 4).toFloat()
                drawLine(Muted.copy(alpha = .35f), Offset(mx, size.height - 3.dp.toPx()), Offset(mx, size.height), 1f)
            }
        }
    }
}

@Composable
private fun TimelineTrack(
    track: Track, project: Project, selectedId: String?, viewport: TimelineViewport, widthPx: Double,
    index: TrackIndex, thumbnails: ThumbnailProvider?, waveforms: WaveformRepository?, editor: EditorViewModel,
    modifier: Modifier, draggingId: String?,
) {
    val density = LocalDensity.current
    val color = when (track.type) { TrackType.AUDIO -> AudioColor; TrackType.OVERLAY, TrackType.TEXT -> OverlayColor; else -> VideoColor }
    val leftTime = viewport.xToTime(-60.0, widthPx)
    val rightTime = viewport.xToTime(widthPx + 60.0, widthPx)
    val clips = remember(index, leftTime, rightTime) { index.visibleClips(leftTime, rightTime) }
    Box(modifier.fillMaxHeight().clipToBounds().background(color.copy(alpha = .06f))) {
        Canvas(Modifier.fillMaxSize()) { drawLine(PanelRaised, Offset(0f, size.height), Offset(size.width, size.height), 1f) }
        if (track.clips.isEmpty()) Text("Add ${when (track.type) { TrackType.AUDIO -> "audio"; TrackType.TEXT -> "text"; else -> "media" }}", Modifier.padding(12.dp).align(Alignment.CenterStart), color = Muted.copy(alpha = .5f), fontSize = 10.sp)
        clips.forEach { clip ->
            key(clip.id) {
                val x = viewport.timeToX(clip.startUs, widthPx)
                val endX = viewport.timeToX(clip.endUs, widthPx)
                val visibleX = x.coerceAtLeast(-20.0)
                val visibleEnd = endX.coerceAtMost(widthPx + 20.0)
                val visibleWidth = (visibleEnd - visibleX).coerceAtLeast(2.0)
                val media = project.media[clip.mediaId]
                val moving=draggingId==clip.id
                val description=remember(clip.text?.text,media?.displayName,clip.id,clip.durationUs,moving) {
                    "Clip ${clip.text?.text ?: media?.displayName ?: clip.id}, ${formatTime(clip.durationUs,true)}${if(moving)", moving" else ""}"
                }
                Box(Modifier.offset { IntOffset(visibleX.roundToInt(), with(density) { 7.dp.roundToPx() }) }
                    .width(with(density) { visibleWidth.toFloat().toDp() }).height(60.dp)
                    .graphicsLayer(alpha = if (track.muted) .55f else 1f)
                    .clip(RoundedCornerShape(7.dp)).background(color)
                    .border(if (selectedId == clip.id) 2.dp else .5.dp,
                        if (selectedId == clip.id) Accent else Color.White.copy(alpha = .12f), RoundedCornerShape(7.dp))
                    .semantics {
                        contentDescription = description
                        onClick("Select clip") { editor.select(clip.id); true }
                    }) {
                    if (media != null && media.kind != MediaKind.AUDIO && track.type != TrackType.AUDIO && thumbnails != null)
                        ThumbnailStrip(media, clip, x, visibleX, visibleWidth, viewport.pixelsPerSecond, thumbnails)
                    if (media != null && (track.type == TrackType.AUDIO || media.kind == MediaKind.AUDIO) && waveforms != null)
                        WaveformStrip(media, clip, x, visibleX, visibleWidth, viewport.pixelsPerSecond, waveforms, Modifier.fillMaxWidth().height(30.dp).align(Alignment.BottomCenter))
                    Text(clip.text?.text ?: media?.displayName ?: "Missing media", Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .48f)).padding(horizontal = 10.dp, vertical = 3.dp), color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (selectedId == clip.id && !track.locked) {
                        if (x >= -1) TrimHandle(true, Modifier.align(Alignment.CenterStart))
                        if (endX <= widthPx + 1) TrimHandle(false, Modifier.align(Alignment.CenterEnd))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrimHandle(left: Boolean, modifier: Modifier) {
    Box(modifier.width(18.dp).fillMaxHeight().background(Accent.copy(alpha = .92f))
        .semantics { contentDescription = if (left) "Trim start" else "Trim end" }, contentAlignment = Alignment.Center) {
        Box(Modifier.width(2.dp).height(18.dp).background(Ink, RoundedCornerShape(1.dp)))
    }
}

@Composable
private fun WaveformStrip(media: MediaReference, clip: Clip, originalX: Double, visibleX: Double, visibleWidth: Double, pixelsPerSecond: Double,
    repository: WaveformRepository, modifier: Modifier) {
    val firstSource=(clip.sourceRange.startUs+(visibleX-originalX)/pixelsPerSecond*MICROS_PER_SECOND*clip.speed)
        .toLong().coerceIn(0,(media.durationUs-1).coerceAtLeast(0))
    val lastSource=(firstSource+visibleWidth/pixelsPerSecond*MICROS_PER_SECOND*clip.speed).toLong()
        .coerceIn(firstSource+1,media.durationUs.coerceAtLeast(firstSource+1))
    val span=(lastSource-firstSource).coerceAtLeast(1)
    val quantum=when { span<500_000 -> 100_000L; span<2_000_000 -> 500_000L
        span<10_000_000 -> 2_000_000L; else -> 5_000_000L }
    val windowStart=firstSource/quantum*quantum
    val windowEnd=minOf(media.durationUs,((lastSource+quantum-1)/quantum)*quantum).coerceAtLeast(windowStart+1)
    val desiredBuckets=(visibleWidth/2*(windowEnd-windowStart)/span).toInt().coerceIn(64,4096)
    val buckets=listOf(64,128,256,512,1024,2048,4096).first{it>=desiredBuckets}
    val slice by produceState<com.bbvideoeditor.app.media.WaveformSlice?>(null,media.uri,windowStart,windowEnd,buckets) {
        repository.progressiveWindow(media,windowStart,windowEnd,buckets).collect { value=it }
    }
    val samples=slice?.peaks ?: FloatArray(0)
    Canvas(modifier) {
        if (samples.isEmpty() || media.durationUs <= 0) return@Canvas
        val step = 3.dp.toPx()
        var x = 0f
        while (x < size.width) {
            val localUs = (visibleX + x - originalX) / pixelsPerSecond * MICROS_PER_SECOND
            val sourceUs = clip.sourceRange.startUs + localUs * clip.speed
            val current=slice ?: return@Canvas
            if(sourceUs<current.startUs || sourceUs>=current.decodedThroughUs){x+=step;continue}
            val bucket = ((sourceUs-current.startUs)/(current.endUs-current.startUs)*samples.size).toInt().coerceIn(samples.indices)
            val height = samples[bucket].coerceIn(0f, 1f) * size.height * .45f
            drawLine(Color.White.copy(alpha = .72f), Offset(x, size.height / 2 - height), Offset(x, size.height / 2 + height), 1.5.dp.toPx())
            x += step
        }
    }
}

@Composable
private fun ThumbnailStrip(media: MediaReference, clip: Clip, originalX: Double, visibleX: Double, visibleWidth: Double, pixelsPerSecond: Double, provider: ThumbnailProvider) {
    val density = LocalDensity.current
    val tilePx = with(density) { 64.dp.toPx().roundToInt() }.coerceAtLeast(1)
    val first = floor((visibleX - originalX) / tilePx).toLong().coerceAtLeast(0)
    val count = (visibleWidth / tilePx).toInt() + 2
    Box(Modifier.fillMaxSize()) {
        repeat(count.coerceAtMost(32)) { offset ->
            val tile = first + offset
            val sourceUs = (clip.sourceRange.startUs + tile * tilePx / pixelsPerSecond * MICROS_PER_SECOND * clip.speed).roundToLong().coerceIn(clip.sourceRange.startUs, clip.sourceRange.endUs - 1)
            key(media.id, tile, sourceUs / 250_000) {
                val bitmap by produceState<android.graphics.Bitmap?>(null, media.uri, sourceUs / 250_000, tilePx) {
                    val tileStart=originalX+tile*tilePx
                    val priority=if(tileStart+tilePx>visibleX && tileStart<visibleX+visibleWidth)
                        com.bbvideoeditor.app.media.AssetPriority.VISIBLE else com.bbvideoeditor.app.media.AssetPriority.NEAR_VIEWPORT
                    value = provider.thumbnail(com.bbvideoeditor.app.media.ThumbnailRequest(media,sourceUs,tilePx,tilePx,priority))
                }
                bitmap?.let { b ->
                    Image(b.asImageBitmap(), null,
                        Modifier.offset { IntOffset((originalX + tile * tilePx - visibleX).roundToInt(), 0) }
                            .width(with(density) { tilePx.toDp() }).fillMaxHeight(), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

/** Local to a timeline session, bounded to the current tracks; clip lists are the semantic key. */
private class TimelineDerivedIndexes {
    private var previous = emptyMap<String,Pair<List<Clip>,TrackIndex>>()
    fun update(tracks:List<Track>):Map<String,TrackIndex> {
        previous=tracks.associate { track -> track.id to (previous[track.id]?.takeIf { it.first==track.clips }
            ?: (track.clips to TrackIndex(track))) }
        return previous.mapValues { it.value.second }
    }
}
