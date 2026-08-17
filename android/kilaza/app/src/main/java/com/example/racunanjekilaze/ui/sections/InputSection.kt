package com.example.racunanjekilaze.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.racunanjekilaze.data.CalculationResult
import com.example.racunanjekilaze.data.formatValue
import com.example.racunanjekilaze.data.getTrakaForm
import com.example.racunanjekilaze.ui.components.AccentGlassCard
import com.example.racunanjekilaze.ui.components.GlassCard
import com.example.racunanjekilaze.ui.components.LabeledTextField
import com.example.racunanjekilaze.ui.components.MaterialGridSelector
import com.example.racunanjekilaze.ui.components.fieldColors
import com.example.racunanjekilaze.ui.theme.Accent
import com.example.racunanjekilaze.ui.theme.BorderColor
import com.example.racunanjekilaze.ui.theme.Copper
import com.example.racunanjekilaze.ui.theme.LayoutTokens
import com.example.racunanjekilaze.ui.theme.ResultAccent
import com.example.racunanjekilaze.ui.theme.ResultGreen
import com.example.racunanjekilaze.ui.theme.SurfaceElevated
import com.example.racunanjekilaze.ui.theme.SurfaceLight
import com.example.racunanjekilaze.ui.theme.TextPrimary
import com.example.racunanjekilaze.ui.theme.TextSecondary
import java.util.Locale

private val CORE_DIAMETER_PRESETS = listOf(
    "400" to "400 mm",
    "500" to "500 mm"
)

internal fun formatCoilWeightTitle(coilCount: Int): String {
    return "Kilaža za $coilCount ${getTrakaForm(coilCount)}"
}

@Composable
fun InputSection(
    radialThickness: String,
    onRadialThicknessChange: (String) -> Unit,
    coreDiameter: String,
    onCoreDiameterChange: (String) -> Unit,
    thickness: String,
    onThicknessChange: (String) -> Unit,
    width: String,
    onWidthChange: (String) -> Unit,
    selectedMaterial: String,
    onMaterialSelected: (String) -> Unit,
    coils: String,
    onCoilsChange: (String) -> Unit,
    layout: LayoutTokens,
    modifier: Modifier = Modifier,
    radialFocus: FocusRequester? = null,
    coreFocus: FocusRequester? = null,
    thicknessFocus: FocusRequester? = null,
    widthFocus: FocusRequester? = null,
    coilsFocus: FocusRequester? = null,
    onCalculate: () -> Unit = {}
) {
    val isRadialError = radialThickness.isNotBlank() && !radialThickness.isPositiveDecimal()
    val isCoreError = coreDiameter.isNotBlank() && !coreDiameter.isPositiveDecimal()
    val isThicknessError = thickness.isNotBlank() && !thickness.isPositiveDecimal()
    val isWidthError = width.isNotBlank() && !width.isPositiveDecimal()
    val isCoilsError = coils.isNotBlank() && coils.trim().toIntOrNull()?.let { it > 0 } != true

    GlassCard(
        modifier = modifier,
        cornerRadius = layout.cardRadius
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = layout.cardPaddingHorizontal,
                vertical = layout.cardPaddingVertical
            ),
            verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing)
        ) {
            Text(
                text = "DIMENZIJE TRAKE",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            MaterialGridSelector(
                label = "Materijal",
                selectedMaterial = selectedMaterial,
                onMaterialSelected = { material ->
                    onMaterialSelected(material)
                    radialFocus?.requestFocus()
                }
            )

            LabeledTextField(
                label = "Poluprečnik trake (mm)",
                value = radialThickness,
                onValueChange = onRadialThicknessChange,
                placeholder = "npr. 150",
                isError = isRadialError,
                keyboardType = KeyboardType.Decimal,
                focusRequester = radialFocus,
                imeAction = ImeAction.Next,
                onImeAction = { coreFocus?.requestFocus() }
            )

            CoreDiameterSelector(
                coreDiameter = coreDiameter,
                onCoreDiameterChange = onCoreDiameterChange,
                isError = isCoreError,
                focusRequester = coreFocus,
                nextFocus = thicknessFocus
            )

            LabeledTextField(
                label = "Debljina materijala (mm)",
                value = thickness,
                onValueChange = onThicknessChange,
                placeholder = "npr. 2",
                isError = isThicknessError,
                keyboardType = KeyboardType.Decimal,
                focusRequester = thicknessFocus,
                imeAction = ImeAction.Next,
                onImeAction = { widthFocus?.requestFocus() }
            )

            LabeledTextField(
                label = "Širina trake (mm)",
                value = width,
                onValueChange = onWidthChange,
                placeholder = "npr. 72",
                isError = isWidthError,
                keyboardType = KeyboardType.Decimal,
                focusRequester = widthFocus,
                imeAction = ImeAction.Next,
                onImeAction = { coilsFocus?.requestFocus() }
            )

            LabeledTextField(
                label = "Broj traka",
                value = coils,
                onValueChange = onCoilsChange,
                placeholder = "prazno = 1",
                isError = isCoilsError,
                keyboardType = KeyboardType.Number,
                focusRequester = coilsFocus,
                imeAction = ImeAction.Done,
                onImeAction = onCalculate
            )
        }
    }
}

@Composable
private fun CoreDiameterSelector(
    coreDiameter: String,
    onCoreDiameterChange: (String) -> Unit,
    isError: Boolean,
    focusRequester: FocusRequester?,
    nextFocus: FocusRequester?
) {
    val trimmedValue = coreDiameter.trim()
    val selectedPreset = CORE_DIAMETER_PRESETS.firstOrNull { it.first == trimmedValue }
    val customValue = if (selectedPreset == null) coreDiameter else ""

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Unutrašnji prečnik (mm)",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CORE_DIAMETER_PRESETS.forEach { (value, label) ->
                CoreDiameterChip(
                    text = label,
                    isSelected = selectedPreset?.first == value,
                    onClick = {
                        onCoreDiameterChange(value)
                        nextFocus?.requestFocus()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = customValue,
                onValueChange = onCoreDiameterChange,
                placeholder = {
                    Text(
                        text = "Proizvoljno",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { nextFocus?.requestFocus() }
                ),
                isError = isError,
                colors = fieldColors(isError = isError),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .then(
                        if (focusRequester != null) Modifier.focusRequester(focusRequester)
                        else Modifier
                    )
            )
        }
    }
}

@Composable
private fun CoreDiameterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val backgroundColor = if (isSelected) SurfaceElevated else SurfaceLight
    val borderColor = if (isSelected) Copper else BorderColor
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val textColor = if (isSelected) Accent else TextPrimary
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics {
                stateDescription = if (isSelected) "Izabran" else "Nije izabran"
            }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun KilazaResultCard(
    preview: CalculationResult,
    layout: LayoutTokens,
    modifier: Modifier = Modifier
) {
    AccentGlassCard(
        modifier = modifier,
        bgColor = ResultGreen,
        cornerRadius = layout.cardRadius
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = layout.cardPaddingHorizontal,
                    vertical = layout.cardPaddingVertical
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatCoilWeightTitle(preview.coilCount).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.heightIn(min = 4.dp))
            Text(
                text = "${formatValue(preview.totalWeightKg)} kg",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                color = ResultAccent,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Jedna traka: ${formatValue(preview.singleRoll.weightKg)} kg",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun String.isPositiveDecimal(): Boolean {
    return trim().replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true
}
