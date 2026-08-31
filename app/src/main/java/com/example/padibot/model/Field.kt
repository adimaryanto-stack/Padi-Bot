package com.example.padibot.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fields")
data class Field(
    @PrimaryKey val id: String,
    val name: String,
    val boundary: List<GeoPoint>,
    val areaM2: Double,
    val perimeterM: Double,
    val markers: List<FieldMarker> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formatArea(): String {
        return if (areaM2 >= 10000.0) {
            String.format("%.2f ha (%.0f m²)", areaM2 / 10000.0, areaM2)
        } else {
            String.format("%.0f m²", areaM2)
        }
    }
}
