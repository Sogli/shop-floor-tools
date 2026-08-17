package com.livnica

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class ShiftScheduler(
    private val cycle: List<Int> = CONFIG.weeklyShiftCycle,
    private val defaultHours: Int = CONFIG.pay.defaultHours
) {
    fun getShiftForDate(
        targetDate: LocalDate,
        referenceDate: LocalDate,
        referenceShift: Shift
    ): Shift? {
        if (targetDate.dayOfWeek == DayOfWeek.SATURDAY) return null

        val refIndex = cycle.indexOf(referenceShift.value)
        if (refIndex < 0) return null

        val refMonday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val targetMonday = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeksDiff = java.time.temporal.ChronoUnit.WEEKS.between(refMonday, targetMonday).toInt()
        val cycleIndex = Math.floorMod(refIndex + weeksDiff, cycle.size)
        val calculatedShift = Shift.fromValue(cycle[cycleIndex])
        return applyForcedOffRules(targetDate, calculatedShift)
    }

    fun populateMonth(repo: ShiftRepository, year: Int, month: Int, startShift: Shift): Int {
        val firstWorkday = (1..LocalDate.of(year, month, 1).lengthOfMonth()).firstOrNull { day ->
            LocalDate.of(year, month, day).dayOfWeek.value <= DayOfWeek.FRIDAY.value
        } ?: return 0

        val records = mutableListOf<BatchRecord>()
        val referenceDate = LocalDate.of(year, month, firstWorkday)

        for (day in 1..LocalDate.of(year, month, 1).lengthOfMonth()) {
            val target = LocalDate.of(year, month, day)
            val shift = getShiftForDate(target, referenceDate, startShift)
            if (shift != null) {
                records.add(BatchRecord(year, month, day, shift, defaultHours))
            }
        }

        repo.setBatch(records)
        return records.size
    }

    fun populateYear(repo: ShiftRepository, year: Int, startShift: Shift): Int {
        val firstWorkday = (1..7).firstOrNull { day ->
            LocalDate.of(year, 1, day).dayOfWeek.value <= DayOfWeek.FRIDAY.value
        } ?: return 0

        val records = mutableListOf<BatchRecord>()
        val referenceDate = LocalDate.of(year, 1, firstWorkday)

        for (month in 1..12) {
            val daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth()
            for (day in 1..daysInMonth) {
                val target = LocalDate.of(year, month, day)
                val shift = getShiftForDate(target, referenceDate, startShift)
                if (shift != null) {
                    records.add(BatchRecord(year, month, day, shift, defaultHours))
                }
            }
        }

        repo.setBatch(records)
        return records.size
    }

    fun populateMonthsAhead(
        repo: ShiftRepository,
        referenceDate: LocalDate,
        referenceShift: Shift,
        monthsAhead: Int,
        brigadeType: BrigadeType,
        fourBrigadeStartIndex: Int? = null
    ): Int {
        val safeMonths = monthsAhead.coerceAtLeast(0)
        val startDate = referenceDate.withDayOfMonth(1)
        val endMonth = startDate.plusMonths(safeMonths.toLong())
        val endDate = endMonth.withDayOfMonth(endMonth.lengthOfMonth())
        val records = mutableListOf<BatchRecord>()

        var date = startDate
        while (!date.isAfter(endDate)) {
            val shift = when (brigadeType) {
                BrigadeType.TROBRIGADA -> getShiftForDate(date, referenceDate, referenceShift)
                BrigadeType.CETVOROBRIGADA -> getFourBrigadeShiftForDate(
                    date,
                    referenceDate,
                    referenceShift,
                    fourBrigadeStartIndex
                )
            }
            if (shift != null) {
                val hours = if (shift in Shift.workShifts()) defaultHours else 0
                records.add(BatchRecord(date.year, date.monthValue, date.dayOfMonth, shift, hours))
            }
            date = date.plusDays(1)
        }

        if (records.isNotEmpty()) {
            repo.setBatch(records)
        }
        return records.size
    }

    private fun getFourBrigadeShiftForDate(
        targetDate: LocalDate,
        referenceDate: LocalDate,
        referenceShift: Shift,
        referenceIndexOverride: Int?
    ): Shift {
        val pattern = listOf(
            Shift.FIRST,
            Shift.FIRST,
            Shift.SECOND,
            Shift.SECOND,
            Shift.THIRD,
            Shift.THIRD,
            Shift.OFF,
            Shift.OFF
        )
        val refIndex = referenceIndexOverride?.coerceIn(0, pattern.lastIndex)
            ?: pattern.indexOf(referenceShift).takeIf { it >= 0 }
            ?: 0
        val dayDiff = ChronoUnit.DAYS.between(referenceDate, targetDate).toInt()
        val patternIndex = Math.floorMod(refIndex + dayDiff, pattern.size)
        return applyForcedOffRules(targetDate, pattern[patternIndex])
    }

    private fun applyForcedOffRules(targetDate: LocalDate, calculatedShift: Shift): Shift {
        return when {
            targetDate.dayOfWeek == DayOfWeek.SUNDAY -> Shift.OFF
            targetDate.dayOfWeek == DayOfWeek.MONDAY && calculatedShift == Shift.FIRST -> Shift.OFF
            else -> calculatedShift
        }
    }

    fun applySickRange(
        repo: ShiftRepository,
        year: Int,
        month: Int,
        startDay: Int,
        endDay: Int,
        sickPayRate: Double? = null
    ): Int {
        var start = startDay
        var end = endDay
        if (start > end) {
            val temp = start
            start = end
            end = temp
        }

        val daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth()
        val records = mutableListOf<BatchRecord>()

        for (day in start..end) {
            if (day in 1..daysInMonth) {
                if (LocalDate.of(year, month, day).dayOfWeek.value <= DayOfWeek.FRIDAY.value) {
                    records.add(BatchRecord(year, month, day, Shift.SICK, defaultHours, sickPayRate))
                }
            }
        }

        if (records.isNotEmpty()) repo.setBatch(records)
        return records.size
    }

    fun applyVacationRange(
        repo: ShiftRepository,
        year: Int,
        month: Int,
        startDay: Int,
        endDay: Int
    ): Int {
        var start = startDay
        var end = endDay
        if (start > end) {
            val temp = start
            start = end
            end = temp
        }

        val daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth()
        val records = mutableListOf<BatchRecord>()

        for (day in start..end) {
            if (day in 1..daysInMonth) {
                if (LocalDate.of(year, month, day).dayOfWeek.value <= DayOfWeek.FRIDAY.value) {
                    records.add(BatchRecord(year, month, day, Shift.VACATION, defaultHours))
                }
            }
        }

        if (records.isNotEmpty()) repo.setBatch(records)
        return records.size
    }
}

