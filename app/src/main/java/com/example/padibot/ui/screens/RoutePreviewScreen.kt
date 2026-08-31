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
import com.example.padibot.theme.*
import com.example.padibot.ui.components.LiveMissionCanvas
import com.example.padibot.ui.components.StepIndicatorHeader
import com.example.padibot.viewmodel.PadiBotViewModel

@Composable
fun RoutePreviewScreen(
    viewModel: PadiBotViewModel,
    onNavigateToExecution: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val selectedField by viewModel.selectedField.collectAsState()
    val generatedRoute by viewModel.generatedRoute.collectAsState()
    val selectedPattern by viewModel.selectedPattern.collectAsState()
    val machineWidth by viewModel.machineWidth.collectAsState()
    val speedMps by viewModel.speedMps.collectAsState()
    val headlandWidth by viewModel.headlandWidth.collectAsState()
    val laneOrientation by viewModel.laneOrientation.collectAsState()

    var missionName by remember(selectedField) {
        mutableStateOf("Misi Tanam ${selectedField?.name ?: "Sawah"} #${System.currentTimeMillis() % 1000}")
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

        // Field Info
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = selectedField?.name ?: "Sawah Tanpa Nama",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Green900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pola: ${selectedPattern.title} • Lebar: ${String.format("%.0f cm", machineWidth * 100)} • Kecepatan: ${String.format("%.2f m/s", speedMps)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray700
                    )
                }
            }
        }

        // Live Route Visualization Canvas
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kanvas Jalur Trajektori",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(shape = RoundedCornerShape(6.dp), color = Green100) {
                            Text(
                                text = "${generatedRoute?.waypoints?.size ?: 0} Waypoint",
                                color = Green800,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LiveMissionCanvas(
                        boundary = selectedField?.boundary ?: emptyList(),
                        waypoints = generatedRoute?.waypoints ?: emptyList(),
                        markers = selectedField?.markers ?: emptyList(),
                        modifier = Modifier.height(280.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Green700, RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Batas Sawah", fontSize = 11.sp, color = Gray700)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF00E676), RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Jalur Tanam Otonom", fontSize = 11.sp, color = Gray700)
                        }
                    }
                }
            }
        }

        // Metrics Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Green900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ringkasan Parameter & Estimasi Hasil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Jalur Tanam", style = MaterialTheme.typography.labelSmall, color = Green300)
                            Text("${generatedRoute?.totalLanes ?: 0} Baris", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("Total Jarak Tempuh", style = MaterialTheme.typography.labelSmall, color = Green300)
                            Text(String.format("%.0f meter", generatedRoute?.totalDistanceM ?: 0.0), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("Estimasi Cakupan", style = MaterialTheme.typography.labelSmall, color = Green300)
                            Text("${generatedRoute?.coveragePct ?: 0.0}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green400)
                        }
                    }
                }
            }
        }

        // Mission Name Input
        item {
            OutlinedTextField(
                value = missionName,
                onValueChange = { missionName = it },
                label = { Text("Nama Misi Tanam") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Action Buttons
        item {
            Button(
                onClick = {
                    viewModel.approveMission(missionName) { approvedMission ->
                        viewModel.startMissionExecution(approvedMission)
                        onNavigateToExecution()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("button_approve_and_start"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green700)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🚀 Approve & Mulai Eksekusi Misi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Composable alias for SimulatorScreenView
 */
@Composable
fun SimulatorScreenView(
    viewModel: PadiBotViewModel,
    onNavigateToExecution: () -> Unit,
    onNavigateBack: () -> Unit
) {
    RoutePreviewScreen(
        viewModel = viewModel,
        onNavigateToExecution = onNavigateToExecution,
        onNavigateBack = onNavigateBack
    )
}

