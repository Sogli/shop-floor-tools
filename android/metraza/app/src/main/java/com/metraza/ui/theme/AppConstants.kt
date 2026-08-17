package com.metraza.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Primary - vibrant teal
val Primary = Color(0xFF4FD8B4)
val PrimaryDark = Color(0xFF005140)
val PrimaryLight = Color(0xFF72F5CF)

// Secondary - warm amber
val Secondary = Color(0xFFFFB95F)
val SecondaryDark = Color(0xFF643F00)

// Surface colors
val Surface = Color(0xFF1A1B1B)
val SurfaceLight = Color(0xFF252727)
val SurfaceElevated = Color(0xFF303333)
val Background = Color(0xFF101111)
val BackgroundGradientStart = Color(0xFF0F1111)
val BackgroundGradientEnd = Color(0xFF181A1A)

// Text colors
val TextPrimary = Color(0xFFE6E1E5)
val TextSecondary = Color(0xFFCAC4D0)
val TextMuted = Color(0xFF938F99)

// Semantic colors
val ErrorColor = Color(0xFFFFB4AB)
val SuccessColor = Color(0xFF7DD992)
val WarningColor = Color(0xFFFFB95F)

// Border colors
val BorderColor = Color(0xFF414747)
val BorderFocused = Primary
val GlassBorder = Color(0xFF5E6764)

// Shadow
val ShadowColor = Color(0xFF000000)
val PrimaryShadow = Primary.copy(alpha = 0.15f)

// Material constants
const val MATERIAL_PLACEHOLDER = "Izaberite materijal"

val MATERIAL_DISPLAY_NAMES = listOf(
    "Cu",
    "CuZn10",
    "CuZn15",
    "CuZn20",
    "CuZn30",
    "CuZn37"
)

val MATERIAL_DENSITIES = mapOf(
    "cu" to 8960,
    "cuzn10" to 8800,
    "10" to 8800,
    "cuzn15" to 8630,
    "15" to 8630,
    "cuzn20" to 8530,
    "20" to 8530,
    "cuzn30" to 8403,
    "30" to 8403,
    "cuzn37" to 8285,
    "37" to 8285
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
        screenPaddingVertical = if (isCompact) 14.dp else 18.dp,
        sectionSpacing = if (isCompact) 10.dp else 12.dp,
        cardPaddingHorizontal = if (isCompact) 14.dp else 18.dp,
        cardPaddingVertical = if (isCompact) 12.dp else 16.dp,
        cardRadius = 14.dp,
        itemPaddingHorizontal = if (isCompact) 10.dp else 12.dp,
        itemPaddingVertical = if (isCompact) 8.dp else 10.dp,
        buttonMinHeight = if (isCompact) 48.dp else 54.dp,
        buttonContentPadding = if (isCompact) {
            PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        } else {
            PaddingValues(horizontal = 18.dp, vertical = 10.dp)
        },
        buttonSpacing = if (isCompact) 8.dp else 10.dp
    )
}
