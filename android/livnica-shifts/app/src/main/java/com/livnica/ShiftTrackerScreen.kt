package com.livnica

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import com.livnica.ui.components.AutoSizeText
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Sealed class za upravljanje stanjem dijaloga - zamenjuje 10+ boolean varijabli */
sealed class DialogState {
    object None : DialogState()
    object AutoFill : DialogState()
    object Food : DialogState()
    object DeleteMonth : DialogState()
}

@Composable
fun LivnicaApp(repo: ShiftRepository) {
    LivnicaTheme {
        ShiftTrackerScreen(repo = repo)
    }
}

@Composable
fun ShiftTrackerScreen(repo: ShiftRepository) {
    val scope = rememberCoroutineScope()
    val repoTick = remember { mutableIntStateOf(0) }

    // Use DisposableEffect to properly clean up the callback when composable leaves composition
    DisposableEffect(repo) {
        val callbackToken = repo.onChange {
            scope.launch {
                repoTick.intValue += 1
            }
        }
        onDispose {
            repo.removeOnChange(callbackToken)
        }
    }

    val today = remember { LocalDate.now() }

    var currentYear by remember { mutableIntStateOf(today.year) }
    var currentMonth by remember { mutableIntStateOf(today.monthValue) }
    val summary = remember(repoTick.intValue, currentYear, currentMonth) {
        repo.getSummary(currentYear, currentMonth)
    }
    val scheduler = remember { ShiftScheduler() }

    var dialogState by remember { mutableStateOf<DialogState>(DialogState.None) }
    val dismissDialog = { dialogState = DialogState.None }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        BackgroundLayer()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = UI.cardPadding.dp,
                    top = 16.dp,
                    end = UI.cardPadding.dp,
                    bottom = (UI.bottomBarHeight + 10).dp
                ),
            verticalArrangement = Arrangement.spacedBy(UI.largeSpacing.dp)
        ) {
            HeaderSection(
                month = currentMonth,
                year = currentYear,
                onPrev = {
                    if (currentMonth == 1) {
                        currentMonth = 12
                        currentYear -= 1
                    } else {
                        currentMonth -= 1
                    }
                },
                onNext = {
                    if (currentMonth == 12) {
                        currentMonth = 1
                        currentYear += 1
                    } else {
                        currentMonth += 1
                    }
                }
            )

            WeekdayHeader()
            key(repoTick.intValue) {
                CalendarSection(
                    year = currentYear,
                    month = currentMonth,
                    today = today,
                    repo = repo
                )
            }
            StatsSection(
                summary = summary,
                onFoodClick = { dialogState = DialogState.Food }
            )
        }

        BottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onAutoFill = { dialogState = DialogState.AutoFill },
            onDeleteMonth = { dialogState = DialogState.DeleteMonth }
        )
    }

    // Renderovanje aktivnog dijaloga koristeći sealed class
    when (dialogState) {
        DialogState.None -> { /* Nema aktivnog dijaloga */ }

        DialogState.AutoFill -> {
            AutoFillDialog(
                today = today,
                scheduler = scheduler,
                repo = repo,
                onDismiss = dismissDialog
            )
        }

        DialogState.Food -> {
            FoodAllowanceDialog(
                repo = repo,
                onDismiss = dismissDialog
            )
        }

        DialogState.DeleteMonth -> {
            DeleteMonthDialog(
                year = currentYear,
                month = currentMonth,
                repo = repo,
                onDismiss = dismissDialog
            )
        }
    }
}

