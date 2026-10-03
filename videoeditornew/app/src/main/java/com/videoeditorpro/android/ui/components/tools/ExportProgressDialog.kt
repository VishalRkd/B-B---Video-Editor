package com.videoeditorpro.android.ui.components.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ExportProgressDialog(
    progressPct: Int?,
    successPath: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    if (progressPct == null && successPath == null && errorMessage == null) return

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    errorMessage != null -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFFF4D4D),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Export Failed",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C3A))
                        ) {
                            Text("Dismiss", color = Color.White)
                        }
                    }

                    successPath != null -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Export Complete!",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Video saved directly to your Gallery / Photos app",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B5C)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    progressPct != null -> {
                        Text(
                            text = "Exporting Video",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Rendering with native C++ engine...",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(24.dp))

                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progressPct / 100f },
                                modifier = Modifier.size(90.dp),
                                color = Color(0xFFFF3B5C),
                                strokeWidth = 8.dp,
                                trackColor = Color(0xFF2C2C3A)
                            )
                            Text(
                                text = "$progressPct%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Please keep the app open",
                            color = Color(0xFF888896),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
