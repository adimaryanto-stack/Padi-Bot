package com.example.padibot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
    onNavigateBack: () -> Unit = {}
) {
    val telemetry by viewModel.telemetry.collectAsState()
    var speedMultiplier by remember { mutableStateOf(0.5f) }
    var activeDirection by remember { mutableStateOf(ManualDirection.STOP) }
    var isPlantingManual by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status overview
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mode Manual RC / Override",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Green900
                        )
                        GpsStatusBadge(gpsStatus = telemetry.gpsStatus, accuracyM = telemetry.positionAccuracyM)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Baterai: ${telemetry.batteryPct.toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Kecepatan: ${String.format("%.2f", telemetry.speedMps)} m/s", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Arah: ${telemetry.headingDeg.toInt()}°", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Virtual D-Pad Controller Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Kontrol Kemudi Traksi (D-Pad)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Gray800
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // D-Pad Grid
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // FORWARD
                        DPadButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            label = "MAJU",
                            isSelected = activeDirection == ManualDirection.FORWARD,
                            onPress = {
                                activeDirection = ManualDirection.FORWARD
                                viewModel.sendManualCommand(ManualDirection.FORWARD, speedMultiplier)
                            },
                            onRelease = {
                                activeDirection = ManualDirection.STOP
                                viewModel.sendManualCommand(ManualDirection.STOP, 0f)
                            },
                            modifier = Modifier.testTag("dpad_up")
                        )

                        // MIDDLE ROW (LEFT, STOP, RIGHT)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DPadButton(
                                icon = Icons.Default.KeyboardArrowLeft,
                                label = "KIRI",
                                isSelected = activeDirection == ManualDirection.LEFT,
                                onPress = {
                                    activeDirection = ManualDirection.LEFT
                                    viewModel.sendManualCommand(ManualDirection.LEFT, speedMultiplier)
                                },
                                onRelease = {
                                    activeDirection = ManualDirection.STOP
                                    viewModel.sendManualCommand(ManualDirection.STOP, 0f)
                                },
                                modifier = Modifier.testTag("dpad_left")
                            )

                            // Center STOP
                            Surface(
                                shape = CircleShape,
                                color = ErrorRed,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clickable {
                                        activeDirection = ManualDirection.STOP
                                        viewModel.sendManualCommand(ManualDirection.STOP, 0f)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("STOP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            DPadButton(
                                icon = Icons.Default.KeyboardArrowRight,
                                label = "KANAN",
                                isSelected = activeDirection == ManualDirection.RIGHT,
                                onPress = {
                                    activeDirection = ManualDirection.RIGHT
                                    viewModel.sendManualCommand(ManualDirection.RIGHT, speedMultiplier)
                                },
                                onRelease = {
                                    activeDirection = ManualDirection.STOP
                                    viewModel.sendManualCommand(ManualDirection.STOP, 0f)
                                },
                                modifier = Modifier.testTag("dpad_right")
                            )
                        }

                        // BACKWARD
                        DPadButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            label = "MUNDUR",
                            isSelected = activeDirection == ManualDirection.BACKWARD,
                            onPress = {
                                activeDirection = ManualDirection.BACKWARD
                                viewModel.sendManualCommand(ManualDirection.BACKWARD, speedMultiplier)
                            },
                            onRelease = {
                                activeDirection = ManualDirection.STOP
                                viewModel.sendManualCommand(ManualDirection.STOP, 0f)
                            },
                            modifier = Modifier.testTag("dpad_down")
                        )
                    }
                }
            }
        }

        // Speed Throttle Slider Card
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
                        Text("Batas Kecepatan Motor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${(speedMultiplier * 100).toInt()}% (${String.format("%.2f", speedMultiplier * 1.2)} m/s)",
                            fontWeight = FontWeight.Bold,
                            color = Green700
                        )
                    }

                    Slider(
                        value = speedMultiplier,
                        onValueChange = { speedMultiplier = it },
                        valueRange = 0.1f..1.0f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = Green700, activeTrackColor = Green600)
                    )
                }
            }
        }

        // Emergency Stop Button
        item {
            EmergencyStopButton(
                onEmergencyStop = {
                    viewModel.emergencyStop("Tombol Darurat Ditekan Saat Manual Control")
                }
            )
        }
    }
}

@Composable
fun DPadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Green700 else Green100,
        modifier = modifier
            .size(68.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Green900,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Green900
            )
        }
    }
}
