package com.example.padibot.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FieldEntity::class, MissionEntity::class, MissionEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PadiBotDatabase : RoomDatabase() {
    abstract fun fieldDao(): FieldDao
    abstract fun missionDao(): MissionDao

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
