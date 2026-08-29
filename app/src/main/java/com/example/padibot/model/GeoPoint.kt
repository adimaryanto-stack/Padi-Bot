package com.example.padibot.model

data class GeoPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    val lat: Double get() = latitude
    val lon: Double get() = longitude

    fun formatDisplay(): String {
        return String.format("%.6f, %.6f", latitude, longitude)
    }
}
