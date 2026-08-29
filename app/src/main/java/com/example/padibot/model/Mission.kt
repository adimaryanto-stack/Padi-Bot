package com.example.padibot.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "missions")
data class Mission(
    @PrimaryKey val id: String,
    val fieldId: String,
    val fieldName: String,
    val name: String,
    val status: MissionStatus,
    val route: List<GeoPoint>,
    val machineWidthM: Double,
    val headlandWidthM: Double,
    val laneOrientationDeg: Double,
    val totalLanes: Int,
    val estimatedDistanceM: Double,
    val estimatedCoveragePct: Double,
    val actualCoveragePct: Double = 0.0,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formatDuration(): String {
        val start = startedAt ?: return "Belum mulai"
        val end = completedAt ?: System.currentTimeMillis()
        val sec = ((end - start) / 1000).coerceAtLeast(0)
        val m = sec / 60
        val s = sec % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
