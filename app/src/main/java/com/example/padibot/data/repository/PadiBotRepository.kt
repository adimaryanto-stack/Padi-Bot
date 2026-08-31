package com.example.padibot.data.repository

import com.example.padibot.algorithm.PolygonMath
import com.example.padibot.data.local.BatteryLogDao
import com.example.padibot.data.local.FieldDao
import com.example.padibot.data.local.MissionDao
import com.example.padibot.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class PadiBotRepository(
    private val fieldDao: FieldDao,
    private val missionDao: MissionDao,
    private val batteryLogDao: BatteryLogDao
) {
    val allFields: Flow<List<Field>> = fieldDao.getAllFields()
    val allMissions: Flow<List<Mission>> = missionDao.getAllMissions()
    val allBatteryLogs: Flow<List<BatteryLog>> = batteryLogDao.getAllLogsAsc()

    fun getRecentBatteryLogs(limit: Int = 60): Flow<List<BatteryLog>> {
        return batteryLogDao.getRecentLogs(limit)
    }

    fun getBatteryLogsSince(timestamp: Long): Flow<List<BatteryLog>> {
        return batteryLogDao.getLogsSince(timestamp)
    }

    suspend fun saveBatteryLog(log: BatteryLog) {
        batteryLogDao.insertLog(log)
    }

    suspend fun clearBatteryLogs() {
        batteryLogDao.deleteAllLogs()
    }

    suspend fun saveField(field: Field) {
        fieldDao.insertField(field)
    }

    suspend fun deleteField(fieldId: String) {
        fieldDao.deleteFieldById(fieldId)
    }

    suspend fun saveMission(mission: Mission) {
        missionDao.insertMission(mission)
    }

    suspend fun updateMissionStatus(missionId: String, status: MissionStatus, coveragePct: Double = 0.0) {
        if (coveragePct > 0) {
            missionDao.updateStatus(missionId, status, coveragePct)
        } else {
            missionDao.updateStatus(missionId, status)
        }
    }

    suspend fun deleteMission(missionId: String) {
        missionDao.deleteMissionById(missionId)
    }

    suspend fun logEvent(missionId: String, type: String, message: String, severity: String = "INFO") {
        missionDao.insertEvent(
            MissionEvent(
                missionId = missionId,
                eventType = type,
                message = message,
                severity = severity,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun getEvents(missionId: String): Flow<List<MissionEvent>> {
        return missionDao.getEventsForMission(missionId)
    }

    suspend fun clearAllData() {
        fieldDao.deleteAll()
        missionDao.deleteAllMissions()
        missionDao.deleteAllEvents()
    }

    suspend fun seedInitialDataIfNeeded() {
        val existing = allFields.firstOrNull()
        if (existing.isNullOrEmpty()) {
            val samplePoints = listOf(
                GeoPoint(-6.923400, 107.610000),
                GeoPoint(-6.923400, 107.610400),
                GeoPoint(-6.923700, 107.610400),
                GeoPoint(-6.923700, 107.610000)
            )
            val (area, perim) = PolygonMath.calculateAreaAndPerimeter(samplePoints)
            val field1 = Field(
                id = UUID.randomUUID().toString(),
                name = "Sawah Blok A (Utara)",
                boundary = samplePoints,
                areaM2 = area,
                perimeterM = perim
            )
            saveField(field1)

            val samplePoints2 = listOf(
                GeoPoint(-6.923800, 107.610100),
                GeoPoint(-6.923850, 107.610500),
                GeoPoint(-6.924150, 107.610450),
                GeoPoint(-6.924100, 107.610050)
            )
            val (area2, perim2) = PolygonMath.calculateAreaAndPerimeter(samplePoints2)
            val field2 = Field(
                id = UUID.randomUUID().toString(),
                name = "Sawah Blok B (Selatan)",
                boundary = samplePoints2,
                areaM2 = area2,
                perimeterM = perim2
            )
            saveField(field2)

            // Seed sample mission
            val sampleMission = Mission(
                id = UUID.randomUUID().toString(),
                fieldId = field1.id,
                fieldName = field1.name,
                name = "Penanaman Padi Ciherang #1",
                status = MissionStatus.COMPLETED,
                route = samplePoints,
                machineWidthM = 1.20,
                headlandWidthM = 1.50,
                laneOrientationDeg = 0.0,
                totalLanes = 6,
                estimatedDistanceM = 120.0,
                estimatedCoveragePct = 96.5,
                actualCoveragePct = 96.0,
                startedAt = System.currentTimeMillis() - 3600000,
                completedAt = System.currentTimeMillis() - 1800000,
                createdAt = System.currentTimeMillis() - 3600000
            )
            saveMission(sampleMission)
            logEvent(sampleMission.id, "INIT", "Misi dibuat dan parameter diset", "INFO")
            logEvent(sampleMission.id, "START", "Misi tanam dimulai oleh operator", "INFO")
            logEvent(sampleMission.id, "FINISH", "Misi selesai dengan cakupan 96.0%", "INFO")
        }

        if (batteryLogDao.getCount() == 0) {
            val now = System.currentTimeMillis()
            val logs = mutableListOf<BatteryLog>()
            // Generate a realistic 2-hour curve: starting at 100% and gradually discharging to 86% during active work
            val sampleCount = 20
            val intervalMs = 6 * 60 * 1000L // every 6 minutes
            var currentPct = 100f
            var currentVolts = 53.2f
            for (i in (sampleCount - 1) downTo 0) {
                val time = now - (i * intervalMs)
                val isPlanting = i < 15
                val currentA = if (isPlanting) 7.2f + (Math.sin(i.toDouble()).toFloat() * 1.2f) else 1.5f
                val watts = currentVolts * currentA
                val temp = 29.5f + (i * 0.25f)
                logs.add(
                    BatteryLog(
                        timestamp = time,
                        batteryPct = currentPct,
                        batteryVoltageV = currentVolts,
                        batteryCurrentA = currentA,
                        powerDrawWatts = watts,
                        batteryTempC = temp,
                        isCharging = false,
                        isPlantingActive = isPlanting
                    )
                )
                // Drain ~0.7% every 6 mins when planting
                currentPct = (currentPct - 0.72f).coerceAtLeast(10f)
                currentVolts = (currentVolts - 0.08f).coerceAtLeast(46f)
            }
            batteryLogDao.insertLogs(logs)
        }
    }
}
