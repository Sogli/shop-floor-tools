package com.livnica

import androidx.compose.ui.graphics.Color

object UI {
    const val buttonHeight = 46
    const val iconButtonSize = 44
    const val saveButtonHeight = 55
    const val cardRadius = 16
    const val cardPadding = 12
    const val headerHeight = 60
    const val statCardHeight = 80
    const val totalCardHeight = 70
    const val compTimeBarHeight = 50
    const val weekdayHeaderHeight = 32
    const val dayButtonHeight = 62
    const val bottomBarHeight = 95
    const val popupWidthRatio = 0.95f
    const val dayPopupHeight = 620
    const val hoursPopupHeight = 200
    const val autoFillPopupHeightRatio = 0.65f
    const val autoPopupHeightRatio = 0.45f
    const val sickPopupHeightRatio = 0.55f
    const val deletePopupHeight = 280
    const val defaultSpacing = 8
    const val largeSpacing = 12
    const val smallSpacing = 4
    const val titleFont = 22
    const val largeFont = 18
    const val normalFont = 16
    const val smallFont = 14
    const val tinyFont = 11
    const val dayFont = 12
    const val indicatorDotSize = 8
    const val indicatorBarHeight = 4
}

data class ModernTheme(
    val bgDark: Color = Color(0xFF0B0F14),
    val bgPrimary: Color = Color(0xFF101826),
    val bgSecondary: Color = Color(0xFF0E1622),
    val bgCard: Color = Color(0xFF121A26),
    val bgCardElevated: Color = Color(0xFF182234),
    val glassBg: Color = Color(0xFF121A26),
    val glassBorder: Color = Color(0xFF2A3446),
    val accentPrimary: Color = Color(0xFF2DE2E6),
    val accentSecondary: Color = Color(0xFFFCA311),
    val success: Color = Color(0xFF00C853),
    val warning: Color = Color(0xFFFFB300),
    val danger: Color = Color(0xFFFF5252),
    val info: Color = Color(0xFF40C4FF),
    val textPrimary: Color = Color(0xFFF8FAFC),
    val textSecondary: Color = Color(0xFFC7CEDB),
    val textMuted: Color = Color(0xFF7F8AA3),
    val textDark: Color = Color(0xFF0B0F14),
    val shadow: Color = Color(0xFF000000),
    val shift1: Color = Color(0xFF00C2FF),
    val shift2: Color = Color(0xFF7AE582),
    val shift3: Color = Color(0xFFFF7AB6),
    val sick: Color = Color(0xFFFF5252),
    val vacation: Color = Color(0xFFFF8F00),
    val off: Color = Color(0xFF424242),
    val empty: Color = Color(0xFF2A3347),
    val doubleShift: Color = Color(0xFFFF9100),
    val compTime: Color = Color(0xFF00BFA5),
    val compTimeUsed: Color = Color(0xFFFF6E40),
    val compTimeDeficit: Color = Color(0xFFFF5252),
    val weekendOt: Color = Color(0xFFFFB300),
    val sunday: Color = Color(0xFFFF9100)
)

data class PayConfig(
    val defaultBasePay: Double = 444.46,
    val defaultHours: Int = 8,
    val shift3Premium: Double = 1.41,
    val overtimePremium: Double = 1.41,
    val sundayBonus: Double = 0.25,
    val sickPayRate: Double = 0.65,
    val vacationPayRate: Double = 1.0,
    val holidayBonus: Double = 1.23,
    val currency: String = "RSD",
    val foodPerDay: Double = 280.0,
    val minuliRadPerYear: Double = 0.005
)

data class AppConfig(
    val theme: ModernTheme = ModernTheme(),
    val pay: PayConfig = PayConfig(),
    val weeklyShiftCycle: List<Int> = listOf(3, 2, 1)
)

val CONFIG = AppConfig()
val THEME = CONFIG.theme
const val MANUAL_HOLIDAY_LABEL = "Ručno unet praznik"

val WEEKDAY_NAMES = listOf("PON", "UTO", "SRE", "\u010cET", "PET", "SUB", "NED")
val MONTH_NAMES = listOf(
    "",
    "Januar",
    "Februar",
    "Mart",
    "April",
    "Maj",
    "Jun",
    "Jul",
    "Avgust",
    "Septembar",
    "Oktobar",
    "Novembar",
    "Decembar"
)

