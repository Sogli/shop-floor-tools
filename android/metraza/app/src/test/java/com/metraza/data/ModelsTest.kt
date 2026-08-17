package com.metraza.data

import com.google.common.truth.Truth.assertThat
import com.metraza.ui.theme.MATERIAL_DENSITIES
import com.metraza.ui.theme.MATERIAL_DISPLAY_NAMES
import org.junit.Test

class ModelsTest {

    @Test
    fun materialDisplayNames_areStringValues() {
        assertThat(MATERIAL_DISPLAY_NAMES).containsExactly(
            "Cu",
            "CuZn10",
            "CuZn15",
            "CuZn20",
            "CuZn30",
            "CuZn37"
        ).inOrder()
    }

    @Test
    fun materialDensities_includeDisplayNamesAndNumericAliases() {
        assertThat(MATERIAL_DENSITIES).containsAtLeastEntriesIn(
            mapOf(
                "cu" to 8960,
                "cuzn10" to 8800,
                "10" to 8800,
                "cuzn15" to 8630,
                "15" to 8630,
                "cuzn20" to 8530,
                "20" to 8530,
                "cuzn30" to 8403,
                "30" to 8403,
                "cuzn37" to 8285,
                "37" to 8285
            )
        )
    }

    @Test
    fun rollResult_defaultOuterDiameterAndTapeRadius_areNull() {
        val result = RollResult(10.0, 5.0)

        assertThat(result.outerDiameterMm).isNull()
        assertThat(result.tapeRadiusMm).isNull()
    }

    @Test
    fun calculationResult_holdsBaseAndVariantRollResults() {
        val result = CalculationResult(
            rolls = 2,
            base = RollResult(10.0, 5.0),
            plusVariant = RollResult(11.0, 5.5),
            minusVariant = RollResult(9.0, 4.5)
        )

        assertThat(result.rolls).isEqualTo(2)
        assertThat(result.base.lengthM).isEqualTo(10.0)
        assertThat(result.plusVariant.weightKg).isEqualTo(5.5)
        assertThat(result.minusVariant.weightKg).isEqualTo(4.5)
    }

    @Test
    fun inputState_defaultValues_areEmpty() {
        val state = InputState()

        assertThat(state.selectedMaterial).isNull()
        assertThat(state.totalWeight).isEmpty()
        assertThat(state.thickness).isEmpty()
        assertThat(state.cutWidth).isEmpty()
        assertThat(state.rolls).isEmpty()
        assertThat(state.innerDiameter).isEmpty()
    }

    @Test
    fun inputState_selectedMaterial_isNullableString() {
        val state = InputState(selectedMaterial = "cu")

        assertThat(state.selectedMaterial).isEqualTo("cu")
    }

    @Test
    fun fieldValidation_validDefaultsToEmptyErrorMessage() {
        val validation = FieldValidation(true)

        assertThat(validation.errorMessage).isEmpty()
    }

    @Test
    fun fieldValidation_invalidKeepsErrorMessage() {
        val validation = FieldValidation(false, "error")

        assertThat(validation.errorMessage).isEqualTo("error")
    }

    @Test
    fun messageState_holdsTextAndType() {
        val message = MessageState("Sačuvano", MessageType.SUCCESS)

        assertThat(message.text).isEqualTo("Sačuvano")
        assertThat(message.type).isEqualTo(MessageType.SUCCESS)
    }
}
