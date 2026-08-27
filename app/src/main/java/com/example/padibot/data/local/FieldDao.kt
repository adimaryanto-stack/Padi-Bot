package com.example.padibot.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldDao {
    @Query("SELECT * FROM fields ORDER BY createdAt DESC")
    fun getAllFields(): Flow<List<FieldEntity>>

    @Query("SELECT * FROM fields WHERE id = :id")
    suspend fun getFieldById(id: String): FieldEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: FieldEntity)

    @Update
    suspend fun updateField(field: FieldEntity)

    @Query("DELETE FROM fields WHERE id = :id")
    suspend fun deleteFieldById(id: String)

    @Query("DELETE FROM fields")
    suspend fun deleteAllFields()
}

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions ORDER BY createdAt DESC")
    fun getAllMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE id = :id")
    suspend fun getMissionById(id: String): MissionEntity?

    @Query("SELECT * FROM missions WHERE fieldId = :fieldId ORDER BY createdAt DESC")
    fun getMissionsByField(fieldId: String): Flow<List<MissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: MissionEntity)

    @Update
    suspend fun updateMission(mission: MissionEntity)

    @Query("DELETE FROM missions WHERE id = :id")
    suspend fun deleteMissionById(id: String)

    @Query("DELETE FROM missions")
    suspend fun deleteAllMissions()

    @Insert
    suspend fun insertEvent(event: MissionEventEntity)

    @Query("SELECT * FROM mission_events WHERE missionId = :missionId ORDER BY timestamp ASC")
    fun getEventsByMission(missionId: String): Flow<List<MissionEventEntity>>
}
