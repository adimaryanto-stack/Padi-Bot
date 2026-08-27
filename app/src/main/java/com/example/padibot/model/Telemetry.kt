package com.example.padibot.model

import kotlinx.serialization.Serializable

@Serializable
enum class GpsStatus {
    NONE,
    GPS,
    DGPS,
    FLOAT,
    RTK;

    val label: String
        get() = when (this) {
            NONE -> "No GPS"
            GPS -> "GPS 3D"
            DGPS -> "DGPS"
            FLOAT -> "RTK Float"
            RTK -> "RTK Fix"
        }
}

@Serializable
data class Telemetry(
    val timestamp: Long = System.currentTimeMillis(),
    val positionLat: Double = -6.9234,
    val positionLon: Double = 107.6100,
    val positionAccuracyM: Float = 1.2f,
    val batteryPct: Float = 85.0f,
    val speedMps: Float = 0.75f,
    val headingDeg: Float = 0.0f,
    val gpsStatus: GpsStatus = GpsStatus.GPS,
    val missionProgressPct: Float = 0.0f,
    val currentLaneIndex: Int = 0,
    val totalLanes: Int = 0,
    val plantedAreaM2: Double = 0.0,
    val remainingMinutes: Int = 0
)
