package com.example.padibot.data.local

import androidx.room.*
import com.example.padibot.model.BatteryLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryLogDao {

    @Query("SELECT * FROM battery_history_logs ORDER BY timestamp ASC")
    fun getAllLogsAsc(): Flow<List<BatteryLog>>

    @Query("SELECT * FROM battery_history_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<BatteryLog>>

    @Query("SELECT * FROM battery_history_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getLogsSince(sinceTimestamp: Long): Flow<List<BatteryLog>>

    @Query("SELECT * FROM battery_history_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLog(): Flow<BatteryLog?>

    @Query("SELECT COUNT(*) FROM battery_history_logs")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BatteryLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<BatteryLog>)

    @Query("DELETE FROM battery_history_logs WHERE timestamp < :olderThanTimestamp")
    suspend fun deleteLogsOlderThan(olderThanTimestamp: Long): Int

    @Query("DELETE FROM battery_history_logs")
    suspend fun deleteAllLogs()
}
