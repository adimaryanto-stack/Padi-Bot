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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.theme.*
import com.example.padibot.ui.components.LiveMissionCanvas
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun RoutePreviewScreen(
    viewModel: PadiBotViewModel,
    onNavigateToExecution: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val selectedField by viewModel.selectedField.collectAsState()
    val generatedRoute by viewModel.generatedRoute.collectAsState()
    val machineWidth by viewModel.machineWidth.collectAsState()
    val headlandWidth by viewModel.headlandWidth.collectAsState()
    val laneOrientation by viewModel.laneOrientation.collectAsState()

    var missionName by remember {
        mutableStateOf("Misi ${selectedField?.name ?: "Sawah"} - Tanam Padi")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Indicator
        item {
            StepIndicatorHeader(currentStep = 2)
        }

        // Live Route Visualization Canvas
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("route_preview_canvas_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visualisasi Jalur Boustrophedon",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("${generatedRoute?.totalLanes ?: 0} Jalur") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LiveMissionCanvas(
                        boundary = selectedField?.boundary ?: emptyList(),
                        waypoints = generatedRoute?.waypoints ?: emptyList(),
                        telemetry = null,
                        modifier = Modifier.height(260.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(color = Green800, label = "Batas Sawah")
                        LegendItem(color = RouteLanePlanned, label = "Jalur Tanam")
                        LegendItem(color = SuccessGreen, label = "Titik Awal (S)")
                        LegendItem(color = ErrorRed, label = "Titik Akhir (E)")
                    }
                }
            }
        }

        // Plan Metrics Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ringkasan Rencana Operasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBlock(
                            label = "Luas Area",
                            value = selectedField?.formatArea() ?: "0 m²"
                        )
                        MetricBlock(
                            label = "Total Jalur",
                            value = "${generatedRoute?.totalLanes ?: 0} Jalur"
                        )
                        MetricBlock(
                            label = "Cakupan Efektif",
                            value = String.format("%.1f%%", generatedRoute?.coveragePct ?: 95.0)
                        )
                    }

                    HorizontalDivider(color = Gray200)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBlock(
                            label = "Total Panjang Lintasan",
                            value = String.format("%.0f meter", generatedRoute?.totalDistanceM ?: 0.0)
                        )
                        MetricBlock(
                            label = "Estimasi Waktu Tanam",
                            value = "±${((generatedRoute?.estimatedDurationSec ?: 0) / 60).coerceAtLeast(1)} Menit"
                        )
                        MetricBlock(
                            label = "Orientasi Jalur",
                            value = "${laneOrientation.toInt()}°"
                        )
                    }
                }
            }
        }

        // Mission Name Input
        item {
            OutlinedTextField(
                value = missionName,
                onValueChange = { missionName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_mission_name"),
                label = { Text("Nama Misi") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Ubah Pengaturan", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        viewModel.approveMission(missionName) { mission ->
                            viewModel.startMissionExecution(mission)
                            onNavigateToExecution()
                        }
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(54.dp)
                        .testTag("button_approve_and_start_mission"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green700)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("✓ Approve & Mulai", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Gray600)
    }
}

@Composable
private fun MetricBlock(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Gray600)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Gray900
        )
    }
}
