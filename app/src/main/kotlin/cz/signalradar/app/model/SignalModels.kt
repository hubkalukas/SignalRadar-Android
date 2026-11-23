package cz.signalradar.app.model

enum class SignalType(val label: String) {
    BLE("BLE"),
    WIFI("Wi-Fi")
}

enum class SignalFilter(val label: String) {
    ALL("Vše"),
    BLE("Bluetooth"),
    WIFI("Wi-Fi")
}

enum class ViewMode(val label: String) {
    COMBINED("Radar + Panel"),
    RADAR("Radar"),
    DASHBOARD("Dashboard")
}

enum class EnvironmentProfile(
    val label: String,
    val shortLabel: String,
    val pathLossExponent: Float,
    val description: String
) {
    OPEN_SPACE("Otevřený prostor", "Volno (n=2.0)", 2.0f, "Přímá viditelnost bez překážek"),
    INDOOR("Interiér", "Interiér (n=2.5)", 2.5f, "Běžná místnost, nábytek, lehké příčky"),
    WALLS("Přes zdi", "Zdi (n=3.2)", 3.2f, "Silnější stěny, více překážek")
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
    val signalQuality: Int, // 0..100
    val angleDegrees: Float, // 0..360 deterministic radar angle
    val lastSeenNanos: Long,
    val packetCount: Long,
    val packetsPerSecond: Float,
    val history: List<Float>, // recent filtered RSSI samples
    val bandLabel: String, // e.g. "2.4 GHz", "5 GHz", "BLE 5"
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
