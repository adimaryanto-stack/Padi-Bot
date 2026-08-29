package com.example.padibot.service

import android.util.Log
import com.example.padibot.model.Field
import com.example.padibot.model.Mission
import com.example.padibot.model.Telemetry
import com.google.firebase.database.*

class FirebaseRealtimeService {

    companion object {
        private const val TAG = "FirebaseRealtimeService"
        const val DATABASE_URL = "https://padibot-22de3-default-rtdb.asia-southeast1.firebasedatabase.app"
    }

    private val database: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance(DATABASE_URL).apply {
                setPersistenceEnabled(true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Database persistence already initialized or fallback: ${e.message}")
            FirebaseDatabase.getInstance(DATABASE_URL)
        }
    }

    private val rootRef: DatabaseReference by lazy { database.reference }
    private val telemetryRef: DatabaseReference by lazy { rootRef.child("telemetry").child("live") }
    private val missionRef: DatabaseReference by lazy { rootRef.child("missions").child("active") }
    private val fieldsRef: DatabaseReference by lazy { rootRef.child("fields") }
    private val commandsRef: DatabaseReference by lazy { rootRef.child("commands").child("incoming") }

    /**
     * Pushes live telemetry (GPS, battery, speed, heading, lane) to Firebase Realtime Database
     */
    fun pushTelemetry(telemetry: Telemetry) {
        try {
            val telemetryMap = mapOf(
                "latitude" to telemetry.latitude,
                "longitude" to telemetry.longitude,
                "accuracyMeters" to telemetry.accuracyMeters,
                "batteryPct" to telemetry.batteryPct,
                "speedMps" to telemetry.speedMps,
                "headingDeg" to telemetry.headingDeg,
                "gpsStatus" to telemetry.gpsStatus.name,
                "missionProgressPct" to telemetry.missionProgressPct,
                "currentLaneIndex" to telemetry.currentLaneIndex,
                "totalLanes" to telemetry.totalLanes,
                "timestamp" to System.currentTimeMillis()
            )
            telemetryRef.setValue(telemetryMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing telemetry to Firebase: ${e.message}", e)
        }
    }

    /**
     * Syncs active mission status & route to Firebase
     */
    fun syncActiveMission(mission: Mission?) {
        if (mission == null) {
            missionRef.removeValue()
            return
        }
        try {
            val missionMap = mapOf(
                "id" to mission.id,
                "fieldId" to mission.fieldId,
                "fieldName" to mission.fieldName,
                "status" to mission.status.name,
                "machineWidthM" to mission.machineWidthM,
                "headlandWidthM" to mission.headlandWidthM,
                "laneOrientationDeg" to mission.laneOrientationDeg,
                "totalLanes" to mission.totalLanes,
                "actualCoveragePct" to mission.actualCoveragePct,
                "startedAt" to mission.startedAt,
                "updatedAt" to System.currentTimeMillis()
            )
            missionRef.setValue(missionMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing mission to Firebase: ${e.message}", e)
        }
    }

    /**
     * Syncs field polygon to Firebase Realtime Database
     */
    fun syncField(field: Field) {
        try {
            val fieldMap = mapOf(
                "id" to field.id,
                "name" to field.name,
                "areaM2" to field.areaM2,
                "perimeterM" to field.perimeterM,
                "boundary" to field.boundary.map { mapOf("lat" to it.lat, "lon" to it.lon) },
                "updatedAt" to System.currentTimeMillis()
            )
            fieldsRef.child(field.id).setValue(fieldMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing field to Firebase: ${e.message}", e)
        }
    }

    /**
     * Listens for remote teleoperation commands from Firebase
     */
    fun listenForRemoteCommands(onCommandReceived: (ManualDirection, Float) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val directionStr = snapshot.child("direction").getValue(String::class.java)
                val speedFactor = snapshot.child("speedFactor").getValue(Double::class.java)?.toFloat() ?: 0.5f

                if (directionStr != null) {
                    val direction = when (directionStr.uppercase()) {
                        "FORWARD", "MAJU" -> ManualDirection.FORWARD
                        "BACKWARD", "MUNDUR" -> ManualDirection.BACKWARD
                        "LEFT", "KIRI" -> ManualDirection.LEFT
                        "RIGHT", "KANAN" -> ManualDirection.RIGHT
                        else -> ManualDirection.STOP
                    }
                    onCommandReceived(direction, speedFactor)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Firebase command listener cancelled: ${error.message}")
            }
        }

        commandsRef.addValueEventListener(listener)
        return listener
    }
}
