package cz.signalradar.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cz.signalradar.app.model.DetectedDevice
import cz.signalradar.app.model.EnvironmentProfile
import cz.signalradar.app.model.ScannerStatus
import cz.signalradar.app.model.SignalFilter
import cz.signalradar.app.model.SignalType
import cz.signalradar.app.model.ViewMode
import cz.signalradar.app.scanner.SignalScannerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SignalRadarUiState(
    val devices: List<DetectedDevice> = emptyList(),
    val allDeviceCount: Int = 0,
    val bleCount: Int = 0,
    val wifiCount: Int = 0,
    val closestDistanceMeters: Float? = null,
    val status: ScannerStatus = ScannerStatus(),
    val searchQuery: String = "",
    val filter: SignalFilter = SignalFilter.ALL,
    val namedOnly: Boolean = false,
    val viewMode: ViewMode = ViewMode.COMBINED,
    val environment: EnvironmentProfile = EnvironmentProfile.INDOOR,
    val radarRangeMeters: Float = 15f,
    val selectedDevice: DetectedDevice? = null,
    val hapticsEnabled: Boolean = false
)

private data class UserControlsState(
    val searchQuery: String = "",
    val filter: SignalFilter = SignalFilter.ALL,
    val namedOnly: Boolean = false,
    val viewMode: ViewMode = ViewMode.COMBINED,
    val environment: EnvironmentProfile = EnvironmentProfile.INDOOR,
    val radarRangeMeters: Float = 15f,
    val selectedDeviceId: String? = null,
    val hapticsEnabled: Boolean = false
)

class SignalRadarViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = SignalScannerEngine(application.applicationContext)
    private val controls = MutableStateFlow(UserControlsState())

    val uiState: StateFlow<SignalRadarUiState> = combine(
        engine.devices,
        engine.status,
        controls
    ) { allDevices, scannerStatus, ctrl ->
        val query = ctrl.searchQuery.trim()
        val filtered = allDevices.filter { dev ->
            val matchesType = when (ctrl.filter) {
                SignalFilter.ALL -> true
                SignalFilter.BLE -> dev.type == SignalType.BLE
                SignalFilter.WIFI -> dev.type == SignalType.WIFI
            }
            val matchesNamed = !ctrl.namedOnly || dev.isNamed
            val matchesQuery = query.isEmpty() ||
                dev.displayName.contains(query, ignoreCase = true) ||
                dev.id.contains(query, ignoreCase = true) ||
                (dev.vendor?.contains(query, ignoreCase = true) == true) ||
                dev.bandLabel.contains(query, ignoreCase = true)
            matchesType && matchesNamed && matchesQuery
        }

        val bleCount = allDevices.count { it.type == SignalType.BLE }
        val wifiCount = allDevices.count { it.type == SignalType.WIFI }
        val closest = filtered.firstOrNull()?.distanceMeters ?: allDevices.firstOrNull()?.distanceMeters
        val selected = ctrl.selectedDeviceId?.let { id -> allDevices.find { it.id == id } }

        SignalRadarUiState(
            devices = filtered,
            allDeviceCount = allDevices.size,
            bleCount = bleCount,
            wifiCount = wifiCount,
            closestDistanceMeters = closest,
            status = scannerStatus,
            searchQuery = ctrl.searchQuery,
            filter = ctrl.filter,
            namedOnly = ctrl.namedOnly,
            viewMode = ctrl.viewMode,
            environment = ctrl.environment,
            radarRangeMeters = ctrl.radarRangeMeters,
            selectedDevice = selected,
            hapticsEnabled = ctrl.hapticsEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SignalRadarUiState()
    )

    fun requiredPermissions(): Array<String> = engine.requiredPermissions().toTypedArray()

    fun hasPermissions(): Boolean = engine.hasRequiredPermissions()

    fun onPermissionsResult(granted: Boolean) {
        if (granted) {
            engine.startScanning(demoMode = false)
        } else {
            engine.stopScanning()
        }
    }

    fun startIfPermitted() {
        if (engine.hasRequiredPermissions()) {
            engine.startScanning(demoMode = false)
        }
    }

    fun toggleScanning() {
        val current = engine.status.value
        if (current.isScanning) {
            engine.stopScanning()
        } else {
            engine.startScanning(demoMode = current.isDemoMode)
        }
    }

    fun toggleDemoMode() {
        val nextDemo = !engine.status.value.isDemoMode
        controls.update { it.copy(selectedDeviceId = null) }
        engine.clearDevices()
        engine.startScanning(demoMode = nextDemo)
    }

    fun startDemoMode() {
        controls.update { it.copy(selectedDeviceId = null) }
        engine.clearDevices()
        engine.startScanning(demoMode = true)
    }

    fun triggerManualPulse() {
        engine.triggerManualPulse()
    }

    fun setSearchQuery(query: String) {
        controls.update { it.copy(searchQuery = query) }
    }

    fun setFilter(filter: SignalFilter) {
        controls.update { it.copy(filter = filter) }
    }

    fun toggleNamedOnly() {
        controls.update { it.copy(namedOnly = !it.namedOnly) }
    }

    fun setViewMode(mode: ViewMode) {
        controls.update { it.copy(viewMode = mode) }
    }

    fun setEnvironment(profile: EnvironmentProfile) {
        controls.update { it.copy(environment = profile) }
        engine.setEnvironmentProfile(profile)
    }

    fun cycleRadarRange() {
        controls.update {
            val next = when {
                it.radarRangeMeters <= 6f -> 15f
                it.radarRangeMeters <= 16f -> 35f
                else -> 5f
            }
            it.copy(radarRangeMeters = next)
        }
    }

    fun selectDevice(device: DetectedDevice?) {
        controls.update { it.copy(selectedDeviceId = device?.id) }
    }

    fun toggleHaptics() {
        controls.update { it.copy(hapticsEnabled = !it.hapticsEnabled) }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}
