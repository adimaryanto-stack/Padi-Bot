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
import com.example.padibot.service.MachineService
import com.example.padibot.service.ManualDirection
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class PadiBotViewModel(application: Application) : AndroidViewModel(application) {

    val database = PadiBotDatabase.getDatabase(application)
    val repository = PadiBotRepository(database.fieldDao(), database.missionDao())
    val machineService = MachineService(viewModelScope)
    val firebaseService = FirebaseRealtimeService()

    val allFields: StateFlow<List<Field>> = repository.allFields
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMissions: StateFlow<List<Mission>> = repository.allMissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedField = MutableStateFlow<Field?>(null)
    val selectedField: StateFlow<Field?> = _selectedField.asStateFlow()

    // Route Generation parameters
    private val _machineWidth = MutableStateFlow(1.50)
    val machineWidth: StateFlow<Double> = _machineWidth.asStateFlow()

    private val _headlandWidth = MutableStateFlow(3.00)
    val headlandWidth: StateFlow<Double> = _headlandWidth.asStateFlow()

    private val _laneOrientation = MutableStateFlow(0.0)
    val laneOrientation: StateFlow<Double> = _laneOrientation.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false) // Default to Versi Light (Terang)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    private val _selectedPattern = MutableStateFlow(RoutePattern.BOUSTROPHEDON)
    val selectedPattern: StateFlow<RoutePattern> = _selectedPattern.asStateFlow()

    private val _generatedRoute = MutableStateFlow<RouteGenerationResult?>(null)
    val generatedRoute: StateFlow<RouteGenerationResult?> = _generatedRoute.asStateFlow()

    // Active Mission Execution
    val activeMission: StateFlow<Mission?> = machineService.activeMission
    val activeMissionStatus: StateFlow<MissionStatus> = machineService.missionStatus
    val telemetry: StateFlow<Telemetry> = machineService.telemetry
    val isMachineConnected: StateFlow<Boolean> = machineService.isConnected
    val machineSettings: StateFlow<MachineSettings> = machineService.settings
    val emergencyStopTriggered = machineService.emergencyStopTriggered

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }

        // Auto-select first field if none selected
        viewModelScope.launch {
            allFields.collectLatest { fields ->
                if (_selectedField.value == null && fields.isNotEmpty()) {
                    _selectedField.value = fields.first()
                    recalculateRoute()
                }
            }
        }

        // Real-time telemetry sync to Firebase Cloud
        viewModelScope.launch {
            telemetry.collectLatest { t ->
                firebaseService.pushTelemetry(t)
            }
        }

        // Active mission sync to Firebase Cloud
        viewModelScope.launch {
            activeMission.collectLatest { m ->
                firebaseService.syncActiveMission(m)
            }
        }

        // Listen for remote cloud commands from Firebase
        firebaseService.listenForRemoteCommands { dir, speed ->
            sendManualCommand(dir, speed)
        }
    }

    fun selectField(field: Field) {
        _selectedField.value = field
        recalculateRoute()
    }

    fun setSelectedPattern(pattern: RoutePattern) {
        _selectedPattern.value = pattern
        recalculateRoute()
    }

    fun setMachineWidth(width: Double) {
        _machineWidth.value = width.coerceIn(0.5, 5.0)
        recalculateRoute()
    }

    fun setHeadlandWidth(headland: Double) {
        _headlandWidth.value = headland.coerceIn(0.5, 10.0)
        recalculateRoute()
    }

    fun setLaneOrientation(orientation: Double) {
        _laneOrientation.value = (orientation % 360.0 + 360.0) % 360.0
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

    fun createField(name: String, points: List<GeoPoint>, onSuccess: (Field) -> Unit) {
        viewModelScope.launch {
            val (area, perim) = PolygonMath.calculateAreaAndPerimeter(points)
            val newField = Field(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Sawah Baru" },
                boundary = points,
                areaM2 = area,
                perimeterM = perim
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
        }
    }

    fun deleteMission(missionId: String) {
        viewModelScope.launch {
            repository.deleteMission(missionId)
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

    fun pauseMissionExecution() {
        val mission = activeMission.value ?: return
        machineService.pauseMission()
        viewModelScope.launch {
            repository.updateMissionStatus(mission.id, MissionStatus.PAUSED)
            repository.logEvent(mission.id, "PAUSE", "Misi dijeda oleh operator", "INFO")
        }
    }

    fun resumeMissionExecution() {
        val mission = activeMission.value ?: return
        machineService.resumeMission()
        viewModelScope.launch {
            repository.updateMissionStatus(mission.id, MissionStatus.RUNNING)
            repository.logEvent(mission.id, "RESUME", "Misi dilanjutkan", "INFO")
        }
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

    fun triggerEmergencyStop() {
        val mission = activeMission.value
        machineService.emergencyStop("Tombol Berhenti Darurat Ditekan")
        if (mission != null) {
            viewModelScope.launch {
                val currentProgress = telemetry.value.missionProgressPct.toDouble()
                repository.updateMissionStatus(mission.id, MissionStatus.STOPPED, currentProgress)
                repository.logEvent(mission.id, "E_STOP", "EMERGENCY STOP DIAKTIFKAN", "CRITICAL")
            }
        }
    }

    fun sendManualCommand(direction: ManualDirection, speedFactor: Float) {
        machineService.handleManualCommand(direction, speedFactor)
    }

    fun updateMachineSettings(settings: MachineSettings) {
        machineService.updateSettings(settings)
    }

    fun injectSimulatorError(errorType: String) {
        machineService.injectError(errorType)
    }

    fun getCurrentGpsLocation(): GeoPoint {
        val t = telemetry.value
        return if (t.latitude != 0.0 && t.longitude != 0.0) {
            GeoPoint(t.latitude, t.longitude)
        } else {
            GeoPoint(-6.923450, 107.610150)
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.seedInitialDataIfNeeded()
        }
    }
}
