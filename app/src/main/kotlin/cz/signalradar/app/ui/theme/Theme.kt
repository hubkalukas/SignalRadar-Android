package cz.signalradar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SignalRadarColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = BgPrimary,
    secondary = AccentGreen,
    onSecondary = BgPrimary,
    tertiary = AccentAmber,
    background = BgPrimary,
    onBackground = InkPrimary,
    surface = BgElevated,
    onSurface = InkPrimary,
    surfaceVariant = BgSurface,
    onSurfaceVariant = InkSecondary,
    outline = Hairline,
    error = AccentRed
)

@Composable
fun SignalRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SignalRadarColorScheme,
        typography = SignalTypography,
        content = content
    )
}
