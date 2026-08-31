package com.example.padibot.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.padibot.service.FirebaseStatusLevel
import com.example.padibot.service.FirebaseSyncState
import com.example.padibot.theme.*

@Composable
fun FirebaseDiagnosticDialog(
    syncState: FirebaseSyncState,
    currentDbUrl: String,
    onDismiss: () -> Unit,
    onTestConnection: (url: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onSaveAndSync: (url: String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf(if (currentDbUrl.isNotBlank()) currentDbUrl else syncState.databaseUrl) }
    var isTesting by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WarningOrange.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = WarningOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Diagnostik Firebase Cloud",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Status & Konfigurasi Sinkronisasi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gray600
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Gray600)
                    }
                }

                HorizontalDivider(color = Gray200)

                // Current Status Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (syncState.status) {
                        FirebaseStatusLevel.CONNECTED -> Green100
                        FirebaseStatusLevel.CONNECTING -> InfoBlue.copy(alpha = 0.12f)
                        else -> Color(0xFFFEF2F2)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (syncState.status) {
                                FirebaseStatusLevel.CONNECTED -> Icons.Default.CloudDone
                                FirebaseStatusLevel.CONNECTING -> Icons.Default.Sync
                                else -> Icons.Default.CloudOff
                            },
                            contentDescription = null,
                            tint = when (syncState.status) {
                                FirebaseStatusLevel.CONNECTED -> SuccessGreen
                                FirebaseStatusLevel.CONNECTING -> InfoBlue
                                else -> ErrorRed
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (syncState.status) {
                                    FirebaseStatusLevel.CONNECTED -> "Status: Terhubung (Online)"
                                    FirebaseStatusLevel.CONNECTING -> "Status: Mencoba Menghubungkan..."
                                    FirebaseStatusLevel.ERROR -> "Status: Offline / Gagal Terhubung"
                                    FirebaseStatusLevel.DISCONNECTED -> "Status: Belum Dikonfigurasi"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = when (syncState.status) {
                                    FirebaseStatusLevel.CONNECTED -> Green900
                                    FirebaseStatusLevel.CONNECTING -> InfoBlue
                                    else -> ErrorRed
                                }
                            )
                            if (syncState.lastErrorMessage != null) {
                                Text(
                                    text = syncState.lastErrorMessage,
                                    fontSize = 11.sp,
                                    color = Gray700,
                                    lineHeight = 15.sp
                                )
                            } else if (syncState.status == FirebaseStatusLevel.CONNECTED) {
                                Text(
                                    text = "Latensi: ${syncState.lastLatencyMs}ms • Terakhir sync: beberapa saat lalu",
                                    fontSize = 11.sp,
                                    color = Green800
                                )
                            }
                        }
                    }
                }

                // URL Input Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "URL Realtime Database Proyek Anda:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray800
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("https://nama-project-default-rtdb...app") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (urlInput.isNotBlank()) {
                                IconButton(onClick = { urlInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )
                    Text(
                        text = "Tip: Cukup masukkan nama proyek Anda (misal 'padibot-jateng') atau URL lengkap dari Firebase Console.",
                        fontSize = 10.sp,
                        color = Gray600
                    )
                }

                // Live Test Feedback Banner
                if (testMessage != null) {
                    val (success, msg) = testMessage!!
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (success) Green100 else Color(0xFFFEF2F2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (success) SuccessGreen else ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (success) Green900 else ErrorRed
                            )
                        }
                    }
                }

                // Action Buttons: Uji Koneksi & Simpan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            testMessage = null
                            onTestConnection(urlInput) { success, msg ->
                                isTesting = false
                                testMessage = Pair(success, msg)
                            }
                        },
                        enabled = !isTesting && urlInput.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uji Ulang", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            onSaveAndSync(urlInput)
                            Toast.makeText(context, "URL Firebase Disimpan!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green700)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan & Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Step-by-step Quick Guide
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Gray100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Langkah Mengaktifkan Firebase:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Gray900
                            )
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText(
                                        "Firebase Rules",
                                        "{\n  \"rules\": {\n    \".read\": true,\n    \".write\": true\n  }\n}"
                                    )
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Aturan Firebase disalin!", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Green700)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin Rules", fontSize = 11.sp, color = Green700)
                            }
                        }

                        Text(
                            text = "1. Buka console.firebase.google.com di browser.\n" +
                                    "2. Pilih Build > Realtime Database > Buat Database.\n" +
                                    "3. Pada tab 'Rules', ubah .read dan .write menjadi true.\n" +
                                    "4. Salin URL database di atas dan paste ke aplikasi ini.",
                            fontSize = 10.sp,
                            color = Gray700,
                            lineHeight = 15.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "{\n  \"rules\": {\n    \".read\": true,\n    \".write\": true\n  }\n}",
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                // Offline-first Guarantee
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Green50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Green700, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Aplikasi PadiBot berjalan secara Offline-First. Semua data peta sawah & rute tanam Anda tetap tersimpan aman di HP Redmi Anda meskipun tanpa koneksi Firebase.",
                            fontSize = 10.sp,
                            color = Green900,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Detailed Settings Link
                TextButton(
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = Gray700)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka Pengaturan Lengkap & Auth Token", fontSize = 12.sp, color = Gray700)
                }
            }
        }
    }
}
