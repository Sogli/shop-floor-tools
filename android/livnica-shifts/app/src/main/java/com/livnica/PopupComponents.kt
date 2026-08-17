package com.livnica

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.livnica.ui.components.AutoSizeText
import kotlin.math.min

@Composable
fun PopupDialog(
    title: String,
    onDismiss: () -> Unit,
    widthRatio: Float = UI.popupWidthRatio,
    height: Dp? = null,
    heightRatio: Float? = null,
    scrollable: Boolean = true,
    accent: Color = THEME.accentPrimary,
    content: @Composable ColumnScope.() -> Unit
) {
    val config = LocalConfiguration.current
    val popupWidth = config.screenWidthDp.dp * widthRatio
    val popupHeight = height ?: heightRatio?.let { config.screenHeightDp.dp * it }
    val overlayInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(THEME.bgDark.copy(alpha = 0.94f))
                .clickable(
                    interactionSource = overlayInteraction,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = popupWidth)
                    .then(
                        if (popupHeight != null) {
                            Modifier.height(popupHeight)
                        } else {
                            Modifier.wrapContentHeight()
                        }
                    )
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(UI.cardPadding.dp),
                    verticalArrangement = Arrangement.spacedBy(UI.defaultSpacing.dp)
                ) {
                    AutoSizeText(
                        text = title,
                        color = THEME.textPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                        minFontSize = MaterialTheme.typography.titleMedium.fontSize,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(accent.copy(alpha = 0.9f))
                    )
                    val useScroll = scrollable && popupHeight != null
                    if (useScroll) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = true)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(UI.largeSpacing.dp)
                        ) {
                            content()
                        }
                    } else if (scrollable) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(UI.largeSpacing.dp)
                        ) {
                            content()
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(UI.largeSpacing.dp)
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PopupCard(
    modifier: Modifier = Modifier,
    bgColor: Color = THEME.bgCard,
    borderColor: Color = THEME.glassBorder,
    spacing: Dp = UI.defaultSpacing.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(modifier = modifier, bgColor = bgColor, borderColor = borderColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            content()
        }
    }
}

@Composable
fun HoursPickerDialog(
    title: String,
    currentHours: Int,
    minHours: Int = 1,
    accent: Color = THEME.accentPrimary,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    PopupDialog(
        title = title,
        onDismiss = onDismiss,
        widthRatio = 0.85f,
        height = UI.hoursPopupHeight.dp,
        scrollable = false,
        accent = accent
    ) {
        val hours = (minHours..CONFIG.pay.defaultHours).toList()
        val rows = hours.chunked(4)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (hour in row) {
                        val isSelected = hour == currentHours
                        RoundedButton(
                            text = hour.toString(),
                            modifier = Modifier
                                .weight(1f)
                                .height(UI.buttonHeight.dp),
                            bgColor = if (isSelected) accent else THEME.bgCard,
                            textColor = if (isSelected) THEME.textDark else THEME.textPrimary,
                            textStyle = MaterialTheme.typography.titleLarge,
                            bold = true,
                            onClick = {
                                onSelect(hour)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompUsedDialog(
    availableHours: Int,
    currentHours: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val maxHours = min(CONFIG.pay.defaultHours, availableHours)
    val hours = (0..maxHours).toList()
    val rows = hours.chunked(5)

    PopupDialog(
        title = "Iskoristi zarađene sate",
        onDismiss = onDismiss,
        widthRatio = 0.85f,
        height = 250.dp,
        scrollable = false,
        accent = THEME.compTimeUsed
    ) {
        AutoSizeText(
            text = "Dostupno: ${availableHours}h",
            color = THEME.compTime,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            maxFontSize = MaterialTheme.typography.bodyLarge.fontSize,
            minFontSize = MaterialTheme.typography.bodyMedium.fontSize,
            maxLines = 1
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (hour in row) {
                        val isSelected = hour == currentHours
                        RoundedButton(
                            text = hour.toString(),
                            modifier = Modifier
                                .weight(1f)
                                .height(UI.buttonHeight.dp),
                            bgColor = if (isSelected) THEME.compTimeUsed else THEME.bgCard,
                            textColor = THEME.textPrimary,
                            textStyle = MaterialTheme.typography.titleLarge,
                            bold = true,
                            onClick = {
                                onSelect(hour)
                                onDismiss()
                            }
                        )
                    }
                    if (row.size < 5) {
                        Spacer(modifier = Modifier.weight((5 - row.size).toFloat()))
                    }
                }
            }
        }
    }
}


