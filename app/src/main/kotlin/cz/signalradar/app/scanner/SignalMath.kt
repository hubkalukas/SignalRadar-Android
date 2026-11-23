package cz.signalradar.app.scanner

import cz.signalradar.app.model.EnvironmentProfile
import cz.signalradar.app.model.SignalType
import kotlin.math.abs
import kotlin.math.pow

class KalmanRssiFilter(
    private val processNoise: Float = 0.65f,
    private val measurementNoise: Float = 5.5f
) {
    private var estimate: Float = Float.NaN
    private var errorCovariance: Float = 1.0f

    fun filter(measurement: Float): Float {
        if (estimate.isNaN()) {
            estimate = measurement
            errorCovariance = 1.0f
            return estimate
        }
        val priorCovariance = errorCovariance + processNoise
        val kalmanGain = priorCovariance / (priorCovariance + measurementNoise)
        estimate += kalmanGain * (measurement - estimate)
        errorCovariance = (1f - kalmanGain) * priorCovariance
        return estimate
    }

    fun current(): Float = estimate
}

object SignalMath {

    fun estimateDistanceMeters(
        rssi: Float,
        type: SignalType,
        txPower: Int?,
        frequencyMhz: Int?,
        environment: EnvironmentProfile
    ): Float {
        val n = environment.pathLossExponent.toDouble()
        val referenceTxPower = when (type) {
            SignalType.BLE -> {
                // Valid BLE calibrated TxPower at 1m is typically between -85 and -35 dBm
                // Android ScanRecord.getTxPowerLevel() returns 127 when unavailable.
                if (txPower != null && txPower in -90..-30) {
                    txPower.toDouble()
                } else {
                    -59.0
                }
            }
            SignalType.WIFI -> {
                // Typical 1m reference RSSI depends on frequency band (higher freq attenuates faster)
                when {
                    frequencyMhz == null -> -40.0
                    frequencyMhz >= 5900 -> -47.0 // 6 GHz
                    frequencyMhz >= 4900 -> -44.0 // 5 GHz
                    else -> -38.0 // 2.4 GHz
                }
            }
        }

        val exponent = (referenceTxPower - rssi.toDouble()) / (10.0 * n)
        val rawDistance = 10.0.pow(exponent).toFloat()
        val maxRange = if (type == SignalType.WIFI) 80.0f else 55.0f
        return rawDistance.coerceIn(0.10f, maxRange)
    }

    fun rssiToQualityPercent(rssi: Float): Int {
        val minRssi = -98f
        val maxRssi = -32f
        val normalized = ((rssi - minRssi) / (maxRssi - minRssi)).coerceIn(0f, 1f)
        return (normalized * 100f).toInt()
    }

    fun deterministicAngleDegrees(id: String): Float {
        var hash = 2166136261L
        for (ch in id) {
            hash = hash xor ch.code.toLong()
            hash = (hash * 16777619L) and 0xFFFFFFFFL
        }
        val golden = (abs(hash) % 3600L) / 10f
        return golden
    }

    fun formatDistance(meters: Float): String {
        return when {
            meters < 1.0f -> String.format("%.2f m", meters)
            meters < 10.0f -> String.format("%.1f m", meters)
            else -> String.format("%.0f m", meters)
        }
    }

    fun formatWifiBand(freqMhz: Int?): String {
        return when {
            freqMhz == null -> "Wi-Fi"
            freqMhz >= 5925 -> "6 GHz"
            freqMhz >= 4900 -> "5 GHz"
            else -> "2.4 GHz"
        }
    }
}

object VendorLookup {

    private val bleCompanies = mapOf(
        0x004C to "Apple",
        0x0006 to "Microsoft",
        0x0075 to "Samsung",
        0x00E0 to "Google",
        0x012D to "Sony",
        0x038F to "Xiaomi",
        0x009E to "Bose",
        0x0057 to "Harman / JBL",
        0x0087 to "Garmin",
        0x067C to "Tile",
        0x027D to "Huawei",
        0x02E5 to "Espressif (ESP32)",
        0x0059 to "Nordic Semi",
        0x0131 to "Cypress",
        0x01AB to "Meta",
        0x0494 to "Sennheiser",
        0x00D2 to "Dialog",
        0x000F to "Broadcom",
        0x001D to "Qualcomm",
        0x000A to "Qualcomm CSR",
        0x0002 to "Intel",
        0x008A to "Jawbone",
        0x0157 to "Anhui Huami (Amazfit)",
        0x074F to "Anker / Soundcore",
        0x00E7 to "Jabra",
        0x0397 to "LEGO",
        0x0553 to "Nintendo",
        0x0310 to "SGL",
        0x022B to "Tesla",
        0x0171 to "Amazon",
        0x0822 to "Shenzhen Goodix",
        0x0046 to "MediaTek",
        0x0065 to "HP",
        0x00B8 to "Logitech"
    )

    private val wifiOuiMap = mapOf(
        "00:1A:2B" to "Ayecom",
        "14:CC:20" to "TP-Link",
        "50:C7:BF" to "TP-Link",
        "C0:25:E9" to "TP-Link",
        "B0:BE:76" to "TP-Link",
        "98:DA:C4" to "TP-Link",
        "18:E8:29" to "Ubiquiti",
        "74:83:C2" to "Ubiquiti",
        "80:2A:A8" to "Ubiquiti",
        "DC:9F:DB" to "Ubiquiti",
        "04:D9:F5" to "ASUS",
        "2C:FD:A1" to "ASUS",
        "AC:9E:17" to "ASUS",
        "B0:6E:BF" to "ASUS",
        "4C:5E:0C" to "MikroTik",
        "CC:2D:E0" to "MikroTik",
        "D4:CA:6D" to "MikroTik",
        "E4:8D:8C" to "MikroTik",
        "C8:0E:14" to "AVM FRITZ!Box",
        "34:31:C4" to "AVM FRITZ!Box",
        "00:25:9C" to "Cisco / Linksys",
        "3C:CE:73" to "Cisco",
        "28:6E:D4" to "Huawei",
        "88:53:D4" to "Huawei",
        "F4:55:9C" to "Huawei",
        "64:CC:2E" to "Xiaomi",
        "78:11:DC" to "Xiaomi",
        "58:B6:23" to "Apple",
        "AC:BC:32" to "Apple",
        "A4:83:E7" to "Apple",
        "D0:22:BE" to "Samsung",
        "8C:F5:A3" to "Samsung",
        "24:0A:C4" to "Espressif (ESP32)",
        "30:AE:A4" to "Espressif (ESP32)"
    )

    fun lookupBleCompany(companyId: Int): String? = bleCompanies[companyId]

    fun lookupWifiOui(bssid: String): String? {
        if (bssid.length < 8) return null
        val prefix = bssid.substring(0, 8).uppercase()
        return wifiOuiMap[prefix]
    }
}
