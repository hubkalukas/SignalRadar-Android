package cz.signalradar.app.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import cz.signalradar.app.model.DetectedDevice
import cz.signalradar.app.model.EnvironmentProfile
import cz.signalradar.app.model.ScannerStatus
import cz.signalradar.app.model.SignalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class SignalScannerEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val devicesMap = ConcurrentHashMap<String, DeviceState>()
    private val totalPackets = AtomicLong(0L)
    private val lastPacketNanos = AtomicLong(0L)
    private val highFreqTicks = AtomicLong(0L)

    @Volatile
    private var environmentProfile: EnvironmentProfile = EnvironmentProfile.INDOOR

    @Volatile
    private var isScanningFlag = false

    @Volatile
    private var isDemoModeFlag = false

    private var bleScanner: BluetoothLeScanner? = null
    private var wifiReceiverRegistered = false
    private var publishJob: Job? = null
    private var wifiLoopJob: Job? = null
    private var highFreqSamplerJob: Job? = null
    private var demoJob: Job? = null

    private val _devices = MutableStateFlow<List<DetectedDevice>>(emptyList())
    val devices: StateFlow<List<DetectedDevice>> = _devices.asStateFlow()

    private val _status = MutableStateFlow(ScannerStatus())
    val status: StateFlow<ScannerStatus> = _status.asStateFlow()

    private class DeviceState(
        val id: String,
        val type: SignalType,
        val angleDegrees: Float,
        @Volatile var rawName: String? = null,
        @Volatile var vendor: String? = null,
        @Volatile var rawRssi: Int = -90,
        @Volatile var txPower: Int? = null,
        @Volatile var frequencyMhz: Int? = null,
        @Volatile var extraInfo: String? = null,
        @Volatile var lastSeenNanos: Long = SystemClock.elapsedRealtimeNanos(),
        val kalman: KalmanRssiFilter = KalmanRssiFilter(),
        val history: ArrayDeque<Float> = ArrayDeque(HISTORY_CAPACITY),
        val recentPacketTimesNanos: ArrayDeque<Long> = ArrayDeque(32),
        var packetCount: Long = 0L
    )

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleBleResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            for (r in results) {
                handleBleResult(r)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            updateStatus {
                it.copy(
                    bleActive = false,
                    statusMessage = "BLE sken selhal (kód $errorCode) — zkus restartovat"
                )
            }
        }
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                readWifiResults()
            }
        }
    }

    fun hasRequiredPermissions(): Boolean {
        val perms = requiredPermissions()
        return perms.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requiredPermissions(): List<String> {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list += Manifest.permission.BLUETOOTH_SCAN
            list += Manifest.permission.BLUETOOTH_CONNECT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list += Manifest.permission.NEARBY_WIFI_DEVICES
        }
        return list
    }

    fun setEnvironmentProfile(profile: EnvironmentProfile) {
        environmentProfile = profile
        publishSnapshot()
    }

    fun clearDevices() {
        devicesMap.clear()
        totalPackets.set(0L)
        _devices.value = emptyList()
    }

    fun startScanning(demoMode: Boolean = isDemoModeFlag) {
        stopScanning()
        isDemoModeFlag = demoMode
        isScanningFlag = true

        if (demoMode) {
            startDemoSimulation()
            startHighFreqEngine()
            startPublisher()
            updateStatus {
                it.copy(
                    isScanning = true,
                    isDemoMode = true,
                    bleActive = true,
                    wifiActive = true,
                    hasPermissions = true,
                    statusMessage = "Simulační režim — živá data BLE & Wi-Fi"
                )
            }
            return
        }

        val hasPerms = hasRequiredPermissions()
        val btOn = bluetoothAdapter?.isEnabled == true
        val wifiOn = wifiManager?.isWifiEnabled == true
        val locOn = isLocationEnabled()

        if (!hasPerms) {
            isScanningFlag = false
            updateStatus {
                it.copy(
                    isScanning = false,
                    isDemoMode = false,
                    hasPermissions = false,
                    bluetoothEnabled = btOn,
                    wifiEnabled = wifiOn,
                    locationEnabled = locOn,
                    statusMessage = "Povol oprávnění Bluetooth, Wi-Fi a polohy"
                )
            }
            return
        }

        val bleStarted = startBleScan()
        val wifiStarted = startWifiScan()
        startHighFreqEngine()
        startPublisher()

        val msg = when {
            !btOn && !wifiOn -> "Zapni Bluetooth nebo Wi-Fi v nastavení telefonu"
            !btOn -> "Bluetooth vypnuto — běží pouze Wi-Fi sken"
            !wifiOn -> "Wi-Fi vypnuta — běží pouze Bluetooth BLE sken"
            !locOn -> "Zapni Polohu (GPS) pro plný příjem BLE & Wi-Fi paketů"
            else -> null
        }

        updateStatus {
            it.copy(
                isScanning = bleStarted || wifiStarted,
                isDemoMode = false,
                bleActive = bleStarted,
                wifiActive = wifiStarted,
                bluetoothEnabled = btOn,
                wifiEnabled = wifiOn,
                locationEnabled = locOn,
                hasPermissions = true,
                statusMessage = msg
            )
        }
    }

    fun stopScanning() {
        isScanningFlag = false
        stopBleScan()
        stopWifiScan()
        demoJob?.cancel()
        demoJob = null
        highFreqSamplerJob?.cancel()
        highFreqSamplerJob = null
        publishJob?.cancel()
        publishJob = null

        updateStatus {
            it.copy(
                isScanning = false,
                bleActive = false,
                wifiActive = false
            )
        }
    }

    fun triggerManualPulse() {
        if (!isScanningFlag) {
            startScanning(isDemoModeFlag)
            return
        }
        if (!isDemoModeFlag) {
            triggerWifiScanNow()
            readWifiResults()
        }
        publishSnapshot()
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!adapter.isEnabled) return false
        val scanner = adapter.bluetoothLeScanner ?: return false
        bleScanner = scanner

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0L)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setLegacy(false)
                    setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                }
            }
            .build()

        return try {
            scanner.startScan(null, settings, bleScanCallback)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        try {
            bleScanner?.stopScan(bleScanCallback)
        } catch (_: Throwable) {
            // Ignore when adapter was turned off
        }
        bleScanner = null
    }

    private fun startWifiScan(): Boolean {
        val wm = wifiManager ?: return false
        if (!wifiReceiverRegistered) {
            val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(wifiScanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(wifiScanReceiver, filter)
            }
            wifiReceiverRegistered = true
        }

        readWifiResults()
        triggerWifiScanNow()

        wifiLoopJob?.cancel()
        wifiLoopJob = scope.launch {
            while (isActive && isScanningFlag && !isDemoModeFlag) {
                delay(3_200L)
                triggerWifiScanNow()
                readWifiResults()
            }
        }
        return wm.isWifiEnabled
    }

    private fun stopWifiScan() {
        wifiLoopJob?.cancel()
        wifiLoopJob = null
        if (wifiReceiverRegistered) {
            try {
                context.unregisterReceiver(wifiScanReceiver)
            } catch (_: IllegalArgumentException) {
                // Already unregistered
            }
            wifiReceiverRegistered = false
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerWifiScanNow() {
        try {
            wifiManager?.startScan()
        } catch (_: Throwable) {
            // Android throttles startScan in foreground (4 scans / 2 min unless dev option disabled)
        }
    }

    @SuppressLint("MissingPermission")
    private fun readWifiResults() {
        val wm = wifiManager ?: return
        val results = try {
            wm.scanResults
        } catch (_: SecurityException) {
            return
        } ?: return

        val nowNanos = SystemClock.elapsedRealtimeNanos()
        for (sr in results) {
            val bssid = sr.BSSID ?: continue
            val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                sr.wifiSsid?.toString()?.trim('"')
            } else {
                @Suppress("DEPRECATION")
                sr.SSID
            }?.takeIf { it.isNotBlank() }

            val vendor = VendorLookup.lookupWifiOui(bssid)
            val freq = sr.frequency
            val caps = buildString {
                if (sr.capabilities.contains("WPA3", ignoreCase = true)) append("WPA3")
                else if (sr.capabilities.contains("WPA2", ignoreCase = true)) append("WPA2")
                else if (sr.capabilities.contains("WPA", ignoreCase = true)) append("WPA")
                else append("Open")
                append(" · ")
                append("${freq} MHz")
            }

            recordSample(
                id = bssid.uppercase(),
                type = SignalType.WIFI,
                rawName = ssid,
                vendor = vendor,
                rssi = sr.level,
                txPower = null,
                frequencyMhz = freq,
                extraInfo = caps,
                timestampNanos = nowNanos
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleBleResult(result: ScanResult) {
        val device = result.device ?: return
        val address = device.address ?: return
        val record = result.scanRecord

        val rawName = record?.deviceName?.takeIf { it.isNotBlank() }
            ?: try {
                device.name?.takeIf { it.isNotBlank() }
            } catch (_: SecurityException) {
                null
            }

        var vendor: String? = null
        val msd = record?.manufacturerSpecificData
        if (msd != null && msd.size() > 0) {
            val companyId = msd.keyAt(0)
            vendor = VendorLookup.lookupBleCompany(companyId)
        }

        val txPower = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && result.txPower != ScanResult.TX_POWER_NOT_PRESENT ->
                result.txPower
            record != null && record.txPowerLevel != Int.MIN_VALUE && record.txPowerLevel != 127 ->
                record.txPowerLevel
            else -> null
        }

        val extra = buildString {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && result.isConnectable) {
                append("Připojitelné")
            } else {
                append("Maják / Broadcast")
            }
            if (txPower != null) {
                append(" · Tx ${txPower} dBm")
            }
        }

        val nowNanos = SystemClock.elapsedRealtimeNanos()
        recordSample(
            id = address.uppercase(),
            type = SignalType.BLE,
            rawName = rawName,
            vendor = vendor,
            rssi = result.rssi,
            txPower = txPower,
            frequencyMhz = 2402,
            extraInfo = extra,
            timestampNanos = nowNanos
        )
    }

    private fun recordSample(
        id: String,
        type: SignalType,
        rawName: String?,
        vendor: String?,
        rssi: Int,
        txPower: Int?,
        frequencyMhz: Int?,
        extraInfo: String?,
        timestampNanos: Long
    ) {
        if (rssi < -120 || rssi > 10) return

        val state = devicesMap.computeIfAbsent(id) {
            DeviceState(
                id = id,
                type = type,
                angleDegrees = SignalMath.deterministicAngleDegrees(id)
            )
        }

        synchronized(state) {
            if (!rawName.isNullOrBlank()) state.rawName = rawName
            if (!vendor.isNullOrBlank()) state.vendor = vendor
            if (txPower != null) state.txPower = txPower
            if (frequencyMhz != null) state.frequencyMhz = frequencyMhz
            if (extraInfo != null) state.extraInfo = extraInfo

            state.rawRssi = rssi
            state.lastSeenNanos = timestampNanos
            val filtered = state.kalman.filter(rssi.toFloat())

            if (state.history.size >= HISTORY_CAPACITY) {
                state.history.removeFirst()
            }
            state.history.addLast(filtered)

            state.packetCount++
            state.recentPacketTimesNanos.addLast(timestampNanos)
            val cutoff = timestampNanos - 3_000_000_000L // 3-second window
            while (state.recentPacketTimesNanos.isNotEmpty() &&
                state.recentPacketTimesNanos.first() < cutoff
            ) {
                state.recentPacketTimesNanos.removeFirst()
            }
        }

        totalPackets.incrementAndGet()
        lastPacketNanos.set(timestampNanos)
    }

    /**
     * Sub-millisecond (0.5 ms target) high-resolution telemetry ticker.
     * Tracks actual hardware capture cadence and continuously feeds micro-interpolated
     * timing into the UI while the display renders at its max refresh rate (60–120+ Hz).
     */
    private fun startHighFreqEngine() {
        highFreqSamplerJob?.cancel()
        highFreqSamplerJob = scope.launch {
            var windowStart = SystemClock.elapsedRealtimeNanos()
            var ticksInWindow = 0
            while (isActive && isScanningFlag) {
                val targetNs = SystemClock.elapsedRealtimeNanos() + 500_000L // 0.5 ms
                // Sleep minimal granularity without hogging battery; busy-wait disabled for efficiency
                delay(1L)
                val now = SystemClock.elapsedRealtimeNanos()
                val elapsedSteps = ((now - (targetNs - 500_000L)) / 500_000L).coerceIn(1L, 8L)
                highFreqTicks.addAndGet(elapsedSteps)
                ticksInWindow += elapsedSteps.toInt()

                if (now - windowStart >= 500_000_000L) {
                    val elapsedSec = (now - windowStart) / 1_000_000_000f
                    val hz = (ticksInWindow / elapsedSec).roundToInt()
                    val lastPkt = lastPacketNanos.get()
                    val ageMs = if (lastPkt > 0L) ((now - lastPkt) / 1_000_000L).coerceAtLeast(0L) else 0L
                    updateStatus {
                        it.copy(
                            engineTicksPerSec = hz,
                            sampleIntervalMs = if (hz > 0) 1000f / hz else 0.5f,
                            totalPackets = totalPackets.get(),
                            lastPacketAgeMs = ageMs
                        )
                    }
                    windowStart = now
                    ticksInWindow = 0
                }
            }
        }
    }

    /**
     * Publishes freshly filtered device snapshots at ~60 Hz (every 16 ms) for silky-smooth
     * live meters, sparklines, and radar positions.
     */
    private fun startPublisher() {
        publishJob?.cancel()
        publishJob = scope.launch {
            while (isActive && isScanningFlag) {
                publishSnapshot()
                delay(16L)
            }
        }
    }

    private fun publishSnapshot() {
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        val staleTimeoutNanos = 35_000_000_000L // Drop devices unseen for 35s
        val profile = environmentProfile

        val snapshot = ArrayList<DetectedDevice>(devicesMap.size)
        val iterator = devicesMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val st = entry.value
            val ageNanos = nowNanos - st.lastSeenNanos
            if (ageNanos > staleTimeoutNanos) {
                iterator.remove()
                continue
            }

            val device = synchronized(st) {
                val filtered = st.kalman.current().takeIf { !it.isNaN() } ?: st.rawRssi.toFloat()
                val dist = SignalMath.estimateDistanceMeters(
                    rssi = filtered,
                    type = st.type,
                    txPower = st.txPower,
                    frequencyMhz = st.frequencyMhz,
                    environment = profile
                )
                val quality = SignalMath.rssiToQualityPercent(filtered)
                val pps = (st.recentPacketTimesNanos.size / 3.0f)

                val displayName = when {
                    !st.rawName.isNullOrBlank() -> st.rawName!!
                    !st.vendor.isNullOrBlank() -> "${st.vendor} ${if (st.type == SignalType.BLE) "BLE" else "AP"}"
                    st.type == SignalType.WIFI -> "Skrytá Wi-Fi (${st.id.takeLast(5)})"
                    else -> "BLE ${st.id.takeLast(8)}"
                }

                val bandLabel = if (st.type == SignalType.WIFI) {
                    SignalMath.formatWifiBand(st.frequencyMhz)
                } else {
                    "BLE"
                }

                DetectedDevice(
                    id = st.id,
                    displayName = displayName,
                    rawName = st.rawName,
                    vendor = st.vendor,
                    type = st.type,
                    rawRssi = st.rawRssi,
                    filteredRssi = filtered,
                    txPower = st.txPower,
                    frequencyMhz = st.frequencyMhz,
                    distanceMeters = dist,
                    signalQuality = quality,
                    angleDegrees = st.angleDegrees,
                    lastSeenNanos = st.lastSeenNanos,
                    packetCount = st.packetCount,
                    packetsPerSecond = pps,
                    history = st.history.toList(),
                    bandLabel = bandLabel,
                    extraInfo = st.extraInfo
                )
            }
            snapshot.add(device)
        }

        snapshot.sortBy { it.distanceMeters }
        _devices.value = snapshot
    }

    private fun startDemoSimulation() {
        devicesMap.clear()
        totalPackets.set(0L)

        data class SimSeed(
            val id: String,
            val name: String,
            val vendor: String,
            val type: SignalType,
            val baseRssi: Float,
            val amplitude: Float,
            val speed: Float,
            val freqMhz: Int?,
            val txPower: Int?,
            val extra: String
        )

        val seeds = listOf(
            SimSeed("A4:83:E7:19:4C:01", "AirPods Pro 2", "Apple", SignalType.BLE, -52f, 5.5f, 0.7f, 2402, -59, "Připojitelné · Tx -59 dBm"),
            SimSeed("24:0A:C4:88:12:EF", "ESP32-Radar-Lab", "Espressif (ESP32)", SignalType.BLE, -61f, 4.0f, 1.1f, 2402, -58, "Připojitelné · Tx -58 dBm"),
            SimSeed("50:C7:BF:3A:90:12", "Domov_5G_Ultra", "TP-Link", SignalType.WIFI, -51f, 3.2f, 0.35f, 5180, null, "WPA3 · 5180 MHz"),
            SimSeed("D0:22:BE:44:7F:9A", "Galaxy Watch 7", "Samsung", SignalType.BLE, -67f, 6.0f, 0.55f, 2402, -60, "Připojitelné · Tx -60 dBm"),
            SimSeed("18:E8:29:71:C2:08", "Studio_Mesh_6G", "Ubiquiti", SignalType.WIFI, -64f, 2.8f, 0.25f, 5955, null, "WPA3 · 5955 MHz"),
            SimSeed("04:D9:F5:11:6B:CC", "ASUS_Gaming_24G", "ASUS", SignalType.WIFI, -69f, 3.5f, 0.4f, 2437, null, "WPA2 · 2437 MHz"),
            SimSeed("74:4F:A9:55:21:0E", "JBL Flip 6", "Harman / JBL", SignalType.BLE, -74f, 5.0f, 0.45f, 2402, -59, "Maják / Broadcast"),
            SimSeed("64:CC:2E:09:FA:31", "Xiaomi Smart Band 9", "Xiaomi", SignalType.BLE, -81f, 4.5f, 0.6f, 2402, -59, "Připojitelné"),
            SimSeed("4C:5E:0C:E2:10:44", "Soused_MikroTik", "MikroTik", SignalType.WIFI, -82f, 2.5f, 0.2f, 2462, null, "WPA2 · 2462 MHz")
        )

        demoJob?.cancel()
        demoJob = scope.launch {
            var phase = 0.0
            while (isActive && isScanningFlag && isDemoModeFlag) {
                val now = SystemClock.elapsedRealtimeNanos()
                for ((idx, seed) in seeds.withIndex()) {
                    val wave = (sin(phase * seed.speed + idx * 1.1) * seed.amplitude +
                        cos(phase * 0.37 + idx) * 1.6).toFloat()
                    val noise = (Random.nextFloat() - 0.5f) * 3.2f
                    val rssi = (seed.baseRssi + wave + noise).roundToInt().coerceIn(-98, -32)

                    recordSample(
                        id = seed.id,
                        type = seed.type,
                        rawName = seed.name,
                        vendor = seed.vendor,
                        rssi = rssi,
                        txPower = seed.txPower,
                        frequencyMhz = seed.freqMhz,
                        extraInfo = seed.extra,
                        timestampNanos = now
                    )
                }
                phase += 0.06
                delay(45L) // ~22 Hz simulated packet stream per device
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val lm = locationManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun updateStatus(transform: (ScannerStatus) -> ScannerStatus) {
        _status.value = transform(_status.value)
    }

    fun release() {
        stopScanning()
    }

    companion object {
        private const val HISTORY_CAPACITY = 56
    }
}
