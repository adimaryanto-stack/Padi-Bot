package com.example.padibot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
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
    val currentSettings by viewModel.machineSettings.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    var connectionType by remember(currentSettings) { mutableStateOf(currentSettings.connectionType) }
    var wifiIp by remember(currentSettings) { mutableStateOf(currentSettings.wifiIp) }
    var wifiPort by remember(currentSettings) { mutableStateOf(currentSettings.wifiPort.toString()) }
    var btName by remember(currentSettings) { mutableStateOf(currentSettings.bluetoothDeviceName) }
    var btMac by remember(currentSettings) { mutableStateOf(currentSettings.bluetoothDeviceMac) }
    var mqttBroker by remember(currentSettings) { mutableStateOf(currentSettings.mqttBroker) }
    var mqttDevice by remember(currentSettings) { mutableStateOf(currentSettings.mqttDeviceId) }

    var saveFeedback by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Connection Mode Selector
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_connection_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Antarmuka Komunikasi Mesin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ConnectionType.entries.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = connectionType == type,
                                onClick = { connectionType = type }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = type.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (connectionType == type) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Gray200)

                    // Specific Config per Connection Type
                    when (connectionType) {
                        ConnectionType.SIMULATOR -> {
                            Text(
                                text = "✓ Mode Simulator Aktif. Pengujian otonom virtual tanpa membutuhkan modul fisik.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Green700
                            )
                        }
                        ConnectionType.WIFI -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = wifiIp,
                                    onValueChange = { wifiIp = it },
                                    label = { Text("IP Address Modul ESP") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = wifiPort,
                                    onValueChange = { wifiPort = it },
                                    label = { Text("Port Komunikasi") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                        ConnectionType.BLUETOOTH -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = btName,
                                    onValueChange = { btName = it },
                                    label = { Text("Nama Perangkat Bluetooth") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = btMac,
                                    onValueChange = { btMac = it },
                                    label = { Text("MAC Address (e.g. 00:11:22:33:44:55)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                        ConnectionType.GSM_MQTT -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = mqttBroker,
                                    onValueChange = { mqttBroker = it },
                                    label = { Text("MQTT Broker Host") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = mqttDevice,
                                    onValueChange = { mqttDevice = it },
                                    label = { Text("Device ID Topik") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val newSet = currentSettings.copy(
                                connectionType = connectionType,
                                wifiIp = wifiIp,
                                wifiPort = wifiPort.toIntOrNull() ?: 80,
                                bluetoothDeviceName = btName,
                                bluetoothDeviceMac = btMac,
                                mqttBroker = mqttBroker,
                                mqttDeviceId = mqttDevice
                            )
                            viewModel.updateMachineSettings(newSet)
                            saveFeedback = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Green700),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simpan Konfigurasi Komunikasi", fontWeight = FontWeight.Bold)
                    }

                    if (saveFeedback) {
                        Text(
                            text = "✓ Pengaturan berhasil disimpan",
                            color = Green700,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Application Theme Settings Card (Versi Light Default)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tema Tampilan Aplikasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pilih skema warna antarmuka untuk kenyamanan penggunaan di bawah terik matahari lapangan:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray600
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isDarkTheme) Green100 else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (!isDarkTheme) BorderStroke(2.dp, Green700) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setDarkTheme(false) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🌞", fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Versi Light (Terang)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (!isDarkTheme) Green900 else Gray800
                                )
                                Text("Aktif (Default)", fontSize = 10.sp, color = Green700, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Green100 else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isDarkTheme) BorderStroke(2.dp, Green700) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setDarkTheme(true) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🌙", fontSize = 24.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Versi Dark (Gelap)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDarkTheme) Green900 else Gray800
                                )
                                Text("Mode Malam", fontSize = 10.sp, color = Gray600)
                            }
                        }
                    }
                }
            }
        }

        // 3. Simulator Fault Injection Testing
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pengujian Keandalan (Fault Injection)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Uji respons sistem keselamatan saat terjadi anomali di lapangan:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray600
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.injectSimulatorError("GPS_LOSS") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hilang GPS", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.injectSimulatorError("LOW_BATTERY") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Baterai Drop", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.injectSimulatorError("CONNECTION_DROP") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Putus Sinyal", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.injectSimulatorError("RESTORE") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Green700)
                        ) {
                            Text("Pulihkan Normal", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 4. Database Management & Reset
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Manajemen Data Lokal (Room Database)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Database offline tersimpan secara persisten pada perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray600
                    )

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset & Muat Ulang Data Sampel")
                    }
                }
            }
        }

        // 5. About App
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PadiBot Mobile GCS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Versi 1.1.0 • Build 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray600
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sistem Kontrol Mesin Tanam Padi Otonom Berbasis Android, RTK GNSS & Arduino",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray700
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Konfirmasi Reset Database", fontWeight = FontWeight.Bold) },
            text = {
                Text("Semua data petak sawah dan riwayat misi yang telah disimpan akan dikembalikan ke data sampel awal.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllUserData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Reset Sekarang")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
