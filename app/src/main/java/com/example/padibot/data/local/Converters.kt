package com.example.padibot.data.local

import androidx.room.TypeConverter
import com.example.padibot.model.GeoPoint
import com.example.padibot.model.MissionStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromGeoPointList(value: List<GeoPoint>?): String {
        return gson.toJson(value ?: emptyList<GeoPoint>())
    }

    @TypeConverter
    fun toGeoPointList(value: String?): List<GeoPoint> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<GeoPoint>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromMissionStatus(status: MissionStatus?): String {
        return status?.name ?: MissionStatus.READY.name
    }

    @TypeConverter
    fun toMissionStatus(value: String?): MissionStatus {
        return try {
            if (value != null) MissionStatus.valueOf(value) else MissionStatus.READY
        } catch (e: Exception) {
            MissionStatus.READY
        }
    }
}
