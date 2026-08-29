package com.example.padibot.data.local

import androidx.room.*
import com.example.padibot.model.Mission
import com.example.padibot.model.MissionEvent
import com.example.padibot.model.MissionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions ORDER BY createdAt DESC")
    fun getAllMissions(): Flow<List<Mission>>

    @Query("SELECT * FROM missions WHERE id = :missionId LIMIT 1")
    suspend fun getMissionById(missionId: String): Mission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: Mission)

    @Query("UPDATE missions SET status = :status, actualCoveragePct = :coveragePct WHERE id = :missionId")
    suspend fun updateStatus(missionId: String, status: MissionStatus, coveragePct: Double)

    @Query("UPDATE missions SET status = :status WHERE id = :missionId")
    suspend fun updateStatus(missionId: String, status: MissionStatus)

    @Delete
    suspend fun deleteMission(mission: Mission)

    @Query("DELETE FROM missions WHERE id = :missionId")
    suspend fun deleteMissionById(missionId: String)

    @Query("DELETE FROM missions")
    suspend fun deleteAllMissions()

    // Events
    @Insert
    suspend fun insertEvent(event: MissionEvent)

    @Query("SELECT * FROM mission_events WHERE missionId = :missionId ORDER BY timestamp ASC")
    fun getEventsForMission(missionId: String): Flow<List<MissionEvent>>

    @Query("DELETE FROM mission_events")
    suspend fun deleteAllEvents()
}
