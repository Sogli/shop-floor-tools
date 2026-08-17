package com.livnica

import java.time.LocalDateTime
import java.time.LocalTime

object TimeRounder {

    // Početak smena
    val FIRST_SHIFT_START: LocalTime = LocalTime.of(6, 0)
    val SECOND_SHIFT_START: LocalTime = LocalTime.of(14, 0)
    val THIRD_SHIFT_START: LocalTime = LocalTime.of(22, 0)

    /**
     * Određuje smenu na osnovu vremena ulaska.
     * - Pre 06:00 ili posle 22:00 → THIRD (noćna)
     * - 06:00 - 13:59 → FIRST
     * - 14:00 - 21:59 → SECOND
     */
    fun detectShift(entryTime: LocalDateTime): Shift {
        val time = entryTime.toLocalTime()
        return when {
            time.isBefore(FIRST_SHIFT_START) -> Shift.THIRD  // Pre 6 ujutru = noćna
            time.isBefore(SECOND_SHIFT_START) -> Shift.FIRST  // 6-14
            time.isBefore(THIRD_SHIFT_START) -> Shift.SECOND  // 14-22
            else -> Shift.THIRD  // Posle 22
        }
    }

    /**
     * Vraća početak smene za datu smenu.
     */
    fun getShiftStart(shift: Shift): LocalTime {
        return when (shift) {
            Shift.FIRST -> FIRST_SHIFT_START
            Shift.SECOND -> SECOND_SHIFT_START
            Shift.THIRD -> THIRD_SHIFT_START
            else -> FIRST_SHIFT_START
        }
    }

    /**
     * Zaokružuje vreme ulaska.
     * - Ako si došao pre ili tačno na početak smene → računa se od početka smene
     * - Ako si kasnio (makar 1 minut) → zaokruži na sledeći pun sat
     *
     * Primer za prvu smenu (06:00):
     * - 05:58 → 06:00 (na vreme)
     * - 06:00 → 06:00 (tačno)
     * - 06:01 → 07:00 (kasniš, gubiš sat)
     * - 06:59 → 07:00 (kasniš)
     * - 07:15 → 08:00 (kasniš)
     */
    fun roundEntry(entryTime: LocalDateTime, shiftStart: LocalTime): LocalTime {
        val entryLocalTime = entryTime.toLocalTime()

        // Ako si došao pre ili tačno na vreme
        if (!entryLocalTime.isAfter(shiftStart)) {
            return shiftStart
        }

        // Kasniš - zaokruži na sledeći pun sat
        return if (entryLocalTime.minute == 0 && entryLocalTime.second == 0) {
            entryLocalTime  // Tačno na pun sat
        } else {
            entryLocalTime.plusHours(1).withMinute(0).withSecond(0).withNano(0)
        }
    }

    /**
     * Zaokružuje vreme izlaska nadole na pun sat.
     *
     * Primeri:
     * - 14:03 → 14:00
     * - 14:58 → 14:00
     * - 15:00 → 15:00 (tačno na pun sat)
     */
    fun roundExit(exitTime: LocalDateTime): LocalTime {
        val exitLocalTime = exitTime.toLocalTime()
        return exitLocalTime.withMinute(0).withSecond(0).withNano(0)
    }

    /**
     * Računa ukupne sate između zaokruženog ulaska i izlaska.
     * Uzima u obzir prelazak preko ponoći za noćnu smenu.
     */
    fun calculateHours(
        entryTime: LocalDateTime,
        exitTime: LocalDateTime,
        shift: Shift
    ): Int {
        val shiftStart = getShiftStart(shift)
        val roundedEntry = roundEntry(entryTime, shiftStart)
        val roundedExit = roundExit(exitTime)

        // Računamo razliku u satima
        var entryHour = roundedEntry.hour
        var exitHour = roundedExit.hour

        // Ako je noćna smena i izlaz je ujutru (prelazak ponoći)
        if (shift == Shift.THIRD && exitHour < 12 && entryHour >= 22) {
            exitHour += 24
        }

        val hours = exitHour - entryHour

        // Osiguraj da nije negativno ili preveliko
        return hours.coerceIn(0, 24)
    }

    /**
     * Vraća sledeću smenu za duplu smenu.
     * FIRST → SECOND
     * SECOND → THIRD
     * THIRD → FIRST
     */
    fun nextShift(shift: Shift): Shift {
        return when (shift) {
            Shift.FIRST -> Shift.SECOND
            Shift.SECOND -> Shift.THIRD
            Shift.THIRD -> Shift.FIRST
            else -> Shift.FIRST
        }
    }

    /**
     * Formatira vreme za prikaz u notifikaciji.
     */
    fun formatTimeChange(actual: LocalTime, rounded: LocalTime): String {
        val actualStr = String.format("%02d:%02d", actual.hour, actual.minute)
        val roundedStr = String.format("%02d:%02d", rounded.hour, rounded.minute)
        return if (actual == rounded) {
            actualStr
        } else {
            "$actualStr → $roundedStr"
        }
    }
}

