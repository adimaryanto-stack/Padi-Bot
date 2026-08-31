package com.example.padibot.model

data class Telemetry(
    val latitude: Double = -6.923450,
    val longitude: Double = 107.610150,
    val headingDeg: Float = 0f,
    val speedMps: Float = 0f,
    val batteryPct: Float = 98f,
    val batteryVoltageV: Float = 51.8f,
    val batteryCurrentA: Float = 7.5f,
    val batteryTempC: Float = 31.5f,
    val batteryHealthPct: Int = 98,
    val isCharging: Boolean = false,
    val missionProgressPct: Float = 0f,
    val rtkFixed: Boolean = true,
    val isPlantingActive: Boolean = false,
    val errorMsg: String? = null,
    val gpsStatus: String = "RTK Fix",
    val positionAccuracyM: Double = 0.02,
    val accuracyMeters: Double = 0.02,
    val lastUpdate: Long = System.currentTimeMillis()
) {
    /**
     * Estimated remaining operating time in minutes based on active mechanical load
     */
    val estimatedRemainingMinutes: Int
        get() {
            if (isCharging) return ((100f - batteryPct) * 1.5f).toInt().coerceAtLeast(1)
            val dischargeRatePerHour = when {
                isPlantingActive -> 18.5f // ~18.5% discharge per hour during active transplanter motor load
                speedMps > 0.1f -> 8.0f   // ~8% discharge per hour during cruising / transit
                else -> 1.8f              // ~1.8% discharge per hour on standby
            }
            return ((batteryPct / dischargeRatePerHour) * 60f).toInt().coerceAtLeast(1)
        }

    /**
     * Human-readable formatted string for remaining battery operational time
     */
    val estimatedRemainingTimeString: String
        get() {
            val totalMins = estimatedRemainingMinutes
            val hours = totalMins / 60
            val mins = totalMins % 60
            return if (hours > 0) {
                "${hours} jam ${mins} mnt"
            } else {
                "${mins} menit"
            }
        }

    /**
     * Estimated remaining plantable area in hectares
     */
    val estimatedRemainingPlantableHa: Double
        get() {
            val totalHours = estimatedRemainingMinutes / 60.0
            // ~0.35 Hectare per hour at standard transplanter operational speed
            return (totalHours * 0.35).coerceAtLeast(0.0)
        }

    /**
     * Power consumption in Watts
     */
    val powerDrawWatts: Float
        get() = batteryVoltageV * batteryCurrentA
}

