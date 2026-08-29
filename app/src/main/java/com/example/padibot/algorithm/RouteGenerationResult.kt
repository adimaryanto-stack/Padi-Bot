package com.example.padibot.algorithm

import com.example.padibot.model.GeoPoint

data class RouteGenerationResult(
    val waypoints: List<GeoPoint>,
    val totalDistanceM: Double,
    val totalLanes: Int,
    val coveragePct: Double
)
