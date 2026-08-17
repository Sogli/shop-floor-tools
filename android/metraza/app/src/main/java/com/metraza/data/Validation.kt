package com.metraza.data

private const val MIN_INPUT = 0.001
private const val MAX_INPUT = 100000.0

fun validateTotalWeight(value: String): FieldValidation {
    val cleaned = value.trim().replace(",", ".")
    if (cleaned.isEmpty()) {
        return FieldValidation(false, "Ukupna težina je obavezno polje.")
    }

    val numValue = cleaned.toDoubleOrNull()
    if (numValue == null) {
        return FieldValidation(false, "Ukupna težina mora biti numerička vrednost.")
    }

    if (numValue < MIN_INPUT || numValue > MAX_INPUT) {
        return FieldValidation(false, "Ukupna težina mora biti između $MIN_INPUT i $MAX_INPUT.")
    }

    return FieldValidation(true)
}

fun validateThickness(value: String): FieldValidation {
    val cleaned = value.trim().replace(",", ".")
    if (cleaned.isEmpty()) {
        return FieldValidation(false, "Debljina je obavezno polje.")
    }

    val numValue = cleaned.toDoubleOrNull()
    if (numValue == null) {
        return FieldValidation(false, "Debljina mora biti numerička vrednost.")
    }

    if (numValue < MIN_INPUT || numValue > MAX_INPUT) {
        return FieldValidation(false, "Debljina mora biti između $MIN_INPUT i $MAX_INPUT.")
    }

    return FieldValidation(true)
}

fun validateCutWidth(value: String): FieldValidation {
    val cleaned = value.trim().replace(",", ".")
    if (cleaned.isEmpty()) {
        return FieldValidation(false, "Širina rezanja je obavezno polje.")
    }

    val numValue = cleaned.toDoubleOrNull()
    if (numValue == null) {
        return FieldValidation(false, "Širina rezanja mora biti numerička vrednost.")
    }

    if (numValue < MIN_INPUT || numValue > MAX_INPUT) {
        return FieldValidation(false, "Širina rezanja mora biti između $MIN_INPUT i $MAX_INPUT.")
    }

    return FieldValidation(true)
}

fun validateRolls(value: String): FieldValidation {
    val cleaned = value.trim()
    if (cleaned.isEmpty()) {
        return FieldValidation(false, "Broj rezova je obavezno polje.")
    }

    val numValue = cleaned.toIntOrNull()
    if (numValue == null) {
        return FieldValidation(false, "Broj rezova mora biti ceo broj.")
    }

    if (numValue < 1 || numValue > MAX_INPUT.toInt()) {
        return FieldValidation(false, "Broj rezova mora biti između 1 i ${MAX_INPUT.toInt()}.")
    }

    return FieldValidation(true)
}

fun validateInnerDiameter(value: String): FieldValidation {
    val cleaned = value.trim().replace(",", ".")
    if (cleaned.isEmpty()) {
        return FieldValidation(true) // Optional field
    }

    val numValue = cleaned.toDoubleOrNull()
    if (numValue == null) {
        return FieldValidation(false, "Unutrašnji prečnik mora biti numerička vrednost.")
    }

    if (numValue < MIN_INPUT || numValue > MAX_INPUT) {
        return FieldValidation(false, "Unutrašnji prečnik mora biti između $MIN_INPUT i $MAX_INPUT.")
    }

    return FieldValidation(true)
}

fun isFormValid(input: InputState): Boolean {
    return input.selectedMaterial != null &&
            validateTotalWeight(input.totalWeight).isValid &&
            validateThickness(input.thickness).isValid &&
            validateCutWidth(input.cutWidth).isValid &&
            validateRolls(input.rolls).isValid &&
            validateInnerDiameter(input.innerDiameter).isValid
}
