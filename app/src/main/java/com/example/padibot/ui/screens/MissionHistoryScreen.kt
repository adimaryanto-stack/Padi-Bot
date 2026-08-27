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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.model.Mission
import com.example.padibot.model.MissionStatus
import com.example.padibot.theme.ErrorRed
import com.example.padibot.theme.Green700
import com.example.padibot.ui.components.MissionStatusBadge
import com.example.padibot.viewmodel.PadiBotViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MissionHistoryScreen(
    viewModel: PadiBotViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val missions by viewModel.allMissions.collectAsState()
    var selectedFilter by remember { mutableStateOf(0) } // 0: Semua, 1: Selesai, 2: Lainnya
    var missionToDelete by remember { mutableStateOf<Mission?>(null) }

    val filteredMissions = missions.filter {
        when (selectedFilter) {
            1 -> it.status == MissionStatus.COMPLETED
            2 -> it.status != MissionStatus.COMPLETED
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == 0,
                onClick = { selectedFilter = 0 },
                label = { Text("Semua (${missions.size})") }
            )
            FilterChip(
                selected = selectedFilter == 1,
                onClick = { selectedFilter = 1 },
                label = { Text("Selesai") }
            )
            FilterChip(
                selected = selectedFilter == 2,
                onClick = { selectedFilter = 2 },
                label = { Text("Draft / Lainnya") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredMissions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = Green700,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Belum Ada Riwayat Misi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Misi yang dijalankan akan tercatat otomatis di sini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredMissions, key = { it.id }) { mission ->
                    MissionCardItem(
                        mission = mission,
                        onClick = { onNavigateToDetail(mission.id) },
                        onDelete = { missionToDelete = mission }
                    )
                }
            }
        }
    }

    if (missionToDelete != null) {
        val m = missionToDelete!!
        AlertDialog(
            onDismissRequest = { missionToDelete = null },
            title = { Text("Hapus Riwayat Misi?", fontWeight = FontWeight.Bold) },
            text = { Text("Riwayat '${m.name}' akan dihapus secara permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMission(m.id)
                        missionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { missionToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun MissionCardItem(
    mission: Mission,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(mission.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        sdf.format(Date(mission.createdAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("mission_item_${mission.id}")
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "📅 $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MissionStatusBadge(status = mission.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Cakupan", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = String.format("%.1f%%", mission.actualCoveragePct.takeIf { it > 0 } ?: mission.estimatedCoveragePct),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Green700
                    )
                }

                Column {
                    Text("Jalur", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "${mission.totalLanes} Jalur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text("Jarak", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = mission.formatDistance(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text("Durasi", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = mission.formatDuration(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
