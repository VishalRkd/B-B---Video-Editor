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
import com.videoeditorpro.android.domain.model.FilterItem
import com.videoeditorpro.android.domain.model.FilterPreset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilter: FilterItem,
    onFilterChanged: (FilterItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf(currentFilter.preset) }
    var brightness by remember { mutableIntStateOf(currentFilter.brightness) }
    var contrast by remember { mutableIntStateOf(currentFilter.contrast) }
    var saturation by remember { mutableIntStateOf(currentFilter.saturation) }

    fun notifyChange() {
        onFilterChanged(
            FilterItem(
                preset = selectedPreset,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation
            )
        )
    }

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
                    text = "Filters & Color Grading",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        selectedPreset = FilterPreset.NONE
                        brightness = 0
                        contrast = 0
                        saturation = 0
                        notifyChange()
                    }
                ) {
                    Text("Reset", color = Color(0xFFFF3B5C), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Presets Horizontal Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(FilterPreset.values()) { preset ->
                    val isSelected = preset == selectedPreset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFFFF3B5C) else Color(0xFF2C2C3A))
                            .clickable {
                                selectedPreset = preset
                                notifyChange()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.displayName,
                            color = if (isSelected) Color.White else Color(0xFFAAAAAA),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Brightness Slider
            Text(
                text = "Brightness: $brightness",
                color = Color(0xFFCCCCCC),
                fontSize = 13.sp
            )
            Slider(
                value = brightness.toFloat(),
                onValueChange = {
                    brightness = it.roundToInt()
                    notifyChange()
                },
                valueRange = -100f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF3B5C),
                    activeTrackColor = Color(0xFFFF3B5C),
                    inactiveTrackColor = Color(0xFF333344)
                )
            )

            // Contrast Slider
            Text(
                text = "Contrast: $contrast",
                color = Color(0xFFCCCCCC),
                fontSize = 13.sp
            )
            Slider(
                value = contrast.toFloat(),
                onValueChange = {
                    contrast = it.roundToInt()
                    notifyChange()
                },
                valueRange = -100f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF3B5C),
                    activeTrackColor = Color(0xFFFF3B5C),
                    inactiveTrackColor = Color(0xFF333344)
                )
            )

            // Saturation Slider
            Text(
                text = "Saturation: $saturation",
                color = Color(0xFFCCCCCC),
                fontSize = 13.sp
            )
            Slider(
                value = saturation.toFloat(),
                onValueChange = {
                    saturation = it.roundToInt()
                    notifyChange()
                },
                valueRange = -100f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF3B5C),
                    activeTrackColor = Color(0xFFFF3B5C),
                    inactiveTrackColor = Color(0xFF333344)
                )
            )

            Spacer(Modifier.height(16.dp))

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
