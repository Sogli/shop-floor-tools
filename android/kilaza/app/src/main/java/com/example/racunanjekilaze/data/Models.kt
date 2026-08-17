package com.example.racunanjekilaze.data

import kotlin.math.round

/** Zaokružuje vrednost na zadati broj decimala. */
private fun roundTo(value: Double, decimals: Int): Double {
    val factor = Math.pow(10.0, decimals.toDouble())
    return round(value * factor) / factor
}

data class RollDimensions(
    val outerDiameterMm: Double,
    val coreDiameterMm: Double,
    val thicknessMm: Double,
    val widthMm: Double
) {
    companion object {
        /** Minimalna dozvoljena debljina trake u mm */
        const val MIN_THICKNESS_MM = 0.01

        /** Maksimalna dozvoljena dimenzija u mm (prečnik, širina) */
        const val MAX_DIMENSION_MM = 5000.0

        fun fromRadialThickness(
            radialThicknessMm: Double,
            coreDiameterMm: Double,
            thicknessMm: Double,
            widthMm: Double
        ): RollDimensions {
            val outerDiameter = coreDiameterMm + (2.0 * radialThicknessMm)
            return RollDimensions(outerDiameter, coreDiameterMm, thicknessMm, widthMm)
        }
    }

    val outerRadiusMm: Double
        get() = outerDiameterMm / 2.0

    val coreRadiusMm: Double
        get() = coreDiameterMm / 2.0

    val radialThicknessMm: Double
        get() = (outerDiameterMm - coreDiameterMm) / 2.0

    fun validate() {
        if (thicknessMm <= 0.0) {
            throw IllegalArgumentException("Debljina materijala mora biti pozitivna vrednost.")
        }
        if (thicknessMm < MIN_THICKNESS_MM) {
            throw IllegalArgumentException(
                "Debljina materijala mora biti najmanje $MIN_THICKNESS_MM mm."
            )
        }
        if (widthMm <= 0.0) {
            throw IllegalArgumentException("Širina trake mora biti pozitivna vrednost.")
        }
        if (widthMm > MAX_DIMENSION_MM) {
            throw IllegalArgumentException(
                "Širina trake ne može biti veća od $MAX_DIMENSION_MM mm."
            )
        }
        if (outerDiameterMm > MAX_DIMENSION_MM) {
            throw IllegalArgumentException(
                "Spoljašnji prečnik ne može biti veći od $MAX_DIMENSION_MM mm."
            )
        }
        if (coreDiameterMm > MAX_DIMENSION_MM) {
            throw IllegalArgumentException(
                "Prečnik jezgra ne može biti veći od $MAX_DIMENSION_MM mm."
            )
        }
        if (outerRadiusMm <= coreRadiusMm) {
            throw IllegalArgumentException(
                "Spoljašnji prečnik mora biti veći od unutrašnjeg prečnika doboša."
            )
        }
    }
}

data class RollResult(
    val lengthM: Double,
    val weightKg: Double
)

data class CalculationResult(
    val singleRoll: RollResult,
    val coilCount: Int,
    val totalWeightKg: Double
) {
    companion object {
        fun fromSingle(roll: RollResult, coilCount: Int): CalculationResult {
            // Zaokružujemo ukupnu težinu na 3 decimale da sprečimo akumulaciju grešaka
            val totalWeight = roundTo(roll.weightKg * coilCount, 3)
            return CalculationResult(
                singleRoll = roll,
                coilCount = coilCount,
                totalWeightKg = totalWeight
            )
        }
    }
}

data class OrderEntry(
    val id: Int,
    val dimensions: RollDimensions,
    val material: String,
    val coilCount: Int,
    val result: CalculationResult
)

enum class MessageType {
    NONE,
    SUCCESS,
    ERROR
}

data class MessageState(
    val text: String,
    val type: MessageType
)
