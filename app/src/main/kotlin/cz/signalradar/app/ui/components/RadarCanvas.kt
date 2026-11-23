package cz.signalradar.app.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.signalradar.app.model.DetectedDevice
import cz.signalradar.app.model.SignalType
import cz.signalradar.app.scanner.SignalMath
import cz.signalradar.app.ui.theme.CardBorder
import cz.signalradar.app.ui.theme.CardSurface
import cz.signalradar.app.ui.theme.CyanElectric
import cz.signalradar.app.ui.theme.DeepSlate
import cz.signalradar.app.ui.theme.EmeraldPulse
import cz.signalradar.app.ui.theme.ObsidianBg
import cz.signalradar.app.ui.theme.TextMuted
import cz.signalradar.app.ui.theme.TextPrimary
import cz.signalradar.app.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interactive real-time radar displaying BLE (cyan) and Wi-Fi (emerald) targets
 * positioned radially by estimated distance in meters.
 */
@Composable
fun SignalRadarScope(
    devices: List<DetectedDevice>,
    selectedDeviceId: String?,
    maxRangeMeters: Float,
    isScanning: Boolean,
    onSelectDevice: (DetectedDevice) -> Unit,
    onCycleRange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulsePhase"
    )

    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun normalizedRadius(distanceMeters: Float): Float {
        // Sqrt compression gives generous visual space to close-range devices (0.2m - 4m)
        val linear = (distanceMeters / maxRangeMeters).coerceIn(0.02f, 1.0f)
        return (0.10f + 0.86f * sqrt(linear)).coerceIn(0.10f, 0.96f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.06f)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0E1A30), DeepSlate, ObsidianBg)
                )
            )
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .onSizeChanged { canvasSize = it }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(devices, maxRangeMeters, canvasSize) {
                    detectTapGestures { tapOffset ->
                        if (canvasSize.width == 0 || devices.isEmpty()) return@detectTapGestures
                        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                        val maxR = minOf(canvasSize.width, canvasSize.height) * 0.44f
                        val hitRadiusPx = 42.dp.toPx()

                        var best: DetectedDevice? = null
                        var bestDist = hitRadiusPx
                        for (dev in devices.take(36)) {
                            val r = normalizedRadius(dev.distanceMeters) * maxR
                            val rad = (dev.angleDegrees - 90f) * (PI / 180.0)
                            val px = center.x + (cos(rad) * r).toFloat()
                            val py = center.y + (sin(rad) * r).toFloat()
                            val d = hypot(tapOffset.x - px, tapOffset.y - py)
                            if (d < bestDist) {
                                bestDist = d
                                best = dev
                            }
                        }
                        if (best != null) {
                            onSelectDevice(best)
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = minOf(size.width, size.height) * 0.44f

            // Ambient radar dish glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyanElectric.copy(alpha = 0.08f),
                        CyanElectric.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR * 1.05f
                ),
                radius = maxR * 1.05f,
                center = center
            )

            // Concentric distance rings
            val ringDistances = when {
                maxRangeMeters <= 6f -> listOf(1f, 2.5f, 5f)
                maxRangeMeters <= 16f -> listOf(2f, 6f, 15f)
                else -> listOf(3f, 12f, 35f)
            }

            val ringLabelStyle = TextStyle(
                color = TextMuted.copy(alpha = 0.85f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )

            ringDistances.forEachIndexed { idx, distM ->
                val r = normalizedRadius(distM) * maxR
                val isOuter = idx == ringDistances.lastIndex
                drawCircle(
                    color = if (isOuter) CyanElectric.copy(alpha = 0.28f) else CardBorder.copy(alpha = 0.85f),
                    radius = r,
                    center = center,
                    style = Stroke(
                        width = if (isOuter) 1.4.dp.toPx() else 1.dp.toPx(),
                        pathEffect = if (!isOuter) PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) else null
                    )
                )
                val labelText = if (distM % 1f == 0f) "${distM.toInt()} m" else "${distM} m"
                val measured = textMeasurer.measure(labelText, ringLabelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(center.x + 4.dp.toPx(), center.y - r + 2.dp.toPx())
                )
            }

            // Crosshair axes (N/S/E/W + diagonals)
            val axisColor = CardBorder.copy(alpha = 0.65f)
            drawLine(axisColor, Offset(center.x - maxR, center.y), Offset(center.x + maxR, center.y), 1f)
            drawLine(axisColor, Offset(center.x, center.y - maxR), Offset(center.x, center.y + maxR), 1f)
            val diag = maxR * 0.7071f
            drawLine(
                axisColor.copy(alpha = 0.35f),
                Offset(center.x - diag, center.y - diag),
                Offset(center.x + diag, center.y + diag),
                1f
            )
            drawLine(
                axisColor.copy(alpha = 0.35f),
                Offset(center.x + diag, center.y - diag),
                Offset(center.x - diag, center.y + diag),
                1f
            )

            // Rotating sweep beam with luminous afterglow
            if (isScanning) {
                rotate(degrees = sweepAngle, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.74f to Color.Transparent,
                            0.94f to CyanElectric.copy(alpha = 0.14f),
                            0.995f to CyanElectric.copy(alpha = 0.42f),
                            1.0f to Color.Transparent,
                            center = center
                        ),
                        radius = maxR,
                        center = center
                    )
                    // Leading crisp beam line
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(CyanElectric.copy(alpha = 0.15f), CyanElectric),
                            start = center,
                            end = Offset(center.x + maxR, center.y)
                        ),
                        start = center,
                        end = Offset(center.x + maxR, center.y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Expanding sonar ring from center
                drawCircle(
                    color = CyanElectric.copy(alpha = (1f - pulsePhase) * 0.22f),
                    radius = maxR * (0.06f + pulsePhase * 0.94f),
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Center user device marker
            drawCircle(
                color = CyanElectric.copy(alpha = 0.25f),
                radius = 9.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = center
            )

            // Plot detected BLE & Wi-Fi devices
            val nowNanos = SystemClock.elapsedRealtimeNanos()
            val visibleDevices = devices.take(32)
            val labelStyle = TextStyle(
                color = TextPrimary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            )
            val distStyle = TextStyle(
                color = TextSecondary,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
            )

            visibleDevices.forEachIndexed { idx, dev ->
                val isSelected = dev.id == selectedDeviceId
                val r = normalizedRadius(dev.distanceMeters) * maxR
                val rad = (dev.angleDegrees - 90f) * (PI / 180.0)
                val pos = Offset(
                    center.x + (cos(rad) * r).toFloat(),
                    center.y + (sin(rad) * r).toFloat()
                )
                val color = if (dev.type == SignalType.BLE) CyanElectric else EmeraldPulse

                val ageMs = ((nowNanos - dev.lastSeenNanos) / 1_000_000L).coerceAtLeast(0L)
                val freshness = (1f - (ageMs / 6000f)).coerceIn(0.35f, 1f)

                // Outer glow halo
                val haloRadius = if (isSelected) 16.dp.toPx() else (8f + (dev.signalQuality / 25f)).dp.toPx()
                drawCircle(
                    color = color.copy(alpha = (if (isSelected) 0.34f else 0.18f) * freshness),
                    radius = haloRadius,
                    center = pos
                )

                // Selected target reticle
                if (isSelected) {
                    val reticleR = 13.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = reticleR,
                        center = pos,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                    drawLine(
                        color = color.copy(alpha = 0.55f),
                        start = center,
                        end = pos,
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }

                // Core blip dot
                drawCircle(
                    color = color.copy(alpha = freshness),
                    radius = if (isSelected) 5.5.dp.toPx() else 4.2.dp.toPx(),
                    center = pos
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f * freshness),
                    radius = if (isSelected) 2.2.dp.toPx() else 1.5.dp.toPx(),
                    center = pos
                )

                // Labels for selected device + top 4 closest devices
                if (isSelected || idx < 4) {
                    val shortName = if (dev.displayName.length > 13) {
                        dev.displayName.take(12) + "…"
                    } else {
                        dev.displayName
                    }
                    val nameLayout = textMeasurer.measure(shortName, labelStyle)
                    val distLayout = textMeasurer.measure(SignalMath.formatDistance(dev.distanceMeters), distStyle)
                    val labelX = (pos.x + 8.dp.toPx()).coerceAtMost(size.width - maxOf(nameLayout.size.width, distLayout.size.width) - 8.dp.toPx())
                    val labelY = (pos.y - 12.dp.toPx()).coerceIn(6.dp.toPx(), size.height - 28.dp.toPx())

                    drawText(nameLayout, topLeft = Offset(labelX, labelY))
                    drawText(distLayout, topLeft = Offset(labelX, labelY + nameLayout.size.height - 1f))
                }
            }
        }

        // Top-left legend pill
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardSurface.copy(alpha = 0.82f))
                .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = CyanElectric, label = "BLE")
            LegendDot(color = EmeraldPulse, label = "Wi-Fi")
        }

        // Top-right range zoom pill (5m / 15m / 35m)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardSurface.copy(alpha = 0.86f))
                .border(1.dp, CyanElectric.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .clickable(onClick = onCycleRange)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "DOSAH ${maxRangeMeters.toInt()} m",
                color = CyanElectric,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom-left hint
        Text(
            text = "Klepni na bod pro zaměření",
            color = TextMuted,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Canvas(modifier = Modifier.padding(top = 0.5.dp).fillMaxSize(0f)) {}
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(color)
                .padding(3.5.dp)
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
