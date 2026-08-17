package com.precnik.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Primary - teal
val Primary = Color(0xFF268C86)
val PrimaryDark = Color(0xFF1A6B66)
val PrimaryLight = Color(0xFF33B8AE)

// Accent - cyan
val Accent = Color(0xFF33B8AE)

// Surface colors
val Surface = Color(0xFF1A1E26)
val SurfaceLight = Color(0xFF242A33)
val SurfaceElevated = Color(0xFF2E3540)
val Background = Color(0xFF0D0F14)
val BackgroundGradientStart = Color(0xFF0A0C10)
val BackgroundGradientEnd = Color(0xFF141820)

// Text colors
val TextPrimary = Color(0xFFF0F2F2)
val TextSecondary = Color(0xFF949FA6)
val TextMuted = Color(0xFF6B7580)

// Semantic colors
val ErrorColor = Color(0xFFE65A50)
val SuccessColor = Color(0xFF4DB87A)

// Border colors
val BorderColor = Color(0xFF3A4250)
val BorderFocused = Accent
val GlassBorder = Color(0xFF4A5260)

// Shadow
val PrimaryShadow = Primary.copy(alpha = 0.15f)

// Layout tokens
data class LayoutTokens(
    val isCompact: Boolean,
    val screenPaddingHorizontal: Dp,
    val screenPaddingVertical: Dp,
    val sectionSpacing: Dp,
    val cardPaddingHorizontal: Dp,
    val cardPaddingVertical: Dp,
    val cardRadius: Dp,
    val buttonMinHeight: Dp,
    val buttonContentPadding: PaddingValues
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
        cardRadius = 16.dp,
        buttonMinHeight = if (isCompact) 48.dp else 54.dp,
        buttonContentPadding = if (isCompact) {
            PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        } else {
            PaddingValues(horizontal = 18.dp, vertical = 10.dp)
        }
    )
}
