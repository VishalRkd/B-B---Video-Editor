package com.bbvideoeditor.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF0C0E12)
val Panel = Color(0xFF16191F)
val PanelRaised = Color(0xFF22262E)
val Accent = Color(0xFFDFFF83)
val Muted = Color(0xFF989FAC)
val VideoColor = Color(0xFF5969AE)
val AudioColor = Color(0xFF286D60)
val OverlayColor = Color(0xFF865886)

@Composable
fun EditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, onPrimary = Ink, background = Ink, surface = Panel,
            surfaceVariant = PanelRaised, onSurface = Color(0xFFF3F4F6),
            onSurfaceVariant = Muted, outline = Color(0xFF383D47),
            secondary = Color(0xFFADA6FF), error = Color(0xFFFF9B96),
        ), content = content,
    )
}
