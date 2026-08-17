package com.livnica

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

class PayCalculator(private val config: PayConfig = CONFIG.pay) {
    fun getSundayHoursForShift(
        weekday: Weekday,
        shift: Shift,
        hours: Int,
        brigadeType: BrigadeType
    ): Int {
        if (brigadeType == BrigadeType.CETVOROBRIGADA) return 0
        if (hours <= 0 || shift == Shift.SICK || shift == Shift.VACATION) return 0

        if (weekday == Weekday.SATURDAY) {
            if (shift == Shift.THIRD) {
                return max(0, hours - 2)
            }
            return 0
        }

        if (weekday == Weekday.SUNDAY) {
            if (shift == Shift.FIRST || shift == Shift.SECOND) return hours
            if (shift == Shift.THIRD) return min(hours, 2)
        }

        return 0
    }

    fun getSundayHours(record: DayRecord, brigadeType: BrigadeType): Int {
        var total = 0
        // Ako je vikend comp time, prva smena ne ide u sunday hours
        if (!record.isWeekendCompTime) {
            total += getSundayHoursForShift(
                record.weekday,
                record.shift,
                record.hours,
                brigadeType
            )
        }
        val shift2 = record.shift2
        if (record.isDoubleShift && !record.isCompTimeOvertime && shift2 != null) {
            total += getSundayHoursForShift(
                record.weekday,
                shift2,
                record.hours2,
                brigadeType
            )
        }
        return total
    }

    fun getOvertimeHours(record: DayRecord, brigadeType: BrigadeType): Int {
        if (record.shift == Shift.SICK || record.shift == Shift.OFF || record.shift == Shift.VACATION) {
            return 0
        }

        // Za vikend comp time, prva smena ne ide u overtime
        val firstShiftHours = if (record.isWeekendCompTime) 0 else record.hours
        // Za drugu smenu comp time, ne ide u overtime
        val secondShiftHours = if (record.isDoubleShift && record.isCompTimeOvertime) 0 else if (record.isDoubleShift) record.hours2 else 0
        val hours = firstShiftHours + secondShiftHours

        if (hours <= 0) return 0
        if (record.weekday.isWeekend && brigadeType != BrigadeType.CETVOROBRIGADA) return hours

        return max(0, hours - config.defaultHours)
    }

    fun getFoodAllowance(record: DayRecord): Double {
        if (record.shift == Shift.SICK || record.shift == Shift.OFF || record.shift == Shift.VACATION) {
            return 0.0
        }

        if (record.totalHours <= 0) return 0.0

        return config.foodPerDay
    }

    fun getNightHours(record: DayRecord): Int {
        var night = 0
        if (record.shift == Shift.THIRD) night += record.hours
        if (record.isDoubleShift && record.shift2 == Shift.THIRD) night += record.hours2
        return night
    }

    fun calculateDailyPay(
        record: DayRecord,
        baseRate: Double,
        brigadeType: BrigadeType
    ): Double {
        val weekday = record.weekday

        if (record.shift == Shift.OFF) {
            if (record.isHoliday && weekday.isWorkday) {
                return config.defaultHours * baseRate
            }
            return 0.0
        }

        if (record.hours <= 0) return 0.0

        if (record.shift == Shift.SICK) {
            if (weekday.isWorkday) {
                val sickRate = record.sickPayRate ?: config.sickPayRate
                return record.hours * baseRate * sickRate
            }
            return 0.0
        }

        if (record.shift == Shift.VACATION) {
            if (weekday.isWorkday) {
                return record.hours * baseRate * config.vacationPayRate
            }
            return 0.0
        }

        val shiftsData = mutableListOf<Pair<Shift, Int>>()
        // Ako je vikend comp time, prva smena ne ide u platu
        if (!record.isWeekendCompTime) {
            shiftsData.add(record.shift to record.hours)
        }
        val shift2 = record.shift2
        if (record.isDoubleShift && !record.isCompTimeOvertime && shift2 != null) {
            shiftsData.add(shift2 to record.hours2)
        }

        var totalPay = 0.0

        if (weekday.isWeekend && brigadeType != BrigadeType.CETVOROBRIGADA) {
            for ((shift, hours) in shiftsData) {
                val shiftMult = if (shift == Shift.THIRD) config.shift3Premium else 1.0
                var pay = hours * baseRate * shiftMult * config.overtimePremium
                val sundayHours = getSundayHoursForShift(weekday, shift, hours, brigadeType)
                if (sundayHours > 0) {
                    pay += sundayHours * baseRate * shiftMult * config.overtimePremium * config.sundayBonus
                }
                totalPay += pay
            }
        } else {
            var regularHoursRemaining = config.defaultHours
            for ((shift, hours) in shiftsData) {
                val shiftMult = if (shift == Shift.THIRD) config.shift3Premium else 1.0
                val regularHours = min(hours, regularHoursRemaining)
                val overtimeHours = hours - regularHours
                regularHoursRemaining -= regularHours

                var pay = regularHours * baseRate * shiftMult
                pay += overtimeHours * baseRate * shiftMult * config.overtimePremium
                totalPay += pay
            }
        }

        if (record.isHoliday) {
            totalPay += totalPay * config.holidayBonus
        }

        return totalPay
    }

