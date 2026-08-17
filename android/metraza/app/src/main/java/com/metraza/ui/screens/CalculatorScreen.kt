package com.metraza.ui.screens

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.metraza.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.shrinkVertically
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.runtime.mutableIntStateOf
import com.metraza.data.CalculationResult
import com.metraza.data.MessageState
import com.metraza.data.MessageType
import com.metraza.data.computeAllResults
import com.metraza.data.formatNumber
import com.metraza.data.parseDouble
import com.metraza.data.parseInt
import com.metraza.data.parseMaterial
import com.metraza.data.getRollWord
import com.metraza.ui.components.AccentGlassCard
import com.metraza.ui.components.GlassCard
import com.metraza.ui.components.LabeledTextField
import com.metraza.ui.components.MaterialGridSelector
import com.metraza.ui.components.PrimaryButton
import com.metraza.ui.theme.BackgroundGradientEnd
import com.metraza.ui.theme.BackgroundGradientStart
import com.metraza.ui.theme.ErrorColor
import com.metraza.ui.theme.MATERIAL_PLACEHOLDER
import com.metraza.ui.theme.Primary
import com.metraza.ui.theme.PrimaryDark
import com.metraza.ui.theme.Secondary
import com.metraza.ui.theme.SuccessColor
import com.metraza.ui.theme.TextPrimary
import com.metraza.ui.theme.TextSecondary
import com.metraza.ui.theme.layoutTokens
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen() {
    var totalWeight by rememberSaveable { mutableStateOf("") }
    var thickness by rememberSaveable { mutableStateOf("") }
    var cutWidth by rememberSaveable { mutableStateOf("") }
    var rolls by rememberSaveable { mutableStateOf("") }
    var innerDiameter by rememberSaveable { mutableStateOf("") }
    var selectedMaterial by rememberSaveable { mutableStateOf(MATERIAL_PLACEHOLDER) }
    var messageState by remember { mutableStateOf(MessageState("", MessageType.NONE)) }

    // Focus requesters za navigaciju između polja
    val focusManager = LocalFocusManager.current
    val weightFocus = remember { FocusRequester() }
    val thicknessFocus = remember { FocusRequester() }
    val widthFocus = remember { FocusRequester() }
    val rollsFocus = remember { FocusRequester() }
    val diameterFocus = remember { FocusRequester() }
    var results by remember { mutableStateOf<List<CalculationResult>>(emptyList()) }
    val layout = layoutTokens()

    // Scroll state i detekcija tastature
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val isKeyboardVisible = WindowInsets.isImeVisible
    var resultsPosition by remember { mutableIntStateOf(0) }

    // Funkcija za izračunavanje
    val doCalculate: () -> Unit = {
        focusManager.clearFocus()
        messageState = MessageState("", MessageType.NONE)
        results = emptyList()
        try {
            val material = parseMaterial(selectedMaterial)
            val weight = parseDouble(totalWeight, "Ukupna težina")
            val thick = parseDouble(thickness, "Debljina")
            val width = parseDouble(cutWidth, "Širina rezanja")
            val rollCount = parseInt(rolls, "Broj rezova")
            val innerD = if (innerDiameter.trim().isNotEmpty()) {
                parseDouble(innerDiameter, "Unutrašnji prečnik")
            } else null

            val calcResults = computeAllResults(weight, width, thick, rollCount, material, innerD)
            if (calcResults.isNotEmpty()) {
                results = calcResults
                messageState = MessageState(
                    "✓ Proračun uspešno završen.",
                    MessageType.SUCCESS
                )
                // Skroluj do rezultata
                coroutineScope.launch {
                    delay(150) // Sačekaj da se UI ažurira
                    scrollState.animateScrollTo(resultsPosition)
                }
            } else {
                messageState = MessageState(
                    "✗ Proverite ulazne parametre.",
                    MessageType.ERROR
                )
            }
        } catch (ex: IllegalArgumentException) {
            messageState = MessageState(
                "✗ ${ex.message}",
                MessageType.ERROR
            )
        }
    }

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
    )

    // Kompaktan spacing kada je tastatura otvorena
    val currentSpacing = if (isKeyboardVisible) 8.dp else layout.sectionSpacing

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
                .navigationBarsPadding()
                .imePadding()
                .padding(
                    horizontal = layout.screenPaddingHorizontal,
                    vertical = if (isKeyboardVisible) 8.dp else layout.screenPaddingVertical
                )
                .padding(bottom = if (isKeyboardVisible) layout.buttonMinHeight + 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(currentSpacing)
        ) {
            // Header with Logo - sakriven kada je tastatura otvorena
            AnimatedVisibility(
                visible = !isKeyboardVisible,
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
            ) {
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
                            .fillMaxWidth(0.72f)
                            .widthIn(max = 300.dp)
                            .heightIn(max = 92.dp)
                            .aspectRatio(aspectRatio)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Metraža",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kalkulator dužine i težine trake",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Input Card
            GlassCard(cornerRadius = layout.cardRadius) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = layout.cardPaddingHorizontal,
                        vertical = if (isKeyboardVisible) 12.dp else layout.cardPaddingVertical
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (isKeyboardVisible) 10.dp else layout.sectionSpacing)
                ) {
                    Text(
                        text = "PARAMETRI",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    MaterialGridSelector(
                        label = "Materijal",
                        selectedMaterial = selectedMaterial,
                        onMaterialSelected = { material ->
                            selectedMaterial = material
                            weightFocus.requestFocus()
                        }
                    )

                    LabeledTextField(
                        label = "Ukupna težina (kg)",
                        value = totalWeight,
                        onValueChange = { totalWeight = it },
                        placeholder = "npr. 1000",
                        keyboardType = KeyboardType.Decimal,
                        focusRequester = weightFocus,
                        imeAction = ImeAction.Next,
                        onImeAction = { thicknessFocus.requestFocus() },
                        suffix = "kg",
                        testTag = "total-weight-input"
                    )

                    LabeledTextField(
                        label = "Debljina (mm)",
                        value = thickness,
                        onValueChange = { thickness = it },
                        placeholder = "npr. 0.5",
                        keyboardType = KeyboardType.Decimal,
                        focusRequester = thicknessFocus,
                        imeAction = ImeAction.Next,
                        onImeAction = { widthFocus.requestFocus() },
                        suffix = "mm",
                        testTag = "thickness-input"
                    )

                    LabeledTextField(
                        label = "Širina rezanja (mm)",
                        value = cutWidth,
                        onValueChange = { cutWidth = it },
                        placeholder = "npr. 100",
                        keyboardType = KeyboardType.Decimal,
                        focusRequester = widthFocus,
                        imeAction = ImeAction.Next,
                        onImeAction = { rollsFocus.requestFocus() },
                        suffix = "mm",
                        testTag = "cut-width-input"
                    )

                    LabeledTextField(
                        label = "Broj rezova",
                        value = rolls,
                        onValueChange = { rolls = it },
                        placeholder = "npr. 5",
                        keyboardType = KeyboardType.Number,
                        focusRequester = rollsFocus,
                        imeAction = ImeAction.Next,
                        onImeAction = { diameterFocus.requestFocus() },
                        suffix = "rez.",
                        testTag = "rolls-input"
                    )

                    LabeledTextField(
                        label = "Unutrašnji prečnik (mm, opciono)",
                        value = innerDiameter,
                        onValueChange = { innerDiameter = it },
                        placeholder = "npr. 150",
                        keyboardType = KeyboardType.Decimal,
                        focusRequester = diameterFocus,
                        imeAction = ImeAction.Done,
                        onImeAction = { doCalculate() },
                        suffix = "mm",
                        supportingText = "Unesi samo ako želiš spoljašnji prečnik.",
                        testTag = "inner-diameter-input"
                    )
                }
            }

            // Calculate Button
            if (!isKeyboardVisible) {
                PrimaryButton(
                    text = "IZRAČUNAJ",
                    onClick = doCalculate,
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = layout.buttonMinHeight,
                    contentPadding = layout.buttonContentPadding,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null
                        )
                    }
                )
            }

            // Message
            AnimatedVisibility(
                visible = messageState.text.isNotBlank(),
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150))
            ) {
                val messageColor = when (messageState.type) {
                    MessageType.SUCCESS -> SuccessColor
                    MessageType.ERROR -> ErrorColor
                    MessageType.NONE -> TextSecondary
                }
                val messageIcon = when (messageState.type) {
                    MessageType.SUCCESS -> Icons.Default.CheckCircle
                    MessageType.ERROR -> Icons.Default.Error
                    MessageType.NONE -> Icons.Default.CheckCircle
                }
                val messageDescription = when (messageState.type) {
                    MessageType.SUCCESS -> "Uspešan proračun"
                    MessageType.ERROR -> "Greška u proračunu"
                    MessageType.NONE -> "Poruka"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = messageIcon,
                        contentDescription = messageDescription,
                        tint = messageColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = messageState.text.removePrefix("✓ ").removePrefix("✗ "),
                        color = messageColor,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Results
            if (results.isNotEmpty()) {
                Box(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        resultsPosition = coordinates.positionInParent().y.toInt()
                    }
                ) {
                    ResultsSection(results = results, layout = layout)
                }
            }
        }

        AnimatedVisibility(
            visible = isKeyboardVisible,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(120)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = layout.screenPaddingHorizontal, vertical = 8.dp)
        ) {
            PrimaryButton(
                text = "IZRAČUNAJ",
                onClick = doCalculate,
                modifier = Modifier.fillMaxWidth(),
                minHeight = layout.buttonMinHeight,
                contentPadding = layout.buttonContentPadding,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun ResultsSection(
    results: List<CalculationResult>,
    layout: com.metraza.ui.theme.LayoutTokens
) {
    GlassCard(cornerRadius = layout.cardRadius) {
        Column(
            modifier = Modifier.padding(
                horizontal = layout.cardPaddingHorizontal,
                vertical = layout.cardPaddingVertical
            ),
            verticalArrangement = Arrangement.spacedBy(layout.sectionSpacing)
        ) {
            Text(
                text = "REZULTATI",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            results.forEachIndexed { index, result ->
                ResultCard(result = result)
                if (index < results.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultCard(result: CalculationResult) {
    val rollWord = getRollWord(result.rolls)
    val rows = listOf(
        ResultVariantRow("Tačna", result.base, TextPrimary),
        ResultVariantRow("+10%", result.plusVariant, SuccessColor),
        ResultVariantRow("-10%", result.minusVariant, ErrorColor)
    )

    AccentGlassCard(
        bgColor = PrimaryDark.copy(alpha = 0.28f),
        cornerRadius = 12.dp,
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Za ${result.rolls} $rollWord",
                style = MaterialTheme.typography.bodyLarge,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )

            ResultHeader(hasDiameter = result.base.outerDiameterMm != null)

            rows.forEach { row ->
                ResultMetricRow(row = row)
            }
        }
    }
}

private data class ResultVariantRow(
    val label: String,
    val variant: com.metraza.data.RollResult,
    val labelColor: androidx.compose.ui.graphics.Color
)

private const val RESULT_TYPE_WEIGHT = 0.75f
private const val RESULT_LENGTH_WEIGHT = 1.0f
private const val RESULT_WEIGHT_WEIGHT = 1.1f
private const val RESULT_DIAMETER_WEIGHT = 1.75f

@Composable
private fun ResultHeader(hasDiameter: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ResultHeaderCell("TIP", Modifier.weight(RESULT_TYPE_WEIGHT), TextAlign.Start)
        ResultHeaderCell("METARA", Modifier.weight(RESULT_LENGTH_WEIGHT), TextAlign.End)
        ResultHeaderCell("TEŽINA", Modifier.weight(RESULT_WEIGHT_WEIGHT), TextAlign.End)
        if (hasDiameter) {
            ResultHeaderCell("PREČNIK", Modifier.weight(RESULT_DIAMETER_WEIGHT), TextAlign.End)
        }
    }
}

@Composable
private fun ResultHeaderCell(text: String, modifier: Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
        textAlign = textAlign
    )
}

@Composable
private fun ResultMetricRow(row: ResultVariantRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyMedium,
            color = row.labelColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(RESULT_TYPE_WEIGHT)
        )
        ResultValueCell("${formatNumber(row.variant.lengthM)} m", Modifier.weight(RESULT_LENGTH_WEIGHT), TextPrimary)
        ResultValueCell("${formatNumber(row.variant.weightKg)} kg", Modifier.weight(RESULT_WEIGHT_WEIGHT), Secondary)
        if (row.variant.outerDiameterMm != null) {
            ResultDiameterCell(row = row, modifier = Modifier.weight(RESULT_DIAMETER_WEIGHT))
        }
    }
}

@Composable
private fun ResultDiameterCell(row: ResultVariantRow, modifier: Modifier) {
    val outerDiameter = row.variant.outerDiameterMm?.roundToInt() ?: return
    val radius = row.variant.tapeRadiusMm
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = TextPrimary)) {
            append("Ø$outerDiameter")
        }
        if (radius != null) {
            withStyle(SpanStyle(color = TextSecondary)) {
                append("/")
            }
            withStyle(SpanStyle(color = Primary)) {
                append("R${formatNumber(radius)}")
            }
        }
        withStyle(SpanStyle(color = TextSecondary)) {
            append(" mm")
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
        textAlign = TextAlign.End
    )
}

@Composable
private fun ResultValueCell(
    text: String,
    modifier: Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
        textAlign = TextAlign.End
    )
}
