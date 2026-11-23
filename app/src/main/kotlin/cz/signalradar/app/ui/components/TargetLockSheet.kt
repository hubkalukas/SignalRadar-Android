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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import cz.signalradar.app.ui.theme.AccentBlue
import cz.signalradar.app.ui.theme.AccentGreen
import cz.signalradar.app.ui.theme.BgControl
import cz.signalradar.app.ui.theme.BgElevated
import cz.signalradar.app.ui.theme.BgSurface
import cz.signalradar.app.ui.theme.Hairline
import cz.signalradar.app.ui.theme.InkPrimary
import cz.signalradar.app.ui.theme.InkSecondary
import cz.signalradar.app.ui.theme.InkTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

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
    val accent = if (device.type == SignalType.BLE) AccentBlue else AccentGreen
    val context = LocalContext.current
    val latestQuality by rememberUpdatedState(device.signalQuality)

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
                // Emulator without vibrator
            }
            delay(intervalMs)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(BgElevated)
            .border(
                width = 0.5.dp,
                color = Hairline,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Sheet grab handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Hairline)
        )

        Spacer(Modifier.height(14.dp))

        // Device header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (device.type == SignalType.BLE) Icons.Rounded.Bluetooth else Icons.Rounded.Wifi,
                    contentDescription = device.type.label,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    color = InkPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(device.bandLabel)
                        if (!device.vendor.isNullOrBlank()) append(" · ${device.vendor}")
                        append(" · ${device.id}")
                    },
                    color = InkSecondary,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BgControl)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Zavřít",
                    tint = InkSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnalogSignalGauge(
                distanceMeters = device.distanceMeters,
                rssi = device.filteredRssi,
                qualityPercent = device.signalQuality,
                proximityLabel = device.proximityBand.label,
                sizeDp = 208.dp
            )
        }

        // Clean single-row metrics separated by subtle dividers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgSurface)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricColumn("Signál", "${device.rawRssi} dBm", Modifier.weight(1f))
            Box(Modifier.width(0.5.dp).height(26.dp).background(Hairline))
            MetricColumn("Frekvence", String.format("%.1f / s", device.packetsPerSecond), Modifier.weight(1f))
            Box(Modifier.width(0.5.dp).height(26.dp).background(Hairline))
            MetricColumn("Paketů", "${device.packetCount}", Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))

        // Signal history chart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Průběh signálu",
                color = InkSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            val minVal = device.history.minOrNull()?.roundToInt() ?: device.rawRssi
            val maxVal = device.history.maxOrNull()?.roundToInt() ?: device.rawRssi
            Text(
                text = "$minVal až $maxVal dBm",
                color = InkTertiary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(6.dp))
        LiveOscilloscope(
            samples = device.history,
            accentColor = accent,
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        )

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Hairline)
        Spacer(Modifier.height(12.dp))

        // Bottom controls: Haptics + Environment selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (hapticsEnabled) InkPrimary else BgSurface)
                    .clickable(onClick = onToggleHaptics)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = null,
                    tint = if (hapticsEnabled) BgElevated else InkSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Vibrace",
                    color = if (hapticsEnabled) BgElevated else InkSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgSurface)
                    .padding(2.dp)
            ) {
                EnvironmentProfile.entries.forEach { prof ->
                    val selected = prof == environment
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) BgControl else Color.Transparent)
                            .clickable { onSelectEnvironment(prof) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = prof.shortLabel,
                            color = if (selected) InkPrimary else InkTertiary,
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = InkPrimary,
            fontSize = 13.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = InkTertiary,
            fontSize = 11.sp
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
