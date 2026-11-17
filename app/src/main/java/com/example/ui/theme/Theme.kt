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

private val LightColorScheme = lightColorScheme(
    primary = DeepForestGreen,
    onPrimary = Color.White,
    primaryContainer = ForestGreenContainer,
    onPrimaryContainer = OnForestContainer,
    secondary = SoftSage,
    onSecondary = Color.White,
    secondaryContainer = SageContainer,
    onSecondaryContainer = OnSageContainer,
    tertiary = BotanicalGold,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = WarmIvory,
    onBackground = CharcoalInk,
    surface = CardIvory,
    onSurface = CharcoalInk,
    surfaceVariant = SoftParchment,
    onSurfaceVariant = MutedMossText,
    outline = HairlineSage,
    outlineVariant = Color(0xFFE7E2D6),
    error = SafetyToxicCrimson,
    onError = Color.White,
    errorContainer = SafetyToxicBg,
    onErrorContainer = SafetyToxicCrimson
)

private val DarkColorScheme = darkColorScheme(
    primary = LuminousSage,
    onPrimary = Color(0xFF0A2618),
    primaryContainer = DarkForestContainer,
    onPrimaryContainer = Color(0xFFD2E6DA),
    secondary = Color(0xFF7CA98B),
    onSecondary = Color(0xFF0E2317),
    secondaryContainer = Color(0xFF21382C),
    onSecondaryContainer = Color(0xFFD6E6DC),
    tertiary = MutedGold,
    onTertiary = Color(0xFF2B1E02),
    tertiaryContainer = DarkGoldContainer,
    onTertiaryContainer = Color(0xFFF6E9CC),
    background = DarkPineBg,
    onBackground = WarmCreamText,
    surface = DarkMossSurface,
    onSurface = WarmCreamText,
    surfaceVariant = DarkElevatedCard,
    onSurfaceVariant = DarkSubtleText,
    outline = DarkBorder,
    outlineVariant = Color(0xFF22362B),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF470C0C),
    errorContainer = Color(0xFF5B1B1B),
    onErrorContainer = Color(0xFFFFDAD6)
)

val FloraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FloraMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FloraTypography,
        shapes = FloraShapes,
        content = content
    )
}
