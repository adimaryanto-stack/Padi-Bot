package com.example.padibot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.padibot.algorithm.PolygonMath
import com.example.padibot.algorithm.RouteGenerationResult
import com.example.padibot.algorithm.RoutePlanner
import com.example.padibot.data.local.PadiBotDatabase
import com.example.padibot.data.repository.PadiBotRepository
import com.example.padibot.model.*
import com.example.padibot.service.BatteryAlertEvent
import com.example.padibot.service.BatteryNotificationService
import com.example.padibot.service.DeviceLocationService
import com.example.padibot.service.DeviceLocationState
import com.example.padibot.service.FirebaseRealtimeService
import com.example.padibot.service.FirebaseSyncState
import com.example.padibot.service.MachineService
import com.example.padibot.service.ManualDirection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class PadiBotViewModel(application: Application) : AndroidViewModel(application) {

    val database = PadiBotDatabase.getDatabase(application)
    val repository = PadiBotRepository(database.fieldDao(), database.missionDao(), database.batteryLogDao())
    val machineService = MachineService(viewModelScope)
    val firebaseService = FirebaseRealtimeService()
    val deviceLocationService = DeviceLocationService(application, viewModelScope)
    val batteryNotificationService = BatteryNotificationService(application)

    val batteryAlertEvents: SharedFlow<BatteryAlertEvent> = batteryNotificationService.batteryAlertEvents

    val deviceLocation: StateFlow<DeviceLocationState> = deviceLocationService.locationState

    val allFields: StateFlow<List<Field>> = repository.allFields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMissions: StateFlow<List<Mission>> = repository.allMissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batteryLogs: StateFlow<List<BatteryLog>> = repository.allBatteryLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedField = MutableStateFlow<Field?>(null)
    val selectedField: StateFlow<Field?> = _selectedField.asStateFlow()

    // Route & Planting parameters
    private val _rowSpacingCm = MutableStateFlow(30.0)
    val rowSpacingCm: StateFlow<Double> = _rowSpacingCm.asStateFlow()

    private val _plantSpacingCm = MutableStateFlow(20.0)
    val plantSpacingCm: StateFlow<Double> = _plantSpacingCm.asStateFlow()

    private val _machineWidth = MutableStateFlow(1.20)
    val machineWidth: StateFlow<Double> = _machineWidth.asStateFlow()

    private val _speedMps = MutableStateFlow(0.8)
    val speedMps: StateFlow<Double> = _speedMps.asStateFlow()

    private val _headlandWidth = MutableStateFlow(1.50)
    val headlandWidth: StateFlow<Double> = _headlandWidth.asStateFlow()

    private val _laneOrientation = MutableStateFlow(0.0)
    val laneOrientation: StateFlow<Double> = _laneOrientation.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    fun setRowSpacingCm(cm: Double) {
        _rowSpacingCm.value = cm.coerceIn(15.0, 50.0)
    }

    fun setPlantSpacingCm(cm: Double) {
        _plantSpacingCm.value = cm.coerceIn(10.0, 40.0)
    }

    fun setSpeedMps(mps: Double) {
        _speedMps.value = mps.coerceIn(0.2, 2.0)
    }

    private val _selectedPattern = MutableStateFlow(RoutePattern.BOUSTROPHEDON)
    val selectedPattern: StateFlow<RoutePattern> = _selectedPattern.asStateFlow()

    private val _generatedRoute = MutableStateFlow<RouteGenerationResult?>(null)
    val generatedRoute: StateFlow<RouteGenerationResult?> = _generatedRoute.asStateFlow()

    // Active Mission Execution
    val activeMission: StateFlow<Mission?> = machineService.activeMission
    val missionStatus: StateFlow<MissionStatus> = machineService.missionStatus
    val activeMissionStatus: StateFlow<MissionStatus> = machineService.missionStatus
    val telemetry: StateFlow<Telemetry> = machineService.telemetry
    val isConnected: StateFlow<Boolean> = machineService.isConnected
    val isMachineConnected: StateFlow<Boolean> = machineService.isConnected
    val machineSettings: StateFlow<MachineSettings> = machineService.settings
    val emergencyStopTriggered = machineService.emergencyStopTriggered
    val firebaseSyncState: StateFlow<FirebaseSyncState> = firebaseService.syncState

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }

        viewModelScope.launch {
            machineSettings.collectLatest { settings ->
                firebaseService.updateConfig(
                    url = settings.firebaseDbUrl,
                    token = settings.firebaseAuthToken,
                    autoSync = settings.firebaseAutoSync
                )
            }
        }

        // Auto test & sync on app launch using embedded hardcoded credentials
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            firebaseService.testConnection()
            syncAllDataToFirebase()
        }

        // Periodic sync to Firebase (every 30 seconds if autoSync is enabled)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000L)
                if (firebaseService.isAutoSyncEnabled) {
                    syncAllDataToFirebase()
                }
            }
        }

        viewModelScope.launch {
            allFields.collectLatest { fields ->
                if (_selectedField.value == null && fields.isNotEmpty()) {
                    _selectedField.value = fields.first()
                    recalculateRoute()
                }
            }
        }

        viewModelScope.launch {
            var lastRecordedTime = 0L
            var lastRecordedPct = -1f
            telemetry.collectLatest { t ->
                firebaseService.pushTelemetry(t)
                batteryNotificationService.processBatteryTelemetry(t)
                val now = System.currentTimeMillis()
                // Record sample in Room database every 20 seconds or when battery percentage shifts
                if (now - lastRecordedTime >= 20_000L || Math.abs(t.batteryPct - lastRecordedPct) >= 0.5f) {
                    lastRecordedTime = now
                    lastRecordedPct = t.batteryPct
                    repository.saveBatteryLog(
                        BatteryLog(
                            timestamp = now,
                            batteryPct = t.batteryPct,
                            batteryVoltageV = t.batteryVoltageV,
                            batteryCurrentA = t.batteryCurrentA,
                            powerDrawWatts = t.powerDrawWatts,
                            batteryTempC = t.batteryTempC,
                            isCharging = t.isCharging,
                            isPlantingActive = t.isPlantingActive
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            activeMission.collectLatest { m ->
                firebaseService.syncActiveMission(m)
            }
        }

        viewModelScope.launch {
            missionStatus.collectLatest { status ->
                if (status == MissionStatus.COMPLETED) {
                    val mission = activeMission.value
                    if (mission != null) {
                        repository.updateMissionStatus(mission.id, MissionStatus.COMPLETED, 100.0)
                        repository.logEvent(
                            mission.id,
                            "COMPLETE",
                            "Misi tanam selesai 100% dan berhasil diselesaikan",
                            "SUCCESS"
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            deviceLocation.collectLatest { loc ->
                if (loc.hasFix && loc.latitude != 0.0 && loc.longitude != 0.0) {
                    machineService.updateTelemetryFromGps(
                        lat = loc.latitude,
                        lon = loc.longitude,
                        accuracyM = loc.accuracyMeters.toDouble().coerceAtLeast(0.01),
                        speed = loc.speedMps,
                        heading = loc.bearingDeg
                    )
                }
            }
        }

        firebaseService.listenForRemoteCommands { dir, speed ->
            sendManualCommand(dir, speed)
        }
    }

    fun selectField(field: Field) {
        _selectedField.value = field
        recalculateRoute()
    }

    fun updatePattern(pattern: RoutePattern) {
        setSelectedPattern(pattern)
    }

    fun setSelectedPattern(pattern: RoutePattern) {
        _selectedPattern.value = pattern
        recalculateRoute()
    }

    fun updateMachineWidth(width: Double) {
        setMachineWidth(width)
    }

    fun setMachineWidth(width: Double) {
        _machineWidth.value = width.coerceIn(0.5, 5.0)
        recalculateRoute()
    }

    fun updateSpeedMps(mps: Double) {
        setSpeedMps(mps)
    }

    fun updateHeadlandWidth(headland: Double) {
        setHeadlandWidth(headland)
    }

    fun setHeadlandWidth(headland: Double) {
        _headlandWidth.value = headland.coerceIn(0.5, 10.0)
        recalculateRoute()
    }

    fun updateLaneOrientation(orientation: Double) {
        setLaneOrientation(orientation)
    }

    fun setLaneOrientation(orientation: Double) {
        _laneOrientation.value = (orientation % 360.0 + 360.0) % 360.0
        recalculateRoute()
    }

    fun generateRoute() {
        recalculateRoute()
    }

    fun recalculateRoute() {
        val field = _selectedField.value ?: return
        if (field.boundary.size >= 3) {
            val result = RoutePlanner.generateCoverageRoute(
                boundary = field.boundary,
                machineWidthM = _machineWidth.value,
                headlandWidthM = _headlandWidth.value,
                orientationDeg = _laneOrientation.value,
                pattern = _selectedPattern.value
            )
            _generatedRoute.value = result
        }
    }

    fun createField(
        name: String,
        points: List<GeoPoint>,
        markers: List<FieldMarker> = emptyList(),
        onSuccess: (Field) -> Unit
    ) {
        viewModelScope.launch {
            val (area, perim) = PolygonMath.calculateAreaAndPerimeter(points)
            val newField = Field(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Sawah Baru" },
                boundary = points,
                areaM2 = area,
                perimeterM = perim,
                markers = markers
            )
            repository.saveField(newField)
            firebaseService.syncField(newField)
            _selectedField.value = newField
            recalculateRoute()
            onSuccess(newField)
        }
    }

    fun deleteField(fieldId: String) {
        viewModelScope.launch {
            repository.deleteField(fieldId)
            if (_selectedField.value?.id == fieldId) {
                _selectedField.value = allFields.value.firstOrNull { it.id != fieldId }
                recalculateRoute()
            }
            syncAllDataToFirebase()
        }
    }

    fun deleteMission(missionId: String) {
        viewModelScope.launch {
            repository.deleteMission(missionId)
            syncAllDataToFirebase()
        }
    }

    fun approveMission(missionName: String, onApproved: (Mission) -> Unit) {
        val field = _selectedField.value ?: return
        val routeResult = _generatedRoute.value ?: return
        val newMission = Mission(
            id = UUID.randomUUID().toString(),
            fieldId = field.id,
            fieldName = field.name,
            name = missionName.ifBlank { "Misi ${field.name} #${allMissions.value.size + 1}" },
            status = MissionStatus.READY,
            route = routeResult.waypoints,
            machineWidthM = _machineWidth.value,
            headlandWidthM = _headlandWidth.value,
            laneOrientationDeg = _laneOrientation.value,
            totalLanes = routeResult.totalLanes,
            estimatedDistanceM = routeResult.totalDistanceM,
            estimatedCoveragePct = routeResult.coveragePct,
            actualCoveragePct = 0.0,
            startedAt = null,
            completedAt = null,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.saveMission(newMission)
            repository.logEvent(newMission.id, "READY", "Misi telah diapprove dan siap dieksekusi", "INFO")
            firebaseService.syncAllData(
                fields = allFields.value,
                missions = allMissions.value + newMission,
                telemetry = telemetry.value,
                batteryLogs = batteryLogs.value,
                settings = machineSettings.value
            )
            onApproved(newMission)
        }
    }

    fun startMissionExecution(mission: Mission) {
        viewModelScope.launch {
            val startedMission = mission.copy(
                status = MissionStatus.RUNNING,
                startedAt = System.currentTimeMillis()
            )
            repository.saveMission(startedMission)
            repository.logEvent(startedMission.id, "START", "Misi tanam dimulai oleh operator", "INFO")
            machineService.startMission(startedMission)
        }
    }

    fun pauseMission() {
        pauseMissionExecution()
    }

    fun pauseMissionExecution() {
        val mission = activeMission.value ?: return
        machineService.pauseMission()
        viewModelScope.launch {
            repository.updateMissionStatus(mission.id, MissionStatus.PAUSED)
            repository.logEvent(mission.id, "PAUSE", "Misi dijeda oleh operator", "INFO")
        }
    }

    fun resumeMission() {
        resumeMissionExecution()
    }

    fun resumeMissionExecution() {
        val mission = activeMission.value ?: return
        machineService.resumeMission()
        viewModelScope.launch {
            repository.updateMissionStatus(mission.id, MissionStatus.RUNNING)
            repository.logEvent(mission.id, "RESUME", "Misi dilanjutkan", "INFO")
        }
    }

    fun stopMission() {
        stopMissionExecution()
    }

    fun stopMissionExecution() {
        val mission = activeMission.value ?: return
        val currentProgress = telemetry.value.missionProgressPct.toDouble()
        machineService.stopMission()
        viewModelScope.launch {
            repository.updateMissionStatus(mission.id, MissionStatus.STOPPED, currentProgress)
            repository.logEvent(mission.id, "STOP", "Misi dihentikan oleh operator pada ${currentProgress.toInt()}%", "WARNING")
        }
    }

    fun emergencyStop(reason: String = "Tombol Berhenti Darurat Ditekan") {
        val mission = activeMission.value
        machineService.emergencyStop(reason)
        if (mission != null) {
            viewModelScope.launch {
                val currentProgress = telemetry.value.missionProgressPct.toDouble()
                repository.updateMissionStatus(mission.id, MissionStatus.STOPPED, currentProgress)
                repository.logEvent(mission.id, "E_STOP", "EMERGENCY STOP: $reason", "CRITICAL")
            }
        }
    }

    fun triggerEmergencyStop() {
        emergencyStop("Tombol Berhenti Darurat Ditekan")
    }

    fun sendManualCommand(direction: ManualDirection, speedFactor: Float) {
        machineService.handleManualCommand(direction, speedFactor)
    }

    fun updateMachineSettings(settings: MachineSettings) {
        machineService.updateSettings(settings)
    }

    fun injectError(errorType: String) {
        injectSimulatorError(errorType)
    }

    fun injectSimulatorError(errorType: String) {
        machineService.injectError(errorType)
    }

    fun getCurrentGpsLocation(): GeoPoint {
        val devLoc = deviceLocation.value
        if (devLoc.hasFix && devLoc.latitude != 0.0 && devLoc.longitude != 0.0) {
            return GeoPoint(devLoc.latitude, devLoc.longitude)
        }
        val t = telemetry.value
        return if (t.latitude != 0.0 && t.longitude != 0.0) {
            GeoPoint(t.latitude, t.longitude)
        } else {
            GeoPoint(-6.923450, 107.610150)
        }
    }

    fun startGpsTracking() {
        deviceLocationService.startLocationUpdates()
    }

    fun stopGpsTracking() {
        deviceLocationService.stopLocationUpdates()
    }

    fun checkGpsPermissions() {
        deviceLocationService.checkStatus()
    }

    fun getEventsForMission(missionId: String): Flow<List<MissionEvent>> {
        return repository.getEvents(missionId)
    }

    fun resetSampleData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.seedInitialDataIfNeeded()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun clearAllUserData() {
        clearAllData()
    }

    fun testFirebaseConnection(
        customUrl: String? = null,
        customToken: String? = null,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val res = firebaseService.testConnection(customUrl, customToken)
            res.onSuccess { msg ->
                onResult?.invoke(true, msg)
            }.onFailure { err ->
                onResult?.invoke(false, err.message ?: "Koneksi Firebase Gagal")
            }
        }
    }

    fun syncAllDataToFirebase(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val fields = allFields.value
            val missions = allMissions.value
            val currentTel = telemetry.value
            val bLogs = batteryLogs.value
            val settings = machineSettings.value
            val res = firebaseService.syncAllData(
                fields = fields,
                missions = missions,
                telemetry = currentTel,
                batteryLogs = bLogs,
                settings = settings
            )
            res.onSuccess { msg ->
                onResult?.invoke(true, msg)
            }.onFailure { err ->
                onResult?.invoke(false, err.message ?: "Gagal sinkronisasi data")
            }
        }
    }

    fun recordBatterySample() {
        val t = telemetry.value
        viewModelScope.launch {
            repository.saveBatteryLog(
                BatteryLog(
                    timestamp = System.currentTimeMillis(),
                    batteryPct = t.batteryPct,
                    batteryVoltageV = t.batteryVoltageV,
                    batteryCurrentA = t.batteryCurrentA,
                    powerDrawWatts = t.powerDrawWatts,
                    batteryTempC = t.batteryTempC,
                    isCharging = t.isCharging,
                    isPlantingActive = t.isPlantingActive
                )
            )
        }
    }

    fun clearBatteryLogs() {
        viewModelScope.launch {
            repository.clearBatteryLogs()
        }
    }

    fun triggerTestBatteryNotification(percentage: Float = 18f) {
        batteryNotificationService.triggerTestAlert(percentage)
    }

    fun simulateLowBattery(percentage: Float = 15f) {
        machineService.injectError("LOW_BATTERY")
    }

    fun restoreBattery() {
        machineService.injectError("RESTORE")
    }
}