    fun calculateMonthSummary(
        records: List<DayRecord>,
        baseRate: Double,
        startingCompBalance: Int = 0,
        yearsOfService: Int = 0,
        brigadeType: BrigadeType = BrigadeType.TROBRIGADA,
        summaryYear: Int = 0,
        summaryMonth: Int = 0
    ): MonthSummary {
        // Koristi prosledjeni year/month ili iz prvog zapisa
        val year = if (summaryYear > 0) summaryYear else records.firstOrNull()?.year ?: 0
        val month = if (summaryMonth > 0) summaryMonth else records.firstOrNull()?.month ?: 0

        if (year == 0 || month == 0) return MonthSummary(0, 0)

        val summary = MonthSummary(year = year, month = month)
        summary.compTimeStarting = startingCompBalance

        // Skup dana koji imaju zapise
        val recordedDays = records.map { it.day }.toSet()

        for (record in records) {
            if (record.shift == Shift.SICK) {
                if (record.weekday.isWorkday) {
                    summary.sickHours += record.hours
                    summary.totalPay += calculateDailyPay(record, baseRate, brigadeType)
                }
            } else if (record.shift == Shift.VACATION) {
                if (record.weekday.isWorkday) {
                    summary.vacationHours += record.hours
                    summary.totalPay += calculateDailyPay(record, baseRate, brigadeType)
                }
            } else if (record.shift == Shift.OFF) {
                if (record.isHoliday && record.weekday.isWorkday) {
                    summary.totalPay += calculateDailyPay(record, baseRate, brigadeType)
                }
            } else if (record.shift in Shift.workShifts() && record.hours > 0) {
                summary.workHours += record.totalHours
                summary.workDays += 1
                summary.overtimeHours += getOvertimeHours(record, brigadeType)
                summary.sundayHours += getSundayHours(record, brigadeType)
                summary.nightHours += getNightHours(record)
                summary.totalPay += calculateDailyPay(record, baseRate, brigadeType)
                summary.foodAllowance += getFoodAllowance(record)
                if (record.isDoubleShift) {
                    summary.doubleShiftDays += 1
                }
            }

            summary.compTimeEarned += record.compTimeEarned
            summary.compTimeDeficit += record.hoursDeficit
            summary.compTimeUsed += record.compHoursUsed
        }

        // Dodaj platu za neupisane državne praznike koji padaju na radne dane
        summary.totalPay += calculateUnrecordedHolidayPay(year, month, recordedDays, baseRate, brigadeType)

        summary.compTimeNet = summary.compTimeEarned - summary.compTimeDeficit - summary.compTimeUsed
        summary.compTimeEnding = summary.compTimeStarting + summary.compTimeNet

        // Ako je deficit pokriven zaradjenim satima, vrati platu za te sate
        // coveredDeficit = koliko sati deficita je pokriveno
        // Ako je ending >= 0, sav deficit je pokriven
        // Ako je ending < 0, pokriven je (deficit + ending) sati
        if (summary.compTimeDeficit > 0) {
            val coveredDeficit = summary.compTimeDeficit - maxOf(0, -summary.compTimeEnding)
            if (coveredDeficit > 0) {
                summary.totalPay += coveredDeficit * baseRate
            }
        }

        if (yearsOfService > 0) {
            val minuliRate = yearsOfService * config.minuliRadPerYear
            summary.minuliRadBonus = summary.totalPay * minuliRate
        }

        return summary
    }

    /**
     * Racuna platu za drzavne praznike koji nisu upisani.
     * Praznici na radnim danima (pon-pet) se racunaju kao placeni cak i ako nisu upisani.
     */
    fun calculateUnrecordedHolidayPay(
        year: Int,
        month: Int,
        recordedDays: Set<Int>,
        baseRate: Double,
        brigadeType: BrigadeType = BrigadeType.TROBRIGADA
    ): Double {
        val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
        var holidayPay = 0.0

        for (day in 1..daysInMonth) {
            // Preskoci dane koji vec imaju zapis
            if (day in recordedDays) continue

            // Proveri da li je drzavni praznik
            if (!isSerbianHoliday(year, month, day)) continue

            val date = LocalDate.of(year, month, day)
            val weekday = Weekday.from(date)

            // Za CETVOROBRIGADA, vikend praznici se ne plaćaju jer rade vikend kao normalan dan
            if (brigadeType == BrigadeType.CETVOROBRIGADA && weekday.isWeekend) continue

            // Proveri da li je radni dan (pon-pet)
            if (!weekday.isWorkday) continue

            // Dodaj platu za 8 sati po osnovnoj satnici
            holidayPay += config.defaultHours * baseRate
        }

        return holidayPay
    }
}

