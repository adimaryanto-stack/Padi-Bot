package com.example.padibot.service

import com.example.padibot.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MachineService(private val scope: CoroutineScope) {

    private val _activeMission = MutableStateFlow<Mission?>(null)
    val activeMission: StateFlow<Mission?> = _activeMission.asStateFlow()

    private val _missionStatus = MutableStateFlow(MissionStatus.READY)
    val missionStatus: StateFlow<MissionStatus> = _missionStatus.asStateFlow()

    private val _telemetry = MutableStateFlow(
        Telemetry(
            latitude = -6.923450,
            longitude = 107.610150,
            headingDeg = 45f,
            speedMps = 0f,
            batteryPct = 98f,
            missionProgressPct = 0f,
            rtkFixed = true,
            isPlantingActive = false,
            gpsStatus = "RTK Fix",
            positionAccuracyM = 0.02,
            accuracyMeters = 0.02
        )
    )
    val telemetry: StateFlow<Telemetry> = _telemetry.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _settings = MutableStateFlow(MachineSettings())
    val settings: StateFlow<MachineSettings> = _settings.asStateFlow()

    private val _emergencyStopTriggered = MutableSharedFlow<String>()
    val emergencyStopTriggered: SharedFlow<String> = _emergencyStopTriggered.asSharedFlow()

    private var simulationJob: Job? = null
    private var currentWaypointIndex = 0

    fun startMission(mission: Mission) {
        _activeMission.value = mission
        _missionStatus.value = MissionStatus.RUNNING
        currentWaypointIndex = 0
        startSimulationLoop(mission)
    }

    fun pauseMission() {
        _missionStatus.value = MissionStatus.PAUSED
        simulationJob?.cancel()
        _telemetry.value = _telemetry.value.copy(speedMps = 0f, isPlantingActive = false)
    }

    fun resumeMission() {
        val mission = _activeMission.value ?: return
        _missionStatus.value = MissionStatus.RUNNING
        startSimulationLoop(mission)
    }

    fun stopMission() {
        _missionStatus.value = MissionStatus.STOPPED
        simulationJob?.cancel()
        _telemetry.value = _telemetry.value.copy(speedMps = 0f, isPlantingActive = false)
    }

    fun emergencyStop(reason: String) {
        _missionStatus.value = MissionStatus.STOPPED
        simulationJob?.cancel()
        _telemetry.value = _telemetry.value.copy(
            speedMps = 0f,
            isPlantingActive = false,
            errorMsg = "E-STOP: $reason"
        )
        scope.launch {
            _emergencyStopTriggered.emit(reason)
        }
    }

    fun handleManualCommand(direction: ManualDirection, speedFactor: Float) {
        val current = _telemetry.value
        val speed = 0.8f * speedFactor
        val delta = 0.000030 * speedFactor

        val (newLat, newLon, newHeading) = when (direction) {
            ManualDirection.FORWARD -> Triple(current.latitude + delta, current.longitude, 0f)
            ManualDirection.BACKWARD -> Triple(current.latitude - delta, current.longitude, 180f)
            ManualDirection.LEFT -> Triple(current.latitude, current.longitude - delta, 270f)
            ManualDirection.RIGHT -> Triple(current.latitude, current.longitude + delta, 90f)
            ManualDirection.STOP -> Triple(current.latitude, current.longitude, current.headingDeg)
        }

        _telemetry.value = current.copy(
            latitude = newLat,
            longitude = newLon,
            headingDeg = newHeading,
            speedMps = if (direction == ManualDirection.STOP) 0f else speed,
            isPlantingActive = direction != ManualDirection.STOP,
            lastUpdate = System.currentTimeMillis()
        )
    }

    fun updateSettings(newSettings: MachineSettings) {
        _settings.value = newSettings
    }

    fun updateTelemetryFromGps(lat: Double, lon: Double, accuracyM: Double = 1.5, speed: Float = 0f, heading: Float = 0f) {
        if (_missionStatus.value != MissionStatus.RUNNING) {
            _telemetry.value = _telemetry.value.copy(
                latitude = lat,
                longitude = lon,
                positionAccuracyM = accuracyM,
                accuracyMeters = accuracyM,
                speedMps = speed,
                headingDeg = if (heading != 0f) heading else _telemetry.value.headingDeg,
                lastUpdate = System.currentTimeMillis()
            )
        }
    }

    fun injectError(errorType: String) {
        when (errorType) {
            "GPS_LOSS" -> {
                _telemetry.value = _telemetry.value.copy(
                    rtkFixed = false,
                    gpsStatus = "No GPS Fix",
                    positionAccuracyM = 8.5,
                    accuracyMeters = 8.5,
                    errorMsg = "Koneksi RTK GPS Terputus!"
                )
            }
            "LOW_BATTERY" -> {
                _telemetry.value = _telemetry.value.copy(
                    batteryPct = 12f,
                    errorMsg = "Baterai Kritis (<15%)!"
                )
            }
            "CONNECTION_DROP" -> {
                _isConnected.value = false
            }
            "RESTORE" -> {
                _isConnected.value = true
                _telemetry.value = _telemetry.value.copy(
                    rtkFixed = true,
                    gpsStatus = "RTK Fix",
                    positionAccuracyM = 0.02,
                    accuracyMeters = 0.02,
                    batteryPct = 95f,
                    errorMsg = null
                )
            }
        }
    }

    private fun startSimulationLoop(mission: Mission) {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val waypoints = mission.route
            if (waypoints.isEmpty()) return@launch

            while (isActive && _missionStatus.value == MissionStatus.RUNNING) {
                if (currentWaypointIndex >= waypoints.size) {
                    _missionStatus.value = MissionStatus.COMPLETED
                    _telemetry.value = _telemetry.value.copy(
                        speedMps = 0f,
                        isPlantingActive = false,
                        missionProgressPct = 100f
                    )
                    break
                }

                val target = waypoints[currentWaypointIndex]
                val current = _telemetry.value
                val progress = ((currentWaypointIndex + 1).toFloat() / waypoints.size.toFloat()) * 100f
                val newBattery = (current.batteryPct - 0.05f).coerceAtLeast(5f)

                _telemetry.value = current.copy(
                    latitude = target.latitude,
                    longitude = target.longitude,
                    speedMps = 0.8f,
                    batteryPct = newBattery,
                    missionProgressPct = progress,
                    isPlantingActive = true,
                    lastUpdate = System.currentTimeMillis()
                )

                currentWaypointIndex++
                delay(1200)
            }
        }
    }
}
