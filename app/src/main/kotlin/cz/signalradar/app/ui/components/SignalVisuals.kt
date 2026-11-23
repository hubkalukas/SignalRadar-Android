package cz.signalradar.app.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.signalradar.app.scanner.SignalMath
import cz.signalradar.app.ui.theme.BgControl
import cz.signalradar.app.ui.theme.Hairline
import cz.signalradar.app.ui.theme.InkPrimary
import cz.signalradar.app.ui.theme.InkSecondary
import cz.signalradar.app.ui.theme.InkTertiary
import cz.signalradar.app.ui.theme.signalColorForQuality
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
                colors = listOf(accentColor.copy(alpha = 0.16f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )
        drawPath(
            path = linePath,
            color = accentColor.copy(alpha = 0.9f),
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val lastY = yFor(points.last())
        drawCircle(
            color = accentColor,
            radius = 2.dp.toPx(),
            center = Offset(w, lastY)
        )
    }
}

@Composable
fun AnalogSignalGauge(
    distanceMeters: Float,
    rssi: Float,
    qualityPercent: Int,
    proximityLabel: String,
    sizeDp: Dp = 212.dp,
    modifier: Modifier = Modifier
) {
    val animatedQuality by animateFloatAsState(
        targetValue = qualityPercent.coerceIn(0, 100) / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "gaugeQuality"
    )

    val activeColor by animateColorAsState(
        targetValue = signalColorForQuality(qualityPercent),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "gaugeColor"
    )

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 9.dp.toPx()
            val inset = strokePx + 10.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = arcSize.width / 2f

            val startAngle = 145f
            val sweepTotal = 250f

            // Quiet neutral track
            drawArc(
                color = BgControl,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Single-tone live arc (clean, no rainbow clash)
            val activeSweep = (sweepTotal * animatedQuality).coerceAtLeast(2f)
            drawArc(
                color = activeColor,
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Fine instrument ticks inside the rim
            val tickCount = 24
            for (i in 0..tickCount) {
                val frac = i / tickCount.toFloat()
                val deg = startAngle + frac * sweepTotal
                val rad = deg * (PI / 180.0)
                val isMajor = i % 6 == 0
                val outerR = radius - strokePx * 0.9f
                val innerR = outerR - if (isMajor) 7.dp.toPx() else 3.5.dp.toPx()
                val p1 = Offset(
                    center.x + (cos(rad) * innerR).toFloat(),
                    center.y + (sin(rad) * innerR).toFloat()
                )
                val p2 = Offset(
                    center.x + (cos(rad) * outerR).toFloat(),
                    center.y + (sin(rad) * outerR).toFloat()
                )
                drawLine(
                    color = if (frac <= animatedQuality) InkSecondary.copy(alpha = 0.75f)
                    else InkTertiary.copy(alpha = 0.32f),
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Crisp needle cap riding the active endpoint
            val needleDeg = startAngle + animatedQuality * sweepTotal
            val needleRad = needleDeg * (PI / 180.0)
            val tip = Offset(
                center.x + (cos(needleRad) * radius).toFloat(),
                center.y + (sin(needleRad) * radius).toFloat()
            )
            drawCircle(
                color = InkPrimary,
                radius = 5.5.dp.toPx(),
                center = tip
            )
            drawCircle(
                color = activeColor,
                radius = 2.6.dp.toPx(),
                center = tip
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = proximityLabel,
                color = activeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = SignalMath.formatDistance(distanceMeters),
                color = InkPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1.1).sp,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${rssi.roundToInt()} dBm · $qualityPercent %",
                color = InkSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun LiveOscilloscope(
    samples: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(92.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padTop = 8.dp.toPx()
        val padBottom = 8.dp.toPx()
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

        listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
            val y = padTop + frac * usableH
            drawLine(
                color = Hairline.copy(alpha = 0.7f),
                start = Offset(0f, y),
                end = Offset(w, y),
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
                colors = listOf(accentColor.copy(alpha = 0.18f), Color.Transparent),
                startY = padTop,
                endY = h
            )
        )
        drawPath(
            path = linePath,
            color = accentColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val lastY = yFor(pts.last())
        drawCircle(
            color = InkPrimary,
            radius = 3.dp.toPx(),
            center = Offset(w - 2.dp.toPx(), lastY)
        )
    }
}
