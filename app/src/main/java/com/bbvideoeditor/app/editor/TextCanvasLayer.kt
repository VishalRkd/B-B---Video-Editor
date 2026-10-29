package com.bbvideoeditor.app.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbvideoeditor.app.diagnostics.beginEngineTrace
import com.bbvideoeditor.app.diagnostics.endEngineTrace
import com.bbvideoeditor.app.render.RasterizedText
import com.bbvideoeditor.app.render.TextBitmapCache
import com.bbvideoeditor.app.ui.Accent
import com.bbvideoeditor.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/** Canvas adapter for the shared text rasterizer. Every gesture writes the real command model. */
@Composable
fun TextCanvasLayer(editor: EditorViewModel, bitmaps: TextBitmapCache, modifier: Modifier = Modifier) {
    val tracing = beginEngineTrace("TextCanvasComposition")
    val state by editor.document.collectAsStateWithLifecycle()
    val transport = editor.transport.collectAsStateWithLifecycle()
    val indices = remember(state.project.timeline.tracks) {
        state.project.timeline.tracks.filter { it.type == TrackType.TEXT }.map(::TrackIndex)
    }
    // Like the native video surface, hold the final frame when transport reaches the end.
    val active by remember(indices, transport, state.durationUs) { derivedStateOf(structuralEqualityPolicy()) {
        val time = transport.value.positionUs.coerceAtMost((state.durationUs - 1).coerceAtLeast(0))
        indices.mapNotNull { it.clipAt(time) }
    } }
    val requested by remember(indices, transport, state.durationUs) { derivedStateOf(structuralEqualityPolicy()) {
        val time = transport.value.positionUs.coerceAtMost((state.durationUs - 1).coerceAtLeast(0))
        indices.flatMap { it.visibleClips(time, time + 1_000_000).take(2) }.map { it.id to requireNotNull(it.text) }
    } }
    val canvas = state.project.canvas
    val rendered by produceState<Pair<Map<String, RasterizedText>, List<String>>>(emptyMap<String, RasterizedText>() to emptyList(), requested, canvas) {
        value = withContext(Dispatchers.Default) {
            val errors = mutableListOf<String>()
            val frames = requested.mapNotNull { (id, text) ->
                runCatching { id to bitmaps.get(text, canvas, minOf(canvas.height, 1080)) }
                    .onFailure { errors += it.message ?: "Unable to render text." }.getOrNull()
            }.toMap()
            frames to errors
        }
    }
    val frames = rendered.first
    LaunchedEffect(rendered.second) { rendered.second.firstOrNull()?.let(editor::message) }
    val currentState by rememberUpdatedState(state)
    val currentActive by rememberUpdatedState(active)
    val currentFrames by rememberUpdatedState(frames)
    Canvas(modifier.testTag("text-canvas")
        .semantics { contentDescription = "Text canvas. Drag to move. Use corner to resize and top handle to rotate." }
        .pointerInput(editor, canvas.aspectRatio) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val scene = TextScene(size.width.toFloat(), size.height.toFloat(), canvas.aspectRatio)
                val selected = currentActive.firstOrNull { it.id == currentState.selectedClipId }
                fun box(clip: Clip) = currentFrames[clip.id]?.let { scene.box(clip.transform, it) }
                val selectedBox = selected?.let(::box)?.withMinimumSize(72.dp.toPx(), 48.dp.toPx())
                val handle = selectedBox?.let {
                    when {
                        (down.position - it.resizeHandle).getDistance() < 20.dp.toPx() -> 1
                        (down.position - it.rotateHandle(28.dp.toPx())).getDistance() < 20.dp.toPx() -> 2
                        else -> 0
                    }
                } ?: 0
                val hit = if (handle != 0) selected else currentActive.asReversed().firstOrNull { box(it)?.contains(down.position, 8.dp.toPx()) == true }
                if (hit == null) return@awaitEachGesture
                val alreadySelected = currentState.selectedClipId == hit.id
                editor.select(hit.id)
                if (currentState.project.findTrackForClip(hit.id)?.locked == true) return@awaitEachGesture
                editor.beginGesture()
                down.consume()
                var transform = hit.transform
                var moved = false
                var released = false
                try {
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        released = pressed.isEmpty()
                        if (released) break
                        val first = pressed.first()
                        if (!moved && (pressed.size > 1 || (first.position - down.position).getDistance() > viewConfiguration.touchSlop)) moved = true
                        if (!moved) continue
                        val geometry = currentFrames[hit.id]?.let { scene.box(transform, it) } ?: continue
                        val pan = event.calculatePan()
                        var zoom = 1f
                        var rotation = 0f
                        var center = geometry.center + pan
                        if (pressed.size > 1) {
                            zoom = event.calculateZoom()
                            rotation = event.calculateRotation()
                            val pivot = event.calculateCentroid(useCurrent = false)
                            center = pivot + rotate(geometry.center - pivot, rotation) * zoom + pan
                        } else if (handle == 1) {
                            zoom = (first.position - geometry.center).getDistance() / (first.previousPosition - geometry.center).getDistance().coerceAtLeast(1f)
                            center = geometry.center
                        } else if (handle == 2) {
                            val before = first.previousPosition - geometry.center
                            val after = first.position - geometry.center
                            rotation = ((atan2(after.y, after.x) - atan2(before.y, before.x)) * 180f / PI.toFloat())
                            center = geometry.center
                        }
                        transform = transform.copy(
                            translationX = ((center.x - scene.center.x) * 2f / scene.width).coerceIn(-1f, 1f),
                            translationY = ((scene.center.y - center.y) * 2f / scene.height).coerceIn(-1f, 1f),
                            scaleX = (transform.scaleX * zoom).coerceIn(.2f, 4f), scaleY = (transform.scaleY * zoom).coerceIn(.2f, 4f),
                            rotationDegrees = ((transform.rotationDegrees - rotation + 540f) % 360f) - 180f,
                        )
                        editor.previewGesture(UpdateClip(hit.id, transform = transform))
                        event.changes.forEach { it.consume() }
                    } while (true)
                    editor.finishGesture()
                    if (!moved && alreadySelected) editor.editText(hit.id)
                } finally { if (!released) editor.cancelGesture() }
            }
        }) {
        val scene = TextScene(size.width, size.height, canvas.aspectRatio)
        active.forEach { clip ->
            val frame = frames[clip.id] ?: return@forEach
            val box = scene.box(clip.transform, frame)
            // Pixels and typography come from the very same rasterizer as TimelineTextOverlay.
            clipRect(scene.center.x - scene.width / 2, scene.center.y - scene.height / 2,
                scene.center.x + scene.width / 2, scene.center.y + scene.height / 2) {
                withTransform({
                    translate(box.center.x, box.center.y)
                    rotate(-clip.transform.rotationDegrees, Offset.Zero)
                    scale(clip.transform.scaleX, clip.transform.scaleY, Offset.Zero)
                }) {
                    val width = frame.logicalWidth / 1080f * scene.height
                    val height = frame.logicalHeight / 1080f * scene.height
                    withTransform({
                        translate(-width / 2, -height / 2)
                        scale(width / frame.bitmap.width, height / frame.bitmap.height, Offset.Zero)
                    }) { drawImage(frame.bitmap.asImageBitmap(), alpha = clip.transform.opacity) }
                }
            }
            if (clip.id == state.selectedClipId && !transport.value.playing) {
                val selectionBox = box.withMinimumSize(72.dp.toPx(), 48.dp.toPx())
                val corners = selectionBox.corners
                corners.indices.forEach { drawLine(Accent, corners[it], corners[(it + 1) % 4], 1.5.dp.toPx()) }
                if (state.project.findTrackForClip(clip.id)?.locked != true) {
                    val rotation = selectionBox.rotateHandle(28.dp.toPx())
                    drawLine(Accent, selectionBox.world(Offset(0f, -selectionBox.height / 2)), rotation, 1.5.dp.toPx())
                    drawCircle(Accent, 6.dp.toPx(), selectionBox.resizeHandle)
                    drawCircle(Color.White, 6.dp.toPx(), rotation)
                }
            }
        }
    }
    endEngineTrace(tracing)
}

