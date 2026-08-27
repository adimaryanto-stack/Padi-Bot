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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.theme.*
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun PlantingSettingsScreen(
    viewModel: PadiBotViewModel,
    onNavigateToPreview: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val selectedField by viewModel.selectedField.collectAsState()
    val machineWidth by viewModel.machineWidth.collectAsState()
    val headlandWidth by viewModel.headlandWidth.collectAsState()
    val laneOrientation by viewModel.laneOrientation.collectAsState()
    val generatedRoute by viewModel.generatedRoute.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Indicator
        item {
            StepIndicatorHeader(currentStep = 1)
        }

        // Active Field Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sawah Terpilih",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gray600
                    )
                    Text(
                        text = selectedField?.name ?: "Sawah Utama",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Green900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Luas: ${selectedField?.formatArea()} • ${selectedField?.boundary?.size ?: 0} Titik Batas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Machine Parameters Configuration Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Parameter Mesin & Pola Jalur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 1. Machine Width
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lebar Kerja Mesin (W)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format("%.2f m", machineWidth),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Green700
                            )
                        }
                        Slider(
                            value = machineWidth.toFloat(),
                            onValueChange = { viewModel.setMachineWidth(it.toDouble()) },
                            valueRange = 0.8f..3.0f,
                            steps = 21,
                            modifier = Modifier.testTag("slider_machine_width"),
                            colors = SliderDefaults.colors(
                                thumbColor = Green700,
                                activeTrackColor = Green700
                            )
                        )
                        Text(
                            text = "ℹ️ Standar mesin tanam padi di Indonesia: 1.20 - 2.00 meter",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    }

                    HorizontalDivider(color = Gray200)

                    // 2. Headland Width
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lebar Headland / Area Putar (H)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = String.format("%.2f m", headlandWidth),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WarningOrange
                            )
                        }
                        Slider(
                            value = headlandWidth.toFloat(),
                            onValueChange = { viewModel.setHeadlandWidth(it.toDouble()) },
                            valueRange = 1.0f..6.0f,
                            steps = 9,
                            modifier = Modifier.testTag("slider_headland_width"),
                            colors = SliderDefaults.colors(
                                thumbColor = WarningOrange,
                                activeTrackColor = WarningOrange
                            )
                        )
                        Text(
                            text = "ℹ️ Ruang bebas di tepi sawah untuk manuver putar mesin",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    }

                    HorizontalDivider(color = Gray200)

                    // 3. Lane Orientation
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Orientasi Sudut Jalur (θ)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${laneOrientation.toInt()}°",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = InfoBlue
                            )
                        }
                        Slider(
                            value = laneOrientation.toFloat(),
                            onValueChange = { viewModel.setLaneOrientation(it.toDouble()) },
                            valueRange = 0f..180f,
                            steps = 35,
                            modifier = Modifier.testTag("slider_lane_orientation"),
                            colors = SliderDefaults.colors(
                                thumbColor = InfoBlue,
                                activeTrackColor = InfoBlue
                            )
                        )
                        Text(
                            text = "ℹ️ Atur arah baris tanaman padi mengikuti kontur pematang",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    }
                }
            }
        }

        // Live Estimation Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Green50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Estimasi Hasil Perencanaan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Green900
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        EstimationTile(
                            label = "Jumlah Jalur",
                            value = "${generatedRoute?.totalLanes ?: 0} Jalur"
                        )
                        EstimationTile(
                            label = "Estimasi Jarak",
                            value = String.format("%.0f m", generatedRoute?.totalDistanceM ?: 0.0)
                        )
                        EstimationTile(
                            label = "Cakupan Area",
                            value = String.format("%.1f%%", generatedRoute?.coveragePct ?: 95.0)
                        )
                    }
                }
            }
        }

        // Generate Button
        item {
            Button(
                onClick = {
                    viewModel.recalculateRoute()
                    onNavigateToPreview()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("button_generate_route"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green700)
            ) {
                Text(
                    text = "Generate & Preview Jalur →",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StepIndicatorHeader(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepItem(step = 1, label = "1. Pengaturan", isActive = currentStep == 1, isDone = currentStep > 1)
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Gray400)
        StepItem(step = 2, label = "2. Preview", isActive = currentStep == 2, isDone = currentStep > 2)
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Gray400)
        StepItem(step = 3, label = "3. Eksekusi", isActive = currentStep == 3, isDone = currentStep > 3)
    }
}

@Composable
private fun StepItem(step: Int, label: String, isActive: Boolean, isDone: Boolean) {
    val color = when {
        isActive -> Green700
        isDone -> SuccessGreen
        else -> Gray600
    }
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
        color = color
    )
}

@Composable
private fun EstimationTile(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Gray600)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Green900
        )
    }
}