@Composable
private fun HeaderSection(month: Int, year: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(UI.headerHeight.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                text = "<",
                modifier = Modifier.size(UI.iconButtonSize.dp),
                bgColor = THEME.bgCardElevated,
                textColor = THEME.textPrimary,
                onClick = onPrev
            )
            AutoSizeText(
                text = "${MONTH_NAMES[month]} $year",
                color = THEME.textPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                minFontSize = MaterialTheme.typography.titleMedium.fontSize,
                maxLines = 1
            )
            IconButton(
                text = ">",
                modifier = Modifier.size(UI.iconButtonSize.dp),
                bgColor = THEME.bgCardElevated,
                textColor = THEME.textPrimary,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UI.weekdayHeaderHeight.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WEEKDAY_NAMES.forEachIndexed { index, name ->
            val color = if (index >= 5) THEME.weekendOt else THEME.textMuted
            Text(
                text = name,
                color = color,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.W700,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CalendarSection(
    year: Int,
    month: Int,
    today: LocalDate,
    repo: ShiftRepository
) {
    val weeks = remember(year, month) { getMonthMatrix(year, month) }
    val scrollState = rememberScrollState()
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(UI.smallSpacing.dp)
        ) {
            for (week in weeks) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(UI.smallSpacing.dp)
                ) {
                    for (day in week) {
                        val record = if (day != 0) repo.get(year, month, day) else null
                        val isToday = day == today.dayOfMonth &&
                            month == today.monthValue &&
                            year == today.year
                        DayButton(
                            day = day,
                            record = record,
                            isToday = isToday,
                            year = year,
                            month = month
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayButton(
    day: Int,
    record: DayRecord?,
    isToday: Boolean,
    year: Int,
    month: Int
) {
    if (day == 0) {
        Spacer(modifier = Modifier.height(UI.dayButtonHeight.dp).weight(1f))
        return
    }

    val isHolidayDay = record?.holidayOverride ?: isSerbianHoliday(year, month, day)
    val workedHoliday = record?.workedOnHoliday ?: false
    val showHolidayIndicator = isHolidayDay || workedHoliday

    val bgColor = when {
        record == null -> THEME.empty
        else -> record.shift.color
    }
    val textColor = when {
        record == null -> THEME.textMuted
        else -> record.shift.textColor
    }

    val detailText = if (record == null) {
        "-"
    } else {
        when (record.shift) {
            Shift.OFF -> "OFF"
            Shift.SICK -> "BOL"
            Shift.VACATION -> "GO"
            else -> if (record.isDoubleShift && record.shift2 != null) {
                "${record.shift.icon} + ${record.shift2.icon}"
            } else {
                record.shift.icon
            }
        }
    }

    val doubleIndicatorColor = if (record?.isDoubleShift == true && record.shift2 != null) {
        if (record.isCompTimeOvertime) THEME.compTime else record.shift2.color
    } else {
        null
    }

    val compIndicatorColor = if (record != null) {
        when {
            record.hoursDeficit > 0 -> THEME.compTimeDeficit
            record.compHoursUsed > 0 -> THEME.compTimeUsed
            record.isCompTimeOvertime -> THEME.compTime
            else -> null
        }
    } else {
        null
    }

    val holidayIndicatorColor = if (showHolidayIndicator) {
        if (isHolidayDay) THEME.warning else THEME.info
    } else {
        null
    }

    val text = buildDayText(
        day,
        detailText,
        MaterialTheme.typography.labelMedium.fontSize,
        MaterialTheme.typography.labelSmall.fontSize
    )

    Box(
        modifier = Modifier
            .height(UI.dayButtonHeight.dp)
            .weight(1f)
    ) {
        if (isToday) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(1.dp)
                    .border(1.dp, THEME.accentPrimary, RoundedCornerShape(14.dp))
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
        ) {
            if (doubleIndicatorColor != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 8.dp, end = 8.dp, bottom = 2.dp)
                        .height(4.dp)
                        .fillMaxWidth()
                        .background(doubleIndicatorColor, RoundedCornerShape(4.dp))
                )
            }

            if (compIndicatorColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .background(compIndicatorColor, CircleShape)
                )
            }

            if (holidayIndicatorColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopStart)
                        .padding(top = 4.dp, start = 4.dp)
                        .background(holidayIndicatorColor, CircleShape)
                )
            }

            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.W700,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = THEME.shadow.copy(alpha = 0.45f),
                        offset = Offset(0f, 1f),
                        blurRadius = 2f
                    )
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            )
        }
    }
}

private fun buildDayText(
    day: Int,
    detail: String,
    dayFontSize: TextUnit,
    detailFontSize: TextUnit
): AnnotatedString {
    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.W700, fontSize = dayFontSize)) {
            append(day.toString())
        }
        append("\n")
        withStyle(SpanStyle(fontSize = detailFontSize, fontWeight = FontWeight.W700)) {
            append(detail)
        }
    }
}

@Composable
private fun StatsSection(
    summary: MonthSummary,
    onFoodClick: () -> Unit
) {
    data class StatItem(
        val label: String,
        val value: String,
        val color: androidx.compose.ui.graphics.Color,
        val onClick: (() -> Unit)? = null
    )

    val stats = listOf(
        StatItem("Sati rada", summary.workHours.toString(), THEME.textPrimary),
        StatItem(
            "Hrana",
            formatNumber(summary.foodAllowance),
            if (summary.foodAllowance > 0) THEME.success else THEME.textMuted,
            onClick = onFoodClick
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UI.statCardHeight.dp),
        horizontalArrangement = Arrangement.spacedBy(UI.defaultSpacing.dp)
    ) {
        for (stat in stats) {
            StatCard(
                label = stat.label,
                value = stat.value,
                color = stat.color,
                modifier = Modifier.weight(1f),
                onClick = stat.onClick
            )
        }
    }
}

@Composable
private fun BottomBar(
    modifier: Modifier,
    onAutoFill: () -> Unit,
    onDeleteMonth: () -> Unit
) {
    val buttons = listOf(
        Quad("D", "Danas", THEME.accentPrimary, onAutoFill),
        Quad("X", "Brisi", THEME.danger, onDeleteMonth)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(THEME.bgSecondary.copy(alpha = 0.94f))
    ) {
        // Buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UI.cardPadding.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(UI.defaultSpacing.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            buttons.forEach { item ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GlowButton(
                        text = item.icon,
                        bgColor = item.color,
                        textColor = THEME.textPrimary,
                        textStyle = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .height(38.dp)
                            .fillMaxWidth(),
                        onClick = item.onClick
                    )
                    Text(
                        text = item.label,
                        color = THEME.textMuted,
                        style = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class Quad(
    val icon: String,
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit
)

private fun getMonthMatrix(year: Int, month: Int): List<List<Int>> {
    val firstDay = LocalDate.of(year, month, 1)
    val daysInMonth = firstDay.lengthOfMonth()
    val firstWeekday = firstDay.dayOfWeek.value

    val weeks = mutableListOf<MutableList<Int>>()
    var week = MutableList(7) { 0 }
    var day = 1
    var index = firstWeekday - 1

    while (day <= daysInMonth) {
        week[index] = day
        day++
        index++
        if (index == 7) {
            weeks.add(week)
            week = MutableList(7) { 0 }
            index = 0
        }
    }
    if (week.any { it != 0 }) {
        weeks.add(week)
    }
    return weeks
}

