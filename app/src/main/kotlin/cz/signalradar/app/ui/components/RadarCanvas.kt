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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import cz.signalradar.app.ui.theme.AccentBlue
import cz.signalradar.app.ui.theme.AccentGreen
import cz.signalradar.app.ui.theme.BgControl
import cz.signalradar.app.ui.theme.BgElevated
import cz.signalradar.app.ui.theme.Hairline
import cz.signalradar.app.ui.theme.HairlineStrong
import cz.signalradar.app.ui.theme.InkPrimary
import cz.signalradar.app.ui.theme.InkSecondary
import cz.signalradar.app.ui.theme.InkTertiary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

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
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun normalizedRadius(distanceMeters: Float): Float {
        val linear = (distanceMeters / maxRangeMeters).coerceIn(0.02f, 1.0f)
        return (0.11f + 0.85f * sqrt(linear)).coerceIn(0.11f, 0.96f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.08f)
            .clip(RoundedCornerShape(22.dp))
            .background(BgElevated)
            .border(0.5.dp, Hairline, RoundedCornerShape(22.dp))
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

            // Subtle darker dish background
            drawCircle(
                color = Color(0xFF0E0E11),
                radius = maxR,
                center = center
            )

            val ringDistances = when {
                maxRangeMeters <= 6f -> listOf(1f, 2.5f, 5f)
                maxRangeMeters <= 16f -> listOf(2f, 6f, 15f)
                else -> listOf(3f, 12f, 35f)
            }

            val ringLabelStyle = TextStyle(
                color = InkTertiary,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace
            )

            ringDistances.forEachIndexed { idx, distM ->
                val r = normalizedRadius(distM) * maxR
                val isOuter = idx == ringDistances.lastIndex
                drawCircle(
                    color = if (isOuter) HairlineStrong else Hairline,
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                val labelText = if (distM % 1f == 0f) "${distM.toInt()} m" else "$distM m"
                val measured = textMeasurer.measure(labelText, ringLabelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(center.x + 5.dp.toPx(), center.y - r + 2.dp.toPx())
                )
            }

            // Quiet crosshair lines
            drawLine(Hairline.copy(alpha = 0.7f), Offset(center.x - maxR, center.y), Offset(center.x + maxR, center.y), 1f)
            drawLine(Hairline.copy(alpha = 0.7f), Offset(center.x, center.y - maxR), Offset(center.x, center.y + maxR), 1f)

            // Understated monochrome sweep beam
            if (isScanning) {
                rotate(degrees = sweepAngle, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.78f to Color.Transparent,
                            0.96f to Color.White.copy(alpha = 0.06f),
                            0.996f to Color.White.copy(alpha = 0.16f),
                            1.0f to Color.Transparent,
                            center = center
                        ),
                        radius = maxR,
                        center = center
                    )
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.42f)),
                            start = center,
                            end = Offset(center.x + maxR, center.y)
                        ),
                        start = center,
                        end = Offset(center.x + maxR, center.y),
                        strokeWidth = 1.3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Center origin (phone)
            drawCircle(
                color = InkPrimary.copy(alpha = 0.14f),
                radius = 7.dp.toPx(),
                center = center
            )
            drawCircle(
                color = InkPrimary,
                radius = 3.dp.toPx(),
                center = center
            )

            // Targets
            val nowNanos = SystemClock.elapsedRealtimeNanos()
            val visibleDevices = devices.take(32)
            val labelStyle = TextStyle(
                color = InkPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            )
            val distStyle = TextStyle(
                color = InkSecondary,
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
                val color = if (dev.type == SignalType.BLE) AccentBlue else AccentGreen

                val ageMs = ((nowNanos - dev.lastSeenNanos) / 1_000_000L).coerceAtLeast(0L)
                val freshness = (1f - (ageMs / 6000f)).coerceIn(0.4f, 1f)

                if (isSelected) {
                    drawLine(
                        color = InkSecondary.copy(alpha = 0.45f),
                        start = center,
                        end = pos,
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    drawCircle(
                        color = InkPrimary,
                        radius = 11.dp.toPx(),
                        center = pos,
                        style = Stroke(width = 1.3.dp.toPx())
                    )
                }

                drawCircle(
                    color = color.copy(alpha = 0.22f * freshness),
                    radius = if (isSelected) 11.dp.toPx() else 8.dp.toPx(),
                    center = pos
                )
                drawCircle(
                    color = color.copy(alpha = freshness),
                    radius = if (isSelected) 5.dp.toPx() else 4.dp.toPx(),
                    center = pos
                )

                if (isSelected || idx < 4) {
                    val shortName = if (dev.displayName.length > 14) {
                        dev.displayName.take(13) + "…"
                    } else {
                        dev.displayName
                    }
                    val nameLayout = textMeasurer.measure(shortName, labelStyle)
                    val distLayout = textMeasurer.measure(SignalMath.formatDistance(dev.distanceMeters), distStyle)
                    val maxW = maxOf(nameLayout.size.width, distLayout.size.width)
                    val labelX = (pos.x + 8.dp.toPx()).coerceAtMost(size.width - maxW - 8.dp.toPx())
                    val labelY = (pos.y - 11.dp.toPx()).coerceIn(6.dp.toPx(), size.height - 28.dp.toPx())

                    drawText(nameLayout, topLeft = Offset(labelX, labelY))
                    drawText(distLayout, topLeft = Offset(labelX, labelY + nameLayout.size.height - 1f))
                }
            }
        }

        // Top-left minimal legend
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = AccentBlue, label = "BT")
            LegendItem(color = AccentGreen, label = "Wi-Fi")
        }

        // Top-right clean range switch
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BgControl)
                .clickable(onClick = onCycleRange)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "${maxRangeMeters.toInt()} m",
                color = InkPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            color = InkSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
