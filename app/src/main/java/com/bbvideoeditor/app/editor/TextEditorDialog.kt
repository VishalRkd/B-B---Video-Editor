package com.bbvideoeditor.app.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bbvideoeditor.app.render.RasterizedText
import com.bbvideoeditor.app.render.TextBitmapCache
import com.bbvideoeditor.app.ui.*
import com.bbvideoeditor.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** A draft is committed as one command. Raster validation also runs before committing the draft. */
@Composable
fun TextEditorDialog(
    clip: Clip?, caption: Boolean, positionUs: Long, project: Project, bitmaps: TextBitmapCache,
    onDismiss: () -> Unit, onSave: (TextContent, Long, Long) -> String?,
) {
    var body by rememberSaveable { mutableStateOf(clip?.text?.text.orEmpty()) }
    var style by rememberSaveable(stateSaver = Saver<TextContent, String>(
        save = { Json.encodeToString(it) }, restore = { Json.decodeFromString<TextContent>(it) },
    )) { mutableStateOf(clip?.text ?: TextContent("Text", fontSize = if (caption) 48f else 64f,
        outlineWidth = if (caption) 2f else 0f, backgroundColor = if (caption) 0x99000000 else 0x00000000)) }
    val initialDuration = clip?.durationUs ?: (project.durationUs - positionUs).coerceIn(1_000_000L, 3_000_000L)
    var start by rememberSaveable { mutableStateOf(seconds(clip?.startUs ?: positionUs)) }
    var duration by rememberSaveable { mutableStateOf(seconds(initialDuration)) }
    var error by remember { mutableStateOf<String?>(null) }
    val invalidColors = remember { mutableStateMapOf<String, Boolean>() }
    val colorValidity: (String, Boolean) -> Unit = { label, valid ->
        if (valid) invalidColors.remove(label) else invalidColors[label] = true
    }
    val content = remember(body, style) { runCatching { style.copy(text = body) }.getOrNull() }
    val raster by produceState<Pair<TextContent?, Result<RasterizedText>>?>(null, content, project.canvas) {
        value = null
        if (content != null) value = content to withContext(Dispatchers.Default) {
            runCatching { bitmaps.get(content, project.canvas, 360) }
        }
    }
    val ready = raster?.takeIf { it.first == content }?.second
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.96f).safeDrawingPadding().imePadding(), color = Panel,
            shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (clip != null) "Edit text" else if (caption) "Add caption" else "Add text", Modifier.weight(1f), fontSize = 20.sp)
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        val startUs = parseSeconds(start)
                        val durationUs = parseSeconds(duration)
                        error = when {
                            content == null -> "Enter between 1 and 2000 characters."
                            startUs == null || durationUs == null || durationUs < project.canvas.frameDurationUs -> "Enter a valid start and a duration of at least one frame."
                            else -> onSave(content, startUs, durationUs)
                        }
                    }, enabled = content != null && ready?.isSuccess == true && invalidColors.isEmpty(), modifier = Modifier.testTag("save-text")) { Text("Save") }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.fillMaxWidth().height(105.dp).background(Color(0xFF17191F), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        ready?.getOrNull()?.let { Image(it.bitmap.asImageBitmap(), "Text style preview", Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit) }
                    }
                    OutlinedTextField(body, { body = it.take(2000); error = null }, label = { Text("Text") },
                        supportingText = { Text("${body.length}/2000 · Line breaks supported") }, minLines = 2, maxLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("text-input"))
                    (error ?: ready?.exceptionOrNull()?.message)?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(start, { start = it; error = null }, label = { Text("Start (seconds)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).testTag("text-start"))
                        OutlinedTextField(duration, { duration = it; error = null }, label = { Text("Duration (seconds)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).testTag("text-duration"))
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextFont.entries.forEach { font -> FilterChip(style.font == font, { style = style.copy(font = font) }, label = { Text(font.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(style.bold, { style = style.copy(bold = !style.bold) }, label = { Text("Bold") })
                        FilterChip(style.italic, { style = style.copy(italic = !style.italic) }, label = { Text("Italic") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextAlignment.entries.forEach { alignment -> FilterChip(style.alignment == alignment, { style = style.copy(alignment = alignment) }, label = { Text(alignment.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
                    }
                    TextStyleSlider("Font size", style.fontSize, 12f..240f) { style = style.copy(fontSize = it) }
                    TextStyleSlider("Wrap width %", style.maxWidthFraction * 100, 20f..100f) { style = style.copy(maxWidthFraction = it / 100) }
                    ColorField("Text color", style.color, colorValidity) { style = style.copy(color = it) }
                    TextStyleSlider("Outline", style.outlineWidth, 0f..16f) { style = style.copy(outlineWidth = it) }
                    if (style.outlineWidth > 0) ColorField("Outline color", style.outlineColor, colorValidity) { style = style.copy(outlineColor = it) }
                    TextStyleSlider("Shadow", style.shadowRadius, 0f..32f) { style = style.copy(shadowRadius = it) }
                    if (style.shadowRadius > 0) ColorField("Shadow color", style.shadowColor, colorValidity) { style = style.copy(shadowColor = it) }
                    ColorField("Background color", style.backgroundColor, colorValidity) { style = style.copy(backgroundColor = it) }
                    if (style.backgroundColor ushr 24 != 0L) {
                        TextStyleSlider("Padding", style.padding, 0f..64f) { style = style.copy(padding = it) }
                        TextStyleSlider("Corner radius", style.cornerRadius, 0f..64f) { style = style.copy(cornerRadius = it) }
                    }
                    Text("After saving, drag text on the preview to move it. Pinch or use the corner to resize; use the top handle to rotate. Adjust offers precise position and opacity controls.", color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TextStyleSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, change: (Float) -> Unit) {
    Column {
        Row { Text(label, Modifier.weight(1f), fontSize = 12.sp); Text(value.roundToInt().toString(), color = Accent, fontSize = 12.sp) }
        Slider(value, change, valueRange = range)
    }
}

@Composable
private fun ColorField(label: String, value: Long, validity: (String, Boolean) -> Unit, change: (Long) -> Unit) {
    var hex by remember(value) { mutableStateOf("%08X".format(Locale.ROOT, value)) }
    LaunchedEffect(hex) { validity(label, hex.length == 8) }
    DisposableEffect(label) { onDispose { validity(label, true) } }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(hex, { input ->
            hex = input.filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }.take(8)
            if (hex.length == 8) hex.toLongOrNull(16)?.let(change)
        }, label = { Text("$label · AARRGGBB") }, singleLine = true, isError = hex.length != 8,
            leadingIcon = { Box(Modifier.size(22.dp).background(Color(value.toInt()), RoundedCornerShape(5.dp))) }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf<Pair<String, Long>>("White" to 0xFFFFFFFF, "Black" to 0xFF000000, "Red" to 0xFFFF5252, "Gold" to 0xFFFFD54F,
                "Blue" to 0xFF64B5F6, "Clear" to 0x00000000).forEach { (name, color) ->
                FilterChip(value == color, { hex = "%08X".format(Locale.ROOT, color); change(color) }, label = { Text(name, fontSize = 11.sp) })
            }
        }
    }
}

private fun seconds(us: Long) = String.format(Locale.ROOT, "%.6f", us / 1_000_000.0).trimEnd('0').trimEnd('.')
private fun parseSeconds(value: String): Long? = value.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 && it <= 864_000 }?.let { (it * 1_000_000).roundToLong() }
