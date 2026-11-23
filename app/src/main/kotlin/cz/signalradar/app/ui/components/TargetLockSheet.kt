package cz.signalradar.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.signalradar.app.model.DetectedDevice
import cz.signalradar.app.model.EnvironmentProfile
import cz.signalradar.app.model.SignalType
import cz.signalradar.app.ui.theme.CardBorder
import cz.signalradar.app.ui.theme.CardSurface
import cz.signalradar.app.ui.theme.CardSurfaceElevated
import cz.signalradar.app.ui.theme.CyanElectric
import cz.signalradar.app.ui.theme.DeepSlate
import cz.signalradar.app.ui.theme.EmeraldPulse
import cz.signalradar.app.ui.theme.TextMuted
import cz.signalradar.app.ui.theme.TextPrimary
import cz.signalradar.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/**
 * Handheld Lock-On tracker panel ("v ruce") with large analog-digital meter,
 * live oscilloscope, environment calibration, and optional Geiger proximity haptics.
 */
@Composable
fun TargetLockPanel(
    device: DetectedDevice,
    environment: EnvironmentProfile,
    hapticsEnabled: Boolean,
    onToggleHaptics: () -> Unit,
    onSelectEnvironment: (EnvironmentProfile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (device.type == SignalType.BLE) CyanElectric else EmeraldPulse
    val context = LocalContext.current
    val latestQuality by rememberUpdatedState(device.signalQuality)

    // Optional Geiger-style tactile pulse: faster & crisper as you walk closer
    LaunchedEffect(hapticsEnabled, device.id) {
        if (!hapticsEnabled) return@LaunchedEffect
        val vibrator = getVibrator(context) ?: return@LaunchedEffect
        while (isActive) {
            val q = latestQuality.coerceIn(0, 100)
            val intervalMs = (1150L - q * 9.5f).toLong().coerceIn(130L, 1150L)
            val amplitude = (45 + q * 1.9f).toInt().coerceIn(45, 235)
            val durationMs = if (q > 75) 24L else 14L
            try {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } catch (_: Throwable) {
                // Ignore on emulators without vibrator hardware
            }
            delay(intervalMs)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(DeepSlate)
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.45f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (device.type == SignalType.BLE) Icons.Rounded.Bluetooth else Icons.Rounded.Wifi,
                    contentDescription = device.type.label,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(device.bandLabel)
                        if (!device.vendor.isNullOrBlank()) append(" · ${device.vendor}")
                        append(" · ${device.id}")
                    },
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Zavřít zaměření",
                    tint = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Central analog-digital distance & RSSI gauge
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnalogSignalGauge(
                distanceMeters = device.distanceMeters,
                rssi = device.filteredRssi,
                qualityPercent = device.signalQuality,
                proximityLabel = device.proximityBand.label,
                accentColor = accent,
                sizeDp = 210.dp
            )
        }

        // Live telemetry mini-cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricMiniTile(
                label = "SUROVÝ RSSI",
                value = "${device.rawRssi} dBm",
                modifier = Modifier.weight(1f)
            )
            MetricMiniTile(
                label = "KADENCE",
                value = String.format("%.1f pkt/s", device.packetsPerSecond),
                modifier = Modifier.weight(1f)
            )
            MetricMiniTile(
                label = "PAKETŮ",
                value = "${device.packetCount}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Live oscilloscope container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardSurface)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ŽIVÝ PRŮBĚH SIGNÁLU",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                val minVal = device.history.minOrNull()?.roundToInt() ?: device.rawRssi
                val maxVal = device.history.maxOrNull()?.roundToInt() ?: device.rawRssi
                Text(
                    text = "MIN $minVal / MAX $maxVal dBm",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(6.dp))
            LiveOscilloscope(
                samples = device.history,
                accentColor = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Handheld Geiger vibration toggle + environment calibration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hapticsEnabled) accent.copy(alpha = 0.16f) else CardSurface)
                    .border(
                        1.dp,
                        if (hapticsEnabled) accent else CardBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onToggleHaptics)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = null,
                    tint = if (hapticsEnabled) accent else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (hapticsEnabled) "Vibrace ZAP" else "Vibrace VYP",
                    color = if (hapticsEnabled) accent else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EnvironmentProfile.entries.forEach { prof ->
                    val selected = prof == environment
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) CardSurfaceElevated else CardSurface)
                            .border(
                                1.dp,
                                if (selected) accent.copy(alpha = 0.7f) else CardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectEnvironment(prof) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (prof) {
                                EnvironmentProfile.OPEN_SPACE -> "Volno"
                                EnvironmentProfile.INDOOR -> "Interiér"
                                EnvironmentProfile.WALLS -> "Zdi"
                            },
                            color = if (selected) TextPrimary else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Suppress("DEPRECATION")
private fun getVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        mgr?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}
