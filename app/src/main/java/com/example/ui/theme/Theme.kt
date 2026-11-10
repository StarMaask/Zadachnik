package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SafetyAmber,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = BlueprintNavyLight,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = EmeraldVerified,
    onTertiary = Color(0xFF052E16),
    background = SlateSurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SlateCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF1B2E44),
    onSurfaceVariant = TextSecondaryDark,
    error = CrimsonAlert,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BlueprintNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8F5),
    onPrimaryContainer = BlueprintNavy,
    secondary = SafetyAmberDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = EmeraldVerified,
    onTertiary = Color.White,
    background = SlateSurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SlateCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = TextSecondaryLight,
    error = CrimsonAlert,
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
