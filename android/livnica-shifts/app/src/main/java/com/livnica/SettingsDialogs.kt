package com.livnica

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.livnica.ui.components.AutoSizeText
import java.time.LocalDate
import java.util.Locale

@Composable
fun AutoFillDialog(
    today: LocalDate,
    scheduler: ShiftScheduler,
    repo: ShiftRepository,
    onDismiss: () -> Unit
) {
    var startShift by remember { mutableStateOf(Shift.FIRST) }
    var isSecondShiftDay by remember { mutableStateOf(false) }
    var monthsInput by remember { mutableStateOf("12") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    PopupDialog(
        title = "Auto-popuna unapred",
        onDismiss = onDismiss,
        widthRatio = UI.popupWidthRatio,
        heightRatio = UI.autoFillPopupHeightRatio,
        scrollable = true,
        accent = THEME.accentSecondary
    ) {
        PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.accentSecondary) {
            AutoSizeText(
                text = "Tekući mesec: ${MONTH_NAMES[today.monthValue]} ${today.year}",
                color = THEME.textPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxFontSize = MaterialTheme.typography.titleMedium.fontSize,
                minFontSize = MaterialTheme.typography.titleSmall.fontSize,
                maxLines = 1
            )
            Text(
                text = "Popunjava tekuće + zadati broj meseci unapred.",
                color = THEME.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        PopupCard {
            SectionLabel(text = "Današnja smena")
            val shiftOptions = listOf(
                ShiftChoice("Prva", Shift.FIRST),
                ShiftChoice("Druga", Shift.SECOND),
                ShiftChoice("Treća", Shift.THIRD),
                ShiftChoice("Slobodan", Shift.OFF)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (option in shiftOptions) {
                    val isSelected = option.shift == startShift
                    RoundedButton(
                        text = option.label,
                        modifier = Modifier.weight(1f),
                        bgColor = if (isSelected) option.shift.color else option.shift.color.copy(alpha = 0.2f),
                        textColor = option.shift.textColor,
                        radius = 14.dp,
                        textStyle = MaterialTheme.typography.labelLarge,
                        bold = true,
                        onClick = { startShift = option.shift }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            SectionLabel(text = "Dan smene")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val firstDaySelected = !isSecondShiftDay
                RoundedButton(
                    text = "Prvi dan",
                    modifier = Modifier.weight(1f),
                    bgColor = if (firstDaySelected) THEME.info else THEME.bgCardElevated,
                    textColor = THEME.textPrimary,
                    radius = 14.dp,
                    textStyle = MaterialTheme.typography.labelLarge,
                    bold = true,
                    onClick = { isSecondShiftDay = false }
                )
                val secondDaySelected = isSecondShiftDay
                RoundedButton(
                    text = "Drugi dan",
                    modifier = Modifier.weight(1f),
                    bgColor = if (secondDaySelected) THEME.info else THEME.bgCardElevated,
                    textColor = THEME.textPrimary,
                    radius = 14.dp,
                    textStyle = MaterialTheme.typography.labelLarge,
                    bold = true,
                    onClick = { isSecondShiftDay = true }
                )
            }
        }

        PopupCard {
            SectionLabel(text = "Meseci unapred")
            ModernTextField(
                value = monthsInput,
                onValueChange = {
                    monthsInput = it.filter(Char::isDigit)
                    errorMessage = null
                },
                hintText = "Unesi broj meseci...",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = THEME.danger,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        GlowButton(
            text = "Popuni",
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            bgColor = THEME.accentSecondary,
            textColor = THEME.textPrimary,
            textStyle = MaterialTheme.typography.labelLarge,
            onClick = {
                val monthsAhead = monthsInput.toIntOrNull()
                when {
                    monthsAhead == null -> {
                        errorMessage = "Unesi broj meseci."
                        return@GlowButton
                    }
                    monthsAhead < 1 -> {
                        errorMessage = "Broj meseci mora biti najmanje 1."
                        return@GlowButton
                    }
                    monthsAhead > 24 -> {
                        errorMessage = "Maksimalno 24 meseca unapred."
                        return@GlowButton
                    }
                }
                if (repo.brigadeTypeValue == BrigadeType.TROBRIGADA && startShift == Shift.OFF) {
                    errorMessage = "Za trobrigadu izaberi radnu smenu."
                    return@GlowButton
                }
                scheduler.populateMonthsAhead(
                    repo,
                    today,
                    startShift,
                    monthsAhead,
                    repo.brigadeTypeValue,
                    resolveFourBrigadeStartIndex(startShift, isSecondShiftDay)
                )
                onDismiss()
            }
        )
    }
}

@Composable
fun SickVacationDialog(
    year: Int,
    month: Int,
    scheduler: ShiftScheduler,
    repo: ShiftRepository,
    onDismiss: () -> Unit
) {
    var absenceType by remember { mutableStateOf(AbsenceType.SICK) }
    var sickPayRate by remember { mutableStateOf(CONFIG.pay.sickPayRate) }
    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Get max days in this month for validation
    val maxDays = remember(year, month) {
        java.time.YearMonth.of(year, month).lengthOfMonth()
    }

    PopupDialog(
        title = "Bolovanje / Godi\u0161nji",
        onDismiss = onDismiss,
        widthRatio = 0.85f,
        heightRatio = UI.sickPopupHeightRatio,
        scrollable = true,
        accent = THEME.info
    ) {
        PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.info) {
            AutoSizeText(
                text = "Unos odsustva",
                color = THEME.textPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxFontSize = MaterialTheme.typography.titleMedium.fontSize,
                minFontSize = MaterialTheme.typography.titleSmall.fontSize,
                maxLines = 1
            )
            Text(
                text = "${MONTH_NAMES[month]} $year",
                color = THEME.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        PopupCard {
            SectionLabel(text = "Tip odsustva")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isSick = absenceType == AbsenceType.SICK
                RoundedButton(
                    text = "BOLOVANJE",
                    modifier = Modifier.weight(1f),
                    bgColor = if (isSick) THEME.sick else THEME.bgCardElevated,
                    textColor = THEME.textPrimary,
                    radius = 14.dp,
                    textStyle = MaterialTheme.typography.labelLarge,
                    bold = true,
                    onClick = { absenceType = AbsenceType.SICK }
                )
                RoundedButton(
                    text = "GODI\u0160NJI",
                    modifier = Modifier.weight(1f),
                    bgColor = if (!isSick) THEME.vacation else THEME.bgCardElevated,
                    textColor = THEME.textPrimary,
                    radius = 14.dp,
                    textStyle = MaterialTheme.typography.labelLarge,
                    bold = true,
                    onClick = { absenceType = AbsenceType.VACATION }
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val isSick = absenceType == AbsenceType.SICK
                Text(
                    text = "Isplata bolovanja",
                    color = if (isSick) THEME.textSecondary else THEME.textMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (rate in listOf(CONFIG.pay.sickPayRate to "${(CONFIG.pay.sickPayRate * 100).toInt()}%", 1.0 to "100%")) {
                        val selected = kotlin.math.abs(sickPayRate - rate.first) < 0.01
                        val bgColor = if (isSick) {
                            if (selected) THEME.sick else THEME.bgCardElevated
                        } else {
                            THEME.bgCard
                        }
                        val textColor = if (isSick) THEME.textPrimary else THEME.textMuted
                        RoundedButton(
                            text = rate.second,
                            modifier = Modifier.weight(1f),
                            bgColor = bgColor,
                            textColor = textColor,
                            radius = 14.dp,
                            textStyle = MaterialTheme.typography.labelLarge,
                            bold = true,
                            enabled = isSick,
                            onClick = { sickPayRate = rate.first }
                        )
                    }
                }
            }
        }

        PopupCard {
            SectionLabel(text = "Opseg dana")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Od",
                        color = THEME.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ModernTextField(
                        value = startText,
                        onValueChange = {
                            startText = it.filter(Char::isDigit)
                            errorMessage = null
                        },
                        hintText = "Početak",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Do",
                        color = THEME.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ModernTextField(
                        value = endText,
                        onValueChange = {
                            endText = it.filter(Char::isDigit)
                            errorMessage = null
                        },
                        hintText = "Kraj",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = THEME.danger,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        GlowButton(
            text = "Upi\u0161i",
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            bgColor = THEME.info,
            textColor = THEME.textPrimary,
            textStyle = MaterialTheme.typography.labelLarge,
            onClick = {
                val start = startText.toIntOrNull()
                val end = endText.toIntOrNull()

                // Validate inputs
                when {
                    start == null -> {
                        errorMessage = "Unesite početni dan."
                        return@GlowButton
                    }
                    end == null -> {
                        errorMessage = "Unesite krajnji dan."
                        return@GlowButton
                    }
                    start < 1 || start > maxDays -> {
                        errorMessage = "Početni dan mora biti između 1 i $maxDays."
                        return@GlowButton
                    }
                    end < 1 || end > maxDays -> {
                        errorMessage = "Krajnji dan mora biti između 1 i $maxDays."
                        return@GlowButton
                    }
                    start > end -> {
                        errorMessage = "Početni dan mora biti manji ili jednak krajnjem."
                        return@GlowButton
                    }
                }

                if (absenceType == AbsenceType.SICK) {
                    scheduler.applySickRange(repo, year, month, start, end, sickPayRate)
                } else {
                    scheduler.applyVacationRange(repo, year, month, start, end)
                }
                onDismiss()
            }
        )
    }
}

@Composable
fun BasePayDialog(
    year: Int,
    month: Int,
    repo: ShiftRepository,
    onDismiss: () -> Unit
) {
    val currPay = repo.basePayValue
    var payInput by remember { mutableStateOf(formatRateInput(currPay)) }
    var netInput by remember { mutableStateOf("") }
    var calcMessage by remember { mutableStateOf<String?>(null) }
    var computedBase by remember { mutableStateOf<Double?>(null) }

    PopupDialog(
        title = "Izmena satnice",
        onDismiss = onDismiss,
        widthRatio = 0.85f,
        heightRatio = UI.autoPopupHeightRatio,
        scrollable = true,
        accent = THEME.success
    ) {
        PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.success) {
            SectionLabel(text = "Trenutna satnica")
            AutoSizeText(
                text = "${formatRateInput(currPay)} ${CONFIG.pay.currency}",
                color = THEME.success,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxFontSize = MaterialTheme.typography.titleMedium.fontSize,
                minFontSize = MaterialTheme.typography.titleSmall.fontSize,
                maxLines = 1
            )
            Text(
                text = "${MONTH_NAMES[month]} $year",
                color = THEME.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        PopupCard {
            SectionLabel(text = "Neto plata za mesec")
            ModernTextField(
                value = netInput,
                onValueChange = {
                    netInput = filterDecimalInput(it)
                    calcMessage = null
                    computedBase = null
                },
                hintText = "Neto plata...",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            RoundedButton(
                text = "Izracunaj satnicu",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                bgColor = THEME.accentSecondary,
                textColor = THEME.textPrimary,
                radius = 14.dp,
                textStyle = MaterialTheme.typography.labelLarge,
                bold = true,
                onClick = {
                    val netValue = netInput.toDoubleOrNull()
                    when {
                        netValue == null -> {
                            calcMessage = "Unesi neto platu."
                            computedBase = null
                        }
                        netValue < 0 -> {
                            calcMessage = "Neto plata mora biti >= 0."
                            computedBase = null
                        }
                        else -> {
                            val computed = repo.calculateBasePayFromNet(year, month, netValue)
                            if (computed == null) {
                                calcMessage = "Nema dovoljno podataka za ovaj mesec."
                                computedBase = null
                            } else {
                                calcMessage = null
                                computedBase = computed
                                payInput = formatRateInput(computed)
                            }
                        }
                    }
                }
            )
            computedBase?.let { value ->
                Text(
                    text = "Izracunata satnica: ${formatRateInput(value)} ${CONFIG.pay.currency}",
                    color = THEME.success,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            calcMessage?.let { message ->
                Text(
                    text = message,
                    color = THEME.danger,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        PopupCard {
            SectionLabel(text = "Nova satnica")
            ModernTextField(
                value = payInput,
                onValueChange = {
                    payInput = filterDecimalInput(it)
                    calcMessage = null
                },
                hintText = "Nova satnica...",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }

        GlowButton(
            text = "Sa\u010duvaj",
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            bgColor = THEME.success,
            textColor = THEME.textDark,
            textStyle = MaterialTheme.typography.labelLarge,
            onClick = {
                val hasNetInput = netInput.isNotBlank()
                if (hasNetInput) {
                    val netValue = netInput.toDoubleOrNull()
                    when {
                        netValue == null -> {
                            calcMessage = "Unesi neto platu."
                            return@GlowButton
                        }
                        netValue < 0 -> {
                            calcMessage = "Neto plata mora biti >= 0."
                            return@GlowButton
                        }
                        else -> {
                            val computed = repo.calculateBasePayFromNet(year, month, netValue)
                            if (computed == null) {
                                calcMessage = "Nema dovoljno podataka za ovaj mesec."
                                return@GlowButton
                            }
                            repo.basePayValue = computed
                            onDismiss()
                            return@GlowButton
                        }
                    }
                }
                val newVal = payInput.toDoubleOrNull()
                if (newVal != null && newVal >= 0) {
                    repo.basePayValue = newVal
                    onDismiss()
                } else {
                    calcMessage = "Unesi satnicu."
                }
            }
        )
    }
}

@Composable
fun FoodAllowanceDialog(
    repo: ShiftRepository,
    onDismiss: () -> Unit
) {
    var foodInput by remember { mutableStateOf(formatRateInput(repo.foodPerDayValue)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    PopupDialog(
        title = "Hrana",
        onDismiss = onDismiss,
        widthRatio = 0.85f,
        heightRatio = UI.autoPopupHeightRatio,
        scrollable = true,
        accent = THEME.accentSecondary
    ) {
        PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.accentSecondary) {
            SectionLabel(text = "Globalni dnevni iznos")
            Text(
                text = "Trenutno po danu: ${formatRateInput(repo.foodPerDayValue)} ${CONFIG.pay.currency}",
                color = THEME.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ModernTextField(
                value = foodInput,
                onValueChange = {
                    foodInput = filterDecimalInput(it)
                    errorMessage = null
                },
                hintText = "Iznos hrane...",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = THEME.danger,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        GlowButton(
            text = "Sa\u010duvaj",
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            bgColor = THEME.accentSecondary,
            textColor = THEME.textPrimary,
            textStyle = MaterialTheme.typography.labelLarge,
            onClick = {
                val newValue = foodInput.toDoubleOrNull()
                if (newValue == null || newValue < 0) {
                    errorMessage = "Unesi ispravan iznos."
                    return@GlowButton
                }
                repo.foodPerDayValue = newValue
                onDismiss()
            }
        )
    }
}

@Composable
fun MinuliRadDialog(
    repo: ShiftRepository,
    onDismiss: () -> Unit
) {
    val currYears = repo.yearsOfServiceValue
    val currPercent = currYears * 0.5
    var yearsInput by remember { mutableStateOf(currYears.toString()) }
    val previewPercent by remember {
        derivedStateOf { (yearsInput.toIntOrNull() ?: 0) * 0.5 }
    }
    val previewColor = if ((yearsInput.toIntOrNull() ?: 0) > 0) {
        THEME.compTime
    } else {
        THEME.textMuted
    }

    PopupDialog(
        title = "Minuli rad",
        onDismiss = onDismiss,
        widthRatio = 0.85f,
        heightRatio = 0.5f,
        scrollable = true,
        accent = THEME.compTime
    ) {
        PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.compTime) {
            SectionLabel(text = "Trenutno")
            AutoSizeText(
                text = "$currYears god. = ${String.format(Locale.US, "%.1f", currPercent)}%",
                color = THEME.compTime,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxFontSize = MaterialTheme.typography.titleMedium.fontSize,
                minFontSize = MaterialTheme.typography.titleSmall.fontSize,
                maxLines = 1
            )
            Text(
                text = "Za svaku godinu staza: +0.5%",
                color = THEME.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        PopupCard {
            SectionLabel(text = "Godine staza")
            ModernTextField(
                value = yearsInput,
                onValueChange = { yearsInput = it.filter(Char::isDigit) },
                hintText = "Godine radnog staza...",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(previewColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Bonus: ${String.format(Locale.US, "%.1f", previewPercent)}%",
                    color = previewColor,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        GlowButton(
            text = "Sa\u010duvaj",
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            bgColor = THEME.compTime,
            textColor = THEME.textDark,
            textStyle = MaterialTheme.typography.labelLarge,
            onClick = {
                val newVal = yearsInput.toIntOrNull() ?: 0
                if (newVal >= 0) {
                    repo.yearsOfServiceValue = newVal
                }
                onDismiss()
            }
        )
    }
}

@Composable
fun VacationBalanceDialog(
    repo: ShiftRepository,
    onDismiss: () -> Unit
) {
    val currentBalance = repo.initialVacationBalanceValue
    val used = repo.getVacationDaysUsed()
    val remaining = repo.getVacationDaysRemaining()
    val exceeded = repo.isVacationExceeded()

    var balanceInput by remember { mutableStateOf(formatVacationInput(currentBalance)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    PopupDialog(
        title = "Godi\u0161nji odmor",
        onDismiss = onDismiss,
        widthRatio = 0.85f,
        heightRatio = 0.55f,
        scrollable = true,
        accent = THEME.vacation
    ) {
        PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.vacation) {
            SectionLabel(text = "Stanje")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Fond:",
                    color = THEME.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatVacationInput(currentBalance)} dana",
                    color = THEME.vacation,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Iskori\u0161\u0107eno:",
                    color = THEME.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$used dana",
                    color = if (exceeded) THEME.danger else THEME.textPrimary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Preostalo:",
                    color = THEME.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatVacationInput(remaining)} dana",
                    color = if (exceeded) THEME.danger else THEME.success,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (exceeded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(THEME.danger.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Prekora\u010den fond godi\u0161njeg!",
                        color = THEME.danger,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        PopupCard {
            SectionLabel(text = "Novi fond")
            Text(
                text = "Unesite ukupan broj dana godi\u0161njeg odmora",
                color = THEME.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            ModernTextField(
                value = balanceInput,
                onValueChange = {
                    balanceInput = filterDecimalInput(it)
                    errorMessage = null
                },
                hintText = "Broj dana...",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = THEME.danger,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        GlowButton(
            text = "Sa\u010duvaj",
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            bgColor = THEME.vacation,
            textColor = THEME.textDark,
            textStyle = MaterialTheme.typography.labelLarge,
            onClick = {
                val newValue = balanceInput.toDoubleOrNull()
                if (newValue == null || newValue < 0) {
                    errorMessage = "Unesite ispravan broj dana."
                    return@GlowButton
                }
                repo.initialVacationBalanceValue = newValue
                onDismiss()
            }
        )
    }
}

private fun formatVacationInput(value: Double): String {
    val formatted = String.format(Locale.US, "%.1f", value)
    return formatted.trimEnd('0').trimEnd('.')
}

private data class ShiftChoice(
    val label: String,
    val shift: Shift,
    val fourIndex: Int? = null
)

private fun resolveFourBrigadeStartIndex(shift: Shift, isSecondDay: Boolean): Int {
    val baseIndex = when (shift) {
        Shift.FIRST -> 0
        Shift.SECOND -> 2
        Shift.THIRD -> 4
        Shift.OFF -> 6
        else -> 0
    }
    return baseIndex + if (isSecondDay) 1 else 0
}

@Composable
fun DeleteMonthDialog(
    year: Int,
    month: Int,
    repo: ShiftRepository,
    onDismiss: () -> Unit
) {
    var deleteAll by remember { mutableStateOf(false) }

    PopupDialog(
        title = "Potvrda brisanja",
        onDismiss = onDismiss,
        widthRatio = 0.85f,
        heightRatio = 0.45f,
        scrollable = true,
        accent = THEME.danger
    ) {
        PopupCard(
            bgColor = THEME.bgCardElevated,
            borderColor = THEME.danger,
            spacing = UI.largeSpacing.dp
        ) {
            AutoSizeText(
                text = "!",
                color = THEME.danger,
                style = MaterialTheme.typography.displaySmall.copy(textAlign = TextAlign.Center),
                maxFontSize = MaterialTheme.typography.displaySmall.fontSize,
                minFontSize = MaterialTheme.typography.headlineMedium.fontSize,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (deleteAll) {
                    "Da li ste sigurni da želite da obrišete SVE podatke?"
                } else {
                    "Da li ste sigurni da želite da obrišete sve podatke za ${MONTH_NAMES[month]}?"
                },
                color = THEME.textPrimary,
                style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        PopupCard {
            SectionLabel(text = "Šta obrisati?")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoundedButton(
                    text = "Ovaj mesec",
                    modifier = Modifier.weight(1f),
                    bgColor = if (!deleteAll) THEME.danger else THEME.bgCardElevated,
                    textColor = THEME.textPrimary,
                    radius = 14.dp,
                    textStyle = MaterialTheme.typography.labelLarge,
                    bold = true,
                    onClick = { deleteAll = false }
                )
                RoundedButton(
                    text = "Sve",
                    modifier = Modifier.weight(1f),
                    bgColor = if (deleteAll) THEME.danger else THEME.bgCardElevated,
                    textColor = THEME.textPrimary,
                    radius = 14.dp,
                    textStyle = MaterialTheme.typography.labelLarge,
                    bold = true,
                    onClick = { deleteAll = true }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(UI.largeSpacing.dp)
        ) {
            RoundedButton(
                text = "Otkaži",
                modifier = Modifier.weight(1f),
                bgColor = THEME.bgCardElevated,
                textColor = THEME.textPrimary,
                borderColor = THEME.glassBorder,
                radius = 14.dp,
                textStyle = MaterialTheme.typography.labelLarge,
                bold = true,
                onClick = onDismiss
            )
            GlowButton(
                text = "Obriši",
                modifier = Modifier.weight(1f),
                bgColor = THEME.danger,
                textColor = THEME.textPrimary,
                textStyle = MaterialTheme.typography.labelLarge,
                onClick = {
                    if (deleteAll) {
                        repo.deleteAllData()
                    } else {
                        repo.deleteMonth(year, month)
                    }
                    onDismiss()
                }
            )
        }
    }
}

private enum class AbsenceType {
    SICK,
    VACATION
}

private fun filterDecimalInput(value: String): String {
    val sb = StringBuilder()
    var dotSeen = false
    for (ch in value) {
        when {
            ch.isDigit() -> sb.append(ch)
            ch == '.' && !dotSeen -> {
                sb.append(ch)
                dotSeen = true
            }
        }
    }
    return sb.toString()
}

private fun formatRateInput(value: Double): String {
    val formatted = String.format(Locale.US, "%.2f", value)
    return formatted.trimEnd('0').trimEnd('.')
}

