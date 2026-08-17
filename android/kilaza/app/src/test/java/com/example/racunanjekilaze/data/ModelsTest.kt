package com.example.racunanjekilaze.data

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit testovi za modele podataka u Models.kt.
 * Pokrivaju validaciju dimenzija, factory metode i izračunata svojstva.
 */
class ModelsTest {

    // ========================================================================
    // RollDimensions.validate()
    // ========================================================================

    @Test
    fun validate_validDimensions_doesNotThrow() {
        // Validne dimenzije ne bacaju exception
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        dimensions.validate() // Ne baca exception
    }

    @Test
    fun validate_zeroThickness_throwsException() {
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.0,
            widthMm = 100.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            dimensions.validate()
        }
    }

    @Test
    fun validate_negativeThickness_throwsException() {
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = -1.0,
            widthMm = 100.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            dimensions.validate()
        }
    }

    @Test
    fun validate_zeroWidth_throwsException() {
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 0.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            dimensions.validate()
        }
    }

    @Test
    fun validate_negativeWidth_throwsException() {
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = -1.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            dimensions.validate()
        }
    }

    @Test
    fun validate_outerRadiusEqualsCoreRadius_throwsException() {
        // outer=400, core=400 -> outerRadius == coreRadius
        val dimensions = RollDimensions(
            outerDiameterMm = 400.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            dimensions.validate()
        }
    }

    @Test
    fun validate_outerRadiusLessThanCoreRadius_throwsException() {
        // outer=300, core=400 -> outerRadius < coreRadius
        val dimensions = RollDimensions(
            outerDiameterMm = 300.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            dimensions.validate()
        }
    }

    // ========================================================================
    // RollDimensions.fromRadialThickness
    // ========================================================================

    @Test
    fun fromRadialThickness_calculatesCorrectOuterDiameter() {
        // radial=50, core=400 -> outer = 400 + 2*50 = 500
        val dimensions = RollDimensions.fromRadialThickness(
            radialThicknessMm = 50.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        assertThat(dimensions.outerDiameterMm).isWithin(0.01).of(500.0)
        assertThat(dimensions.coreDiameterMm).isWithin(0.01).of(400.0)
        assertThat(dimensions.thicknessMm).isWithin(0.01).of(0.5)
        assertThat(dimensions.widthMm).isWithin(0.01).of(100.0)
    }

    // ========================================================================
    // RollDimensions properties
    // ========================================================================

    @Test
    fun outerRadiusMm_returnsHalfOfOuterDiameter() {
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        assertThat(dimensions.outerRadiusMm).isWithin(0.01).of(250.0)
    }

    @Test
    fun coreRadiusMm_returnsHalfOfCoreDiameter() {
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        assertThat(dimensions.coreRadiusMm).isWithin(0.01).of(200.0)
    }

    @Test
    fun radialThicknessMm_returnsCorrectValue() {
        // radialThickness = (500 - 400) / 2 = 50
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        assertThat(dimensions.radialThicknessMm).isWithin(0.01).of(50.0)
    }

    // ========================================================================
    // CalculationResult.fromSingle
    // ========================================================================

    @Test
    fun fromSingle_calculatesCorrectTotalWeight() {
        val roll = RollResult(lengthM = 100.0, weightKg = 50.0)
        val result = CalculationResult.fromSingle(roll, 3)

        assertThat(result.singleRoll).isEqualTo(roll)
        assertThat(result.coilCount).isEqualTo(3)
        // totalWeight = 50.0 * 3 = 150.0
        assertThat(result.totalWeightKg).isWithin(0.01).of(150.0)
    }

    @Test
    fun fromSingle_singleCoil_totalEqualsRollWeight() {
        val roll = RollResult(lengthM = 141.37, weightKg = 63.49)
        val result = CalculationResult.fromSingle(roll, 1)

        assertThat(result.totalWeightKg).isWithin(0.01).of(63.49)
    }
}
