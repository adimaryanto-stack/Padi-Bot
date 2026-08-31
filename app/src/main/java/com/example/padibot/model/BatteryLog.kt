package com.example.padibot.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Room database entity recording time-series battery telemetry for robot battery life tracking.
 */
@Entity(tableName = "battery_history_logs")
data class BatteryLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val batteryPct: Float = 100f,
    val batteryVoltageV: Float = 51.8f,
    val batteryCurrentA: Float = 7.5f,
    val powerDrawWatts: Float = 388.5f,
    val batteryTempC: Float = 31.5f,
    val isCharging: Boolean = false,
    val isPlantingActive: Boolean = false
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedShortTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
