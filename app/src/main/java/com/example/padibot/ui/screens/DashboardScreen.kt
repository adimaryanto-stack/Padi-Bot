package com.example.padibot.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.R
import com.example.padibot.model.Field
import com.example.padibot.model.Mission
import com.example.padibot.theme.*
import com.example.padibot.ui.components.EmergencyStopButton
import com.example.padibot.ui.components.GpsStatusBadge
import com.example.padibot.ui.components.MissionStatusBadge
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun DashboardScreen(
    viewModel: PadiBotViewModel,
    onNavigateToPlantingSettings: () -> Unit,
    onNavigateToManualControl: () -> Unit,
    onNavigateToFields: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMissionDetail: (String) -> Unit
) {
    val selectedField by viewModel.selectedField.collectAsState()
    val allFields by viewModel.allFields.collectAsState()
    val allMissions by viewModel.allMissions.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val isConnected by viewModel.isMachineConnected.collectAsState()
    val settings by viewModel.machineSettings.collectAsState()

    var showFieldSelectorDialog by remember { mutableStateOf(false) }
    val lastMission = allMissions.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Card Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("dashboard_hero_card")
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_hero_paddy),
                            contentDescription = "PadiBot Sawah Hero",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "PadiBot Smart Planter",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Presisi Pertanian • Otonom & Efisien",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Machine Telemetry Status Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("dashboard_telemetry_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status Mesin Tanam",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        GpsStatusBadge(
                            gpsStatus = telemetry.gpsStatus,
                            accuracyM = telemetry.positionAccuracyM
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryTile(
                            label = "Baterai Mesin",
                            value = "${telemetry.batteryPct.toInt()}%",
                            icon = Icons.Default.BatteryChargingFull,
                            tint = if (telemetry.batteryPct < 20f) ErrorRed else SuccessGreen
                        )
                        TelemetryTile(
                            label = "Kecepatan",
                            value = String.format("%.2f m/s", telemetry.speedMps),
                            icon = Icons.Default.Speed,
                            tint = InfoBlue
                        )
                        TelemetryTile(
                            label = "Mode Komunikasi",
                            value = when (settings.connectionType) {
                                com.example.padibot.model.ConnectionType.SIMULATOR -> "Simulator"
                                com.example.padibot.model.ConnectionType.WIFI -> "WiFi"
                                com.example.padibot.model.ConnectionType.BLUETOOTH -> "Bluetooth"
                                com.example.padibot.model.ConnectionType.GSM_MQTT -> "GSM 4G"
                            },
                            icon = Icons.Default.Sensors,
                            tint = Green700
                        )
                    }
                }
            }
        }

        // 3. Active Field Selection Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("dashboard_active_field_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Grass,
                                contentDescription = null,
                                tint = Green700,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sawah Aktif",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = { showFieldSelectorDialog = true },
                            modifier = Modifier.testTag("button_switch_field")
                        ) {
                            Text("Ganti Sawah", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (selectedField != null) {
                        val field = selectedField!!
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = field.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Green900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "📐 Luas: ${field.formatArea()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "📍 ${field.boundary.size} Titik Batas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Belum ada sawah dipilih",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray600
                        )
                    }
                }
            }
        }

        // 4. Primary Action: MULAI MISI BARU
        item {
            Button(
                onClick = onNavigateToPlantingSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("button_start_new_mission"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green700)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🌾  MULAI MISI BARU",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 5. Secondary Quick Action Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToManualControl,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("button_manual_control"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Games, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kontrol Manual", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onNavigateToFields,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("button_view_fields"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Peta Sawah", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 6. Last Mission Card
        if (lastMission != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMissionDetail(lastMission.id) }
                        .testTag("dashboard_last_mission_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Misi Terakhir",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            MissionStatusBadge(status = lastMission.status)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = lastMission.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Cakupan: ${String.format("%.1f%%", lastMission.actualCoveragePct.takeIf { it > 0 } ?: lastMission.estimatedCoveragePct)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Green700,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Jalur: ${lastMission.totalLanes} Jalur",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 7. Safety Emergency Stop
        item {
            EmergencyStopButton(
                onEmergencyStop = {
                    viewModel.triggerEmergencyStop()
                }
            )
        }
    }

    // Switch Field Dialog
    if (showFieldSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showFieldSelectorDialog = false },
            title = { Text("Pilih Sawah Aktif", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allFields.forEach { field ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectField(field)
                                    showFieldSelectorDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedField?.id == field.id) Green100 else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = field.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${field.formatArea()} • ${field.boundary.size} titik",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (selectedField?.id == field.id) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Green700
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFieldSelectorDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun TelemetryTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Gray600
        )
    }
}
