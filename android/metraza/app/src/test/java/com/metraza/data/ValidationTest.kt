package com.metraza.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValidationTest {

    @Test
    fun validateTotalWeight_acceptsValidCommaAndBoundaryValues() {
        assertThat(validateTotalWeight("100").isValid).isTrue()
        assertThat(validateTotalWeight("10,5").isValid).isTrue()
        assertThat(validateTotalWeight("0.001").isValid).isTrue()
        assertThat(validateTotalWeight("100000").isValid).isTrue()
    }

    @Test
    fun validateTotalWeight_rejectsInvalidValues() {
        assertThat(validateTotalWeight("").isValid).isFalse()
        assertThat(validateTotalWeight("").errorMessage).contains("obavezno polje")
        assertThat(validateTotalWeight("abc").errorMessage).contains("numerička vrednost")
        assertThat(validateTotalWeight("0.0001").errorMessage).contains("između")
        assertThat(validateTotalWeight("100001").errorMessage).contains("između")
    }

    @Test
    fun validateThickness_acceptsValidAndRejectsInvalidValues() {
        assertThat(validateThickness("0.5").isValid).isTrue()
        assertThat(validateThickness("").errorMessage).contains("obavezno")
        assertThat(validateThickness("abc").errorMessage).contains("numerička vrednost")
        assertThat(validateThickness("0.0001").errorMessage).contains("između")
    }

    @Test
    fun validateCutWidth_acceptsValidAndRejectsInvalidValues() {
        assertThat(validateCutWidth("100").isValid).isTrue()
        assertThat(validateCutWidth("").errorMessage).contains("obavezno polje")
        assertThat(validateCutWidth("abc").errorMessage).contains("numerička vrednost")
        assertThat(validateCutWidth("100001").errorMessage).contains("između")
    }

    @Test
    fun validateRolls_acceptsIntegersAndRejectsInvalidValues() {
        assertThat(validateRolls("5").isValid).isTrue()
        assertThat(validateRolls("").errorMessage).contains("obavezno polje")
        assertThat(validateRolls("5.5").errorMessage).contains("ceo broj")
        assertThat(validateRolls("0").errorMessage).contains("između 1")
    }

    @Test
    fun validateInnerDiameter_isOptionalButValidatesProvidedValue() {
        assertThat(validateInnerDiameter("").isValid).isTrue()
        assertThat(validateInnerDiameter("150").isValid).isTrue()
        assertThat(validateInnerDiameter("abc").errorMessage).contains("numerička vrednost")
        assertThat(validateInnerDiameter("0.0001").errorMessage).contains("između")
    }

    @Test
    fun isFormValid_validInputWithStringMaterial_returnsTrue() {
        val input = InputState(
            selectedMaterial = "Cu",
            totalWeight = "100",
            thickness = "0.5",
            cutWidth = "50",
            rolls = "5",
            innerDiameter = ""
        )

        assertThat(isFormValid(input)).isTrue()
    }

    @Test
    fun isFormValid_noMaterial_returnsFalse() {
        val input = InputState(
            selectedMaterial = null,
            totalWeight = "100",
            thickness = "0.5",
            cutWidth = "50",
            rolls = "5",
            innerDiameter = ""
        )

        assertThat(isFormValid(input)).isFalse()
    }

    @Test
    fun isFormValid_invalidRequiredField_returnsFalse() {
        val input = InputState(
            selectedMaterial = "cu",
            totalWeight = "",
            thickness = "0.5",
            cutWidth = "50",
            rolls = "5",
            innerDiameter = ""
        )

        assertThat(isFormValid(input)).isFalse()
    }

    @Test
    fun isFormValid_emptyOptionalDiameter_returnsTrue() {
        val input = InputState(
            selectedMaterial = "cu",
            totalWeight = "100",
            thickness = "0.5",
            cutWidth = "50",
            rolls = "5",
            innerDiameter = ""
        )

        assertThat(isFormValid(input)).isTrue()
    }
}
