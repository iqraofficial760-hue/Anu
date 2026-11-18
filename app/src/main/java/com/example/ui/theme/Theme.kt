package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PartyDarkColorScheme = darkColorScheme(
    primary = PartyGold,
    onPrimary = DeepObsidianText,
    primaryContainer = CardPlumTop,
    onPrimaryContainer = PartyGoldSoft,
    secondary = LiveCoral,
    onSecondary = PureWhite,
    secondaryContainer = ElevatedPlum,
    onSecondaryContainer = SoftLavender,
    tertiary = LiveEmerald,
    onTertiary = DeepObsidianText,
    background = MidnightPurple,
    onBackground = PureWhite,
    surface = RoyalPlumSurface,
    onSurface = PureWhite,
    surfaceVariant = ElevatedPlum,
    onSurfaceVariant = SoftLavender,
    outline = DeepIndigoBorder,
    outlineVariant = PartyGoldDim
)

val PartyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PartyLiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PartyDarkColorScheme,
        typography = PartyTypography,
        shapes = PartyShapes,
        content = content
    )
}
