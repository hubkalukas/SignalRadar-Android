package cz.signalradar.app.scanner

import cz.signalradar.app.model.EnvironmentProfile
import cz.signalradar.app.model.SignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalMathTest {

    @Test
    fun bleDistanceAtReferenceTxPowerIsOneMeter() {
        val d = SignalMath.estimateDistanceMeters(
            rssi = -59f,
            type = SignalType.BLE,
            txPower = -59,
            frequencyMhz = 2402,
            environment = EnvironmentProfile.INDOOR
        )
        assertEquals(1.0f, d, 0.05f)
    }

    @Test
    fun weakerSignalProducesLargerDistance() {
        val near = SignalMath.estimateDistanceMeters(
            rssi = -52f,
            type = SignalType.BLE,
            txPower = -59,
            frequencyMhz = 2402,
            environment = EnvironmentProfile.INDOOR
        )
        val far = SignalMath.estimateDistanceMeters(
            rssi = -80f,
            type = SignalType.BLE,
            txPower = -59,
            frequencyMhz = 2402,
            environment = EnvironmentProfile.INDOOR
        )
        assertTrue("Expected $far > $near", far > near)
    }

    @Test
    fun kalmanFilterSmoothsNoiseWhileTrackingTrend() {
        val filter = KalmanRssiFilter()
        val first = filter.filter(-70f)
        assertEquals(-70f, first, 0.01f)

        val spiked = filter.filter(-50f)
        assertTrue("Spiked value $spiked should be smoothed between -70 and -50", spiked > -70f && spiked < -50f)
    }

    @Test
    fun qualityPercentIsBoundedZeroToHundred() {
        assertEquals(0, SignalMath.rssiToQualityPercent(-115f))
        assertEquals(100, SignalMath.rssiToQualityPercent(-20f))
        val mid = SignalMath.rssiToQualityPercent(-65f)
        assertTrue(mid in 40..60)
    }
}
