package com.livnica

import java.util.Locale
import kotlin.math.roundToInt

fun formatCurrency(value: Double): String {
    val rounded = value.roundToInt().toDouble()
    val formatted = String.format(Locale.US, "%,.0f", rounded)
    return formatted.replace(",", ".")
}

fun formatNumber(value: Double): String {
    val formatted = String.format(Locale.US, "%,.0f", value)
    return formatted.replace(",", ".")
}

