package io.github.gilnetizen.aseh.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF315C45),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB5F1C9),
    onPrimaryContainer = Color(0xFF002111),
    secondary = Color(0xFF506352),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E8D4),
    onSecondaryContainer = Color(0xFF0E1F12),
    background = Color(0xFFFBF9F5),
    onBackground = Color(0xFF1A1C1A),
    surface = Color(0xFFFBF9F5),
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFDEE5DD),
    onSurfaceVariant = Color(0xFF424943),
    outline = Color(0xFF727971),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF99D5AD),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF12512F),
    onPrimaryContainer = Color(0xFFB5F1C9),
    secondary = Color(0xFFB7CCB8),
    onSecondary = Color(0xFF233427),
    secondaryContainer = Color(0xFF394B3C),
    onSecondaryContainer = Color(0xFFD3E8D4),
    background = Color(0xFF111412),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF111412),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF424943),
    onSurfaceVariant = Color(0xFFC2C9C1),
    outline = Color(0xFF8C938B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/**
 * The shared ASEH Material theme seed.
 *
 * It deliberately uses a fixed, reviewable palette rather than device-derived
 * colors so contrast and screenshots remain deterministic across API levels.
 */
@Composable
fun AsehTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
