package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val DarkColorScheme = darkColorScheme(
    primary = StudioGold,
    onPrimary = Color(0xFF171306),
    primaryContainer = StudioGoldContainerDark,
    onPrimaryContainer = Color(0xFFFCE39D),
    secondary = ViewfinderCyan,
    onSecondary = Color(0xFF071D31),
    secondaryContainer = Color(0xFF132C44),
    onSecondaryContainer = Color(0xFFD0E9FF),
    tertiary = FilmedEmerald,
    onTertiary = Color(0xFF042619),
    tertiaryContainer = FilmedTintDark,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = ObsidianBackground,
    onBackground = InkPrimaryDark,
    surface = SlateSurface,
    onSurface = InkPrimaryDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = InkSecondaryDark,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateSurfaceVariant,
    surfaceContainerHigh = SlateSurfaceHigh,
    outline = SlateOutline,
    outlineVariant = SlateOutlineSubtle,
    error = AlertCoral,
    onError = Color(0xFF2B0707),
    errorContainer = Color(0xFF3E1416),
    onErrorContainer = Color(0xFFFECACA),
)

private val LightColorScheme = lightColorScheme(
    primary = StudioGoldDark,
    onPrimary = Color.White,
    primaryContainer = StudioGoldContainerLight,
    onPrimaryContainer = Color(0xFF2B1F00),
    secondary = ViewfinderCyanLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEEFF),
    onSecondaryContainer = Color(0xFF092842),
    tertiary = FilmedEmeraldLight,
    onTertiary = Color.White,
    tertiaryContainer = FilmedTintLight,
    onTertiaryContainer = Color(0xFF0B3B1D),
    background = PaperBackground,
    onBackground = InkPrimaryLight,
    surface = PaperSurface,
    onSurface = InkPrimaryLight,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = InkSecondaryLight,
    surfaceContainerLowest = PaperSurface,
    surfaceContainerLow = PaperBackground,
    surfaceContainer = PaperSurfaceVariant,
    surfaceContainerHigh = PaperSurfaceHigh,
    outline = PaperOutline,
    outlineVariant = Color(0xFFE3DFD4),
    error = AlertCoralLight,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

val ShotDeskShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun ShotDeskTheme(
    darkTheme: Boolean = true,
    isArabic: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyFor(isArabic),
        shapes = ShotDeskShapes,
        content = content,
    )
}
