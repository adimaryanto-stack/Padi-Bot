package com.example.padibot.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.ConnectionType
import com.example.padibot.model.MachineSettings
import com.example.padibot.theme.*
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun SettingsScreen(
    viewModel: PadiBotViewModel
) {
    val settings by viewModel.machineSettings.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    var wifiIp by remember(settings) { mutableStateOf(settings.wifiIp) }
    var wifiPort by remember(settings) { mutableStateOf(settings.wifiPort.toString()) }
    var btName by remember(settings) { mutableStateOf(settings.bluetoothDeviceName) }
    var mqttBroker by remember(settings) { mutableStateOf(settings.mqttBroker) }
    var mqttId by remember(settings) { mutableStateOf(settings.mqttDeviceId) }

    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Green700, modifier = Modifier.size(32.dp))
                    Column {
                        Text("Pengaturan Konektivitas & Alat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("ESP32 • RTK GPS • Simulator • IoT Cloud", style = MaterialTheme.typography.bodySmall, color = Gray600)
                    }
                }
            }
        }

        // Connection Mode Selection
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Protokol Komunikasi Mesin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ConnectionType.values().forEach { connType ->
                        val isSelected = settings.connectionType == connType
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Green50 else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Green700) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateMachineSettings(settings.copy(connectionType = connType))
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateMachineSettings(settings.copy(connectionType = connType))
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Green700)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = connType.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Green900 else Gray900
                                )
                            }
                        }
                    }
                }
            }
        }

        // Specific IP/Port/BT Settings
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Parameter Jaringan Hardware", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = wifiIp,
                        onValueChange = { wifiIp = it },
                        label = { Text("IP Address ESP32 WiFi") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = wifiPort,
                        onValueChange = { wifiPort = it },
                        label = { Text("Port Server (HTTP / WebSocket)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = btName,
                        onValueChange = { btName = it },
                        label = { Text("Nama Perangkat Bluetooth") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.updateMachineSettings(
                                settings.copy(
                                    wifiIp = wifiIp,
                                    wifiPort = wifiPort.toIntOrNull() ?: 80,
                                    bluetoothDeviceName = btName,
                                    mqttBroker = mqttBroker,
                                    mqttDeviceId = mqttId
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Green700),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Konfigurasi Jaringan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Safety Simulator Testing / Fault Injection
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Uji Simulasi Keamanan (Fault Injection)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Simulasikan skenario kegagalan sensor untuk menguji respon keselamatan otomatis:", style = MaterialTheme.typography.bodySmall, color = Gray700)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.injectError("GPS_LOSS") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Hilang GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.injectError("LOW_BATTERY") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Baterai Low", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.injectError("RESTORE") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Green100, contentColor = Green800)
                        ) {
                            Text("Normal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Database Management / Reset
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Penyimpanan & Database Lokal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Database Room SQLite menyimpan semua poligon sawah, trajektori tanam, dan catatan audit misi.", style = MaterialTheme.typography.bodySmall, color = Gray600)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetSampleData() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Muat Data Contoh")
                        }

                        Button(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Hapus Semua Data")
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Hapus Semua Data?", fontWeight = FontWeight.Bold) },
            text = { Text("Semua peta sawah dan riwayat misi akan dihapus permanen dari perangkat.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
