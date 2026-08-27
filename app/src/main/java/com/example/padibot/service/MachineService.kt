package com.example.padibot.service

import com.example.padibot.algorithm.PolygonMath
import com.example.padibot.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

enum class ManualDirection {
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT,
    STOP
}

class MachineService(private val scope: CoroutineScope) {

    private val _settings = MutableStateFlow(MachineSettings())
    val settings: StateFlow<MachineSettings> = _settings.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _telemetry = MutableStateFlow(
        Telemetry(
            timestamp = System.currentTimeMillis(),
            positionLat = -6.923400,
            positionLon = 107.610000,
            positionAccuracyM = 1.2f,
            batteryPct = 92.0f,
            speedMps = 0.0f,
            headingDeg = 0.0f,
            gpsStatus = GpsStatus.GPS,
            missionProgressPct = 0.0f,
            currentLaneIndex = 0,
            totalLanes = 0
        )
    )
    val telemetry: StateFlow<Telemetry> = _telemetry.asStateFlow()

    private val _activeMission = MutableStateFlow<Mission?>(null)
    val activeMission: StateFlow<Mission?> = _activeMission.asStateFlow()

    private val _missionStatus = MutableStateFlow(MissionStatus.DRAFT)
    val missionStatus: StateFlow<MissionStatus> = _missionStatus.asStateFlow()

    private val _emergencyStopTriggered = MutableSharedFlow<String>()
    val emergencyStopTriggered: SharedFlow<String> = _emergencyStopTriggered.asSharedFlow()

    private var simulationJob: Job? = null
    private var currentWaypointIndex = 0

    fun updateSettings(newSettings: MachineSettings) {
        _settings.value = newSettings
    }

    fun setConnectionStatus(connected: Boolean) {
        _isConnected.value = connected
    }

    fun startMission(mission: Mission) {
        _activeMission.value = mission
        _missionStatus.value = MissionStatus.RUNNING
        currentWaypointIndex = 0

        simulationJob?.cancel()
        simulationJob = scope.launch {
            runSimulationLoop(mission)
        }
    }

    fun pauseMission() {
        if (_missionStatus.value == MissionStatus.RUNNING) {
            _missionStatus.value = MissionStatus.PAUSED
            simulationJob?.cancel()
            _telemetry.value = _telemetry.value.copy(speedMps = 0.0f)
        }
    }

    fun resumeMission() {
        val mission = _activeMission.value ?: return
        if (_missionStatus.value == MissionStatus.PAUSED) {
            _missionStatus.value = MissionStatus.RUNNING
            simulationJob?.cancel()
            simulationJob = scope.launch {
                runSimulationLoop(mission)
            }
        }
    }

    fun stopMission() {
        _missionStatus.value = MissionStatus.STOPPED
        simulationJob?.cancel()
        _telemetry.value = _telemetry.value.copy(speedMps = 0.0f)
    }

    fun emergencyStop(reason: String = "Tombol Berhenti Darurat Ditekan") {
        _missionStatus.value = MissionStatus.STOPPED
        simulationJob?.cancel()
        _telemetry.value = _telemetry.value.copy(
            speedMps = 0.0f
        )
        scope.launch {
            _emergencyStopTriggered.emit(reason)
        }
    }

