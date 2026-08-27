package com.example.padibot.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.Field
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
    onRerunMission: (Mission) -> Unit
) {
    val allMissions by viewModel.allMissions.collectAsState()
    val allFields by viewModel.allFields.collectAsState()
    val mission = allMissions.find { it.id == missionId }
    val field = allFields.find { it.id == mission?.fieldId }

    val eventsState = produceState<List<MissionEvent>>(initialValue = emptyList(), key1 = missionId) {
        viewModel.repository.getEvents(missionId).collect {
            value = it
        }
    }

    if (mission == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Misi tidak ditemukan")
        }
        return
    }

    val dateFormatted = remember(mission.createdAt) {
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
        sdf.format(Date(mission.createdAt))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("mission_detail_header_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mission.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "📅 $dateFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MissionStatusBadge(status = mission.status)
                    }
                }
            }
        }

        // Map Trajectory Canvas
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Visualisasi Lintasan Tanam (${field?.name ?: mission.fieldName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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

        // Comprehensive Audit Telemetry
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Laporan & Statistik Misi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailStatBox("Luas Sawah", field?.formatArea() ?: "0 m²")
                        DetailStatBox("Cakupan Riil", String.format("%.1f%%", mission.actualCoveragePct.takeIf { it > 0 } ?: mission.estimatedCoveragePct))
                        DetailStatBox("Total Jalur", "${mission.totalLanes} Jalur")
                    }

                    HorizontalDivider(color = Gray200)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailStatBox("Panjang Lintasan", mission.formatDistance())
                        DetailStatBox("Durasi Tanam", mission.formatDuration())
                        DetailStatBox("Lebar Mesin", String.format("%.2f m", mission.machineWidthM))
                    }
                }
            }
        }

        // Timeline Log Events
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Log Peristiwa & Riwayat Operasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (eventsState.value.isEmpty()) {
                        Text(
                            text = "Belum ada log peristiwa tercatat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray600
                        )
                    } else {
                        eventsState.value.forEach { event ->
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (event.severity) {
                                                "CRITICAL" -> ErrorRed.copy(alpha = 0.15f)
                                                "WARNING" -> WarningOrange.copy(alpha = 0.15f)
                                                else -> Green100
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = event.eventType,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (event.severity) {
                                            "CRITICAL" -> ErrorRed
                                            "WARNING" -> WarningOrange
                                            else -> Green800
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = event.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Gray600
                                )
                            }
                        }
                    }
                }
            }
        }

        // Re-run Button
        item {
            Button(
                onClick = {
                    if (field != null) {
                        viewModel.selectField(field)
                    }
                    viewModel.startMissionExecution(mission)
                    onRerunMission(mission)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("button_rerun_mission"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green700)
            ) {
                Icon(imageVector = Icons.Default.Replay, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Jalankan Ulang Misi Ini", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun DetailStatBox(label: String, value: String) {
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
