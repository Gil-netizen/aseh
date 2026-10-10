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

private val HighContrastLightColors = lightColorScheme(
    primary = Color(0xFF00381D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8FAD5),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF243D2B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2F5E5),
    onSecondaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F2EF),
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
    error = Color(0xFF8C0009),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD7),
    onErrorContainer = Color(0xFF310001),
)

private val HighContrastDarkColors = darkColorScheme(
    primary = Color(0xFFB5F7C8),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0A4E2A),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFD9F4DD),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF253E2B),
    onSecondaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1D211E),
    onSurfaceVariant = Color.White,
    outline = Color.White,
    error = Color(0xFFFFB4AB),
    onError = Color.Black,
    errorContainer = Color(0xFF7F0007),
    onErrorContainer = Color.White,
)

private val LowLightColors = darkColorScheme(
    primary = Color(0xFFFFD18A),
    onPrimary = Color(0xFF2A1700),
    primaryContainer = Color(0xFF4A2D00),
    onPrimaryContainer = Color(0xFFFFE2B8),
    secondary = Color(0xFFE3C9A5),
    onSecondary = Color(0xFF291D0D),
    secondaryContainer = Color(0xFF3F3020),
    onSecondaryContainer = Color(0xFFFBE0BB),
    background = Color(0xFF080604),
    onBackground = Color(0xFFE8DCCB),
    surface = Color(0xFF080604),
    onSurface = Color(0xFFE8DCCB),
    surfaceVariant = Color(0xFF2A241D),
    onSurfaceVariant = Color(0xFFD7C7B3),
    outline = Color(0xFF9F8F7C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF3E0908),
    onErrorContainer = Color(0xFFFFDAD6),
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
    highContrast: Boolean = false,
    lowLight: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = when {
            highContrast && darkTheme -> HighContrastDarkColors
            highContrast -> HighContrastLightColors
            lowLight -> LowLightColors
            darkTheme -> DarkColors
            else -> LightColors
        },
        content = content,
    )
}
