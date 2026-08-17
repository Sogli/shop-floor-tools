package com.metraza.data

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculatorTest {

    @Test
    fun parseDouble_acceptsDotCommaIntegerAndWhitespace() {
        assertThat(parseDouble("10.5", "Test Field")).isEqualTo(10.5)
        assertThat(parseDouble("10,5", "Test Field")).isEqualTo(10.5)
        assertThat(parseDouble("100", "Test Field")).isEqualTo(100.0)
        assertThat(parseDouble(" 10.5 ", "Test Field")).isEqualTo(10.5)
    }

    @Test
    fun parseDouble_rejectsInvalidAndOutOfRangeValues() {
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            parseDouble("", "Test Field")
        }.message).contains("obavezno polje")
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            parseDouble("abc", "Test Field")
        }.message).contains("numerička vrednost")
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            parseDouble("0.0001", "Test Field")
        }.message).contains("između")
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            parseDouble("100001", "Test Field")
        }.message).contains("između")
    }

    @Test
    fun parseDouble_acceptsBoundaryValues() {
        assertThat(parseDouble("0.001", "Test Field")).isEqualTo(0.001)
        assertThat(parseDouble("100000", "Test Field")).isEqualTo(100000.0)
    }

    @Test
    fun parseInt_acceptsIntegerAndRejectsInvalidValues() {
        assertThat(parseInt("5", "Test Field")).isEqualTo(5)
        assertThat(parseInt("100000", "Test Field")).isEqualTo(100000)
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            parseInt("", "Test Field")
        }.message).contains("obavezno polje")
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            parseInt("5.5", "Test Field")
        }.message).contains("ceo broj")
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            parseInt("0", "Test Field")
        }.message).contains("između 1")
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            parseInt("-1", "Test Field")
        }.message).contains("između 1")
    }

    @Test
    fun parseMaterial_returnsNormalizedStringForDisplayNamesAndNumericAliases() {
        assertThat(parseMaterial("Cu")).isEqualTo("cu")
        assertThat(parseMaterial(" cuzn10 ")).isEqualTo("cuzn10")
        assertThat(parseMaterial("10")).isEqualTo("10")
    }

    @Test
    fun parseMaterial_rejectsMissingPlaceholderAndUnknownMaterial() {
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            parseMaterial("")
        }.message).contains("odaberite materijal")
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            parseMaterial("Izaberite materijal")
        }.message).contains("odaberite materijal")
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            parseMaterial("InvalidMat")
        }.message).contains("Nepoznat materijal")
    }

    @Test
    fun getDensity_allCurrentMaterials_returnCorrectDensities() {
        assertThat(getDensity("Cu")).isEqualTo(8960)
        assertThat(getDensity("CuZn10")).isEqualTo(8800)
        assertThat(getDensity("CuZn15")).isEqualTo(8630)
        assertThat(getDensity("CuZn20")).isEqualTo(8530)
        assertThat(getDensity("CuZn30")).isEqualTo(8403)
        assertThat(getDensity("CuZn37")).isEqualTo(8285)
    }

    @Test
    fun getDensity_acceptsNumericBrassAliases() {
        assertThat(getDensity("10")).isEqualTo(8800)
        assertThat(getDensity("15")).isEqualTo(8630)
        assertThat(getDensity("20")).isEqualTo(8530)
        assertThat(getDensity("30")).isEqualTo(8403)
        assertThat(getDensity("37")).isEqualTo(8285)
    }

    @Test
    fun computeAllResults_returnsDescendingRollCountsWithEvenWeightDistribution() {
        val results = computeAllResults(
            totalWeight = 100.0,
            cutWidthMm = 50.0,
            thicknessMm = 0.5,
            maxRolls = 5,
            material = "Cu",
            innerDiameterMm = null
        )

        assertThat(results.map { it.rolls }).containsExactly(5, 4, 3, 2, 1).inOrder()
        assertThat(results.first { it.rolls == 5 }.base.weightKg).isWithin(0.01).of(20.0)
    }

    @Test
    fun computeAllResults_plusAndMinusVariantsScaleBaseValuesByTenPercent() {
        val result = computeAllResults(100.0, 50.0, 0.5, 1, "Cu", null).single()

        assertThat(result.plusVariant.weightKg).isWithin(0.001).of(result.base.weightKg * 1.1)
        assertThat(result.plusVariant.lengthM).isWithin(0.001).of(result.base.lengthM * 1.1)
        assertThat(result.minusVariant.weightKg).isWithin(0.001).of(result.base.weightKg * 0.9)
        assertThat(result.minusVariant.lengthM).isWithin(0.001).of(result.base.lengthM * 0.9)
    }

    @Test
    fun computeAllResults_outerDiameterAndTapeRadiusDependOnInnerDiameter() {
        val withDiameter = computeAllResults(100.0, 50.0, 0.5, 1, "Cu", 100.0).single()
        val withoutDiameter = computeAllResults(100.0, 50.0, 0.5, 1, "Cu", null).single()

        assertThat(withDiameter.base.outerDiameterMm).isNotNull()
        assertThat(withDiameter.base.tapeRadiusMm).isNotNull()
        assertThat(withDiameter.plusVariant.outerDiameterMm).isNotNull()
        assertThat(withDiameter.minusVariant.outerDiameterMm).isNotNull()
        assertThat(withoutDiameter.base.outerDiameterMm).isNull()
        assertThat(withoutDiameter.base.tapeRadiusMm).isNull()
    }

    @Test
    fun computeAllResults_cu100kgKnownLength() {
        val result = computeAllResults(100.0, 50.0, 0.5, 1, "Cu", null).single()

        assertThat(result.base.lengthM).isWithin(0.1).of(446.43)
    }

    @Test
    fun calculateOuterDiameter_basicAndKnownCases() {
        val outerD = calculateOuterDiameter(
            lengthM = 100.0,
            thicknessMm = 0.5,
            innerDiameterMm = 100.0
        )

        assertThat(outerD).isGreaterThan(100.0)
        assertThat(outerD).isWithin(0.1).of(271.41)
    }

    @Test
    fun getRollWord_usesSerbianPluralForms() {
        assertThat(getRollWord(1)).isEqualTo("rez")
        assertThat(getRollWord(2)).isEqualTo("reza")
        assertThat(getRollWord(3)).isEqualTo("reza")
        assertThat(getRollWord(4)).isEqualTo("reza")
        assertThat(getRollWord(5)).isEqualTo("rezova")
        assertThat(getRollWord(10)).isEqualTo("rezova")
    }

    @Test
    fun formatNumber_usesCurrentWholeNumberFormatting() {
        assertThat(formatNumber(10.5)).isEqualTo("11")
        assertThat(formatNumber(10.555)).isEqualTo("11")
        assertThat(formatNumber(100.0)).isEqualTo("100")
    }
}
