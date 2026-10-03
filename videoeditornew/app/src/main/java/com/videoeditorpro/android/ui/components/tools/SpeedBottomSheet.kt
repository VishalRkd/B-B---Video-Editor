package com.videoeditorpro.android.ui.components.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedBottomSheet(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var speed by remember { mutableFloatStateOf(currentSpeed) }
    val presets = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E28),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF555566)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playback Speed",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(speed * 100).roundToInt() / 100f}x",
                    color = Color(0xFFFF3B5C),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Presets Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    val isSelected = (speed - preset).let { it >= -0.05f && it <= 0.05f }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFFFF3B5C) else Color(0xFF2C2C3A))
                            .clickable {
                                speed = preset
                                onSpeedSelected(preset)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${preset}x",
                            color = if (isSelected) Color.White else Color(0xFFAAAAAA),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Continuous Slider
            Slider(
                value = speed,
                onValueChange = {
                    val rounded = (it * 10).roundToInt() / 10f
                    speed = rounded
                    onSpeedSelected(rounded)
                },
                valueRange = 0.2f..4.0f,
                steps = 37,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF3B5C),
                    activeTrackColor = Color(0xFFFF3B5C),
                    inactiveTrackColor = Color(0xFF333344)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0.2x", color = Color.Gray, fontSize = 11.sp)
                Text("1.0x (Normal)", color = Color.Gray, fontSize = 11.sp)
                Text("4.0x", color = Color.Gray, fontSize = 11.sp)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B5C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
