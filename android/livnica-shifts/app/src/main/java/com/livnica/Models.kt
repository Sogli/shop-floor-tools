package com.livnica

import java.time.LocalDate
import org.json.JSONObject

enum class Shift(val value: Int) {
    OFF(0),
    FIRST(1),
    SECOND(2),
    THIRD(3),
    SICK(4),
    VACATION(5);

    val displayName: String
        get() = when (this) {
            OFF -> "OFF"
            FIRST -> "S1"
            SECOND -> "S2"
            THIRD -> "S3"
            SICK -> "BOL"
            VACATION -> "GO"
        }

    val fullName: String
        get() = when (this) {
            OFF -> "Slobodan dan"
            FIRST -> "Prva smena"
            SECOND -> "Druga smena"
            THIRD -> "Tre\u0107a smena"
            SICK -> "Bolovanje"
            VACATION -> "Godi\u0161nji odmor"
        }

    val color
        get() = when (this) {
            OFF -> THEME.off
            FIRST -> THEME.shift1
            SECOND -> THEME.shift2
            THIRD -> THEME.shift3
            SICK -> THEME.sick
            VACATION -> THEME.vacation
        }

    val textColor
        get() = when (this) {
            FIRST, SECOND, VACATION -> THEME.textDark
            else -> THEME.textPrimary
        }

    val icon: String
        get() = when (this) {
            FIRST -> "Prva"
            SECOND -> "Druga"
            THIRD -> "Treća"
            else -> displayName
        }

    companion object {
        fun fromValue(value: Int): Shift {
            return values().firstOrNull { it.value == value } ?: OFF
        }

        fun workShifts(): List<Shift> = listOf(FIRST, SECOND, THIRD)

        fun paidNonWork(): List<Shift> = listOf(SICK, VACATION)
    }
}

enum class BrigadeType(val value: String, val label: String) {
    TROBRIGADA("trob", "Trobrigada"),
    CETVOROBRIGADA("cetv", "Četvorobrigada");

    companion object {
        fun fromValue(value: String?): BrigadeType {
            return values().firstOrNull { it.value == value } ?: CETVOROBRIGADA
        }
    }
}

enum class OvertimeType(val value: Int) {
    PAID(0),
    COMP_TIME(1);

    companion object {
        fun fromValue(value: Int): OvertimeType {
            return if (value == 1) COMP_TIME else PAID
        }
    }
}

enum class Weekday {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    val isWeekend: Boolean
        get() = this >= SATURDAY

    val isWorkday: Boolean
        get() = this <= FRIDAY

    companion object {
        fun from(date: LocalDate): Weekday {
            return values()[date.dayOfWeek.ordinal]
        }
    }
}

data class DayKey(val year: Int, val month: Int, val day: Int)

data class MonthKey(val year: Int, val month: Int) : Comparable<MonthKey> {
    override fun compareTo(other: MonthKey): Int {
        return if (year != other.year) year - other.year else month - other.month
    }
}

