package com.metraza.data

data class RollResult(
    val lengthM: Double,
    val weightKg: Double,
    val outerDiameterMm: Double? = null,
    val tapeRadiusMm: Double? = null
)

data class CalculationResult(
    val rolls: Int,
    val base: RollResult,
    val plusVariant: RollResult,
    val minusVariant: RollResult
)

data class InputState(
    val selectedMaterial: String? = null,
    val totalWeight: String = "",
    val thickness: String = "",
    val cutWidth: String = "",
    val rolls: String = "",
    val innerDiameter: String = ""
)

data class FieldValidation(
    val isValid: Boolean,
    val errorMessage: String = ""
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
