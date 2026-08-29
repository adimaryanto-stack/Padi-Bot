package com.example.padibot.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.Mission
import com.example.padibot.model.MissionStatus
import com.example.padibot.theme.*
import com.example.padibot.ui.components.MissionStatusBadge
import com.example.padibot.viewmodel.PadiBotViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MissionHistoryScreen(
    viewModel: PadiBotViewModel,
    onNavigateToMissionDetail: (String) -> Unit = {},
    onNavigateToDetail: (String) -> Unit = onNavigateToMissionDetail
) {
    val allMissions by viewModel.allMissions.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    val filteredMissions = remember(allMissions, selectedFilter) {
        when (selectedFilter) {
            "COMPLETED" -> allMissions.filter { it.status == MissionStatus.COMPLETED }
            "RUNNING" -> allMissions.filter { it.status == MissionStatus.RUNNING || it.status == MissionStatus.PAUSED }
            "STOPPED" -> allMissions.filter { it.status == MissionStatus.STOPPED || it.status == MissionStatus.ERROR }
            else -> allMissions
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Green700, modifier = Modifier.size(32.dp))
                    Column {
                        Text("Riwayat & Log Misi Tanam", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${allMissions.size} Total Catatan Aktivitas Autonomous", style = MaterialTheme.typography.bodySmall, color = Gray600)
                    }
                }
            }
        }

        // Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Semua (${allMissions.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "COMPLETED",
                    onClick = { selectedFilter = "COMPLETED" },
                    label = { Text("Selesai") }
                )
                FilterChip(
                    selected = selectedFilter == "RUNNING",
                    onClick = { selectedFilter = "RUNNING" },
                    label = { Text("Aktif/Jeda") }
                )
                FilterChip(
                    selected = selectedFilter == "STOPPED",
                    onClick = { selectedFilter = "STOPPED" },
                    label = { Text("Berhenti") }
                )
            }
        }

        if (filteredMissions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tidak Ada Catatan Misi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Belum ada riwayat penanaman yang cocok dengan filter.", style = MaterialTheme.typography.bodySmall, color = Gray600)
                    }
                }
            }
        } else {
            items(filteredMissions) { mission ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (onNavigateToDetail != onNavigateToMissionDetail) {
                                onNavigateToDetail(mission.id)
                            } else {
                                onNavigateToMissionDetail(mission.id)
                            }
                        }
                        .testTag("mission_item_${mission.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mission.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Green900
                                )
                                Text(
                                    text = "Petak: ${mission.fieldName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray700
                                )
                            }
                            MissionStatusBadge(status = mission.status)
                        }

                        Divider(color = Gray200)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Jalur Tanam", style = MaterialTheme.typography.labelSmall, color = Gray600)
                                Text("${mission.totalLanes} Baris", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Cakupan", style = MaterialTheme.typography.labelSmall, color = Gray600)
                                Text("${String.format("%.1f", if (mission.actualCoveragePct > 0) mission.actualCoveragePct else mission.estimatedCoveragePct)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Green700)
                            }
                            Column {
                                Text("Durasi", style = MaterialTheme.typography.labelSmall, color = Gray600)
                                Text(mission.formatDuration(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Waktu", style = MaterialTheme.typography.labelSmall, color = Gray600)
                                Text(dateFormat.format(Date(mission.createdAt)), style = MaterialTheme.typography.bodySmall, color = Gray700)
                            }
                        }
                    }
                }
            }
        }
    }
}
