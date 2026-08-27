package com.example.padibot.model

import kotlinx.serialization.Serializable

@Serializable
enum class MissionStatus {
    DRAFT,
    READY,
    RUNNING,
    PAUSED,
    COMPLETED,
    STOPPED,
    ERROR;

    val label: String
        get() = when (this) {
            DRAFT -> "Draft"
            READY -> "Siap"
            RUNNING -> "Berjalan"
            PAUSED -> "Dijeda"
            COMPLETED -> "Selesai"
            STOPPED -> "Dihentikan"
            ERROR -> "Error"
        }
}

@Serializable
data class Mission(
    val id: String,
    val fieldId: String,
    val fieldName: String = "",
    val name: String,
    val status: MissionStatus = MissionStatus.DRAFT,
    val route: List<Waypoint> = emptyList(),
    val machineWidthM: Double = 1.5,
    val headlandWidthM: Double = 3.0,
    val laneOrientationDeg: Double = 0.0,
    val totalLanes: Int = 0,
    val estimatedDistanceM: Double = 0.0,
    val estimatedCoveragePct: Double = 95.0,
    val actualCoveragePct: Double = 0.0,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formatDuration(): String {
        val start = startedAt ?: return "00:00"
        val end = completedAt ?: System.currentTimeMillis()
        val totalSecs = ((end - start) / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    fun formatDistance(): String {
        return if (estimatedDistanceM >= 1000) {
            String.format("%.2f km", estimatedDistanceM / 1000.0)
        } else {
            String.format("%.0f m", estimatedDistanceM)
        }
    }
}
