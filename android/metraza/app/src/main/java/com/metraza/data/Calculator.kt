package com.metraza.data

import com.metraza.ui.theme.MATERIAL_DENSITIES
import com.metraza.ui.theme.MATERIAL_PLACEHOLDER
import java.util.Locale

private const val MIN_INPUT = 0.001
private const val MAX_INPUT = 100000.0
private const val VARIANT_INCREASE = 10
private const val VARIANT_DECREASE = 10

fun parseDouble(raw: String, fieldName: String): Double {
    val cleaned = raw.trim().replace(",", ".")
    if (cleaned.isEmpty()) {
        throw IllegalArgumentException("$fieldName je obavezno polje.")
    }

    val value = cleaned.toDoubleOrNull()
        ?: throw IllegalArgumentException("$fieldName mora biti numerička vrednost.")

    if (value < MIN_INPUT || value > MAX_INPUT) {
        throw IllegalArgumentException(
            "$fieldName mora biti između $MIN_INPUT i $MAX_INPUT."
        )
    }
    return value
}

fun parseInt(raw: String, fieldName: String): Int {
    val cleaned = raw.trim()
    if (cleaned.isEmpty()) {
        throw IllegalArgumentException("$fieldName je obavezno polje.")
    }

    val value = cleaned.toIntOrNull()
        ?: throw IllegalArgumentException("$fieldName mora biti ceo broj.")

    if (value < 1 || value > MAX_INPUT.toInt()) {
        throw IllegalArgumentException(
            "$fieldName mora biti između 1 i ${MAX_INPUT.toInt()}."
        )
    }
    return value
}

fun parseMaterial(raw: String): String {
    val material = raw.trim().lowercase(Locale.ROOT)
    if (material.isEmpty() || material == MATERIAL_PLACEHOLDER.lowercase()) {
        throw IllegalArgumentException("Molimo odaberite materijal iz liste.")
    }

    val lookupKey = material.replace("cuzn", "")
    if (!MATERIAL_DENSITIES.containsKey(material) && !MATERIAL_DENSITIES.containsKey(lookupKey)) {
        throw IllegalArgumentException("Nepoznat materijal. Koristite Cu, CuZn10, CuZn15, CuZn20, CuZn30 ili CuZn37.")
    }
    return material
}

fun getDensity(material: String): Int {
    val key = material.lowercase(Locale.ROOT)
    val lookupKey = key.replace("cuzn", "")
    return MATERIAL_DENSITIES[key] ?: MATERIAL_DENSITIES[lookupKey]
        ?: throw IllegalArgumentException("Materijal '$material' nije prepoznat.")
}

fun computeAllResults(
    totalWeight: Double,
    cutWidthMm: Double,
    thicknessMm: Double,
    maxRolls: Int,
    material: String,
    innerDiameterMm: Double? = null
): List<CalculationResult> {
    val results = ArrayList<CalculationResult>(maxRolls)
    for (rollCount in maxRolls downTo 1) {
        val (baseLength, baseWeight) = calculateRollMetrics(
            totalWeight,
            cutWidthMm,
            thicknessMm,
            rollCount,
            material
        )
        val increaseFactor = 1 + VARIANT_INCREASE / 100.0
        val decreaseFactor = 1 - VARIANT_DECREASE / 100.0

        val innerD = innerDiameterMm
        val baseDiameter: Double?
        val baseTapeRadius: Double?
        val plusDiameter: Double?
        val plusTapeRadius: Double?
        val minusDiameter: Double?
        val minusTapeRadius: Double?

        val plusLength = baseLength * increaseFactor
        val minusLength = baseLength * decreaseFactor

        if (innerD != null) {
            baseDiameter = calculateOuterDiameter(baseLength, thicknessMm, innerD)
            baseTapeRadius = (baseDiameter - innerD) / 2.0
            plusDiameter = calculateOuterDiameter(plusLength, thicknessMm, innerD)
            plusTapeRadius = (plusDiameter - innerD) / 2.0
            minusDiameter = calculateOuterDiameter(minusLength, thicknessMm, innerD)
            minusTapeRadius = (minusDiameter - innerD) / 2.0
        } else {
            baseDiameter = null
            baseTapeRadius = null
            plusDiameter = null
            plusTapeRadius = null
            minusDiameter = null
            minusTapeRadius = null
        }

        results.add(
            CalculationResult(
                rolls = rollCount,
                base = RollResult(baseLength, baseWeight, baseDiameter, baseTapeRadius),
                plusVariant = RollResult(plusLength, baseWeight * increaseFactor, plusDiameter, plusTapeRadius),
                minusVariant = RollResult(minusLength, baseWeight * decreaseFactor, minusDiameter, minusTapeRadius)
            )
        )
    }
    return results
}

private fun calculateRollMetrics(
    totalWeight: Double,
    cutWidthMm: Double,
    thicknessMm: Double,
    rollCount: Int,
    material: String
): Pair<Double, Double> {
    val density = getDensity(material)

    val cutWidthM = cutWidthMm * 0.001
    val thicknessM = thicknessMm * 0.001
    val crossSection = cutWidthM * thicknessM
    if (crossSection < Math.ulp(1.0)) {
        throw IllegalArgumentException("Proizvod širine i debljine je previše mali za precizan proračun.")
    }

    val volume = totalWeight / density
    val totalLength = volume / crossSection

    return Pair(totalLength / rollCount, totalWeight / rollCount)
}

fun getRollWord(count: Int): String {
    return when {
        count == 1 -> "rez"
        count in 2..4 -> "reza"
        else -> "rezova"
    }
}

fun formatNumber(value: Double): String {
    return String.format(Locale.US, "%.0f", value)
}

fun calculateOuterDiameter(lengthM: Double, thicknessMm: Double, innerDiameterMm: Double): Double {
    val lengthMm = lengthM * 1000.0
    val area = lengthMm * thicknessMm
    // Area of annulus = pi * (R^2 - r^2) = pi/4 * (D^2 - d^2)
    // Area of cross section (side view) = L * T
    // Using the volume conservation approach from Precnik app:
    // OD = sqrt(ID^2 + (4/PI) * L_mm * T_mm)
    return kotlin.math.sqrt(
        innerDiameterMm * innerDiameterMm + (4.0 / Math.PI) * area
    )
}
