package com.example.racunanjekilaze.ui.screens

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalculatorScreenStateTest {
    @Test
    fun currentPreviewUsesValidInputsWithoutAddingEntry() {
        val state = CalculatorScreenState()

        state.selectedMaterial = "Cu"
        state.radialThickness = "150"
        state.coreDiameter = "400"
        state.thickness = "2"
        state.width = "72"
        state.coils = "3"

        val preview = state.currentPreview

        assertThat(preview).isNotNull()
        assertThat(preview!!.coilCount).isEqualTo(3)
        assertThat(preview.singleRoll.lengthM).isWithin(0.01).of(129.59)
        assertThat(preview.singleRoll.weightKg).isWithin(0.001).of(167.202)
        assertThat(preview.totalWeightKg).isWithin(0.001).of(501.606)
        assertThat(state.orderEntries).isEmpty()
    }

    @Test
    fun currentPreviewIsNullUntilAllRequiredInputsAreValid() {
        val state = CalculatorScreenState()

        state.selectedMaterial = "Cu"
        state.radialThickness = "150"
        state.coreDiameter = "400"
        state.thickness = "2"
        state.width = ""
        state.coils = "3"

        assertThat(state.currentPreview).isNull()
    }

    @Test
    fun currentPreviewDefaultsBlankCoilsToOne() {
        val state = CalculatorScreenState()

        state.selectedMaterial = "Cu"
        state.radialThickness = "150"
        state.coreDiameter = "400"
        state.thickness = "2"
        state.width = "72"
        state.coils = ""

        val preview = state.currentPreview

        assertThat(preview).isNotNull()
        assertThat(preview!!.coilCount).isEqualTo(1)
        assertThat(preview.singleRoll.weightKg).isWithin(0.001).of(167.202)
        assertThat(preview.totalWeightKg).isWithin(0.001).of(167.202)
    }

    @Test
    fun currentPreviewAcceptsDecimalCommaInputs() {
        val state = CalculatorScreenState()

        state.selectedMaterial = "CuZn30"
        state.radialThickness = "150,5"
        state.coreDiameter = "400"
        state.thickness = "1,5"
        state.width = "72"
        state.coils = "2"

        assertThat(state.currentPreview).isNotNull()
    }
}
