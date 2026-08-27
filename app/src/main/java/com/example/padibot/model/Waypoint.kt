package com.example.padibot.model

import kotlinx.serialization.Serializable

@Serializable
enum class WaypointType {
    START,
    PLANTING,
    TRANSITION,
    END
}

@Serializable
data class Waypoint(
    val lat: Double,
    val lon: Double,
    val order: Int,
    val type: WaypointType = WaypointType.PLANTING,
    val laneIndex: Int = 0
)
