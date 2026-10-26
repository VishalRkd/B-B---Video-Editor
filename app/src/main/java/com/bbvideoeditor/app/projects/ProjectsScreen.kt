package com.bbvideoeditor.app.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbvideoeditor.app.editor.formatTime
import com.bbvideoeditor.app.ui.*
import com.bbvideoeditor.core.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(state: WorkspaceState, onCreate: (String) -> Unit, onOpen: (String) -> Unit, onDelete: (String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Project?>(null) }
    Column(Modifier.fillMaxSize().background(Ink).safeDrawingPadding().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(Accent, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Ink, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(10.dp)); Text("FRAME", color = Color.White, letterSpacing = 4.sp, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f)); Text("STUDIO", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(40.dp))
        Text("Make something\nworth watching.", fontSize = 33.sp, lineHeight = 39.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.height(12.dp)); Text("Your footage. Your rhythm. Your story.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(26.dp))
        Button(onClick = { creating = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(15.dp)) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("New project", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f)); Icon(Icons.Default.NorthEast, null, Modifier.size(20.dp))
        }
        Spacer(Modifier.height(34.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Text("YOUR PROJECTS", color = Muted, letterSpacing = 2.sp, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("${state.projects.size}", color = Muted, fontSize = 11.sp) }
        Spacer(Modifier.height(14.dp))
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.projects.isEmpty() && !state.busy) {
            Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("A blank canvas, endless possibilities.", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp)); Text("Create your first project to get started.", color = Muted, fontSize = 12.sp)
            }
        } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(state.projects, key = { it.id }) { project ->
                Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).clickable { onOpen(project.id) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).background(PanelRaised, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, tint = Accent, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(project.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(5.dp))
                        Text("${formatTime(project.durationUs)}  ·  ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(project.updatedAt))}", color = Muted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { deleting = project }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.DeleteOutline, "Delete ${project.name}", tint = Muted, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
    if (creating) {
        var name by remember { mutableStateOf("Untitled project") }
        AlertDialog(onDismissRequest = { creating = false }, title = { Text("New project") }, text = { OutlinedTextField(name, { name = it }, label = { Text("Project name") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { creating = false; onCreate(name.trim()) }, enabled = name.isNotBlank()) { Text("Create") } },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancel") } })
    }
    deleting?.let { project ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete ${project.name}?") }, text = { Text("This deletes the saved edit. Your original media and exported videos are kept.") },
            confirmButton = { TextButton(onClick = { deleting = null; onDelete(project.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}
