package com.example.proyecto_acis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFCEEF78),
    onPrimary = Color(0xFF101711),
    primaryContainer = Color(0xFF303E29),
    onPrimaryContainer = Color(0xFFCEEF78),
    secondary = Color(0xFFCEEF78),
    onSecondary = Color(0xFF101711),
    secondaryContainer = Color(0xFF303E29),
    onSecondaryContainer = Color(0xFFCEEF78),
    tertiary = Color(0xFFCEEF78),
    onTertiary = Color(0xFF101711),
    tertiaryContainer = Color(0xFF303E29),
    onTertiaryContainer = Color(0xFFCEEF78),
    background = Color(0xFF101514),
    onBackground = Color(0xFFF1F3ED),
    surface = Color(0xFF1C2422),
    onSurface = Color(0xFFF1F3ED),
    surfaceVariant = Color(0xFF313B37),
    onSurfaceVariant = Color(0xFFA9B4AE),
    surfaceContainerLow = Color(0xFF171E1C),
    surfaceContainer = Color(0xFF1C2422),
    surfaceContainerHigh = Color(0xFF27312D),
    outline = Color(0xFF849189),
    outlineVariant = Color(0xFF3A4640),
)

private val LightColorScheme = lightColorScheme(
    primary = AcisBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E3FF),
    onPrimaryContainer = Color(0xFF002D60),
    secondary = AcisTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E4FF),
    onSecondaryContainer = Color(0xFF17324F),
    tertiary = AcisAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDAE2FF),
    onTertiaryContainer = Color(0xFF24314A),
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF1A1C20),
    surface = Color.White,
    onSurface = Color(0xFF1A1C20),
    surfaceVariant = Color(0xFFE1E5EE),
    onSurfaceVariant = Color(0xFF454B55),
    surfaceContainerLow = Color(0xFFF0F2F7),
    surfaceContainer = Color(0xFFE8EAF0),
    surfaceContainerHigh = Color(0xFFDEE2EA),
    outline = Color(0xFF737985),
    outlineVariant = Color(0xFFC4CAD5),
)

@Composable
fun ProyectoACISTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
