package com.example.racunanjekilaze.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.racunanjekilaze.ui.theme.Accent
import com.example.racunanjekilaze.ui.theme.BorderColor
import com.example.racunanjekilaze.ui.theme.Copper
import com.example.racunanjekilaze.ui.theme.MATERIAL_DISPLAY_NAMES
import com.example.racunanjekilaze.ui.theme.SurfaceElevated
import com.example.racunanjekilaze.ui.theme.SurfaceLight
import com.example.racunanjekilaze.ui.theme.TextPrimary
import com.example.racunanjekilaze.ui.theme.TextSecondary

@Composable
fun MaterialGridSelector(
    label: String,
    selectedMaterial: String,
    onMaterialSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val materials = MATERIAL_DISPLAY_NAMES
    val firstRow = materials.take(3)
    val secondRow = materials.drop(3)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Prvi red - 3 materijala
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                firstRow.forEach { material ->
                    MaterialChip(
                        material = material,
                        isSelected = material == selectedMaterial,
                        onClick = { onMaterialSelected(material) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Drugi red - 3 materijala
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                secondRow.forEach { material ->
                    MaterialChip(
                        material = material,
                        isSelected = material == selectedMaterial,
                        onClick = { onMaterialSelected(material) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialChip(
    material: String,
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
            text = material,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
