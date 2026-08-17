package com.example.racunanjekilaze.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.racunanjekilaze.data.OrderEntry
import com.example.racunanjekilaze.data.formatValue
import com.example.racunanjekilaze.ui.components.AccentGlassCard
import com.example.racunanjekilaze.ui.components.GlassCard
import com.example.racunanjekilaze.ui.components.SmallButton
import com.example.racunanjekilaze.ui.theme.Accent
import com.example.racunanjekilaze.ui.theme.ErrorColor
import com.example.racunanjekilaze.ui.theme.LayoutTokens
import com.example.racunanjekilaze.ui.theme.Surface
import com.example.racunanjekilaze.ui.theme.SurfaceLight
import com.example.racunanjekilaze.ui.theme.TextPrimary
import com.example.racunanjekilaze.ui.theme.TextSecondary
import java.util.Locale

import com.example.racunanjekilaze.ui.theme.SuccessColor

@Composable
fun OrderListSection(
    entries: List<OrderEntry>,
    entryCountText: String,
    onDeleteEntry: (Int) -> Unit,
    layout: LayoutTokens,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = layout.cardRadius
    ) {
        val entrySpacing = if (layout.isCompact) 6.dp else 8.dp
        Column(
            modifier = Modifier.padding(
                horizontal = layout.cardPaddingHorizontal,
                vertical = layout.cardPaddingVertical
            ),
            verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STAVKE NALOGA",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.7f)
                )
                Text(
                    text = entryCountText,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.3f)
                )
            }
            if (entries.isEmpty()) {
                Text(
                    text = "Nalog je prazan. Unesi dimenzije i dodaj prvu stavku.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(entrySpacing),
                    modifier = Modifier.heightIn(max = 400.dp),
                    userScrollEnabled = true
                ) {
                    items(
                        items = entries,
                        key = { it.id }
                    ) { entry ->
                        OrderEntryRow(
                            entry = entry,
                            onDelete = { onDeleteEntry(entry.id) },
                            layout = layout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderEntryRow(
    entry: OrderEntry,
    onDelete: () -> Unit,
    layout: LayoutTokens
) {
    val dimensions = entry.dimensions
    val thicknessText = String.format(Locale.getDefault(), "%.2g", dimensions.thicknessMm)
    val primaryText = "${entry.material}  •  $thicknessText x ${formatValue(dimensions.widthMm, 0)} mm"
    val secondaryText = "${entry.coilCount} kom  •  ${formatValue(entry.result.singleRoll.lengthM)} m"
    val singleWeightText = "Jedna traka: ${formatValue(entry.result.singleRoll.weightKg)} kg"
    val totalWeightText = "${formatValue(entry.result.totalWeightKg)} kg"
    val rowSpacing = if (layout.isCompact) 8.dp else 12.dp

    AccentGlassCard(
        bgColor = SurfaceLight,
        cornerRadius = 8.dp,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = layout.itemPaddingHorizontal,
                    vertical = layout.itemPaddingVertical
                ),
            horizontalArrangement = Arrangement.spacedBy(rowSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = singleWeightText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SuccessColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = totalWeightText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Accent,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                SmallButton(
                    text = if (layout.isCompact) "×" else "Obriši",
                    onClick = onDelete,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .widthIn(min = 44.dp)
                        .semantics { contentDescription = "Obriši stavku" },
                    containerColor = Surface,
                    contentColor = ErrorColor
                )
            }
        }
    }
}
