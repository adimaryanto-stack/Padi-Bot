package com.example.padibot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import com.example.padibot.service.ManualDirection
import com.example.padibot.theme.*
import com.example.padibot.ui.components.EmergencyStopButton
import com.example.padibot.ui.components.GpsStatusBadge
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun ManualControlScreen(
    viewModel: PadiBotViewModel,
    onNavigateBack: () -> Unit
) {
    val telemetry by viewModel.telemetry.collectAsState()
    var speedMode by remember { mutableStateOf(1) } // 0: Slow, 1: Normal, 2: Fast
    val speedFactor = when (speedMode) {
        0 -> 0.4f
        1 -> 0.75f
        else -> 1.0f
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Strip
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("manual_control_status_card")
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
                            text = "Posisi: ${String.format("%.6f, %.6f", telemetry.positionLat, telemetry.positionLon)}",
                            style = CoordinateFont
                        )
                        Text(
                            text = "Arah: ${telemetry.headingDeg.toInt()}° • Kecepatan: ${String.format("%.2f m/s", telemetry.speedMps)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    }

                    GpsStatusBadge(
                        gpsStatus = telemetry.gpsStatus,
                        accuracyM = telemetry.positionAccuracyM
                    )
                }
            }
        }

        // Speed Selector Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kecepatan Manual: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = speedMode == 0,
                    onClick = { speedMode = 0 },
                    label = { Text("Lambat") }
                )
                Spacer(modifier = Modifier.width(6.dp))
                FilterChip(
                    selected = speedMode == 1,
                    onClick = { speedMode = 1 },
                    label = { Text("Normal") }
                )
                Spacer(modifier = Modifier.width(6.dp))
                FilterChip(
                    selected = speedMode == 2,
                    onClick = { speedMode = 2 },
                    label = { Text("Cepat") }
                )
            }
        }

        // Interactive D-Pad Controller
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Up Button
                    IconButton(
                        onClick = { viewModel.sendManualCommand(ManualDirection.FORWARD, speedFactor) },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Green700)
                            .testTag("dpad_button_forward")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Maju",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Middle Row: Left, Stop, Right
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.sendManualCommand(ManualDirection.LEFT, speedFactor) },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Green700)
                                .testTag("dpad_button_left")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Belok Kiri",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // STOP Center Button
                        IconButton(
                            onClick = { viewModel.sendManualCommand(ManualDirection.STOP, speedFactor) },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(ErrorRed)
                                .testTag("dpad_button_stop")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Berhenti",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.sendManualCommand(ManualDirection.RIGHT, speedFactor) },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Green700)
                                .testTag("dpad_button_right")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Belok Kanan",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Down Button
                    IconButton(
                        onClick = { viewModel.sendManualCommand(ManualDirection.BACKWARD, speedFactor) },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Green700)
                            .testTag("dpad_button_backward")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Mundur",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        // Safety E-Stop
        item {
            EmergencyStopButton(
                onEmergencyStop = { viewModel.triggerEmergencyStop() }
            )
        }
    }
}
