package cz.signalradar.app.ui.theme

import androidx.compose.ui.graphics.Color

val BgPrimary = Color(0xFF0A0A0C)
val BgElevated = Color(0xFF131316)
val BgSurface = Color(0xFF1A1A1F)
val BgControl = Color(0xFF232329)
val Hairline = Color(0xFF27272E)
val HairlineStrong = Color(0xFF383842)

val AccentBlue = Color(0xFF4DA3FF)
val AccentGreen = Color(0xFF34C759)
val AccentAmber = Color(0xFFFF9F0A)
val AccentRed = Color(0xFFFF453A)

val InkPrimary = Color(0xFFF5F5F7)
val InkSecondary = Color(0xFF9E9EA8)
val InkTertiary = Color(0xFF63636E)

fun signalColorForQuality(quality: Int): Color = when {
    quality >= 70 -> AccentGreen
    quality >= 45 -> AccentBlue
    quality >= 25 -> AccentAmber
    else -> AccentRed
}
