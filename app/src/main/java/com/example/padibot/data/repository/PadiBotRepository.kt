package com.example.padibot.data.repository

import com.example.padibot.algorithm.PolygonMath
import com.example.padibot.algorithm.RoutePlanner
import com.example.padibot.data.local.FieldDao
import com.example.padibot.data.local.FieldEntity
import com.example.padibot.data.local.MissionDao
import com.example.padibot.data.local.MissionEntity
import com.example.padibot.data.local.MissionEventEntity
import com.example.padibot.model.Field
import com.example.padibot.model.GeoPoint
import com.example.padibot.model.Mission
import com.example.padibot.model.MissionEvent
import com.example.padibot.model.MissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class PadiBotRepository(
    private val fieldDao: FieldDao,
    private val missionDao: MissionDao
) {
    val allFields: Flow<List<Field>> = fieldDao.getAllFields().map { entities ->
        entities.map { it.toDomain() }
    }

    val allMissions: Flow<List<Mission>> = missionDao.getAllMissions().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getFieldById(id: String): Field? {
        return fieldDao.getFieldById(id)?.toDomain()
    }

    suspend fun saveField(field: Field) {
        val (area, perimeter) = PolygonMath.calculateAreaAndPerimeter(field.boundary)
        val updatedField = field.copy(
            areaM2 = area,
            perimeterM = perimeter,
            updatedAt = System.currentTimeMillis()
        )
        fieldDao.insertField(FieldEntity.fromDomain(updatedField))
    }

    suspend fun deleteField(id: String) {
        fieldDao.deleteFieldById(id)
    }

    suspend fun getMissionById(id: String): Mission? {
        return missionDao.getMissionById(id)?.toDomain()
    }

    suspend fun saveMission(mission: Mission) {
        missionDao.insertMission(MissionEntity.fromDomain(mission))
    }

    suspend fun updateMissionStatus(id: String, status: MissionStatus, actualCoverage: Double? = null) {
        val current = missionDao.getMissionById(id) ?: return
        val domain = current.toDomain()
        val now = System.currentTimeMillis()
        val updated = domain.copy(
            status = status,
            actualCoveragePct = actualCoverage ?: domain.actualCoveragePct,
            completedAt = if (status == MissionStatus.COMPLETED || status == MissionStatus.STOPPED) now else domain.completedAt
        )
        missionDao.updateMission(MissionEntity.fromDomain(updated))
    }

    suspend fun deleteMission(id: String) {
        missionDao.deleteMissionById(id)
    }

    suspend fun logEvent(missionId: String, eventType: String, message: String, severity: String = "INFO") {
        missionDao.insertEvent(
            MissionEventEntity(
                missionId = missionId,
                eventType = eventType,
                message = message,
                severity = severity,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun getEvents(missionId: String): Flow<List<MissionEvent>> {
        return missionDao.getEventsByMission(missionId).map { list ->
            list.map {
                MissionEvent(
                    id = it.id,
                    missionId = it.missionId,
                    eventType = it.eventType,
                    message = it.message,
                    severity = it.severity,
                    timestamp = it.timestamp
                )
            }
        }
    }

    suspend fun seedInitialDataIfNeeded() {
        val existing = fieldDao.getFieldById("sawah-utama-001")
        if (existing == null) {
            // Sawah Utama (Karawang)
            val boundaryUtama = listOf(
                GeoPoint(-6.923400, 107.610000),
                GeoPoint(-6.923400, 107.610400),
                GeoPoint(-6.923750, 107.610400),
                GeoPoint(-6.923750, 107.610000)
            )
            val (area1, perim1) = PolygonMath.calculateAreaAndPerimeter(boundaryUtama)
            val field1 = Field(
                id = "sawah-utama-001",
                name = "Sawah Utama Karawang",
                boundary = boundaryUtama,
                areaM2 = area1,
                perimeterM = perim1,
                createdAt = System.currentTimeMillis() - 86400000L * 2
            )
            saveField(field1)

            // Sawah Timur (Subang)
            val boundaryTimur = listOf(
                GeoPoint(-6.924200, 107.611000),
                GeoPoint(-6.924150, 107.611350),
                GeoPoint(-6.924450, 107.611400),
                GeoPoint(-6.924550, 107.611050)
            )
            val (area2, perim2) = PolygonMath.calculateAreaAndPerimeter(boundaryTimur)
            val field2 = Field(
                id = "sawah-timur-002",
                name = "Sawah Timur Subang",
                boundary = boundaryTimur,
                areaM2 = area2,
                perimeterM = perim2,
                createdAt = System.currentTimeMillis() - 86400000L * 5
            )
            saveField(field2)

            // Sample Completed Mission
            val routeRes = RoutePlanner.generateCoverageRoute(boundaryUtama, 1.5, 3.0, 0.0)
            val sampleMission = Mission(
                id = "mission-demo-001",
                fieldId = field1.id,
                fieldName = field1.name,
                name = "Misi Penanaman Ciherang #1",
                status = MissionStatus.COMPLETED,
                route = routeRes.waypoints,
                machineWidthM = 1.5,
                headlandWidthM = 3.0,
                laneOrientationDeg = 0.0,
                totalLanes = routeRes.totalLanes,
                estimatedDistanceM = routeRes.totalDistanceM,
                estimatedCoveragePct = routeRes.coveragePct,
                actualCoveragePct = 96.5,
                startedAt = System.currentTimeMillis() - 7200000L,
                completedAt = System.currentTimeMillis() - 6400000L,
                createdAt = System.currentTimeMillis() - 7200000L
            )
            saveMission(sampleMission)
            logEvent(sampleMission.id, "START", "Misi tanam dimulai oleh operator", "INFO")
            logEvent(sampleMission.id, "STATUS", "Jalur 1 sampai 8 selesai dengan mulus", "INFO")
            logEvent(sampleMission.id, "COMPLETED", "Misi selesai dengan cakupan 96.5%", "INFO")
        }
    }

    suspend fun clearAllData() {
        fieldDao.deleteAllFields()
        missionDao.deleteAllMissions()
    }
}
