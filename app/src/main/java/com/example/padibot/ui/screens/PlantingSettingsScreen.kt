package com.example.padibot.ui.screens

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
import com.example.padibot.model.RoutePattern
import com.example.padibot.theme.*
import com.example.padibot.ui.components.*
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun PlantingSettingsScreen(
    viewModel: PadiBotViewModel,
    onNavigateToRoutePreview: () -> Unit = {},
    onNavigateToPreview: () -> Unit = onNavigateToRoutePreview,
    onNavigateToCreateField: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val selectedField by viewModel.selectedField.collectAsState()
    val machineWidth by viewModel.machineWidth.collectAsState()
    val headlandWidth by viewModel.headlandWidth.collectAsState()
    val laneOrientation by viewModel.laneOrientation.collectAsState()
    val selectedPattern by viewModel.selectedPattern.collectAsState()

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

        // Active Field Selection Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Petak Sawah Target",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (selectedField != null) {
                        Text(
                            text = selectedField!!.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Green800
                        )
                        Text(
                            text = "Luas: ${selectedField!!.formatArea()} • ${selectedField!!.boundary.size} Titik",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    } else {
                        Text(
                            text = "⚠️ Belum ada sawah yang dipilih!",
                            color = ErrorRed,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateToCreateField,
                            colors = ButtonDefaults.buttonColors(containerColor = Green700)
                        ) {
                            Text("+ Buat / Pilih Sawah")
                        }
                    }
                }
            }
        }

        // Route Pattern Selection
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pola Trajektori Tanam (Route Pattern)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    RoutePattern.values().forEach { pattern ->
                        val isSelected = selectedPattern == pattern
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Green50 else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Green700) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updatePattern(pattern) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updatePattern(pattern) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Green700)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = pattern.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Green900 else Gray900
                                    )
                                    Text(
                                        text = pattern.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gray600
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Machine & Headland Width Settings
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Spesifikasi Alat & Margin Headland",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Machine Width Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lebar Kerja Mesin (Row Spacing):", style = MaterialTheme.typography.bodySmall)
                            Text("${String.format("%.0f cm", machineWidth * 100)} (${String.format("%.2f m", machineWidth)})", fontWeight = FontWeight.Bold, color = Green700)
                        }
                        Slider(
                            value = machineWidth.toFloat(),
                            onValueChange = { viewModel.updateMachineWidth(it.toDouble()) },
                            valueRange = 0.6f..2.5f,
                            steps = 18,
                            colors = SliderDefaults.colors(thumbColor = Green700, activeTrackColor = Green600)
                        )
                    }

                    // Headland Margin Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lebar Margin Putaran (Headland):", style = MaterialTheme.typography.bodySmall)
                            Text("${String.format("%.2f meter", headlandWidth)}", fontWeight = FontWeight.Bold, color = Green700)
                        }
                        Slider(
                            value = headlandWidth.toFloat(),
                            onValueChange = { viewModel.updateHeadlandWidth(it.toDouble()) },
                            valueRange = 1.0f..4.0f,
                            steps = 11,
                            colors = SliderDefaults.colors(thumbColor = Green700, activeTrackColor = Green600)
                        )
                    }

                    // Lane Orientation Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Orientasi Arah Baris (Azimuth):", style = MaterialTheme.typography.bodySmall)
                            Text("${laneOrientation.toInt()}°", fontWeight = FontWeight.Bold, color = Green700)
                        }
                        Slider(
                            value = laneOrientation.toFloat(),
                            onValueChange = { viewModel.updateLaneOrientation(it.toDouble()) },
                            valueRange = 0f..180f,
                            steps = 35,
                            colors = SliderDefaults.colors(thumbColor = Green700, activeTrackColor = Green600)
                        )
                    }
                }
            }
        }

        // Generate and Preview Button
        item {
            Button(
                onClick = {
                    viewModel.generateRoute()
                    if (onNavigateToPreview != onNavigateToRoutePreview) {
                        onNavigateToPreview()
                    } else {
                        onNavigateToRoutePreview()
                    }
                },
                enabled = selectedField != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("button_generate_route"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green700)
            ) {
                Icon(imageVector = Icons.Default.AltRoute, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Jalur Trajektori →", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
