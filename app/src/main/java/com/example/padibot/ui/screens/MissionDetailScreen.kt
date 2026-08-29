package com.example.padibot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.Mission
import com.example.padibot.model.MissionEvent
import com.example.padibot.theme.*
import com.example.padibot.ui.components.LiveMissionCanvas
import com.example.padibot.ui.components.MissionStatusBadge
import com.example.padibot.viewmodel.PadiBotViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MissionDetailScreen(
    missionId: String,
    viewModel: PadiBotViewModel,
    onNavigateBack: () -> Unit,
    onRerunMission: () -> Unit = {}
) {
    val allMissions by viewModel.allMissions.collectAsState()
    val allFields by viewModel.allFields.collectAsState()
    val mission = allMissions.find { it.id == missionId }
    val field = allFields.find { it.id == mission?.fieldId }
    val events by viewModel.getEventsForMission(missionId).collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    if (mission == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Misi tidak ditemukan.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mission Overview Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(mission.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green900)
                            Text("Petak: ${mission.fieldName}", style = MaterialTheme.typography.bodySmall, color = Gray600)
                        }
                        MissionStatusBadge(status = mission.status)
                    }

                    Divider(color = Gray200)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Lebar Mesin", style = MaterialTheme.typography.labelSmall, color = Gray600)
                            Text("${String.format("%.0f cm", mission.machineWidthM * 100)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Jalur Tanam", style = MaterialTheme.typography.labelSmall, color = Gray600)
                            Text("${mission.totalLanes} Baris", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Jarak Tempuh", style = MaterialTheme.typography.labelSmall, color = Gray600)
                            Text(String.format("%.0f m", mission.estimatedDistanceM), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Durasi", style = MaterialTheme.typography.labelSmall, color = Gray600)
                            Text(mission.formatDuration(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live/Past Map Canvas
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Jalur Misi Terlaksana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    LiveMissionCanvas(
                        boundary = field?.boundary ?: emptyList(),
                        waypoints = mission.route,
                        telemetry = null,
                        modifier = Modifier.height(240.dp)
                    )
                }
            }
        }

        // Mission Audit Events Log
        item {
            Text("Log Peristiwa & Audit Misi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (events.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada log peristiwa untuk misi ini.", style = MaterialTheme.typography.bodySmall, color = Gray600)
                    }
                }
            }
        } else {
            items(events) { event ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (event.severity == "ERROR") ErrorRed else Green700
                            ) {
                                Text(
                                    text = event.eventType,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(text = event.message, style = MaterialTheme.typography.bodyMedium)
                        }

                        Text(
                            text = dateFormat.format(Date(event.timestamp)),
                            style = CoordinateFont,
                            fontSize = 11.sp,
                            color = Gray600
                        )
                    }
                }
            }
        }
    }
}
