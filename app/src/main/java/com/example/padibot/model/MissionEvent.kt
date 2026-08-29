package com.example.padibot.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mission_events")
data class MissionEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val missionId: String,
    val eventType: String,
    val message: String,
    val severity: String,
    val timestamp: Long = System.currentTimeMillis()
)
