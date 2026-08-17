package com.livnica.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.livnica.ModernTextField
import com.livnica.THEME

/**
 * Represents the state of a form field including its value and any validation error.
 */
data class FormFieldState(
    val value: String = "",
    val error: String? = null
) {
    val isValid: Boolean get() = error == null
    val hasError: Boolean get() = error != null
}

/**
 * A form field component with built-in label and error display.
 * Provides consistent styling and accessibility across all forms.
 */
@Composable
fun FormField(
    state: FormFieldState,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    @Suppress("UNUSED_PARAMETER") enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = if (state.hasError) THEME.danger else THEME.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ModernTextField(
            value = state.value,
            onValueChange = onValueChange,
            hintText = placeholder,
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth()
        )
        AnimatedVisibility(visible = state.hasError) {
            Text(
                text = state.error ?: "",
                color = THEME.danger,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
            )
        }
    }
}

/**
 * Common validators for form fields.
 * Each validator returns null if valid, or an error message if invalid.
 */
object Validators {
    /**
     * Validates that a field is not empty.
     */
    fun required(message: String = "Obavezno polje"): (String) -> String? = { value ->
        if (value.isBlank()) message else null
    }

    /**
     * Validates that a field contains a valid number.
     */
    fun numeric(message: String = "Unesi broj"): (String) -> String? = { value ->
        if (value.isBlank()) null // Let required() handle empty
        else if (value.toDoubleOrNull() == null) message
        else null
    }

    /**
     * Validates that a field contains a non-negative number.
     */
    fun nonNegative(message: String = "Vrednost mora biti >= 0"): (String) -> String? = { value ->
        val num = value.toDoubleOrNull()
        when {
            value.isBlank() -> null
            num == null -> "Unesi broj"
            num < 0 -> message
            else -> null
        }
    }

    /**
     * Validates that a number is within a specific range.
     */
    fun range(
        min: Double,
        max: Double,
        message: String = "Vrednost mora biti izmedju $min i $max"
    ): (String) -> String? = { value ->
        val num = value.toDoubleOrNull()
        when {
            value.isBlank() -> null
            num == null -> "Unesi broj"
            num < min || num > max -> message
            else -> null
        }
    }

    /**
     * Validates that a field contains a valid integer.
     */
    fun integer(message: String = "Unesi ceo broj"): (String) -> String? = { value ->
        if (value.isBlank()) null
        else if (value.toIntOrNull() == null) message
        else null
    }

    /**
     * Combines multiple validators, returning the first error found.
     */
    fun combine(vararg validators: (String) -> String?): (String) -> String? = { value ->
        validators.firstNotNullOfOrNull { it(value) }
    }
}

/**
 * Common input filters for form fields.
 */
object InputFilters {
    /**
     * Filters input to only allow decimal numbers (digits and single dot).
     */
    fun decimal(value: String): String {
        var hasDot = false
        return value.filter { char ->
            when {
                char.isDigit() -> true
                char == '.' && !hasDot -> {
                    hasDot = true
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Filters input to only allow digits.
     */
    fun digitsOnly(value: String): String = value.filter { it.isDigit() }

    /**
     * Filters input for coordinate values (allows negative, digits, and single dot).
     */
    fun coordinate(value: String): String {
        var hasDot = false
        var hasMinus = false
        return value.filterIndexed { index, char ->
            when {
                char.isDigit() -> true
                char == '.' && !hasDot -> {
                    hasDot = true
                    true
                }
                char == '-' && index == 0 && !hasMinus -> {
                    hasMinus = true
                    true
                }
                else -> false
            }
        }
    }
}

