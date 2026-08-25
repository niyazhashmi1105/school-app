package com.tenderbuds.schoolapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E6FF),
    onPrimaryContainer = Color(0xFF1A2470),
    secondary = BrandPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E2FF),
    onSecondaryContainer = Color(0xFF34195A),
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    error = DangerRed,
    onError = Color.White,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFE7E6F0),
    onSurfaceVariant = Color(0xFF5B5A68),
    outline = Color(0xFF8D8C99),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BAEFF),
    onPrimary = Color(0xFF10205C),
    primaryContainer = BrandIndigoDark,
    onPrimaryContainer = Color(0xFFE1E6FF),
    secondary = Color(0xFFD7B7FF),
    onSecondary = Color(0xFF3B1965),
    secondaryContainer = BrandPurpleDark,
    onSecondaryContainer = Color(0xFFF0E2FF),
    tertiary = Color(0xFF7FDB92),
    onTertiary = Color(0xFF0A3313),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF2C2B36),
    onSurfaceVariant = Color(0xFFC7C5D3),
    outline = Color(0xFF908F9C),
)

@Composable
fun TenderBudsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TenderBudsTypography,
        content = content
    )
}
