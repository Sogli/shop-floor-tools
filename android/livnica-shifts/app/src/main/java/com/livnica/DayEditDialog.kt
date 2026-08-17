package com.livnica

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.livnica.ui.components.AutoSizeText
import com.livnica.ui.components.ResponsiveInfoRow
import java.time.LocalDate

@Composable
fun DayEditDialog(
    year: Int,
    month: Int,
    day: Int,
    record: DayRecord?,
    repo: ShiftRepository,
    onDismiss: () -> Unit
) {
    val dayDate = remember(year, month, day) { LocalDate.of(year, month, day) }
    val autoHoliday = remember(year, month, day) { isSerbianHoliday(year, month, day) }
    val initialSickRate = when {
        record?.shift == Shift.SICK && record.sickPayRate != null -> record.sickPayRate
        else -> CONFIG.pay.sickPayRate
    }

    var shift1 by remember { mutableStateOf(record?.shift ?: Shift.OFF) }
    var hours1 by remember { mutableIntStateOf(record?.hours ?: CONFIG.pay.defaultHours) }
    var shift2 by remember { mutableStateOf(record?.shift2 ?: Shift.THIRD) }
    var hours2 by remember { mutableIntStateOf(record?.hours2 ?: CONFIG.pay.defaultHours) }
    var hasDouble by remember { mutableStateOf(record?.isDoubleShift ?: false) }
    var overtimeType by remember { mutableStateOf(record?.overtimeType ?: OvertimeType.PAID) }
    var weekendOvertimeType by remember { mutableStateOf(record?.weekendOvertimeType ?: OvertimeType.PAID) }
    var compUsed by remember { mutableIntStateOf(record?.compHoursUsed ?: 0) }
    var sickPayRate by remember { mutableStateOf(initialSickRate) }
    var holidayOverride by remember { mutableStateOf(record?.holidayOverride) }
    var holidayWorkOverride by remember { mutableStateOf(record?.holidayWorkOverride) }

    val isWeekend = remember(dayDate) { dayDate.dayOfWeek.value >= 6 }

    val isHolidayEffective = holidayOverride ?: autoHoliday

    val summaryText by remember {
        derivedStateOf {
            val base = if (shift1 in Shift.workShifts()) {
                "${shift1.displayName} ${hours1}h"
            } else {
                shift1.displayName
            }
            val doublePart = if (hasDouble) {
                " + ${shift2.displayName} ${hours2}h"
            } else {
                ""
            }
            val holidayPart = if (isHolidayEffective) " | Praznik" else ""
            base + doublePart + holidayPart
        }
    }

    val holidayText by remember {
        derivedStateOf {
            if (!isHolidayEffective) {
                null
            } else {
                val baseName = getHolidayName(year, month, day)
                val name = when {
                    holidayOverride == false -> null
                    holidayOverride == true && baseName == null -> MANUAL_HOLIDAY_LABEL
                    else -> baseName
                } ?: return@derivedStateOf null
                val isWorkday = dayDate.dayOfWeek.value <= 5
                when {
                    shift1 in Shift.workShifts() -> "Praznik: $name (rad +${(CONFIG.pay.holidayBonus * 100).toInt()}%)"
                    isWorkday -> "Praznik: $name (neradni, 100%)"
                    else -> "Praznik: $name (vikend)"
                }
            }
        }
    }

    var hoursPicker by remember { mutableStateOf<HoursPickerState?>(null) }
    var showCompDialog by remember { mutableStateOf(false) }

    PopupDialog(
        title = "$day. ${MONTH_NAMES[month]}",
        onDismiss = onDismiss,
        height = UI.dayPopupHeight.dp,
        scrollable = true
    ) {
        PopupCard(bgColor = THEME.bgCardElevated) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AutoSizeText(
                        text = "%02d".format(day),
                        color = THEME.accentPrimary,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        maxFontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        minFontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        maxLines = 1
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AutoSizeText(
                        text = "${MONTH_NAMES[month]} $year",
                        color = THEME.textPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxFontSize = MaterialTheme.typography.titleMedium.fontSize,
                        minFontSize = MaterialTheme.typography.titleSmall.fontSize,
                        maxLines = 1
                    )
                    Text(
                        text = "${WEEKDAY_NAMES[dayDate.dayOfWeek.ordinal]} | ${dayDate.dayOfMonth.toString().padStart(2, '0')}.${dayDate.monthValue.toString().padStart(2, '0')}.$year",
                        color = THEME.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summaryText,
                        color = THEME.textMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .height(32.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }

        PopupCard {
            SectionLabel(text = "Praznik")
            holidayText?.let { text ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(THEME.warning.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = text,
                        color = THEME.warning,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isHolidayEffective,
                    onCheckedChange = { checked ->
                        val previousOverride = holidayOverride
                        holidayOverride = checked
                        if (!checked) {
                            holidayWorkOverride = false
                        } else {
                            if (previousOverride == false || holidayWorkOverride == false) {
                                holidayWorkOverride = null
                            }
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = THEME.accentPrimary,
                        uncheckedColor = THEME.textMuted,
                        checkmarkColor = THEME.textDark
                    )
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Tretiraj kao praznik",
                        color = THEME.textPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Isključi ako firma ne računa ovaj datum, uključi za ručni praznik",
                        color = THEME.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (record != null) {
            val dailyPay = repo.calculateDailyPay(record)
            if (dailyPay > 0) {
                PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.success) {
                    Text(
                        text = "Zarada",
                        color = THEME.textSecondary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ResponsiveInfoRow(
                        label = "Ukupno:",
                        value = "${formatNumber(dailyPay)} ${CONFIG.pay.currency}",
                        labelStyle = MaterialTheme.typography.bodyMedium,
                        valueStyle = MaterialTheme.typography.bodyLarge,
                        labelColor = THEME.textSecondary,
                        valueColor = THEME.success,
                        valueFontWeight = FontWeight.Bold
                    )
                    if (record.workedOnHoliday) {
                        Box(
                            modifier = Modifier
                                .background(THEME.warning.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Rad na praznik",
                                color = THEME.warning,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (record.shift in Shift.workShifts()) {
                        val compText = when {
                            record.hoursDeficit > 0 -> "Minus sati: -${record.hoursDeficit}h"
                            record.compTimeEarned > 0 -> "Zarađeno sati: +${record.compTimeEarned}h"
                            else -> "Pun dan (${CONFIG.pay.defaultHours}h)"
                        }
                        val compColor = when {
                            record.hoursDeficit > 0 -> THEME.danger
                            record.compTimeEarned > 0 -> THEME.compTime
                            else -> THEME.textMuted
                        }
                        Box(
                            modifier = Modifier
                                .background(compColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = compText,
                                color = compColor,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        PopupCard {
            SectionLabel(text = "Smena")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UI.buttonHeight.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val shifts = listOf(Shift.FIRST, Shift.SECOND, Shift.THIRD, Shift.SICK, Shift.VACATION, Shift.OFF)
                for (shift in shifts) {
                    val isSelected = shift == shift1
                    RoundedButton(
                        text = shift.displayName,
                        modifier = Modifier.weight(1f),
                        bgColor = if (isSelected) shift.color else shift.color.copy(alpha = 0.2f),
                        textColor = shift.textColor,
                        radius = 14.dp,
                        textStyle = MaterialTheme.typography.labelLarge,
                        bold = true,
                        onClick = {
                            shift1 = shift
                            if (shift == Shift.VACATION || shift == Shift.SICK) {
                                hours1 = CONFIG.pay.defaultHours
                            }
                        }
                    )
                }
            }
            if (shift1 != Shift.VACATION && shift1 != Shift.SICK) {
                RoundedButton(
                    text = "Sati: $hours1",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UI.buttonHeight.dp),
                    bgColor = THEME.bgCardElevated,
                    textColor = THEME.textPrimary,
                    radius = 14.dp,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    bold = true,
                    onClick = {
                        hoursPicker = HoursPickerState(
                            title = "Izaberi sate",
                            current = hours1,
                            accent = THEME.accentPrimary,
                            target = HoursTarget.SHIFT1,
                            minHours = 1
                        )
                    }
                )
            }
            if (dayDate.dayOfWeek.value <= 5 && shift1 in Shift.workShifts()) {
                RoundedButton(
                    text = "Postavi 0h (nisam radio)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    bgColor = THEME.bgCardElevated,
                    textColor = THEME.textPrimary,
                    radius = 14.dp,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    bold = true,
                    onClick = {
                        hours1 = 0
                        hasDouble = false
                    }
                )
            }
            if (shift1 == Shift.SICK) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Isplata bolovanja",
                        color = THEME.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val rates = listOf(CONFIG.pay.sickPayRate to "${(CONFIG.pay.sickPayRate * 100).toInt()}%", 1.0 to "100%")
                        for (rate in rates) {
                            val isSelected = kotlin.math.abs(sickPayRate - rate.first) < 0.01
                            RoundedButton(
                                text = rate.second,
                                modifier = Modifier.weight(1f),
                                bgColor = if (isSelected) THEME.sick else THEME.bgCardElevated,
                                textColor = THEME.textPrimary,
                                radius = 14.dp,
                                textStyle = MaterialTheme.typography.labelLarge,
                                bold = true,
                                onClick = { sickPayRate = rate.first }
                            )
                        }
                    }
                }
            }
            if (isWeekend && shift1 in Shift.workShifts()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Tip vikend rada",
                        color = THEME.textSecondary,
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
                        val paidSelected = weekendOvertimeType == OvertimeType.PAID
                        RoundedButton(
                            text = "PLA\u0106ENO",
                            modifier = Modifier.weight(1f),
                            bgColor = if (paidSelected) THEME.success else THEME.bgCard,
                            textColor = if (paidSelected) THEME.textDark else THEME.textPrimary,
                            radius = 14.dp,
                            textStyle = MaterialTheme.typography.labelLarge,
                            bold = true,
                            onClick = { weekendOvertimeType = OvertimeType.PAID }
                        )
                        val compSelected = weekendOvertimeType == OvertimeType.COMP_TIME
                        RoundedButton(
                            text = "ZA SATE",
                            modifier = Modifier.weight(1f),
                            bgColor = if (compSelected) THEME.compTime else THEME.bgCard,
                            textColor = if (compSelected) THEME.textDark else THEME.textPrimary,
                            radius = 14.dp,
                            textStyle = MaterialTheme.typography.labelLarge,
                            bold = true,
                            onClick = { weekendOvertimeType = OvertimeType.COMP_TIME }
                        )
                    }
                }
            }
        }

        val summary = repo.getSummary(year, month)
        val availableComp = summary.compTimeStarting
        if (compUsed > 0 || availableComp > 0) {
            PopupCard {
                SectionLabel(text = "Kompenzacija")
                Text(
                    text = "Bilans: ${availableComp}h",
                    color = THEME.textMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                RoundedButton(
                    text = "Iskoristi sate: ${compUsed}h",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    bgColor = THEME.compTimeUsed.copy(alpha = 0.4f),
                    textColor = THEME.textPrimary,
                    borderColor = THEME.compTimeUsed,
                    radius = 14.dp,
                    textStyle = MaterialTheme.typography.labelLarge,
                    bold = true,
                    onClick = { showCompDialog = true }
                )
            }
        }

        PopupCard {
            if (hasDouble) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Druga smena",
                        color = THEME.doubleShift,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    RoundedButton(
                        text = "UKLONI",
                        modifier = Modifier
                            .height(24.dp)
                            .weight(0.4f),
                        bgColor = THEME.danger,
                        textColor = THEME.textPrimary,
                        textStyle = MaterialTheme.typography.labelMedium,
                        bold = true,
                        onClick = { hasDouble = false }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UI.buttonHeight.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (shift in listOf(Shift.FIRST, Shift.SECOND, Shift.THIRD)) {
                        val isSelected = shift == shift2
                        RoundedButton(
                            text = shift.displayName,
                            modifier = Modifier.weight(1f),
                            bgColor = if (isSelected) shift.color else shift.color.copy(alpha = 0.2f),
                            textColor = shift.textColor,
                            radius = 14.dp,
                            textStyle = MaterialTheme.typography.labelLarge,
                            bold = true,
                            onClick = { shift2 = shift }
                        )
                    }
                }
                RoundedButton(
                    text = "Sati: $hours2",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UI.buttonHeight.dp),
                    bgColor = THEME.bgCardElevated,
                    textColor = THEME.textPrimary,
                    radius = 14.dp,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    bold = true,
                    onClick = {
                        hoursPicker = HoursPickerState(
                            title = "Izaberi sate (druga smena)",
                            current = hours2,
                            accent = THEME.doubleShift,
                            target = HoursTarget.SHIFT2,
                            minHours = 1
                        )
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val paidSelected = overtimeType == OvertimeType.PAID
                    RoundedButton(
                        text = "PLA\u0106ENO",
                        modifier = Modifier.weight(1f),
                        bgColor = if (paidSelected) THEME.success else THEME.bgCard,
                        textColor = if (paidSelected) THEME.textDark else THEME.textPrimary,
                        radius = 14.dp,
                        textStyle = MaterialTheme.typography.labelLarge,
                        bold = true,
                        onClick = { overtimeType = OvertimeType.PAID }
                    )
                    val compSelected = overtimeType == OvertimeType.COMP_TIME
                    RoundedButton(
                        text = "ZA SATE",
                        modifier = Modifier.weight(1f),
                        bgColor = if (compSelected) THEME.compTime else THEME.bgCard,
                        textColor = if (compSelected) THEME.textDark else THEME.textPrimary,
                        radius = 14.dp,
                        textStyle = MaterialTheme.typography.labelLarge,
                        bold = true,
                        onClick = { overtimeType = OvertimeType.COMP_TIME }
                    )
                }
            } else {
                RoundedButton(
                    text = "+ DODAJ DRUGU SMENU",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    bgColor = THEME.doubleShift,
                    textColor = THEME.textPrimary,
                    radius = 14.dp,
                    textStyle = MaterialTheme.typography.labelLarge,
                    bold = true,
                    onClick = { hasDouble = true }
                )
            }
        }

        GlowButton(
            text = "SA\u010cUVAJ",
            modifier = Modifier
                .fillMaxWidth()
                .height(UI.saveButtonHeight.dp),
            bgColor = THEME.success,
            textColor = THEME.textDark,
            textStyle = MaterialTheme.typography.labelLarge,
            onClick = {
                repo.set(
                    year = year,
                    month = month,
                    day = day,
                    shift = shift1,
                    hours = hours1,
                    shift2 = if (hasDouble) shift2 else null,
                    hours2 = if (hasDouble) hours2 else 0,
                    overtimeType = if (hasDouble) overtimeType else OvertimeType.PAID,
                    weekendOvertimeType = if (isWeekend && shift1 in Shift.workShifts()) weekendOvertimeType else OvertimeType.PAID,
                    compHoursUsed = compUsed,
                    holidayWorkOverride = holidayWorkOverride,
                    holidayOverride = holidayOverride,
                    sickPayRate = if (shift1 == Shift.SICK) sickPayRate else null
                )
                onDismiss()
            }
        )
    }

    hoursPicker?.let { picker ->
        HoursPickerDialog(
            title = picker.title,
            currentHours = picker.current,
            minHours = picker.minHours,
            accent = picker.accent,
            onSelect = { selected ->
                when (picker.target) {
                    HoursTarget.SHIFT1 -> hours1 = selected
                    HoursTarget.SHIFT2 -> hours2 = selected
                }
            },
            onDismiss = { hoursPicker = null }
        )
    }

    if (showCompDialog) {
        val summary = repo.getSummary(year, month)
        val availableComp = summary.compTimeStarting
        CompUsedDialog(
            availableHours = availableComp,
            currentHours = compUsed,
            onSelect = { selected -> compUsed = selected },
            onDismiss = { showCompDialog = false }
        )
    }
}

private enum class HoursTarget {
    SHIFT1,
    SHIFT2
}

private data class HoursPickerState(
    val title: String,
    val current: Int,
    val accent: Color,
    val target: HoursTarget,
    val minHours: Int
)

