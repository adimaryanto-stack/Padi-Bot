package com.example.padibot.service

import android.util.Log
import com.example.padibot.model.Field
import com.example.padibot.model.Mission
import com.example.padibot.model.Telemetry
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class FirebaseRealtimeService {

    companion object {
        const val DATABASE_URL = "https://padibot-project-default-rtdb.firebaseio.com"
        private const val TAG = "FirebaseRTDB"
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun pushTelemetry(telemetry: Telemetry) {
        scope.launch {
            try {
                val json = gson.toJson(telemetry)
                sendPutRequest("$DATABASE_URL/telemetry/live.json", json)
            } catch (e: Exception) {
                Log.d(TAG, "Sync telemetry offline/skip: ${e.message}")
            }
        }
    }

    fun syncField(field: Field) {
        scope.launch {
            try {
                val json = gson.toJson(field)
                sendPutRequest("$DATABASE_URL/fields/${field.id}.json", json)
            } catch (e: Exception) {
                Log.d(TAG, "Sync field offline/skip: ${e.message}")
            }
        }
    }

    fun syncActiveMission(mission: Mission?) {
        if (mission == null) return
        scope.launch {
            try {
                val json = gson.toJson(mission)
                sendPutRequest("$DATABASE_URL/missions/active.json", json)
            } catch (e: Exception) {
                Log.d(TAG, "Sync mission offline/skip: ${e.message}")
            }
        }
    }

    fun listenForRemoteCommands(onCommand: (ManualDirection, Float) -> Unit) {
        // Ready for cloud polling/listeners
    }

    private fun sendPutRequest(urlString: String, jsonBody: String) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.doOutput = true
            conn.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
            conn.responseCode
        } catch (e: Exception) {
            // Gracefully handled for offline/local-first mode
        } finally {
            conn?.disconnect()
        }
    }
}
