package com.videoeditorpro.android.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videoeditorpro.android.domain.model.VideoClipItem
import kotlin.math.roundToInt

/**
 * Scalable, density-independent timeline component supporting multi-clip visualization,
 * precise trimming handles with generous touch targets, and buttery smooth playhead scrubbing.
 */
@Composable
fun TimelineView(
    thumbnails: List<Bitmap>,
    durationMs: Int,
    currentPositionMs: Int,
    trimStartMs: Int,
    trimEndMs: Int,
    onSeek: (positionMs: Int) -> Unit,
    onTrimChanged: (startMs: Int, endMs: Int) -> Unit,
    modifier: Modifier = Modifier,
    clips: List<VideoClipItem> = emptyList(),
    selectedClipIndex: Int = 0,
    onSelectClipIndex: (Int) -> Unit = {}
) {
    val density = LocalDensity.current
    var timelineWidthPx by remember { mutableStateOf(1f) }

    val safeDuration = durationMs.coerceAtLeast(1).toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF16161E), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // 1. Timecode HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B5C))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatTimecode(currentPositionMs),
                    color = Color(0xFFFF3B5C),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
            Text(
                text = formatTimecode(durationMs),
                color = Color(0xFF888896),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }

        // 2. Timeline Track Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF22222E))
                .onSizeChanged { timelineWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(timelineWidthPx, durationMs) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newPct = (change.position.x / timelineWidthPx).coerceIn(0f, 1f)
                        val targetMs = (newPct * durationMs).toInt()
                        onSeek(targetMs)
                    }
                }
        ) {
            // Filmstrip Thumbnails
            Row(modifier = Modifier.fillMaxSize()) {
                if (thumbnails.isNotEmpty()) {
                    thumbnails.forEach { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1C1C24)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading filmstrip...", color = Color(0xFF6E6E80), fontSize = 11.sp)
                    }
                }
            }

            // Multi-clip segment dividers (if multiple clips present)
            if (clips.size > 1) {
                var accumMs = 0
                clips.forEachIndexed { idx, clip ->
                    accumMs += clip.effectiveDurationMs
                    if (idx < clips.size - 1) {
                        val dividerX = (accumMs.toFloat() / safeDuration) * timelineWidthPx
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(dividerX.roundToInt(), 0) }
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF000000))
                        )
                    }
                }
            }

            // Trimming Calculations
            val leftRatio = (trimStartMs / safeDuration).coerceIn(0f, 1f)
            val rightRatio = (trimEndMs / safeDuration).coerceIn(0f, 1f)
            val leftOffsetPx = leftRatio * timelineWidthPx
            val trimWidthPx = ((rightRatio - leftRatio) * timelineWidthPx).coerceAtLeast(0f)

            // Density-accurate DP values
            val leftOffsetDp = with(density) { leftOffsetPx.toDp() }
            val trimWidthDp = with(density) { trimWidthPx.toDp() }

            // Dimmed Non-active Left Region
            if (leftOffsetPx > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(leftOffsetDp)
                        .background(Color(0xAA000000))
                )
            }

            // Dimmed Non-active Right Region
            val rightOffsetPx = rightRatio * timelineWidthPx
            val rightWidthPx = (timelineWidthPx - rightOffsetPx).coerceAtLeast(0f)
            if (rightWidthPx > 0) {
                val rightWidthDp = with(density) { rightWidthPx.toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(rightOffsetPx.roundToInt(), 0) }
                        .fillMaxHeight()
                        .width(rightWidthDp)
                        .background(Color(0xAA000000))
                )
            }

            // Active Trim Region Outline Box
            Box(
                modifier = Modifier
                    .offset { IntOffset(leftOffsetPx.roundToInt(), 0) }
                    .width(trimWidthDp)
                    .fillMaxHeight()
                    .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(4.dp))
            )

            // Left Trim Handle (Expanded 36dp touch target)
            val handleTouchWidthDp = 36.dp
            val handleTouchHalfPx = with(density) { (handleTouchWidthDp / 2).toPx() }
            val leftTouchOffsetPx = (leftOffsetPx - handleTouchHalfPx).coerceAtLeast(0f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(leftTouchOffsetPx.roundToInt(), 0) }
                    .width(handleTouchWidthDp)
                    .fillMaxHeight()
                    .pointerInput(timelineWidthPx, safeDuration) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = ((dragAmount.x / timelineWidthPx) * safeDuration).toInt()
                            val newStart = (trimStartMs + deltaMs).coerceIn(0, trimEndMs - 300)
                            onTrimChanged(newStart, trimEndMs)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Visual Handle Bar
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .fillMaxHeight()
                        .background(
                            Color(0xFFFFD700),
                            RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (i in 0 until 3) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .background(Color.Black, CircleShape)
                            )
                        }
                    }
                }
            }

            // Right Trim Handle (Expanded 36dp touch target)
            val rightTouchOffsetPx = (rightOffsetPx - handleTouchHalfPx).coerceAtLeast(0f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(rightTouchOffsetPx.roundToInt(), 0) }
                    .width(handleTouchWidthDp)
                    .fillMaxHeight()
                    .pointerInput(timelineWidthPx, safeDuration) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = ((dragAmount.x / timelineWidthPx) * safeDuration).toInt()
                            val newEnd = (trimEndMs + deltaMs).coerceIn(trimStartMs + 300, durationMs)
                            onTrimChanged(trimStartMs, newEnd)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Visual Handle Bar
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .fillMaxHeight()
                        .background(
                            Color(0xFFFFD700),
                            RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (i in 0 until 3) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .background(Color.Black, CircleShape)
                            )
                        }
                    }
                }
            }

            // Playhead Cursor (Red indicator + top triangle/capsule)
            val playheadRatio = (currentPositionMs / safeDuration).coerceIn(0f, 1f)
            val playheadX = (playheadRatio * timelineWidthPx).coerceAtLeast(0f)

            Box(
                modifier = Modifier
                    .offset { IntOffset((playheadX - with(density) { 6.dp.toPx() }).roundToInt(), 0) }
                    .width(12.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Top cap
                Box(
                    modifier = Modifier
                        .size(10.dp, 6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFFF3B5C))
                )
                // Vertical needle
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFFF3B5C))
                )
            }
        }
    }
}

private fun formatTimecode(ms: Int): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val tenths = (ms % 1000) / 100
    return String.format("%02d:%02d.%d", minutes, seconds, tenths)
}
