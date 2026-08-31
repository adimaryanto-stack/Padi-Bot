package com.example.padibot.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.padibot.model.BatteryLog
import com.example.padibot.model.Field
import com.example.padibot.model.Mission
import com.example.padibot.model.MissionEvent

@Database(
    entities = [Field::class, Mission::class, MissionEvent::class, BatteryLog::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PadiBotDatabase : RoomDatabase() {

    abstract fun fieldDao(): FieldDao
    abstract fun missionDao(): MissionDao
    abstract fun batteryLogDao(): BatteryLogDao

    companion object {
        @Volatile
        private var INSTANCE: PadiBotDatabase? = null

        fun getDatabase(context: Context): PadiBotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PadiBotDatabase::class.java,
                    "padibot_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
