package com.example.padibot.model

import kotlinx.serialization.Serializable

@Serializable
data class Field(
    val id: String,
    val name: String,
    val boundary: List<GeoPoint>,
    val areaM2: Double,
    val perimeterM: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun formatArea(): String {
        return if (areaM2 >= 10000) {
            String.format("%.2f ha (%.0f m²)", areaM2 / 10000.0, areaM2)
        } else {
            String.format("%.0f m²", areaM2)
        }
    }

    fun formatPerimeter(): String {
        return String.format("%.1f m", perimeterM)
    }
}
