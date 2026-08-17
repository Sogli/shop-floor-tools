package com.example.racunanjekilaze.data

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

/**
 * Unit testovi za sve funkcije u Calculator.kt.
 * Pokrivaju parsiranje, validaciju materijala, proračun dužine i težine,
 * formatiranje i srpsku pluralizaciju.
 */
class CalculatorTest {

    // ========================================================================
    // parsePositiveDouble
    // ========================================================================

    @Test
    fun parsePositiveDouble_normalInput_returnsValue() {
        val result = parsePositiveDouble("150.5", "Test")
        assertThat(result).isWithin(0.01).of(150.5)
    }

    @Test
    fun parsePositiveDouble_emptyString_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePositiveDouble("", "Test")
        }
    }

    @Test
    fun parsePositiveDouble_negativeValue_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePositiveDouble("-5", "Test")
        }
    }

    @Test
    fun parsePositiveDouble_zero_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePositiveDouble("0", "Test")
        }
    }

    @Test
    fun parsePositiveDouble_commaAsSeparator_returnsValue() {
        // Podrška za zarez kao decimalni separator
        val result = parsePositiveDouble("150,5", "Test")
        assertThat(result).isWithin(0.01).of(150.5)
    }

    @Test
    fun parsePositiveDouble_leadingAndTrailingSpaces_returnsValue() {
        val result = parsePositiveDouble(" 150.5 ", "Test")
        assertThat(result).isWithin(0.01).of(150.5)
    }

    @Test
    fun parsePositiveDouble_nonNumeric_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePositiveDouble("abc", "Test")
        }
    }

    // ========================================================================
    // parsePositiveInt
    // ========================================================================

    @Test
    fun parsePositiveInt_normalInput_returnsValue() {
        val result = parsePositiveInt("3", "Test")
        assertThat(result).isEqualTo(3)
    }

    @Test
    fun parsePositiveInt_emptyString_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePositiveInt("", "Test")
        }
    }

    @Test
    fun parsePositiveInt_negativeValue_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePositiveInt("-1", "Test")
        }
    }

    @Test
    fun parsePositiveInt_zero_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePositiveInt("0", "Test")
        }
    }

    @Test
    fun parsePositiveInt_decimalNumber_throwsException() {
        // toIntOrNull vraca null za "3.5"
        assertThrows(IllegalArgumentException::class.java) {
            parsePositiveInt("3.5", "Test")
        }
    }

    @Test
    fun parsePositiveInt_nonNumeric_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePositiveInt("abc", "Test")
        }
    }

    // ========================================================================
    // parseOptionalDouble
    // ========================================================================

    @Test
    fun parseOptionalDouble_emptyString_returnsNull() {
        val result = parseOptionalDouble("")
        assertThat(result).isNull()
    }

    @Test
    fun parseOptionalDouble_validInput_returnsValue() {
        val result = parseOptionalDouble("100.5")
        assertThat(result).isWithin(0.01).of(100.5)
    }

    @Test
    fun parseOptionalDouble_negativeValue_returnsNull() {
        // Negativna vrednost nije > 0, pa se vraća null
        val result = parseOptionalDouble("-5")
        assertThat(result).isNull()
    }

    @Test
    fun parseOptionalDouble_zero_returnsNull() {
        // Nula nije > 0, pa se vraća null
        val result = parseOptionalDouble("0")
        assertThat(result).isNull()
    }

    @Test
    fun parseOptionalDouble_commaAsSeparator_returnsValue() {
        val result = parseOptionalDouble("100,5")
        assertThat(result).isWithin(0.01).of(100.5)
    }

    // ========================================================================
    // parseMaterial
    // ========================================================================

    @Test
    fun parseMaterial_validMaterial_returnsMaterial() {
        val result = parseMaterial("Cu")
        assertThat(result).isEqualTo("Cu")
    }

    @Test
    fun parseMaterial_placeholder_throwsException() {
        // "Izaberite" je placeholder vrednost
        assertThrows(IllegalArgumentException::class.java) {
            parseMaterial("Izaberite")
        }
    }

    @Test
    fun parseMaterial_emptyString_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            parseMaterial("")
        }
    }

    @Test
    fun parseMaterial_unknownMaterial_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            parseMaterial("Bronze")
        }
    }

    // ========================================================================
    // normalizeMaterial
    // ========================================================================

    @Test
    fun normalizeMaterial_cu_returnsLowercase() {
        val result = normalizeMaterial("Cu")
        assertThat(result).isEqualTo("cu")
    }

    @Test
    fun normalizeMaterial_uppercase_returnsLowercase() {
        val result = normalizeMaterial("CUZN10")
        assertThat(result).isEqualTo("cuzn10")
    }

    @Test
    fun normalizeMaterial_alias_returnsNormalized() {
        // Alias "10" se mapira na "cuzn10"
        val result = normalizeMaterial("10")
        assertThat(result).isEqualTo("cuzn10")
    }

    @Test
    fun normalizeMaterial_unknownMaterial_returnsLowercase() {
        // Nepoznat materijal se samo prebacuje u lowercase
        val result = normalizeMaterial("xyz")
        assertThat(result).isEqualTo("xyz")
    }

    // ========================================================================
    // getDensity
    // ========================================================================

    @Test
    fun getDensity_cu_returnsDensity() {
        val result = getDensity("Cu")
        assertThat(result).isWithin(0.01).of(8960.0)
    }

    @Test
    fun getDensity_cuzn37_returnsDensity() {
        val result = getDensity("cuzn37")
        assertThat(result).isWithin(0.01).of(8285.0)
    }

    @Test
    fun getDensity_alias_returnsDensity() {
        // Alias "10" -> "cuzn10" -> 8800.0
        val result = getDensity("10")
        assertThat(result).isWithin(0.01).of(8800.0)
    }

    @Test
    fun getDensity_nonExistent_returnsNull() {
        val result = getDensity("nepostojeci")
        assertThat(result).isNull()
    }

    @Test
    fun getDensity_allMaterials_returnCorrectValues() {
        // Provera svih 6 materijala
        assertThat(getDensity("cu")).isWithin(0.01).of(8960.0)
        assertThat(getDensity("cuzn10")).isWithin(0.01).of(8800.0)
        assertThat(getDensity("cuzn15")).isWithin(0.01).of(8686.0)
        assertThat(getDensity("cuzn20")).isWithin(0.01).of(8530.0)
        assertThat(getDensity("cuzn30")).isWithin(0.01).of(8403.0)
        assertThat(getDensity("cuzn37")).isWithin(0.01).of(8285.0)
    }

    // ========================================================================
    // getDensityOrThrow
    // ========================================================================

    @Test
    fun getDensityOrThrow_validMaterial_returnsDensity() {
        val result = getDensityOrThrow("Cu")
        assertThat(result).isWithin(0.01).of(8960.0)
    }

    @Test
    fun getDensityOrThrow_invalidMaterial_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            getDensityOrThrow("nepostojeci")
        }
    }

    // ========================================================================
    // calculateRollLength
    // ========================================================================

    @Test
    fun calculateRollLength_knownDimensions_returnsExpectedLength() {
        // outer=500mm, core=400mm, thickness=0.5mm
        // outerR=250, coreR=200
        // area = PI * (250+200) * (250-200) = PI * 450 * 50 = PI * 22500
        // lengthMm = PI * 22500 / 0.5 = PI * 45000
        // lengthM = PI * 45000 / 1000 = PI * 45 ≈ 141.37 m
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        val result = calculateRollLength(dimensions)
        val expected = PI * 45.0 // ≈ 141.37 m
        assertThat(result).isWithin(0.01).of(expected)
    }

    // ========================================================================
    // calculateRollWeight
    // ========================================================================

    @Test
    fun calculateRollWeight_knownValues_returnsExpectedWeight() {
        // length=100m, width=100mm(0.1m), thickness=0.5mm(0.0005m), density=8960
        // volume = 100 * 0.1 * 0.0005 = 0.005 m3
        // weight = 0.005 * 8960 = 44.8 kg
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        val result = calculateRollWeight(100.0, dimensions, 8960.0)
        assertThat(result).isWithin(0.01).of(44.8)
    }

    // ========================================================================
    // computeRoll
    // ========================================================================

    @Test
    fun computeRoll_validInput_returnsRollResult() {
        // Integracioni test sa validnim dimenzijama i materijalom
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        val result = computeRoll(dimensions, "Cu")

        // Dužina: PI * 45 ≈ 141.37 m (zaokruženo na 2 decimale)
        assertThat(result.lengthM).isWithin(0.01).of(PI * 45.0)

        // Težina: zaokružena dužina * 0.1 * 0.0005 * 8960 (zaokruženo na 3 decimale)
        val expectedWeight = calculateRollWeight(result.lengthM, dimensions, 8960.0)
        assertThat(result.weightKg).isWithin(0.001).of(expectedWeight)
    }

    @Test
    fun computeRoll_invalidMaterial_throwsException() {
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            computeRoll(dimensions, "nepostojeci")
        }
    }

    // ========================================================================
    // computeFullResult
    // ========================================================================

    @Test
    fun computeFullResult_multipleCoils_correctTotalWeight() {
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        val result = computeFullResult(dimensions, "Cu", 3)

        // Ukupna težina = težina jedne rolne * 3
        assertThat(result.coilCount).isEqualTo(3)
        assertThat(result.totalWeightKg).isWithin(0.01).of(result.singleRoll.weightKg * 3)
    }

    // ========================================================================
    // getTrakaForm (akuzativ - "traku/trake/traka")
    // ========================================================================

    @Test
    fun getTrakaForm_one_returnsSingular() {
        assertThat(getTrakaForm(1)).isEqualTo("traku")
    }

    @Test
    fun getTrakaForm_two_returnsPlural234() {
        assertThat(getTrakaForm(2)).isEqualTo("trake")
    }

    @Test
    fun getTrakaForm_three_returnsPlural234() {
        assertThat(getTrakaForm(3)).isEqualTo("trake")
    }

    @Test
    fun getTrakaForm_four_returnsPlural234() {
        assertThat(getTrakaForm(4)).isEqualTo("trake")
    }

    @Test
    fun getTrakaForm_five_returnsPluralGenitive() {
        assertThat(getTrakaForm(5)).isEqualTo("traka")
    }

    @Test
    fun getTrakaForm_zero_returnsPluralGenitive() {
        assertThat(getTrakaForm(0)).isEqualTo("traka")
    }

    @Test
    fun getTrakaForm_hundred_returnsPluralGenitive() {
        assertThat(getTrakaForm(100)).isEqualTo("traka")
    }

    // ========================================================================
    // getTrakaFormNominative (nominativ - "traka/trake/traka")
    // ========================================================================

    @Test
    fun getTrakaFormNominative_one_returnsSingular() {
        assertThat(getTrakaFormNominative(1)).isEqualTo("traka")
    }

    @Test
    fun getTrakaFormNominative_two_returnsPlural234() {
        assertThat(getTrakaFormNominative(2)).isEqualTo("trake")
    }

    @Test
    fun getTrakaFormNominative_five_returnsPluralGenitive() {
        assertThat(getTrakaFormNominative(5)).isEqualTo("traka")
    }

    @Test
    fun getTrakaFormNominative_zero_returnsPluralGenitive() {
        assertThat(getTrakaFormNominative(0)).isEqualTo("traka")
    }

    // ========================================================================
    // formatValue
    // ========================================================================

    @Test
    fun formatValue_normalValue_containsExpectedDigits() {
        val result = formatValue(44.8)
        // Bez zadatih decimala formatValue zaokružuje na najbliži ceo broj.
        assertThat(result).contains("45")
    }

    @Test
    fun formatValue_zero_returnsFormattedZero() {
        val result = formatValue(0.0)
        // Treba da sadrži "0" sa decimalama
        assertThat(result).contains("0")
    }

    @Test
    fun formatValue_withDecimals_returnsFormattedWithPrecision() {
        val result = formatValue(123.456, 3)
        assertThat(result).contains("123")
    }

    // ========================================================================
    // formatEntryCount
    // ========================================================================

    @Test
    fun formatEntryCount_zero_returnsPluralGenitive() {
        assertThat(formatEntryCount(0)).isEqualTo("0 stavki")
    }

    @Test
    fun formatEntryCount_one_returnsSingular() {
        assertThat(formatEntryCount(1)).isEqualTo("1 stavka")
    }

    @Test
    fun formatEntryCount_two_returnsPlural234() {
        assertThat(formatEntryCount(2)).isEqualTo("2 stavke")
    }

    @Test
    fun formatEntryCount_three_returnsPlural234() {
        assertThat(formatEntryCount(3)).isEqualTo("3 stavke")
    }

    @Test
    fun formatEntryCount_four_returnsPlural234() {
        assertThat(formatEntryCount(4)).isEqualTo("4 stavke")
    }

    @Test
    fun formatEntryCount_five_returnsPluralGenitive() {
        assertThat(formatEntryCount(5)).isEqualTo("5 stavki")
    }

    @Test
    fun formatEntryCount_hundred_returnsPluralGenitive() {
        assertThat(formatEntryCount(100)).isEqualTo("100 stavki")
    }

    // ========================================================================
    // formatRemainingWeight
    // ========================================================================

    @Test
    fun formatRemainingWeight_null_returnsNull() {
        val result = formatRemainingWeight(null)
        assertThat(result).isNull()
    }

    @Test
    fun formatRemainingWeight_positive_returnsPreostalo() {
        val result = formatRemainingWeight(100.0)
        assertThat(result).isNotNull()
        assertThat(result!!.first).isEqualTo("PREOSTALO:")
        assertThat(result.second).contains("100")
        assertThat(result.third).isFalse()
    }

    @Test
    fun formatRemainingWeight_negative_returnsVisak() {
        val result = formatRemainingWeight(-50.0)
        assertThat(result).isNotNull()
        assertThat(result!!.first).isEqualTo("VISAK:")
        assertThat(result.second).contains("50")
        assertThat(result.third).isTrue()
    }

    @Test
    fun formatRemainingWeight_zero_returnsTacno() {
        val result = formatRemainingWeight(0.0)
        assertThat(result).isNotNull()
        assertThat(result!!.first).isEqualTo("STATUS:")
        assertThat(result.second).isEqualTo("TACNO")
        assertThat(result.third).isTrue()
    }

    // ========================================================================
    // Granični slučajevi - validacija ulaznih podataka
    // ========================================================================

    @Test
    fun validate_thicknessBelowMinimum_throwsException() {
        // Debljina ispod dozvoljenog minimuma (0.01 mm)
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.005,
            widthMm = 100.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            dimensions.validate()
        }
    }

    @Test
    fun validate_thicknessAtMinimum_doesNotThrow() {
        // Debljina tačno na minimumu (0.01 mm) - treba da prođe
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.01,
            widthMm = 100.0
        )
        dimensions.validate() // Ne baca izuzetak
    }

    @Test
    fun validate_outerDiameterExceedsMax_throwsException() {
        // Spoljašnji prečnik iznad dozvoljenog maksimuma (5000 mm)
        val dimensions = RollDimensions(
            outerDiameterMm = 6000.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            dimensions.validate()
        }
    }

    @Test
    fun validate_widthExceedsMax_throwsException() {
        // Širina trake iznad dozvoljenog maksimuma (5000 mm)
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 6000.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            dimensions.validate()
        }
    }

    @Test
    fun validate_coreDiameterExceedsMax_throwsException() {
        // Prečnik jezgra iznad dozvoljenog maksimuma (5000 mm)
        val dimensions = RollDimensions(
            outerDiameterMm = 5000.0,
            coreDiameterMm = 5001.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            dimensions.validate()
        }
    }

    // ========================================================================
    // Granični slučajevi - preciznost proračuna
    // ========================================================================

    @Test
    fun calculateRollLength_verySmallThickness_returnsRoundedResult() {
        // Veoma mala debljina (0.05 mm) daje veliku dužinu - provera zaokruživanja
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.05,
            widthMm = 100.0
        )
        val result = calculateRollLength(dimensions)
        // Proveravamo da je rezultat zaokružen na tačno 2 decimale
        val rounded = String.format("%.2f", result).toDouble()
        assertThat(result).isEqualTo(rounded)
    }

    @Test
    fun calculateRollLength_veryLargeOuterDiameter_returnsRoundedResult() {
        // Veoma veliki spoljašnji prečnik (3000 mm)
        val dimensions = RollDimensions(
            outerDiameterMm = 3000.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        val result = calculateRollLength(dimensions)
        // Proveravamo da je rezultat zaokružen na tačno 2 decimale
        val rounded = String.format("%.2f", result).toDouble()
        assertThat(result).isEqualTo(rounded)
    }

    @Test
    fun calculateRollLength_manyDecimalPlacesInInput_returnsRoundedResult() {
        // Ulaz sa mnogo decimalnih mesta
        val dimensions = RollDimensions(
            outerDiameterMm = 500.123456789,
            coreDiameterMm = 400.987654321,
            thicknessMm = 0.5123456789,
            widthMm = 100.111111111
        )
        val result = calculateRollLength(dimensions)
        // Rezultat mora biti zaokružen na 2 decimale
        val rounded = String.format("%.2f", result).toDouble()
        assertThat(result).isEqualTo(rounded)
    }

    @Test
    fun calculateRollLength_outerBarelyLargerThanCore_returnsSmallLength() {
        // Spoljašnji prečnik jedva veći od jezgra
        val dimensions = RollDimensions(
            outerDiameterMm = 400.1,
            coreDiameterMm = 400.0,
            thicknessMm = 0.05,
            widthMm = 100.0
        )
        val result = calculateRollLength(dimensions)
        // Rezultat treba da bude mali ali pozitivan i zaokružen
        assertThat(result).isGreaterThan(0.0)
        val rounded = String.format("%.2f", result).toDouble()
        assertThat(result).isEqualTo(rounded)
    }

    @Test
    fun calculateRollWeight_roundedToThreeDecimals() {
        // Proveravamo da je težina zaokružena na 3 decimale
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        val length = calculateRollLength(dimensions)
        val weight = calculateRollWeight(length, dimensions, 8960.0)
        val rounded = String.format("%.3f", weight).toDouble()
        assertThat(weight).isEqualTo(rounded)
    }

    @Test
    fun computeFullResult_manyCoils_totalWeightPrecise() {
        // Akumulacija preciznosti sa velikim brojem rolni
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        val result = computeFullResult(dimensions, "Cu", 100)

        // Ukupna težina mora biti zaokružena na 3 decimale
        val rounded = String.format("%.3f", result.totalWeightKg).toDouble()
        assertThat(result.totalWeightKg).isEqualTo(rounded)

        // Ukupna težina mora odgovarati pojedinačnoj * broj rolni (zaokruženo)
        assertThat(result.totalWeightKg).isWithin(0.001).of(result.singleRoll.weightKg * 100)
    }

    @Test
    fun computeFullResult_manyDecimalInputs_multipleCoils_stableResult() {
        // Provera stabilnosti sa mnogo decimalnih mesta i većim brojem rolni
        val dimensions = RollDimensions(
            outerDiameterMm = 501.333333,
            coreDiameterMm = 399.666667,
            thicknessMm = 0.3333333,
            widthMm = 150.777777
        )
        val result = computeFullResult(dimensions, "CuZn37", 50)

        // Dužina zaokružena na 2 decimale
        val lengthRounded = String.format("%.2f", result.singleRoll.lengthM).toDouble()
        assertThat(result.singleRoll.lengthM).isEqualTo(lengthRounded)

        // Težina zaokružena na 3 decimale
        val weightRounded = String.format("%.3f", result.singleRoll.weightKg).toDouble()
        assertThat(result.singleRoll.weightKg).isEqualTo(weightRounded)

        // Ukupna težina zaokružena na 3 decimale
        val totalRounded = String.format("%.3f", result.totalWeightKg).toDouble()
        assertThat(result.totalWeightKg).isEqualTo(totalRounded)
    }

    @Test
    fun computeRoll_allMaterials_roundedResults() {
        // Proveravamo zaokruživanje za sve materijale
        val dimensions = RollDimensions(
            outerDiameterMm = 500.0,
            coreDiameterMm = 400.0,
            thicknessMm = 0.5,
            widthMm = 100.0
        )
        val materials = listOf("Cu", "CuZn10", "CuZn15", "CuZn20", "CuZn30", "CuZn37")
        for (material in materials) {
            val result = computeRoll(dimensions, material)
            val lengthRounded = String.format("%.2f", result.lengthM).toDouble()
            val weightRounded = String.format("%.3f", result.weightKg).toDouble()
            assertThat(result.lengthM).isEqualTo(lengthRounded)
            assertThat(result.weightKg).isEqualTo(weightRounded)
        }
    }
}
