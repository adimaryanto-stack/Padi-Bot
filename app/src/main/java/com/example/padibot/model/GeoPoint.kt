package com.example.padibot.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(
    val lat: Double,
    val lon: Double
) {
    fun formatDisplay(): String {
        return String.format("%.6f, %.6f", lat, lon)
    }
}
