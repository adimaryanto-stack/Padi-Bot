package com.example.padibot.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.padibot.model.Mission
import com.example.padibot.model.MissionStatus
import com.example.padibot.model.Waypoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val fieldId: String,
    val fieldName: String,
    val name: String,
    val status: String,
    val routeJson: String,
    val machineWidthM: Double,
    val headlandWidthM: Double,
    val laneOrientationDeg: Double,
    val totalLanes: Int,
    val estimatedDistanceM: Double,
    val estimatedCoveragePct: Double,
    val actualCoveragePct: Double,
    val startedAt: Long?,
    val completedAt: Long?,
    val createdAt: Long
) {
    fun toDomain(): Mission {
        val route = try {
            Json.decodeFromString<List<Waypoint>>(routeJson)
        } catch (e: Exception) {
            emptyList()
        }
        val statusEnum = try {
            MissionStatus.valueOf(status)
        } catch (e: Exception) {
            MissionStatus.DRAFT
        }
        return Mission(
            id = id,
            fieldId = fieldId,
            fieldName = fieldName,
            name = name,
            status = statusEnum,
            route = route,
            machineWidthM = machineWidthM,
            headlandWidthM = headlandWidthM,
            laneOrientationDeg = laneOrientationDeg,
            totalLanes = totalLanes,
            estimatedDistanceM = estimatedDistanceM,
            estimatedCoveragePct = estimatedCoveragePct,
            actualCoveragePct = actualCoveragePct,
            startedAt = startedAt,
            completedAt = completedAt,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(mission: Mission): MissionEntity {
            return MissionEntity(
                id = mission.id,
                fieldId = mission.fieldId,
                fieldName = mission.fieldName,
                name = mission.name,
                status = mission.status.name,
                routeJson = Json.encodeToString(mission.route),
                machineWidthM = mission.machineWidthM,
                headlandWidthM = mission.headlandWidthM,
                laneOrientationDeg = mission.laneOrientationDeg,
                totalLanes = mission.totalLanes,
                estimatedDistanceM = mission.estimatedDistanceM,
                estimatedCoveragePct = mission.estimatedCoveragePct,
                actualCoveragePct = mission.actualCoveragePct,
                startedAt = mission.startedAt,
                completedAt = mission.completedAt,
                createdAt = mission.createdAt
            )
        }
    }
}

@Entity(tableName = "mission_events")
data class MissionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val missionId: String,
    val eventType: String,
    val message: String,
    val severity: String,
    val timestamp: Long
)
