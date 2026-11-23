package cz.signalradar.app.model

enum class SignalType(val label: String) {
    BLE("Bluetooth"),
    WIFI("Wi-Fi")
}

enum class SignalFilter(val label: String) {
    ALL("Vše"),
    BLE("Bluetooth"),
    WIFI("Wi-Fi")
}

enum class ViewMode(val label: String) {
    COMBINED("Přehled"),
    RADAR("Radar"),
    DASHBOARD("Seznam")
}

enum class EnvironmentProfile(
    val label: String,
    val shortLabel: String,
    val pathLossExponent: Float
) {
    OPEN_SPACE("Venku", "Volno", 2.0f),
    INDOOR("Interiér", "Interiér", 2.5f),
    WALLS("Přes zdi", "Zdi", 3.2f)
}

enum class ProximityBand(val label: String) {
    IMMEDIATE("Na dosah"),
    VERY_NEAR("Velmi blízko"),
    NEAR("Blízko"),
    MEDIUM("Střední"),
    FAR("Daleko")
}

data class DetectedDevice(
    val id: String,
    val displayName: String,
    val rawName: String?,
    val vendor: String?,
    val type: SignalType,
    val rawRssi: Int,
    val filteredRssi: Float,
    val txPower: Int?,
    val frequencyMhz: Int?,
    val distanceMeters: Float,
    val signalQuality: Int,
    val angleDegrees: Float,
    val lastSeenNanos: Long,
    val packetCount: Long,
    val packetsPerSecond: Float,
    val history: List<Float>,
    val bandLabel: String,
    val extraInfo: String? = null
) {
    val isNamed: Boolean
        get() = !rawName.isNullOrBlank()

    val proximityBand: ProximityBand
        get() = when {
            distanceMeters < 1.0f -> ProximityBand.IMMEDIATE
            distanceMeters < 3.0f -> ProximityBand.VERY_NEAR
            distanceMeters < 8.0f -> ProximityBand.NEAR
            distanceMeters < 18.0f -> ProximityBand.MEDIUM
            else -> ProximityBand.FAR
        }
}

data class ScannerStatus(
    val isScanning: Boolean = false,
    val isDemoMode: Boolean = false,
    val bleActive: Boolean = false,
    val wifiActive: Boolean = false,
    val bluetoothEnabled: Boolean = true,
    val wifiEnabled: Boolean = true,
    val locationEnabled: Boolean = true,
    val hasPermissions: Boolean = false,
    val sampleIntervalMs: Float = 0.5f,
    val engineTicksPerSec: Int = 2000,
    val totalPackets: Long = 0L,
    val lastPacketAgeMs: Long = 0L,
    val statusMessage: String? = null
)
