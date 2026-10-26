package com.bbvideoeditor.app.export

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbvideoeditor.app.EditorApplication
import com.bbvideoeditor.app.ui.*
import com.bbvideoeditor.core.Project
import kotlinx.coroutines.CancellationException
import kotlin.math.roundToInt

@Composable
fun ExportScreen(project: Project, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val exports = (context.applicationContext as EditorApplication).exports
    val status by exports.state.collectAsStateWithLifecycle()
    var settings by remember(project.id) { mutableStateOf(ExportSettings(frameRate = project.canvas.frameRate.takeIf { it in listOf(24,25,30,50,60) } ?: 30)) }
    var capabilities by remember(project.canvas) { mutableStateOf<List<EncoderCapability>?>(null) }
    var capabilityError by remember { mutableStateOf<String?>(null) }
    var openError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(project.canvas) {
        try {
            val available = ExportCapabilities.inspect(project)
            capabilities = available
            settings = settings.supportedFallback(project, available) ?: settings
        }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { capabilityError = "Could not inspect the device encoders: ${e.message}" }
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
        if (uri != null) exports.start(project, settings.copy(documentUri = uri.toString()))
    }
    val dimensions = settings.dimensions(project)
    fun supported(candidate: ExportSettings): Boolean = capabilities?.let { candidate.supportedBy(project, it) } == true
    val running = status.active
    val showStatus = status.projectId == project.id || running
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Ink).safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Export video", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(project.name, fontSize = 12.sp, color = Muted)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close export", tint = MaterialTheme.colorScheme.onSurface) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Surface(color = Panel, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("${dimensions.first} × ${dimensions.second}", fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        Text("${settings.codec.label} · ${settings.frameRate} FPS · MP4", color = Accent, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Estimated size  ≈ ${settings.estimatedBytes(project.durationUs) / 1_000_000} MB", color = Muted, fontSize = 12.sp)
                        Text("${if (Build.VERSION.SDK_INT >= 29) "Movies / Frame" else "Choose a file location"}  ·  AAC audio", color = Muted, fontSize = 11.sp)
                    }
                }
                ExportChoice("Resolution") {
                    listOf(720,1080,2160).forEach { value ->
                        FilterChip(selected = settings.resolution == value, onClick = { settings = settings.copy(resolution = value) }, label = { Text(if (value == 2160) "4K" else "${value}p") }, enabled = !running && supported(settings.copy(resolution = value)))
                    }
                }
                ExportChoice("Frame rate") {
                    listOf(24,25,30,50,60).forEach { value ->
                        FilterChip(selected = settings.frameRate == value, onClick = { settings = settings.copy(frameRate = value) }, label = { Text("$value") }, enabled = !running && supported(settings.copy(frameRate = value)))
                    }
                }
                ExportChoice("Video codec") {
                    VideoCodec.entries.forEach { codec ->
                        FilterChip(selected = settings.codec == codec, onClick = { settings = settings.copy(codec = codec) }, label = { Text(codec.label) }, enabled = !running && supported(settings.copy(codec = codec)))
                    }
                }
                Column {
                    Row { Text("Bitrate / quality", color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)); Text("${settings.bitrate / 1_000_000} Mbps", color = Accent) }
                    Slider(value = settings.bitrate / 1_000_000f, onValueChange = { settings = settings.copy(bitrate = it.roundToInt() * 1_000_000) }, valueRange = 1f..80f, enabled = !running)
                    Text("Higher bitrate preserves more detail and uses more storage. The encoder may adjust settings for this device.", color = Muted, fontSize = 11.sp)
                }
                if (capabilities == null && capabilityError == null) Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Checking device capabilities…", color = Muted, fontSize = 12.sp) }
                capabilityError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (capabilities != null && !supported(settings)) Text("This device does not advertise an encoder for these settings. Select a supported combination.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                if (showStatus && status.phase != ExportPhase.IDLE) {
                    Surface(color = Panel, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(when (status.phase) {
                                ExportPhase.PREPARING -> "Preparing export"
                                ExportPhase.RUNNING -> "Exporting${status.progress?.let { " · $it%" } ?: "…"}"
                                ExportPhase.PUBLISHING -> "Saving your video"
                                ExportPhase.SUCCEEDED -> "Your video is ready"
                                ExportPhase.CANCELLED -> "Export cancelled"
                                ExportPhase.FAILED -> "Export could not finish"
                                else -> "Export"
                            }, fontWeight = FontWeight.SemiBold)
                            if (running) {
                                status.progress?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth()) } ?: LinearProgressIndicator(Modifier.fillMaxWidth())
                                Text("You can leave this screen. Export continues in the background.", color = Muted, fontSize = 12.sp)
                            }
                            Text(status.error ?: status.detail, color = if (status.error != null) MaterialTheme.colorScheme.error else Muted, fontSize = 12.sp)
                            if (status.phase == ExportPhase.SUCCEEDED) {
                                Text("%.1f MB".format(status.outputBytes / 1_000_000.0), color = Muted, fontSize = 12.sp)
                                Button(onClick = {
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(status.outputUri), "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                                    catch (_: Exception) { openError = "No video player is available. The video is saved in your device's media storage." }
                                }) { Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Open video") }
                            }
                        }
                    }
                }
                openError?.let { Text(it, color = Muted, fontSize = 12.sp) }
            }
            Surface(color = Panel) {
                Column(Modifier.padding(20.dp)) {
                    if (running) OutlinedButton(onClick = exports::cancel, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Cancel export") }
                    else Button(onClick = {
                        if (Build.VERSION.SDK_INT < 29) documentPicker.launch("${project.name}.mp4") else exports.start(project, settings)
                    }, enabled = project.durationUs > 0 && supported(settings), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                        Text(if (status.phase == ExportPhase.SUCCEEDED) "Export again" else "Export video", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(10.dp)); Icon(Icons.Default.NorthEast, null, Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportChoice(title: String, content: @Composable RowScope.() -> Unit) {
    Column { Text(title, color = Color.White, fontWeight = FontWeight.Medium); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}
