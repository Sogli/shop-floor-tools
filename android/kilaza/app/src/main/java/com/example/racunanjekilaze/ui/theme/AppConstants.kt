package com.example.racunanjekilaze.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Copper color palette
val Copper = Color(0xFFB87333)
val CopperDark = Color(0xFF8E4E1B)
val CopperLight = Color(0xFFD4956B)
val Accent = Color(0xFFD9893D)
val ResultGreen = Color(0xFF1A6B66)
val ResultAccent = Color(0xFF52D1C6)

// Surface colors
val Surface = Color(0xFF14171C)
val SurfaceLight = Color(0xFF1E232B)
val SurfaceElevated = Color(0xFF262C35)
val Background = Color(0xFF0C0E12)
val BackgroundGradientStart = Color(0xFF0B0D11)
val BackgroundGradientEnd = Color(0xFF171B22)

// Text colors
val TextPrimary = Color(0xFFF2EFEA)
val TextSecondary = Color(0xFF9E9A93)
val TextMuted = Color(0xFF6B6860)

// Semantic colors
val ErrorColor = Color(0xFFE05A50)
val SuccessColor = Color(0xFF52AD70)
val WarningColor = Color(0xFFD9A83D)

// Border colors
val BorderColor = Color(0xFF3A3530)
val BorderFocused = Accent
val GlassBorder = Color(0xFF4A4540)

// Shadow
val ShadowColor = Color(0xFF000000)
val CopperShadow = Copper.copy(alpha = 0.15f)

// Material constants
const val MATERIAL_PLACEHOLDER = "Izaberite"

val MATERIAL_DISPLAY_NAMES = listOf(
    "Cu",
    "CuZn10",
    "CuZn15",
    "CuZn20",
    "CuZn30",
    "CuZn37"
)

val MATERIAL_DENSITIES = mapOf(
    "cu" to 8960.0,
    "cuzn10" to 8800.0,
    "cuzn15" to 8686.0,
    "cuzn20" to 8530.0,
    "cuzn30" to 8403.0,
    "cuzn37" to 8285.0
)

val MATERIAL_ALIASES = mapOf(
    "10" to "cuzn10",
    "15" to "cuzn15",
    "20" to "cuzn20",
    "30" to "cuzn30",
    "37" to "cuzn37"
)

// Layout tokens
data class LayoutTokens(
    val isCompact: Boolean,
    val screenPaddingHorizontal: Dp,
    val screenPaddingVertical: Dp,
    val sectionSpacing: Dp,
    val cardPaddingHorizontal: Dp,
    val cardPaddingVertical: Dp,
    val cardRadius: Dp,
    val itemPaddingHorizontal: Dp,
    val itemPaddingVertical: Dp,
    val buttonMinHeight: Dp,
    val buttonContentPadding: PaddingValues,
    val buttonSpacing: Dp
)

@Composable
fun layoutTokens(): LayoutTokens {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 360

    return LayoutTokens(
        isCompact = isCompact,
        screenPaddingHorizontal = if (isCompact) 12.dp else 16.dp,
        screenPaddingVertical = if (isCompact) 10.dp else 14.dp,
        sectionSpacing = if (isCompact) 8.dp else 10.dp,
        cardPaddingHorizontal = if (isCompact) 12.dp else 16.dp,
        cardPaddingVertical = if (isCompact) 10.dp else 14.dp,
        cardRadius = 10.dp,
        itemPaddingHorizontal = if (isCompact) 10.dp else 12.dp,
        itemPaddingVertical = if (isCompact) 8.dp else 10.dp,
        buttonMinHeight = if (isCompact) 48.dp else 52.dp,
        buttonContentPadding = if (isCompact) {
            PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        } else {
            PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        },
        buttonSpacing = if (isCompact) 8.dp else 10.dp
    )
}
