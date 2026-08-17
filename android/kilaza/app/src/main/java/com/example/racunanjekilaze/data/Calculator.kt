package com.example.racunanjekilaze.data

import com.example.racunanjekilaze.ui.theme.MATERIAL_ALIASES
import com.example.racunanjekilaze.ui.theme.MATERIAL_DENSITIES
import com.example.racunanjekilaze.ui.theme.MATERIAL_PLACEHOLDER
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.round

private const val MM_TO_M = 1000.0

/** Zaokružuje vrednost na zadati broj decimala. */
private fun roundTo(value: Double, decimals: Int): Double {
    val factor = Math.pow(10.0, decimals.toDouble())
    return round(value * factor) / factor
}

fun normalizeMaterial(materialInput: String): String {
    val material = materialInput.trim().lowercase(Locale.US)
    return MATERIAL_ALIASES[material] ?: material
}

fun getDensity(materialInput: String): Double? {
    val normalized = normalizeMaterial(materialInput)
    return MATERIAL_DENSITIES[normalized]
}

fun getDensityOrThrow(materialInput: String): Double {
    return getDensity(materialInput) ?: throw IllegalArgumentException(
        "Nepoznat materijal. Koristite Cu, CuZn10, CuZn15, CuZn20, CuZn30, CuZn37."
    )
}

fun calculateRollLength(dimensions: RollDimensions): Double {
    dimensions.validate()
    val outerR = dimensions.outerRadiusMm
    val coreR = dimensions.coreRadiusMm
    val crossSectionalArea = PI * (outerR + coreR) * (outerR - coreR)
    val lengthMm = crossSectionalArea / dimensions.thicknessMm
    // Zaokružujemo dužinu na 2 decimale (metri)
    return roundTo(lengthMm / MM_TO_M, 2)
}

fun calculateRollWeight(lengthM: Double, dimensions: RollDimensions, density: Double): Double {
    val widthM = dimensions.widthMm / MM_TO_M
    val thicknessM = dimensions.thicknessMm / MM_TO_M
    val volumeM3 = lengthM * widthM * thicknessM
    // Zaokružujemo težinu na 3 decimale (kilogrami)
    return roundTo(volumeM3 * density, 3)
}

fun computeRoll(dimensions: RollDimensions, material: String): RollResult {
    val density = getDensityOrThrow(material)
    val length = calculateRollLength(dimensions)
    val weight = calculateRollWeight(length, dimensions, density)
    return RollResult(lengthM = length, weightKg = weight)
}

fun computeFullResult(
    dimensions: RollDimensions,
    material: String,
    coilCount: Int
): CalculationResult {
    val roll = computeRoll(dimensions, material)
    return CalculationResult.fromSingle(roll, coilCount)
}

fun parsePositiveDouble(raw: String, fieldName: String): Double {
    val cleaned = raw.trim().replace(',', '.')
    if (cleaned.isEmpty()) {
        throw IllegalArgumentException("$fieldName je obavezno polje.")
    }
    val value = cleaned.toDoubleOrNull()
        ?: throw IllegalArgumentException("$fieldName mora biti numericka vrednost.")
    if (value <= 0.0) {
        throw IllegalArgumentException("$fieldName mora biti pozitivna vrednost.")
    }
    return value
}

fun parsePositiveInt(raw: String, fieldName: String): Int {
    val cleaned = raw.trim()
    if (cleaned.isEmpty()) {
        throw IllegalArgumentException("$fieldName je obavezno polje.")
    }
    val value = cleaned.toIntOrNull()
        ?: throw IllegalArgumentException("$fieldName mora biti ceo broj.")
    if (value <= 0) {
        throw IllegalArgumentException("$fieldName mora biti najmanje 1.")
    }
    return value
}

fun parseOptionalDouble(raw: String): Double? {
    val cleaned = raw.trim().replace(',', '.')
    if (cleaned.isEmpty()) {
        return null
    }
    val value = cleaned.toDoubleOrNull() ?: return null
    return value.takeIf { it > 0.0 }
}

fun parseMaterial(raw: String): String {
    val material = raw.trim()
    if (material.isEmpty() || material.equals(MATERIAL_PLACEHOLDER, ignoreCase = true)) {
        throw IllegalArgumentException("Odaberite materijal iz liste.")
    }
    if (getDensity(material) == null) {
        throw IllegalArgumentException(
            "Nepoznat materijal. Koristite Cu, CuZn10, CuZn15, CuZn20, CuZn30, CuZn37."
        )
    }
    return material
}

fun getTrakaForm(count: Int): String {
    return when {
        count == 1 -> "traku"
        count in 2..4 -> "trake"
        else -> "traka"
    }
}

fun getTrakaFormNominative(count: Int): String {
    return when {
        count == 1 -> "traka"
        count in 2..4 -> "trake"
        else -> "traka"
    }
}

fun formatValue(value: Double): String {
    return String.format(Locale.getDefault(), "%.0f", value)
}

fun formatValue(value: Double, decimals: Int): String {
    val pattern = "%.${decimals}f"
    return String.format(Locale.getDefault(), pattern, value)
}

fun formatEntryCount(count: Int): String {
    return when {
        count == 1 -> "1 stavka"
        count in 2..4 -> "$count stavke"
        else -> "$count stavki"
    }
}

fun formatRemainingWeight(remainingWeight: Double?): Triple<String, String, Boolean>? {
    return when {
        remainingWeight == null -> null
        remainingWeight > 0 -> Triple("PREOSTALO:", "${formatValue(remainingWeight)} kg", false)
        remainingWeight < 0 -> Triple("VISAK:", "${formatValue(abs(remainingWeight))} kg", true)
        else -> Triple("STATUS:", "TACNO", true)
    }
}
