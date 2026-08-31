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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.R
import com.example.padibot.model.MissionStatus
import com.example.padibot.theme.*
import com.example.padibot.ui.components.*
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun DashboardScreen(
    viewModel: PadiBotViewModel,
    onNavigateToPlantingSettings: () -> Unit = {},
    onNavigateToManualControl: () -> Unit = {},
    onNavigateToFields: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToMissionDetail: (String) -> Unit = {},
    onNavigateToCreateField: () -> Unit = {},
    onNavigateToFieldList: () -> Unit = onNavigateToFields,
    onNavigateToExecution: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val selectedField by viewModel.selectedField.collectAsState()
    val activeMission by viewModel.activeMission.collectAsState()
    val missionStatus by viewModel.missionStatus.collectAsState()
    val allFields by viewModel.allFields.collectAsState()
    val batteryLogs by viewModel.batteryLogs.collectAsState()
    val firebaseSyncState by viewModel.firebaseSyncState.collectAsState()
    val machineSettings by viewModel.machineSettings.collectAsState()

    var showFirebaseDiagnostic by remember { mutableStateOf(false) }

    val locationPermState = rememberLocationPermissionState { granted ->
        if (granted) {
            viewModel.startGpsTracking()
        }
    }

    if (showFirebaseDiagnostic) {
        FirebaseDiagnosticDialog(
            syncState = firebaseSyncState,
            currentDbUrl = machineSettings.firebaseDbUrl,
            onDismiss = { showFirebaseDiagnostic = false },
            onTestConnection = { testUrl, onResult ->
                viewModel.testFirebaseConnection(testUrl, machineSettings.firebaseAuthToken, onResult)
            },
            onSaveAndSync = { newUrl ->
                viewModel.updateMachineSettings(
                    machineSettings.copy(
                        firebaseDbUrl = newUrl,
                        firebaseAutoSync = true
                    )
                )
                viewModel.syncAllDataToFirebase()
            },
            onOpenSettings = {
                showFirebaseDiagnostic = false
                onNavigateToSettings()
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Location Permission Banner if not fully granted
        if (!locationPermState.isFullyGranted) {
            item {
                LocationPermissionCard(
                    state = locationPermState,
                    title = "Izin Lokasi Akurat (GPS/GNSS)",
                    description = "Izinkan akses lokasi presisi tinggi untuk memantau posisi robot dan menyinkronkan koordinat sawah secara real-time."
                )
            }
        }

        // Hero Card with Indonesian Rice Paddy Header & Quick Status
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Green900),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card")
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_hero_paddy),
                        contentDescription = "PadiBot Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentScale = ContentScale.Crop,
                        alpha = 0.4f
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PadiBot Autonomous",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Sistem Mesin Tanam Padi Pintar Berbasis GIS",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Green200
                                )
                            }

                            GpsStatusBadge(
                                gpsStatus = telemetry.gpsStatus,
                                accuracyM = telemetry.positionAccuracyM
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status badges row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x33FFFFFF)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BatteryChargingFull,
                                        contentDescription = null,
                                        tint = if (telemetry.batteryPct > 20) SuccessGreen else ErrorRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${telemetry.batteryPct.toInt()}%",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x33FFFFFF)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = InfoBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${String.format("%.2f", telemetry.speedMps)} m/s",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            MissionStatusBadge(status = missionStatus)

                            FirebaseStatusBadge(
                                syncState = firebaseSyncState,
                                onClick = { showFirebaseDiagnostic = true }
                            )
                        }
                    }
                }
            }
        }

        // Low Battery Visual Alert Banner (< 20%)
        item {
            BatteryAlertBanner(
                telemetry = telemetry,
                onPauseMission = { viewModel.pauseMission() }
            )
        }

        // Active Mission Card (if running/paused)
        item {
            if (activeMission != null && (missionStatus == MissionStatus.RUNNING || missionStatus == MissionStatus.PAUSED)) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Misi Sedang Berjalan",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = activeMission?.name ?: "Penanaman Padi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Button(
                                onClick = onNavigateToExecution,
                                colors = ButtonDefaults.buttonColors(containerColor = Green700)
                            ) {
                                Text("Buka Monitor →", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { telemetry.missionProgressPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Green700,
                            trackColor = Green100,
                        )
                    }
                }
            }
        }

        // Robot Battery & Power Monitoring Card
        item {
            BatteryMonitorCard(
                telemetry = telemetry,
                missionStatus = missionStatus,
                onViewHistory = onNavigateToHistory
            )
        }

        // Time-series Battery History Chart (Stored in SQLite Database)
        item {
            BatteryHistoryChartCard(
                batteryLogs = batteryLogs,
                currentTelemetry = telemetry,
                onRecordSample = { viewModel.recordBatterySample() },
                onClearLogs = { viewModel.clearBatteryLogs() }
            )
        }

        // Quick Primary Actions
        item {
            Text(
                text = "Aksi Cepat Operasional",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Gray900
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionDashboardCard(
                    title = "Petakan Sawah",
                    subtitle = "Gambar / GPS",
                    icon = Icons.Default.AddLocationAlt,
                    color = Green700,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_map_field"),
                    onClick = onNavigateToCreateField
                )

                ActionDashboardCard(
                    title = "Rencana Tanam",
                    subtitle = "Jalur Otomatis",
                    icon = Icons.Default.AltRoute,
                    color = Green800,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_plan_route"),
                    onClick = onNavigateToPlantingSettings
                )

                ActionDashboardCard(
                    title = "Manual RC",
                    subtitle = "D-Pad Joystick",
                    icon = Icons.Default.SportsEsports,
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_manual_rc"),
                    onClick = onNavigateToManualControl
                )
            }
        }

        // Selected Field or Field Selector Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Agriculture, contentDescription = null, tint = Green700)
                            Text(
                                text = "Petak Sawah Terpilih",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = onNavigateToFields) {
                            Text("Ganti Sawah (${allFields.size})")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (selectedField != null) {
                        val field = selectedField!!
                        Text(
                            text = field.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Green800
                        )
                        Text(
                            text = "📐 Luas: ${field.formatArea()} • ${field.boundary.size} Batas" + if (field.markers.isNotEmpty()) " • ${field.markers.size} Penanda 🚩" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        FieldMapCanvas(
                            boundary = field.boundary,
                            markers = field.markers,
                            heightDp = 140
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onNavigateToPlantingSettings,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Green700),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Grass, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mulai Konfigurasi Penanaman Sawah Ini", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🌱", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Belum Ada Petak Sawah Terpilih", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onNavigateToCreateField,
                                    colors = ButtonDefaults.buttonColors(containerColor = Green700)
                                ) {
                                    Text("+ Buat Peta Sawah Pertama")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Emergency Stop Button
        item {
            EmergencyStopButton(
                onEmergencyStop = {
                    viewModel.emergencyStop("Tombol Darurat Ditekan dari Dashboard")
                }
            )
        }
    }
}
