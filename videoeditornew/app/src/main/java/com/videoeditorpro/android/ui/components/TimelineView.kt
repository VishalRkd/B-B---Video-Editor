package com.videoeditorpro.android.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun TimelineView(
    thumbnails: List<Bitmap>,
    durationMs: Int,
    currentPositionMs: Int,
    trimStartMs: Int,
    trimEndMs: Int,
    onSeek: (positionMs: Int) -> Unit,
    onTrimChanged: (startMs: Int, endMs: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var timelineWidthPx by remember { mutableStateOf(1f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF18181F), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Timecode display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTimecode(currentPositionMs),
                color = Color(0xFFFF3B5C),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            Text(
                text = formatTimecode(durationMs),
                color = Color(0xFFAAAAAA),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }

        // Timeline track box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF262630))
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
            // Filmstrip Thumbnails Row
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
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
                            .background(Color(0xFF22222B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading Filmstrip...", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }

            // Trim Highlight Border
            val safeDuration = durationMs.coerceAtLeast(1).toFloat()
            val leftRatio = (trimStartMs / safeDuration).coerceIn(0f, 1f)
            val rightRatio = (trimEndMs / safeDuration).coerceIn(0f, 1f)
            val leftOffsetPx = leftRatio * timelineWidthPx
            val trimWidthPx = ((rightRatio - leftRatio) * timelineWidthPx).coerceAtLeast(0f)

            // Dimmed areas outside trim
            if (leftOffsetPx > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width((leftOffsetPx / 2.7f).dp)
                        .background(Color(0x99000000))
                )
            }

            // Active trim region border
            Box(
                modifier = Modifier
                    .offset { IntOffset(leftOffsetPx.roundToInt(), 0) }
                    .width((trimWidthPx / 2.7f).dp)
                    .fillMaxHeight()
                    .background(Color.Transparent)
            )

            // Left Trim Handle (Yellow)
            Box(
                modifier = Modifier
                    .offset { IntOffset((leftOffsetPx - 10f).coerceAtLeast(0f).roundToInt(), 0) }
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFFFD700), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    .pointerInput(timelineWidthPx, safeDuration) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = ((dragAmount.x / timelineWidthPx) * safeDuration).toInt()
                            val newStart = (trimStartMs + deltaMs).coerceIn(0, trimEndMs - 500)
                            onTrimChanged(newStart, trimEndMs)
                        }
                    }
            )

            // Right Trim Handle (Yellow)
            val rightOffsetPx = rightRatio * timelineWidthPx
            Box(
                modifier = Modifier
                    .offset { IntOffset((rightOffsetPx - 12f).coerceAtLeast(0f).roundToInt(), 0) }
                    .width(12.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFFFD700), RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .pointerInput(timelineWidthPx, safeDuration) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = ((dragAmount.x / timelineWidthPx) * safeDuration).toInt()
                            val newEnd = (trimEndMs + deltaMs).coerceIn(trimStartMs + 500, durationMs)
                            onTrimChanged(trimStartMs, newEnd)
                        }
                    }
            )

            // Playhead Line (Red)
            val playheadRatio = (currentPositionMs / safeDuration).coerceIn(0f, 1f)
            val playheadX = (playheadRatio * timelineWidthPx).coerceAtLeast(0f)
            Box(
                modifier = Modifier
                    .offset { IntOffset(playheadX.roundToInt(), 0) }
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFFF3B5C))
            )
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
