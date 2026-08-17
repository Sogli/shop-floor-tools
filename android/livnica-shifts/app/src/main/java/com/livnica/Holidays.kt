package com.livnica

import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

data class Holiday(val month: Int, val day: Int, val name: String)

private val fixedHolidays = listOf(
    Holiday(1, 1, "Nova godina"),
    Holiday(1, 2, "Nova godina"),
    Holiday(1, 7, "Božić"),
    Holiday(2, 15, "Dan državnosti"),
    Holiday(2, 16, "Dan državnosti"),
    Holiday(2, 17, "Dan državnosti"),
    Holiday(5, 1, "Praznik rada"),
    Holiday(5, 2, "Praznik rada"),
    Holiday(11, 11, "Dan primirja")
)

private val holidayCache = ConcurrentHashMap<Int, List<Holiday>>()

/**
 * Calculates Orthodox Easter date using the Meeus Julian algorithm.
 * The algorithm computes the date on the Julian calendar, then converts to Gregorian.
 *
 * The Julian-to-Gregorian offset varies by century:
 * - 1900-2099: 13 days
 * - 2100-2199: 14 days
 * - etc.
 */
fun orthodoxEasterSunday(year: Int): LocalDate {
    // Meeus Julian algorithm for computing Easter on Julian calendar
    val a = year % 4
    val b = year % 7
    val c = year % 19
    val d = (19 * c + 15) % 30
    val e = (2 * a + 4 * b - d + 34) % 7
    val month = (d + e + 114) / 31
    val day = ((d + e + 114) % 31) + 1

    // Julian Easter date (interpreted as Gregorian for LocalDate)
    val julianEaster = LocalDate.of(year, month, day)

    // Calculate Julian-to-Gregorian offset for the given year
    // Formula: century - century/4 - 2 (where century = year/100)
    val century = year / 100
    val julianToGregorianOffset = (century - century / 4 - 2).toLong()

    return julianEaster.plusDays(julianToGregorianOffset)
}

fun getSerbianHolidays(year: Int): List<Holiday> {
    holidayCache[year]?.let { return it }

    val holidays = mutableListOf<Holiday>()
    holidays.addAll(fixedHolidays)

    val easter = orthodoxEasterSunday(year)
    val easterRelated = listOf(
        Holiday(easter.minusDays(2).monthValue, easter.minusDays(2).dayOfMonth, "Veliki petak"),
        Holiday(easter.minusDays(1).monthValue, easter.minusDays(1).dayOfMonth, "Velika subota"),
        Holiday(easter.monthValue, easter.dayOfMonth, "Uskrs"),
        Holiday(easter.plusDays(1).monthValue, easter.plusDays(1).dayOfMonth, "Uskršnji ponedeljak")
    )
    holidays.addAll(easterRelated)

    val merged = linkedMapOf<Pair<Int, Int>, MutableList<String>>()
    for (holiday in holidays) {
        val key = holiday.month to holiday.day
        val names = merged.getOrPut(key) { mutableListOf() }
        if (!names.contains(holiday.name)) {
            names.add(holiday.name)
        }
    }

    val combined = merged.map { (key, names) ->
        Holiday(key.first, key.second, names.joinToString(" / "))
    }.sortedWith(compareBy({ it.month }, { it.day }))

    // Ograničenje keša na 5 godina - izbaci najstariju ako prelazi limit
    if (holidayCache.size >= 5 && !holidayCache.containsKey(year)) {
        val oldest = holidayCache.keys.minOrNull()
        if (oldest != null) {
            holidayCache.remove(oldest)
        }
    }
    holidayCache[year] = combined
    return combined
}

fun isSerbianHoliday(year: Int, month: Int, day: Int): Boolean {
    return getSerbianHolidays(year).any { it.month == month && it.day == day }
}

fun getHolidayName(year: Int, month: Int, day: Int): String? {
    return getSerbianHolidays(year).firstOrNull { it.month == month && it.day == day }?.name
}

