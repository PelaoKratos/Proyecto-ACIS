package com.example.proyecto_acis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AcisBlueDark,
    onPrimary = Color(0xFF003642),
    primaryContainer = Color(0xFF004D5F),
    onPrimaryContainer = Color(0xFFD0EEF5),
    secondary = AcisTealDark,
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF155047),
    onSecondaryContainer = Color(0xFFC0EEE0),
    tertiary = AcisAmberDark,
    onTertiary = Color(0xFF512D00),
    tertiaryContainer = Color(0xFF704000),
    onTertiaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF101A1D),
    onBackground = Color(0xFFDFE7E9),
    surface = Color(0xFF172124),
    onSurface = Color(0xFFDFE7E9),
    surfaceVariant = Color(0xFF3E4A4D),
    onSurfaceVariant = Color(0xFFBEC9CC),
    surfaceContainerLow = Color(0xFF1B292D),
    surfaceContainer = Color(0xFF243338),
    surfaceContainerHigh = Color(0xFF2D3C41),
    outline = Color(0xFF8B9A9F),
    outlineVariant = Color(0xFF46565B),
)

private val LightColorScheme = lightColorScheme(
    primary = AcisBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0EEF5),
    onPrimaryContainer = Color(0xFF003642),
    secondary = AcisTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC0EEE0),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = AcisAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF512D00),
    background = Color(0xFFF5F8F9),
    onBackground = Color(0xFF172124),
    surface = Color.White,
    onSurface = Color(0xFF172124),
    surfaceVariant = Color(0xFFDEE8EB),
    onSurfaceVariant = Color(0xFF455457),
    surfaceContainerLow = Color(0xFFF0F5F6),
    surfaceContainer = Color(0xFFE8F0F2),
    surfaceContainerHigh = Color(0xFFDFE9EC),
    outline = Color(0xFF71868D),
    outlineVariant = Color(0xFFC4D4D9),
)

@Composable
fun ProyectoACISTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
