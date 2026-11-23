package cz.signalradar.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.signalradar.app.scanner.SignalMath
import cz.signalradar.app.ui.theme.AmberSignal
import cz.signalradar.app.ui.theme.CardBorder
import cz.signalradar.app.ui.theme.CoralWeak
import cz.signalradar.app.ui.theme.CyanElectric
import cz.signalradar.app.ui.theme.EmeraldPulse
import cz.signalradar.app.ui.theme.TextMuted
import cz.signalradar.app.ui.theme.TextPrimary
import cz.signalradar.app.ui.theme.TextSecondary
import cz.signalradar.app.ui.theme.signalColorForQuality
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Compact live sparkline drawn inside every minimalist dashboard card.
 */
@Composable
fun SignalSparkline(
    samples: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (samples.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val padY = h * 0.16f
        val rawMin = samples.minOrNull() ?: -90f
        val rawMax = samples.maxOrNull() ?: -40f
        val mid = (rawMin + rawMax) * 0.5f
        val halfSpan = maxOf((rawMax - rawMin) * 0.65f, 8f)
        val minRssi = (mid - halfSpan).coerceAtLeast(-102f)
        val maxRssi = (mid + halfSpan).coerceAtMost(-26f)

        fun yFor(rssi: Float): Float {
            val norm = ((rssi - minRssi) / (maxRssi - minRssi).coerceAtLeast(1f)).coerceIn(0f, 1f)
            return h - padY - norm * (h - 2f * padY)
        }

        // Subtle mid reference line
        drawLine(
            color = CardBorder.copy(alpha = 0.45f),
            start = Offset(0f, h * 0.5f),
            end = Offset(w, h * 0.5f),
            strokeWidth = 1f
        )

        val points = if (samples.size == 1) listOf(samples.first(), samples.first()) else samples
        val stepX = w / (points.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val fillPath = Path()
        points.forEachIndexed { idx, rssi ->
            val x = idx * stepX
            val y = yFor(rssi)
            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (idx - 1) * stepX
                val prevY = yFor(points[idx - 1])
                val ctrlX = (prevX + x) * 0.5f
                linePath.cubicTo(ctrlX, prevY, ctrlX, y, x, y)
                fillPath.cubicTo(ctrlX, prevY, ctrlX, y, x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = 0.28f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )
        drawPath(
            path = linePath,
            color = accentColor,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Glowing live head dot
        val lastY = yFor(points.last())
        drawCircle(
            color = accentColor.copy(alpha = 0.28f),
            radius = 5.dp.toPx(),
            center = Offset(w, lastY)
        )
        drawCircle(
            color = accentColor,
            radius = 2.4.dp.toPx(),
            center = Offset(w, lastY)
        )
    }
}

/**
 * High-precision analog-digital needle gauge for handheld target lock-on.
 * Shows live distance in meters, dBm, percentage quality, and a spring-damped needle.
 */
@Composable
fun AnalogSignalGauge(
    distanceMeters: Float,
    rssi: Float,
    qualityPercent: Int,
    proximityLabel: String,
    accentColor: Color,
    sizeDp: Dp = 216.dp,
    modifier: Modifier = Modifier
) {
    val animatedQuality by animateFloatAsState(
        targetValue = qualityPercent.coerceIn(0, 100) / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "gaugeQuality"
    )

    val liveColor = signalColorForQuality(qualityPercent)

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 12.dp.toPx()
            val inset = strokePx + 10.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = arcSize.width / 2f

            val startAngle = 145f
            val sweepTotal = 250f

            // Background track
            drawArc(
                color = CardBorder.copy(alpha = 0.65f),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active glowing gradient arc (rotated so sweep starts cleanly at startAngle = 145°)
            val activeSweep = (sweepTotal * animatedQuality).coerceAtLeast(2f)
            rotate(degrees = startAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to CoralWeak,
                        0.22f to AmberSignal,
                        0.46f to CyanElectric,
                        0.70f to EmeraldPulse,
                        1.0f to EmeraldPulse,
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            // Precision graduation ticks around inner rim
            val tickCount = 24
            for (i in 0..tickCount) {
                val frac = i / tickCount.toFloat()
                val deg = startAngle + frac * sweepTotal
                val rad = deg * (PI / 180.0)
                val isMajor = i % 6 == 0
                val outerR = radius - strokePx * 0.78f
                val innerR = outerR - if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                val p1 = Offset(
                    center.x + (cos(rad) * innerR).toFloat(),
                    center.y + (sin(rad) * innerR).toFloat()
                )
                val p2 = Offset(
                    center.x + (cos(rad) * outerR).toFloat(),
                    center.y + (sin(rad) * outerR).toFloat()
                )
                drawLine(
                    color = if (frac <= animatedQuality) liveColor.copy(alpha = 0.9f)
                    else TextMuted.copy(alpha = 0.35f),
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Compact precision pointer riding along the arc rim (never overlaps center readout)
            val needleDeg = startAngle + animatedQuality * sweepTotal
            val needleRad = needleDeg * (PI / 180.0)
            val tipR = radius + strokePx * 0.05f
            val baseR = radius - strokePx * 1.05f
            val tip = Offset(
                center.x + (cos(needleRad) * tipR).toFloat(),
                center.y + (sin(needleRad) * tipR).toFloat()
            )
            val base = Offset(
                center.x + (cos(needleRad) * baseR).toFloat(),
                center.y + (sin(needleRad) * baseR).toFloat()
            )
            drawLine(
                color = Color.White,
                start = base,
                end = tip,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = liveColor.copy(alpha = 0.48f),
                radius = 7.dp.toPx(),
                center = tip
            )
            drawCircle(
                color = Color.White,
                radius = 3.2.dp.toPx(),
                center = tip
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = proximityLabel.uppercase(),
                color = liveColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = SignalMath.formatDistance(distanceMeters),
                color = TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${rssi.roundToInt()} dBm · $qualityPercent%",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Full-width real-time RSSI oscilloscope for target inspection.
 */
@Composable
fun LiveOscilloscope(
    samples: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(112.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padTop = 10.dp.toPx()
        val padBottom = 10.dp.toPx()
        val usableH = (h - padTop - padBottom).coerceAtLeast(1f)
        val rawMin = samples.minOrNull() ?: -85f
        val rawMax = samples.maxOrNull() ?: -45f
        val mid = (rawMin + rawMax) * 0.5f
        val halfSpan = maxOf((rawMax - rawMin) * 0.7f, 9f)
        val minRssi = (mid - halfSpan).coerceAtLeast(-102f)
        val maxRssi = (mid + halfSpan).coerceAtMost(-26f)

        fun yFor(rssi: Float): Float {
            val norm = ((rssi - minRssi) / (maxRssi - minRssi).coerceAtLeast(1f)).coerceIn(0f, 1f)
            return padTop + (1f - norm) * usableH
        }

        // Horizontal reference grid lines
        listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
            val y = padTop + frac * usableH
            drawLine(
                color = CardBorder.copy(alpha = 0.55f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        // Vertical time grid lines
        for (i in 1..5) {
            val x = w * (i / 6f)
            drawLine(
                color = CardBorder.copy(alpha = 0.35f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
        }

        if (samples.isEmpty()) return@Canvas
        val pts = if (samples.size == 1) listOf(samples.first(), samples.first()) else samples
        val stepX = w / (pts.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val areaPath = Path()

        pts.forEachIndexed { idx, value ->
            val x = idx * stepX
            val y = yFor(value)
            if (idx == 0) {
                linePath.moveTo(x, y)
                areaPath.moveTo(x, h)
                areaPath.lineTo(x, y)
            } else {
                val prevX = (idx - 1) * stepX
                val prevY = yFor(pts[idx - 1])
                val midX = (prevX + x) * 0.5f
                linePath.cubicTo(midX, prevY, midX, y, x, y)
                areaPath.cubicTo(midX, prevY, midX, y, x, y)
            }
        }
        areaPath.lineTo(w, h)
        areaPath.close()

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = 0.32f), Color.Transparent),
                startY = padTop,
                endY = h
            )
        )
        drawPath(
            path = linePath,
            color = accentColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val lastY = yFor(pts.last())
        drawCircle(
            color = accentColor.copy(alpha = 0.32f),
            radius = 8.dp.toPx(),
            center = Offset(w - 2.dp.toPx(), lastY)
        )
        drawCircle(
            color = Color.White,
            radius = 3.2.dp.toPx(),
            center = Offset(w - 2.dp.toPx(), lastY)
        )
    }
}
