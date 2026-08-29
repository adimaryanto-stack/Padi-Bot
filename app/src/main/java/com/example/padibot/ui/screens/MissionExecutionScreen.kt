package com.example.padibot.ui.screens

import androidx.compose.foundation.background
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
import com.example.padibot.model.MissionStatus
import com.example.padibot.theme.*
import com.example.padibot.ui.components.*
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun MissionExecutionScreen(
    viewModel: PadiBotViewModel,
    onNavigateToManualControl: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateBackToHome: () -> Unit = onNavigateToHistory
) {
    val activeMission by viewModel.activeMission.collectAsState()
    val missionStatus by viewModel.missionStatus.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val selectedField by viewModel.selectedField.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status and Progress Header
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
                        Column {
                            Text(
                                text = activeMission?.name ?: "Misi Tanam Aktif",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Green900
                            )
                            Text(
                                text = "Petak: ${activeMission?.fieldName ?: selectedField?.name ?: "Sawah"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray600
                            )
                        }
                        MissionStatusBadge(status = missionStatus)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Progress Penanaman", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${String.format("%.1f", telemetry.missionProgressPct)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Green700
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { telemetry.missionProgressPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = Green600,
                        trackColor = Green100,
                    )
                }
            }
        }

        // Live Telemetry Grid
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Telemetri Mesin Realtime", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        GpsStatusBadge(gpsStatus = telemetry.gpsStatus, accuracyM = telemetry.positionAccuracyM)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryMetricCard(
                            label = "Baterai",
                            value = "${telemetry.batteryPct.toInt()}%",
                            icon = Icons.Default.BatteryChargingFull,
                            color = if (telemetry.batteryPct > 20f) SuccessGreen else ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMetricCard(
                            label = "Kecepatan",
                            value = "${String.format("%.2f", telemetry.speedMps)} m/s",
                            icon = Icons.Default.Speed,
                            color = InfoBlue,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMetricCard(
                            label = "Heading",
                            value = "${telemetry.headingDeg.toInt()}°",
                            icon = Icons.Default.Navigation,
                            color = WarningOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (telemetry.isPlantingActive) Green100 else Gray200,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (telemetry.isPlantingActive) "🌱" else "⏸️", fontSize = 16.sp)
                                Text(
                                    text = if (telemetry.isPlantingActive) "Mekanisme Tanam: AKTIF (Bibit Tertanam)" else "Mekanisme Tanam: STANDBY",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (telemetry.isPlantingActive) Green900 else Gray800
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live GIS Map with Machine Position
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Pelacakan Posisi Otonom",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LiveMissionCanvas(
                        boundary = selectedField?.boundary ?: emptyList(),
                        waypoints = activeMission?.route ?: emptyList(),
                        telemetry = telemetry,
                        modifier = Modifier.height(280.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Koordinat: ${String.format("%.6f", telemetry.latitude)}, ${String.format("%.6f", telemetry.longitude)}",
                        style = CoordinateFont,
                        fontSize = 11.sp,
                        color = Gray600
                    )
                }
            }
        }

        // Mission Control Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (missionStatus == MissionStatus.RUNNING) {
                    Button(
                        onClick = { viewModel.pauseMission() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("button_pause_mission"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Jeda Misi", fontWeight = FontWeight.Bold)
                    }
                } else if (missionStatus == MissionStatus.PAUSED || missionStatus == MissionStatus.READY) {
                    Button(
                        onClick = { viewModel.resumeMission() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("button_resume_mission"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green700)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lanjutkan", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onNavigateToManualControl,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("button_switch_manual"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.SportsEsports, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual D-Pad", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Emergency Stop Button
        item {
            EmergencyStopButton(
                onEmergencyStop = {
                    viewModel.emergencyStop("Tombol Darurat Ditekan Operator")
                }
            )
        }
    }
}

/**
 * Composable alias for ExecutionView
 */
@Composable
fun ExecutionView(
    viewModel: PadiBotViewModel,
    onNavigateToManualControl: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateBackToHome: () -> Unit = onNavigateToHistory
) {
    MissionExecutionScreen(
        viewModel = viewModel,
        onNavigateToManualControl = onNavigateToManualControl,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateBackToHome = onNavigateBackToHome
    )
}

