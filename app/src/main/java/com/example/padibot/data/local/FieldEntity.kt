package com.example.padibot.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.padibot.model.Field
import com.example.padibot.model.GeoPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "fields")
data class FieldEntity(
    @PrimaryKey val id: String,
    val name: String,
    val boundaryJson: String,
    val areaM2: Double,
    val perimeterM: Double,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Field {
        val boundary = try {
            Json.decodeFromString<List<GeoPoint>>(boundaryJson)
        } catch (e: Exception) {
            emptyList()
        }
        return Field(
            id = id,
            name = name,
            boundary = boundary,
            areaM2 = areaM2,
            perimeterM = perimeterM,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(field: Field): FieldEntity {
            return FieldEntity(
                id = field.id,
                name = field.name,
                boundaryJson = Json.encodeToString(field.boundary),
                areaM2 = field.areaM2,
                perimeterM = field.perimeterM,
                createdAt = field.createdAt,
                updatedAt = field.updatedAt
            )
        }
    }
}
