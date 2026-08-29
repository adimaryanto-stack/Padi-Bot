package com.example.padibot.model

data class Telemetry(
    val latitude: Double = -6.923450,
    val longitude: Double = 107.610150,
    val headingDeg: Float = 0f,
    val speedMps: Float = 0f,
    val batteryPct: Float = 98f,
    val missionProgressPct: Float = 0f,
    val rtkFixed: Boolean = true,
    val isPlantingActive: Boolean = false,
    val errorMsg: String? = null,
    val gpsStatus: String = "RTK Fix",
    val positionAccuracyM: Double = 0.02,
    val accuracyMeters: Double = 0.02,
    val lastUpdate: Long = System.currentTimeMillis()
)
