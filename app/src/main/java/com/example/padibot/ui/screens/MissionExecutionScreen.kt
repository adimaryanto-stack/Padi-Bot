package com.example.padibot.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.MissionStatus
import com.example.padibot.theme.*
import com.example.padibot.ui.components.EmergencyStopButton
import com.example.padibot.ui.components.GpsStatusBadge
import com.example.padibot.ui.components.LiveMissionCanvas
import com.example.padibot.ui.components.MissionStatusBadge
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun MissionExecutionScreen(
    viewModel: PadiBotViewModel,
    onNavigateBackToHome: () -> Unit
) {
    val activeMission by viewModel.activeMission.collectAsState()
    val missionStatus by viewModel.activeMissionStatus.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val selectedField by viewModel.selectedField.collectAsState()

    var showStopDialog by remember { mutableStateOf(false) }

    val progressPct = telemetry.missionProgressPct
    val isRunning = missionStatus == MissionStatus.RUNNING
    val isPaused = missionStatus == MissionStatus.PAUSED
    val isCompleted = missionStatus == MissionStatus.COMPLETED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Mission Header Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("mission_execution_header_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = activeMission?.name ?: "Misi Tanam Aktif",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedField?.name ?: "Sawah"} • ${telemetry.currentLaneIndex} / ${activeMission?.totalLanes ?: telemetry.totalLanes} Jalur",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    MissionStatusBadge(status = missionStatus)
                }
            }
        }

        // 2. Live Field Dynamic Canvas
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    LiveMissionCanvas(
                        boundary = selectedField?.boundary ?: emptyList(),
                        waypoints = activeMission?.route ?: emptyList(),
                        telemetry = telemetry,
                        modifier = Modifier.height(280.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Posisi Robot: ${String.format("%.6f, %.6f", telemetry.positionLat, telemetry.positionLon)}",
                            style = CoordinateFont,
                            color = Gray600
                        )
                        Text(
                            text = "Heading: ${telemetry.headingDeg.toInt()}°",
                            style = CoordinateFont,
                            color = Gray800
                        )
                    }
                }
            }
        }

        // 3. Compact Telemetry Grid Strip
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryStatItem(
                        icon = Icons.Default.BatteryChargingFull,
                        label = "Baterai",
                        value = "${telemetry.batteryPct.toInt()}%",
                        color = if (telemetry.batteryPct < 20f) ErrorRed else SuccessGreen
                    )
                    TelemetryStatItem(
                        icon = Icons.Default.LocationOn,
                        label = "GPS",
                        value = "Fix (±${String.format("%.1f", telemetry.positionAccuracyM)}m)",
                        color = Green700
                    )
                    TelemetryStatItem(
                        icon = Icons.Default.Speed,
                        label = "Kecepatan",
                        value = String.format("%.2f m/s", telemetry.speedMps),
                        color = InfoBlue
                    )
                    TelemetryStatItem(
                        icon = Icons.Default.ViewStream,
                        label = "Jalur",
                        value = "${telemetry.currentLaneIndex}/${activeMission?.totalLanes ?: telemetry.totalLanes}",
                        color = Gray900
                    )
                }
            }
        }

        // 4. Progress Bar & Time Remaining
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Green50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kemajuan Penanaman",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Green900
                        )
                        Text(
                            text = "${progressPct.toInt()}% Selesai",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Green700
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { (progressPct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = Green700,
                        trackColor = Gray200
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Estimasi Selesai: ±${telemetry.remainingMinutes} Menit",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                        Text(
                            text = "Area Tertanam: ±${telemetry.plantedAreaM2.toInt()} m²",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    }
                }
            }
        }

        // 5. Mission Control Buttons (Pause, Resume, Stop)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isRunning) {
                    Button(
                        onClick = { viewModel.pauseMissionExecution() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("button_pause_mission"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PAUSE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else if (isPaused) {
                    Button(
                        onClick = { viewModel.resumeMissionExecution() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("button_resume_mission"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green700)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESUME", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                OutlinedButton(
                    onClick = { showStopDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("button_stop_mission"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = ErrorRed)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("STOP MISI", fontWeight = FontWeight.Bold, color = ErrorRed)
                }
            }
        }

        // 6. Emergency Stop Safety Button
        item {
            EmergencyStopButton(
                onEmergencyStop = {
                    viewModel.triggerEmergencyStop()
                }
            )
        }
    }

    // Stop Mission Confirmation Dialog
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Hentikan Misi Tanam?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Mesin akan berhenti di posisi saat ini dan status misi akan disimpan sebagai dihentikan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.stopMissionExecution()
                        showStopDialog = false
                        onNavigateBackToHome()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Hentikan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStopDialog = false }) {
                    Text("Lanjutkan Misi")
                }
            }
        )
    }

    // Mission Completed Dialog
    if (isCompleted) {
        AlertDialog(
            onDismissRequest = onNavigateBackToHome,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Misi Tanam Selesai!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Seluruh jalur tanam pada sawah berhasil diselesaikan dengan sukses.")
                    Text("• Total Jalur: ${activeMission?.totalLanes} Jalur", fontWeight = FontWeight.SemiBold)
                    Text("• Estimasi Cakupan: 96.5%", fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = onNavigateBackToHome,
                    colors = ButtonDefaults.buttonColors(containerColor = Green700)
                ) {
                    Text("Kembali ke Beranda", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun TelemetryStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Gray600
        )
    }
}
