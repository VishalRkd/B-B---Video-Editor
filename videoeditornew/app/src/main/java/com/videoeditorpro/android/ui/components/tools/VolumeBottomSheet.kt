package com.videoeditorpro.android.ui.components.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeBottomSheet(
    currentVolume: Int,
    isMuted: Boolean,
    onVolumeChanged: (volume: Int, muted: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var volume by remember { mutableIntStateOf(currentVolume) }
    var muted by remember { mutableStateOf(isMuted) }

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
                    text = "Audio Volume",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (muted) "Muted" else "$volume%",
                    color = if (muted) Color.Gray else Color(0xFFFF3B5C),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Mute Switch Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (muted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Mute Original Audio", color = Color.White, fontSize = 14.sp)
                }

                Switch(
                    checked = muted,
                    onCheckedChange = {
                        muted = it
                        onVolumeChanged(volume, it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFF3B5C)
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            // Volume Slider (0% to 200%)
            Slider(
                value = volume.toFloat(),
                onValueChange = {
                    val newVol = it.roundToInt()
                    volume = newVol
                    if (muted && newVol > 0) muted = false
                    onVolumeChanged(newVol, muted)
                },
                valueRange = 0f..200f,
                enabled = !muted,
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
                Text("0%", color = Color.Gray, fontSize = 11.sp)
                Text("100% (Default)", color = Color.Gray, fontSize = 11.sp)
                Text("200% (Boost)", color = Color.Gray, fontSize = 11.sp)
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
