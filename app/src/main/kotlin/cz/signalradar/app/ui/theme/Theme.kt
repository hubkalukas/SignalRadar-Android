package cz.signalradar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SignalRadarColorScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = ObsidianBg,
    secondary = EmeraldPulse,
    onSecondary = ObsidianBg,
    tertiary = AmberSignal,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = CoralWeak
)

@Composable
fun SignalRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SignalRadarColorScheme,
        typography = SignalTypography,
        content = content
    )
}
