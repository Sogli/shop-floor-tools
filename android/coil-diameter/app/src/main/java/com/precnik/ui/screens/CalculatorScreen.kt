package com.precnik.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.precnik.R
import com.precnik.ui.components.AccentGlassCard
import com.precnik.ui.components.GlassCard
import com.precnik.ui.components.LabeledTextField
import com.precnik.ui.components.PrimaryButton
import com.precnik.ui.theme.Accent
import com.precnik.ui.theme.BackgroundGradientEnd
import com.precnik.ui.theme.BackgroundGradientStart
import com.precnik.ui.theme.ErrorColor
import com.precnik.ui.theme.PrimaryDark
import com.precnik.ui.theme.SuccessColor
import com.precnik.ui.theme.TextPrimary
import com.precnik.ui.theme.TextSecondary
import com.precnik.ui.theme.layoutTokens
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun CalculatorScreen() {
    var coreDiameter by rememberSaveable { mutableStateOf("") }
    var length by rememberSaveable { mutableStateOf("") }
    var thickness by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val layout = layoutTokens()

    // Focus requesters za navigaciju
    val focusManager = LocalFocusManager.current
    val coreFocus = remember { FocusRequester() }
    val lengthFocus = remember { FocusRequester() }
    val thicknessFocus = remember { FocusRequester() }

    val doCalculate: () -> Unit = {
        focusManager.clearFocus()
        result = null
        message = ""
        isError = false

        try {
            val core = parsePositiveDouble(coreDiameter, "Unutrašnji prečnik")
            val len = parsePositiveDouble(length, "Dužina")
            val thick = parsePositiveDouble(thickness, "Debljina")

            val lengthMm = len * 1000.0
            val outerDiameter = sqrt(
                core * core + (4.0 / Math.PI) * lengthMm * thick
            )
            result = outerDiameter.roundToInt()
            message = "✓ Proračun uspešno završen."
            isError = false
        } catch (ex: IllegalArgumentException) {
            message = "✗ ${ex.message}"
            isError = true
        }
    }

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = layout.screenPaddingHorizontal,
                    vertical = layout.screenPaddingVertical
                ),
            verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing)
        ) {
            // Header with Logo
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val logoPainter = painterResource(id = R.drawable.logo)
                val aspectRatio = remember(logoPainter) {
                    val width = logoPainter.intrinsicSize.width
                    val height = logoPainter.intrinsicSize.height
                    if (width.isNaN() || height.isNaN() || height == 0f) 3f else width / height
                }
                Image(
                    painter = logoPainter,
                    contentDescription = "Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Prečnik",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Kalkulator spoljašnjeg prečnika",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Input Card
            GlassCard(cornerRadius = layout.cardRadius) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = layout.cardPaddingHorizontal,
                        vertical = layout.cardPaddingVertical
                    ),
                    verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing)
                ) {
                    Text(
                        text = "PARAMETRI",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    LabeledTextField(
                        label = "Unutrašnji prečnik (mm)",
                        value = coreDiameter,
                        onValueChange = { coreDiameter = it },
                        placeholder = "npr. 400",
                        keyboardType = KeyboardType.Decimal,
                        focusRequester = coreFocus,
                        imeAction = ImeAction.Next,
                        onImeAction = { lengthFocus.requestFocus() }
                    )

                    LabeledTextField(
                        label = "Dužina (m)",
                        value = length,
                        onValueChange = { length = it },
                        placeholder = "npr. 500",
                        keyboardType = KeyboardType.Decimal,
                        focusRequester = lengthFocus,
                        imeAction = ImeAction.Next,
                        onImeAction = { thicknessFocus.requestFocus() }
                    )

                    LabeledTextField(
                        label = "Debljina (mm)",
                        value = thickness,
                        onValueChange = { thickness = it },
                        placeholder = "npr. 0.5",
                        keyboardType = KeyboardType.Decimal,
                        focusRequester = thicknessFocus,
                        imeAction = ImeAction.Done,
                        onImeAction = doCalculate
                    )
                }
            }

            // Calculate Button
            PrimaryButton(
                text = "IZRAČUNAJ",
                onClick = doCalculate,
                modifier = Modifier.fillMaxWidth(),
                minHeight = layout.buttonMinHeight,
                contentPadding = layout.buttonContentPadding
            )

            // Message
            AnimatedVisibility(
                visible = message.isNotBlank(),
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150))
            ) {
                Text(
                    text = message,
                    color = if (isError) ErrorColor else SuccessColor,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Result Card
            if (result != null) {
                AccentGlassCard(
                    bgColor = PrimaryDark,
                    cornerRadius = layout.cardRadius
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = layout.cardPaddingHorizontal,
                                vertical = layout.cardPaddingVertical
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SPOLJAŠNJI PREČNIK",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$result mm",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 56.sp
                            ),
                            color = Accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun parsePositiveDouble(raw: String, fieldName: String): Double {
    val cleaned = raw.trim().replace(',', '.')
    if (cleaned.isEmpty()) {
        throw IllegalArgumentException("$fieldName je obavezno polje.")
    }

    val value = cleaned.toDoubleOrNull()
        ?: throw IllegalArgumentException("$fieldName mora biti numerička vrednost.")

    if (value <= 0.0) {
        throw IllegalArgumentException("$fieldName mora biti pozitivna vrednost.")
    }
    return value
}