class DayRecord(
    val year: Int,
    val month: Int,
    val day: Int,
    val shift: Shift,
    hours: Int,
    val shift2: Shift? = null,
    hours2: Int = 0,
    val overtimeType: OvertimeType = OvertimeType.PAID,
    val weekendOvertimeType: OvertimeType = OvertimeType.PAID,
    compHoursUsed: Int = 0,
    val holidayWorkOverride: Boolean? = null,
    val holidayOverride: Boolean? = null,
    sickPayRate: Double? = null
) {
    val hours: Int = hours.coerceIn(0, 24)
    val hours2: Int = hours2.coerceIn(0, 24)
    val compHoursUsed: Int = compHoursUsed.coerceIn(0, 24)
    val sickPayRate: Double? = run {
        var rate = sickPayRate
        if (rate != null) {
            rate = when {
                rate <= 0.0 -> null
                rate > 1.0 -> 1.0
                else -> rate
            }
        }
        if (shift != Shift.SICK) null else rate
    }

    val date: LocalDate = LocalDate.of(year, month, day)
    val weekday: Weekday = Weekday.from(date)
    val key: DayKey = DayKey(year, month, day)
    val monthKey: MonthKey = MonthKey(year, month)

    val isWorkDay: Boolean
        get() = shift in Shift.workShifts() && hours > 0

    val isDoubleShift: Boolean
        get() = shift2 != null && shift2 in Shift.workShifts() && hours2 > 0

    val isCompTimeOvertime: Boolean
        get() = isDoubleShift && overtimeType == OvertimeType.COMP_TIME

    val isWeekendCompTime: Boolean
        get() = weekday.isWeekend && shift in Shift.workShifts() && weekendOvertimeType == OvertimeType.COMP_TIME

    val totalHours: Int
        get() = hours + if (isDoubleShift) hours2 else 0

    val compTimeEarned: Int
        get() {
            var earned = 0
            // Vikend sati za kompenzaciju (prva smena)
            if (isWeekendCompTime) {
                earned += hours
            }
            // Druga smena za kompenzaciju
            if (isCompTimeOvertime) {
                earned += hours2
            }
            return earned
        }

    val hoursDeficit: Int
        get() {
            if (!weekday.isWorkday) return 0
            if (shift !in Shift.workShifts()) return 0
            val regularExpected = CONFIG.pay.defaultHours
            // Koristimo totalHours da bismo uračunali i double shift
            val workedHours = if (isDoubleShift && !isCompTimeOvertime) totalHours else hours
            return if (workedHours < regularExpected) regularExpected - workedHours else 0
        }

    val netCompChange: Int
        get() = compTimeEarned - hoursDeficit - compHoursUsed

    val isHoliday: Boolean by lazy {
        holidayOverride ?: isSerbianHoliday(year, month, day)
    }

    val holidayName: String?
        get() {
            val baseName = getHolidayName(year, month, day)
            if (holidayOverride == false) return null
            if (holidayOverride == true && baseName == null) return MANUAL_HOLIDAY_LABEL
            return baseName
        }

    val workedOnHoliday: Boolean
        get() {
            if (!isHoliday) return false
            if (holidayWorkOverride != null) return holidayWorkOverride
            return shift in Shift.workShifts() && totalHours > 0
        }

    fun toMap(): Map<String, Any> {
        val data = linkedMapOf<String, Any>(
            "shift" to shift.value,
            "hours" to hours
        )
        val s2 = shift2
        if (isDoubleShift && s2 != null) {
            data["shift2"] = s2.value
            data["hours2"] = hours2
            data["overtime_type"] = overtimeType.value
        }
        if (weekendOvertimeType != OvertimeType.PAID) {
            data["weekend_overtime_type"] = weekendOvertimeType.value
        }
        if (compHoursUsed > 0) {
            data["comp_hours_used"] = compHoursUsed
        }
        if (holidayWorkOverride != null) {
            data["holiday_work_override"] = holidayWorkOverride
        }
        if (holidayOverride != null) {
            data["holiday_override"] = holidayOverride
        }
        if (sickPayRate != null) {
            data["sick_pay_rate"] = sickPayRate
        }
        return data
    }

    companion object {
        fun fromJson(year: Int, month: Int, day: Int, data: JSONObject): DayRecord {
            val shift = Shift.fromValue(data.optInt("shift", 0))
            val hours = data.optInt("hours", 0)
            val shift2 = if (data.has("shift2")) Shift.fromValue(data.optInt("shift2", 0)) else null
            val hours2 = data.optInt("hours2", 0)
            val overtimeType = OvertimeType.fromValue(data.optInt("overtime_type", 0))
            val weekendOvertimeType = OvertimeType.fromValue(data.optInt("weekend_overtime_type", 0))
            val compHoursUsed = data.optInt("comp_hours_used", 0)
            val holidayWorkOverride = if (data.has("holiday_work_override")) {
                data.optBoolean("holiday_work_override")
            } else {
                null
            }
            val holidayOverride = if (data.has("holiday_override")) {
                data.optBoolean("holiday_override")
            } else {
                null
            }
            val sickPayRate = if (data.has("sick_pay_rate")) {
                data.optDouble("sick_pay_rate")
            } else {
                null
            }
            return DayRecord(
                year = year,
                month = month,
                day = day,
                shift = shift,
                hours = hours,
                shift2 = shift2,
                hours2 = hours2,
                overtimeType = overtimeType,
                weekendOvertimeType = weekendOvertimeType,
                compHoursUsed = compHoursUsed,
                holidayWorkOverride = holidayWorkOverride,
                holidayOverride = holidayOverride,
                sickPayRate = sickPayRate
            )
        }
    }
}

data class MonthSummary(
    var year: Int,
    var month: Int,
    var workHours: Int = 0,
    var overtimeHours: Int = 0,
    var sickHours: Int = 0,
    var vacationHours: Int = 0,
    var sundayHours: Int = 0,
    var nightHours: Int = 0,
    var totalPay: Double = 0.0,
    var workDays: Int = 0,
    var doubleShiftDays: Int = 0,
    var foodAllowance: Double = 0.0,
    var compTimeEarned: Int = 0,
    var compTimeDeficit: Int = 0,
    var compTimeUsed: Int = 0,
    var compTimeNet: Int = 0,
    var compTimeStarting: Int = 0,
    var compTimeEnding: Int = 0,
    var minuliRadBonus: Double = 0.0
) {
    val totalWithMinuli: Double
        get() = totalPay + minuliRadBonus

    val displayName: String
        get() = if (month in 1..12) {
            "${MONTH_NAMES[month]} $year"
        } else {
            "Mesec $month $year"
        }
}

