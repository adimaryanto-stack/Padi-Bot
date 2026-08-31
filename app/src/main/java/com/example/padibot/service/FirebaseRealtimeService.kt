package com.example.padibot.service

import android.util.Log
import com.example.padibot.model.BatteryLog
import com.example.padibot.model.Field
import com.example.padibot.model.MachineSettings
import com.example.padibot.model.Mission
import com.example.padibot.model.Telemetry
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

enum class FirebaseStatusLevel {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class FirebaseSyncState(
    val status: FirebaseStatusLevel = FirebaseStatusLevel.DISCONNECTED,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L,
    val lastResponseCode: Int = 0,
    val lastErrorMessage: String? = null,
    val lastLatencyMs: Long = 0L,
    val databaseUrl: String = ""
)

class FirebaseRealtimeService {

    companion object {
        private const val TAG = "FirebaseRTDB"
        const val DEFAULT_DATABASE_URL = "https://padibot-22de3-default-rtdb.asia-southeast1.firebasedatabase.app"
        const val DEFAULT_AUTH_TOKEN = "EI3vfJn9M89e1DpEDI4vDCqMBDAYEGN2kNxOJNMt"
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var databaseUrl: String = DEFAULT_DATABASE_URL
        private set

    var authToken: String = DEFAULT_AUTH_TOKEN
        private set

    var isAutoSyncEnabled: Boolean = true
        private set

    private val _syncState = MutableStateFlow(FirebaseSyncState(databaseUrl = databaseUrl))
    val syncState: StateFlow<FirebaseSyncState> = _syncState.asStateFlow()

    fun normalizeUrl(input: String): String {
        var clean = input.trim()
        if (clean.isBlank()) return DEFAULT_DATABASE_URL
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = if (clean.contains(".firebaseio.com") || clean.contains(".firebasedatabase.app")) {
                "https://$clean"
            } else {
                // User entered only project-id, e.g. "padibot-demo"
                "https://$clean-default-rtdb.asia-southeast1.firebasedatabase.app"
            }
        }
        return clean.removeSuffix("/").removeSuffix(".json")
    }

    fun updateConfig(url: String, token: String, autoSync: Boolean = true) {
        val cleanUrl = if (url.isBlank()) DEFAULT_DATABASE_URL else normalizeUrl(url)
        val cleanToken = if (token.isBlank()) DEFAULT_AUTH_TOKEN else token.trim()
        this.databaseUrl = cleanUrl
        this.authToken = cleanToken
        this.isAutoSyncEnabled = autoSync
        _syncState.value = _syncState.value.copy(
            databaseUrl = this.databaseUrl
        )
    }

    private fun buildEndpointUrl(path: String): String {
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        val baseUrl = databaseUrl.removeSuffix("/")
        return if (authToken.isNotBlank()) {
            "$baseUrl$cleanPath.json?auth=$authToken"
        } else {
            "$baseUrl$cleanPath.json"
        }
    }

