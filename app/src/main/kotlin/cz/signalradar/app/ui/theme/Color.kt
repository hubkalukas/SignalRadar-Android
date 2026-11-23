package cz.signalradar.app.ui.theme

import androidx.compose.ui.graphics.Color

val ObsidianBg = Color(0xFF060A12)
val DeepSlate = Color(0xFF0C1322)
val CardSurface = Color(0xFF10192B)
val CardSurfaceElevated = Color(0xFF162238)
val CardBorder = Color(0xFF1F304D)
val CardBorderBright = Color(0xFF2D466E)

val CyanElectric = Color(0xFF00E5FF)
val CyanSoft = Color(0xFF38BDF8)
val EmeraldPulse = Color(0xFF00E676)
val AmberSignal = Color(0xFFFFB300)
val CoralWeak = Color(0xFFFF5252)
val VioletAccent = Color(0xFFA78BFA)

val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

fun signalColorForQuality(quality: Int): Color = when {
    quality >= 72 -> EmeraldPulse
    quality >= 48 -> CyanElectric
    quality >= 26 -> AmberSignal
    else -> CoralWeak
}