private fun rotate(offset: Offset, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    return Offset(offset.x * cos(radians) - offset.y * sin(radians), offset.x * sin(radians) + offset.y * cos(radians))
}

private class TextScene(viewWidth: Float, viewHeight: Float, aspect: Float) {
    val width = minOf(viewWidth, viewHeight * aspect).coerceAtLeast(1f)
    val height = width / aspect
    val center = Offset(viewWidth / 2f, viewHeight / 2f)
    fun box(transform: Transform, frame: RasterizedText) = TextBox(
        center + Offset(transform.translationX * width / 2f, -transform.translationY * height / 2f),
        frame.logicalWidth / 1080f * height * transform.scaleX, frame.logicalHeight / 1080f * height * transform.scaleY,
        -transform.rotationDegrees,
    )
}

private data class TextBox(val center: Offset, val width: Float, val height: Float, val rotation: Float) {
    fun withMinimumSize(minWidth: Float, minHeight: Float) = copy(width = maxOf(width, minWidth), height = maxOf(height, minHeight))
    fun world(offset: Offset) = center + rotate(offset, rotation)
    fun contains(point: Offset, padding: Float): Boolean {
        val local = rotate(point - center, -rotation)
        return abs(local.x) <= width / 2 + padding && abs(local.y) <= height / 2 + padding
    }
    val corners get() = listOf(Offset(-width / 2, -height / 2), Offset(width / 2, -height / 2), Offset(width / 2, height / 2), Offset(-width / 2, height / 2)).map(::world)
    val resizeHandle get() = world(Offset(width / 2, height / 2))
    fun rotateHandle(distance: Float) = world(Offset(0f, -height / 2 - distance))
}