    fun handleManualCommand(direction: ManualDirection, speedFactor: Float = 0.5f) {
        if (_missionStatus.value == MissionStatus.RUNNING) {
            pauseMission()
        }

        val current = _telemetry.value
        val speedMps = (_settings.value.maxSpeedMps * speedFactor).toFloat()

        when (direction) {
            ManualDirection.FORWARD -> {
                val rad = Math.toRadians(current.headingDeg.toDouble())
                val dLat = (speedMps * cos(rad) * 0.000009)
                val dLon = (speedMps * sin(rad) * 0.000009)
                _telemetry.value = current.copy(
                    positionLat = current.positionLat + dLat,
                    positionLon = current.positionLon + dLon,
                    speedMps = speedMps
                )
            }
            ManualDirection.BACKWARD -> {
                val rad = Math.toRadians(current.headingDeg.toDouble())
                val dLat = (speedMps * cos(rad) * 0.000009)
                val dLon = (speedMps * sin(rad) * 0.000009)
                _telemetry.value = current.copy(
                    positionLat = current.positionLat - dLat,
                    positionLon = current.positionLon - dLon,
                    speedMps = speedMps
                )
            }
            ManualDirection.LEFT -> {
                _telemetry.value = current.copy(
                    headingDeg = (current.headingDeg - 15f + 360f) % 360f,
                    speedMps = speedMps * 0.5f
                )
            }
            ManualDirection.RIGHT -> {
                _telemetry.value = current.copy(
                    headingDeg = (current.headingDeg + 15f) % 360f,
                    speedMps = speedMps * 0.5f
                )
            }
            ManualDirection.STOP -> {
                _telemetry.value = current.copy(speedMps = 0.0f)
            }
        }
    }

    fun injectError(errorType: String) {
        when (errorType) {
            "GPS_LOSS" -> {
                _telemetry.value = _telemetry.value.copy(
                    gpsStatus = GpsStatus.NONE,
                    positionAccuracyM = 15.0f
                )
            }
            "LOW_BATTERY" -> {
                _telemetry.value = _telemetry.value.copy(
                    batteryPct = 12.0f
                )
            }
            "CONNECTION_DROP" -> {
                _isConnected.value = false
                pauseMission()
            }
            "RESTORE" -> {
                _isConnected.value = true
                _telemetry.value = _telemetry.value.copy(
                    gpsStatus = GpsStatus.GPS,
                    positionAccuracyM = 1.2f,
                    batteryPct = 88.0f
                )
            }
        }
    }

    private suspend fun runSimulationLoop(mission: Mission) {
        val waypoints = mission.route
        if (waypoints.isEmpty()) return

        val totalPoints = waypoints.size
        val totalDistance = mission.estimatedDistanceM

        while (currentWaypointIndex < totalPoints && _missionStatus.value == MissionStatus.RUNNING) {
            val targetWp = waypoints[currentWaypointIndex]
            val current = _telemetry.value

            // Compute heading towards target
            val dLat = targetWp.lat - current.positionLat
            val dLon = targetWp.lon - current.positionLon
            val heading = (Math.toDegrees(atan2(dLon, dLat)).toFloat() + 360f) % 360f

            // Smooth interpolation
            val steps = 4
            val latStep = (targetWp.lat - current.positionLat) / steps
            val lonStep = (targetWp.lon - current.positionLon) / steps

            for (s in 1..steps) {
                if (_missionStatus.value != MissionStatus.RUNNING) break

                val progress = (currentWaypointIndex.toFloat() + (s.toFloat() / steps)) / totalPoints
                val progressPct = (progress * 100f).coerceIn(0f, 100f)
                val remainingDist = totalDistance * (1.0 - progress)
                val remainingMins = (remainingDist / (0.75 * 60)).toInt()

                _telemetry.value = _telemetry.value.copy(
                    timestamp = System.currentTimeMillis(),
                    positionLat = current.positionLat + latStep * s,
                    positionLon = current.positionLon + lonStep * s,
                    speedMps = 0.75f,
                    headingDeg = heading,
                    batteryPct = (current.batteryPct - 0.03f).coerceAtLeast(5.0f),
                    missionProgressPct = progressPct,
                    currentLaneIndex = targetWp.laneIndex + 1,
                    totalLanes = mission.totalLanes,
                    plantedAreaM2 = (progress * (mission.totalLanes * mission.machineWidthM * 30.0)),
                    remainingMinutes = remainingMins
                )
                delay(250)
            }

            currentWaypointIndex++
        }

        if (currentWaypointIndex >= totalPoints && _missionStatus.value == MissionStatus.RUNNING) {
            _missionStatus.value = MissionStatus.COMPLETED
            _telemetry.value = _telemetry.value.copy(
                speedMps = 0.0f,
                missionProgressPct = 100.0f,
                currentLaneIndex = mission.totalLanes
            )
        }
    }
}
