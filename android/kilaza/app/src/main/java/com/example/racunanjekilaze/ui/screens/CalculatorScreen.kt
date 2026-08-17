package com.example.racunanjekilaze.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.racunanjekilaze.data.CalculationResult
import com.example.racunanjekilaze.ui.components.PrimaryButton
import com.example.racunanjekilaze.ui.sections.HeaderSection
import com.example.racunanjekilaze.ui.sections.InputSection
import com.example.racunanjekilaze.ui.sections.KilazaResultCard
import com.example.racunanjekilaze.ui.theme.BackgroundGradientEnd
import com.example.racunanjekilaze.ui.theme.BackgroundGradientStart
import com.example.racunanjekilaze.ui.theme.ErrorColor
import com.example.racunanjekilaze.ui.theme.ResultGreen
import com.example.racunanjekilaze.ui.theme.SuccessColor
import com.example.racunanjekilaze.ui.theme.layoutTokens

@Composable
fun CalculatorScreen() {
    val state = rememberCalculatorScreenState()
    val layout = layoutTokens()
    val scrollState = rememberScrollState()

    val radialFocus = remember { FocusRequester() }
    val coreFocus = remember { FocusRequester() }
    val thicknessFocus = remember { FocusRequester() }
    val widthFocus = remember { FocusRequester() }
    val coilsFocus = remember { FocusRequester() }
    var calculatedPreview by remember { mutableStateOf<CalculationResult?>(null) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val doCalculate = {
        val preview = state.currentPreview
        if (preview == null) {
            calculatedPreview = null
            isError = true
            message = "Popuni sva obavezna polja ispravno."
        } else {
            calculatedPreview = preview
            isError = false
            message = "✓ Proračun uspešno završen."
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
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(bottom = layout.screenPaddingVertical)
        ) {
            HeaderSection(
                layout = layout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = layout.screenPaddingVertical)
            )
            Spacer(modifier = Modifier.height(layout.sectionSpacing))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = layout.screenPaddingHorizontal),
                verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing)
            ) {
                InputSection(
                    radialThickness = state.radialThickness,
                    onRadialThicknessChange = { state.radialThickness = it },
                    coreDiameter = state.coreDiameter,
                    onCoreDiameterChange = { state.coreDiameter = it },
                    thickness = state.thickness,
                    onThicknessChange = { state.thickness = it },
                    width = state.width,
                    onWidthChange = { state.width = it },
                    selectedMaterial = state.selectedMaterial,
                    onMaterialSelected = { state.selectedMaterial = it },
                    coils = state.coils,
                    onCoilsChange = { state.coils = it },
                    layout = layout,
                    radialFocus = radialFocus,
                    coreFocus = coreFocus,
                    thicknessFocus = thicknessFocus,
                    widthFocus = widthFocus,
                    coilsFocus = coilsFocus,
                    onCalculate = doCalculate
                )

                PrimaryButton(
                    text = "IZRAČUNAJ",
                    onClick = doCalculate,
                    containerColor = ResultGreen,
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = layout.buttonMinHeight,
                    contentPadding = layout.buttonContentPadding
                )

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

                calculatedPreview?.let { preview ->
                    KilazaResultCard(
                        preview = preview,
                        layout = layout
                    )
                }
            }
        }
    }
}
