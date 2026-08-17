package com.example.racunanjekilaze.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.racunanjekilaze.data.formatRemainingWeight
import com.example.racunanjekilaze.data.formatValue
import com.example.racunanjekilaze.ui.components.AccentGlassCard
import com.example.racunanjekilaze.ui.components.AutoSizeText
import com.example.racunanjekilaze.ui.components.ResponsiveInfoRow
import com.example.racunanjekilaze.ui.theme.Accent
import com.example.racunanjekilaze.ui.theme.LayoutTokens
import com.example.racunanjekilaze.ui.theme.SurfaceElevated
import com.example.racunanjekilaze.ui.theme.SuccessColor
import com.example.racunanjekilaze.ui.theme.TextSecondary

@Composable
fun TotalSection(
    totalWeightKg: Double,
    totalCoilsText: String,
    remainingWeight: Double?,
    layout: LayoutTokens,
    modifier: Modifier = Modifier
) {
    val totalWeightText = "${formatValue(totalWeightKg)} kg"
    val remainingInfo = formatRemainingWeight(remainingWeight)
    val remainingColor = if (remainingInfo?.third == true) SuccessColor else Accent

    AccentGlassCard(
        modifier = modifier,
        bgColor = SurfaceElevated,
        cornerRadius = layout.cardRadius
    ) {
        val innerSpacing = if (layout.isCompact) 6.dp else 8.dp
        Column(
            modifier = Modifier.padding(
                horizontal = layout.cardPaddingHorizontal,
                vertical = layout.cardPaddingVertical
            ),
            verticalArrangement = Arrangement.spacedBy(innerSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.6f)) {
                    Text(
                        text = "UKUPNA TEŽINA",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = totalCoilsText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AutoSizeText(
                    text = totalWeightText,
                    style = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.End),
                    color = Color.White,
                    maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                    maxLines = 1,
                    modifier = Modifier.weight(0.4f)
                )
            }
            if (remainingInfo != null) {
                CompositionLocalProvider(LocalContentColor provides remainingColor) {
                    ResponsiveInfoRow(
                        label = remainingInfo.first,
                        value = remainingInfo.second
                    )
                }
            }
        }
    }
}