    /**
     * Tests connectivity to the configured or provided Firebase Realtime Database
     */
    suspend fun testConnection(testUrl: String? = null, testToken: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val targetUrl = (testUrl?.trim()?.removeSuffix("/") ?: databaseUrl).ifBlank { DEFAULT_DATABASE_URL }
        val targetToken = testToken?.trim() ?: authToken
        val startTime = System.currentTimeMillis()

        _syncState.value = _syncState.value.copy(status = FirebaseStatusLevel.CONNECTING)

        val pingUrl = if (targetToken.isNotBlank()) {
            "$targetUrl/health_check.json?auth=$targetToken"
        } else {
            "$targetUrl/health_check.json"
        }

        var conn: HttpURLConnection? = null
        try {
            val url = URL(pingUrl)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
            }

            val testPayload = gson.toJson(
                mapOf(
                    "status" to "online",
                    "client" to "PadiBot-Android",
                    "timestamp" to System.currentTimeMillis()
                )
            )

            conn.outputStream.use { os ->
                os.write(testPayload.toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            val latency = System.currentTimeMillis() - startTime

            if (responseCode in 200..299) {
                _syncState.value = _syncState.value.copy(
                    status = FirebaseStatusLevel.CONNECTED,
                    lastSyncTime = System.currentTimeMillis(),
                    lastResponseCode = responseCode,
                    lastErrorMessage = null,
                    lastLatencyMs = latency
                )
                Result.success("Terhubung ke Firebase (${responseCode} OK - ${latency}ms)")
            } else {
                val errorBody = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) {
                    ""
                }
                val msg = when (responseCode) {
                    401 -> "401 Unauthorized: Aturan keamanan Firebase memerlukan Auth Token / Database Secret."
                    404 -> "404 Not Found: URL Realtime Database tidak valid."
                    403 -> "403 Forbidden: Akses database ditolak oleh Firebase Security Rules."
                    else -> "HTTP $responseCode: $errorBody"
                }
                _syncState.value = _syncState.value.copy(
                    status = FirebaseStatusLevel.ERROR,
                    lastResponseCode = responseCode,
                    lastErrorMessage = msg,
                    lastLatencyMs = latency
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            val errorMsg = when {
                e is java.net.UnknownHostException -> "Koneksi Gagal: Cek koneksi internet smartphone atau format URL Firebase."
                e is java.net.SocketTimeoutException -> "Timeout (6s): Server Firebase tidak merespon tepat waktu."
                else -> "Error: ${e.localizedMessage ?: e.message}"
            }
            _syncState.value = _syncState.value.copy(
                status = FirebaseStatusLevel.ERROR,
                lastResponseCode = 0,
                lastErrorMessage = errorMsg,
                lastLatencyMs = latency
            )
            Result.failure(Exception(errorMsg, e))
        } finally {
            conn?.disconnect()
        }
    }

    fun pushTelemetry(telemetry: Telemetry) {
        if (!isAutoSyncEnabled) return
        scope.launch {
            try {
                val json = gson.toJson(telemetry)
                val responseCode = sendPutRequest(buildEndpointUrl("/telemetry/live"), json)
                if (responseCode in 200..299) {
                    _syncState.value = _syncState.value.copy(
                        status = FirebaseStatusLevel.CONNECTED,
                        lastSyncTime = System.currentTimeMillis(),
                        lastResponseCode = responseCode,
                        lastErrorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "Sync telemetry offline/skip: ${e.message}")
            }
        }
    }

    fun syncField(field: Field) {
        scope.launch {
            try {
                val json = gson.toJson(field)
                val responseCode = sendPutRequest(buildEndpointUrl("/fields/${field.id}"), json)
                if (responseCode in 200..299) {
                    _syncState.value = _syncState.value.copy(
                        status = FirebaseStatusLevel.CONNECTED,
                        lastSyncTime = System.currentTimeMillis(),
                        lastResponseCode = responseCode,
                        lastErrorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "Sync field offline/skip: ${e.message}")
            }
        }
    }

    fun syncActiveMission(mission: Mission?) {
        if (mission == null || !isAutoSyncEnabled) return
        scope.launch {
            try {
                val json = gson.toJson(mission)
                val responseCode = sendPutRequest(buildEndpointUrl("/missions/active"), json)
                if (responseCode in 200..299) {
                    _syncState.value = _syncState.value.copy(
                        status = FirebaseStatusLevel.CONNECTED,
                        lastSyncTime = System.currentTimeMillis(),
                        lastResponseCode = responseCode,
                        lastErrorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "Sync mission offline/skip: ${e.message}")
            }
        }
    }

    /**
     * Manually or automatically sync all local fields, mission histories, battery logs, and settings to Firebase
     */
    suspend fun syncAllData(
        fields: List<Field>,
        missions: List<Mission>,
        telemetry: Telemetry,
        batteryLogs: List<BatteryLog> = emptyList(),
        settings: MachineSettings? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(isSyncing = true)
        try {
            val startTime = System.currentTimeMillis()

            // 1. Sync fields map
            val fieldsPayload = gson.toJson(fields.associateBy { it.id })
            val fieldsRes = sendPutRequest(buildEndpointUrl("/fields"), fieldsPayload)

            // 2. Sync missions map
            val missionsPayload = gson.toJson(missions.associateBy { it.id })
            val missionsRes = sendPutRequest(buildEndpointUrl("/missions/history"), missionsPayload)

            // 3. Sync live telemetry
            val telemetryPayload = gson.toJson(telemetry)
            val telemetryRes = sendPutRequest(buildEndpointUrl("/telemetry/live"), telemetryPayload)

            // 4. Sync battery logs history
            if (batteryLogs.isNotEmpty()) {
                val batteryPayload = gson.toJson(batteryLogs.associateBy { it.id.toString() })
                sendPutRequest(buildEndpointUrl("/telemetry/battery_history"), batteryPayload)
            }

            // 5. Sync device settings & machine configuration
            if (settings != null) {
                val settingsPayload = gson.toJson(settings)
                sendPutRequest(buildEndpointUrl("/settings/machine"), settingsPayload)
            }

            // 6. Sync metadata & last synced timestamp
            val syncMetaPayload = gson.toJson(
                mapOf(
                    "last_synced_at" to System.currentTimeMillis(),
                    "total_fields" to fields.size,
                    "total_missions" to missions.size,
                    "total_battery_logs" to batteryLogs.size,
                    "battery_pct" to telemetry.batteryPct,
                    "device_model" to "PadiBot Transplanter Autonomous v2.4",
                    "status" to "ONLINE"
                )
            )
            sendPutRequest(buildEndpointUrl("/sync_status"), syncMetaPayload)

            val latency = System.currentTimeMillis() - startTime

            if (fieldsRes in 200..299 && missionsRes in 200..299) {
                _syncState.value = _syncState.value.copy(
                    status = FirebaseStatusLevel.CONNECTED,
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis(),
                    lastResponseCode = fieldsRes,
                    lastErrorMessage = null,
                    lastLatencyMs = latency
                )
                Result.success("Semua Data Tersinkron: ${fields.size} Sawah, ${missions.size} Riwayat Misi, ${batteryLogs.size} Log Baterai & Telemetri (${latency}ms)")
            } else {
                val errorMsg = when {
                    fieldsRes == 401 || missionsRes == 401 -> "401 Unauthorized: Aturan keamanan Firebase Realtime Database memblokir akses tulis. Tambahkan Auth Token atau atur database rules."
                    fieldsRes == 404 || missionsRes == 404 -> "404 Not Found: URL Realtime Database salah."
                    else -> "Gagal sinkronisasi (HTTP $fieldsRes / $missionsRes)"
                }
                _syncState.value = _syncState.value.copy(
                    status = FirebaseStatusLevel.ERROR,
                    isSyncing = false,
                    lastResponseCode = fieldsRes,
                    lastErrorMessage = errorMsg,
                    lastLatencyMs = latency
                )
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = when {
                e is java.net.UnknownHostException -> "Gagal Sinkronisasi: Cek koneksi internet smartphone Redmi Anda."
                e is java.net.SocketTimeoutException -> "Timeout Sinkronisasi: Jaringan lambat atau server Firebase tidak merespon."
                else -> "Gagal Sinkronisasi: ${e.localizedMessage ?: e.message}"
            }
            _syncState.value = _syncState.value.copy(
                status = FirebaseStatusLevel.ERROR,
                isSyncing = false,
                lastErrorMessage = errorMsg
            )
            Result.failure(Exception(errorMsg, e))
        }
    }

    fun listenForRemoteCommands(onCommand: (ManualDirection, Float) -> Unit) {
        // Polling background loop ready for cloud control
    }

    private fun sendPutRequest(urlString: String, jsonBody: String): Int {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
            }
            conn.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
            conn.responseCode
        } catch (e: Exception) {
            Log.w(TAG, "Request to $urlString failed: ${e.message}")
            throw e
        } finally {
            conn?.disconnect()
        }
    }
}
