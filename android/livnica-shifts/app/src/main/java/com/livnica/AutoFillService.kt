package com.livnica

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Servis za automatsko popunjavanje smena na osnovu gate eventa.
 */
class AutoFillService(
    private val shiftRepo: ShiftRepository
) {

    data class GateSession(
        val enterTime: LocalDateTime,
        val exitTime: LocalDateTime
    )

    /**
     * Obrađuje sesiju (ulaz + izlaz) i upisuje smenu.
     *
     * @param enterTimestamp Timestamp ulaska u milisekundama
     * @param exitTimestamp Timestamp izlaska u milisekundama
     * @return Opis upisane smene za notifikaciju, ili null ako nije upisano
     */
    fun processSession(enterTimestamp: Long, exitTimestamp: Long): AutoFillResult? {
        val zone = ZoneId.systemDefault()
        val enterTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(enterTimestamp), zone)
        val exitTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(exitTimestamp), zone)

        return processSession(GateSession(enterTime, exitTime))
    }

    /**
     * Obrađuje sesiju i upisuje smenu.
     */
    fun processSession(session: GateSession): AutoFillResult? {
        val enterTime = session.enterTime
        val exitTime = session.exitTime

        // Dan se računa po vremenu ulaska
        val year = enterTime.year
        val month = enterTime.monthValue
        val day = enterTime.dayOfMonth

        // Proveri da li dan već ima smenu
        val existingRecord = shiftRepo.get(year, month, day)

        // Odredi smenu
        val shift = if (existingRecord != null && existingRecord.shift in Shift.workShifts()) {
            // Koristi postojeću smenu
            existingRecord.shift
        } else {
            // Detektuj po vremenu ulaska
            TimeRounder.detectShift(enterTime)
        }

        // Izračunaj sate
        val totalHours = TimeRounder.calculateHours(enterTime, exitTime, shift)

        if (totalHours <= 0) {
            return null
        }

        // Sačuvaj postojeća polja iz record-a pre prepisivanja
        val preservedOvertimeType = existingRecord?.overtimeType ?: OvertimeType.PAID
        val preservedCompHoursUsed = existingRecord?.compHoursUsed ?: 0
        val preservedHolidayOverride = existingRecord?.holidayOverride
        val preservedHolidayWorkOverride = existingRecord?.holidayWorkOverride
        val preservedSickPayRate = existingRecord?.sickPayRate

        // Proveri da li je dupla smena (preko 8 sati)
        val defaultHours = CONFIG.pay.defaultHours
        return if (totalHours > defaultHours) {
            // Dupla smena
            val hours1 = defaultHours
            val hours2 = totalHours - defaultHours
            val shift2 = TimeRounder.nextShift(shift)

            shiftRepo.set(
                year = year,
                month = month,
                day = day,
                shift = shift,
                hours = hours1,
                shift2 = shift2,
                hours2 = hours2,
                overtimeType = preservedOvertimeType,
                compHoursUsed = preservedCompHoursUsed,
                holidayOverride = preservedHolidayOverride,
                holidayWorkOverride = preservedHolidayWorkOverride,
                sickPayRate = preservedSickPayRate
            )

            AutoFillResult(
                year = year,
                month = month,
                day = day,
                shift = shift,
                hours = hours1,
                shift2 = shift2,
                hours2 = hours2,
                entryRounded = TimeRounder.roundEntry(enterTime, TimeRounder.getShiftStart(shift)),
                exitRounded = TimeRounder.roundExit(exitTime),
                entryActual = enterTime.toLocalTime(),
                exitActual = exitTime.toLocalTime()
            )
        } else {
            // Normalna smena
            shiftRepo.set(
                year = year,
                month = month,
                day = day,
                shift = shift,
                hours = totalHours,
                overtimeType = preservedOvertimeType,
                compHoursUsed = preservedCompHoursUsed,
                holidayOverride = preservedHolidayOverride,
                holidayWorkOverride = preservedHolidayWorkOverride,
                sickPayRate = preservedSickPayRate
            )

            AutoFillResult(
                year = year,
                month = month,
                day = day,
                shift = shift,
                hours = totalHours,
                shift2 = null,
                hours2 = 0,
                entryRounded = TimeRounder.roundEntry(enterTime, TimeRounder.getShiftStart(shift)),
                exitRounded = TimeRounder.roundExit(exitTime),
                entryActual = enterTime.toLocalTime(),
                exitActual = exitTime.toLocalTime()
            )
        }
    }
}

/**
 * Rezultat auto-popunjavanja za notifikaciju.
 */
data class AutoFillResult(
    val year: Int,
    val month: Int,
    val day: Int,
    val shift: Shift,
    val hours: Int,
    val shift2: Shift?,
    val hours2: Int,
    val entryRounded: java.time.LocalTime,
    val exitRounded: java.time.LocalTime,
    val entryActual: java.time.LocalTime,
    val exitActual: java.time.LocalTime
) {
    fun toNotificationText(): String {
        val entryStr = TimeRounder.formatTimeChange(entryActual, entryRounded)
        val exitStr = TimeRounder.formatTimeChange(exitActual, exitRounded)

        val shiftStr = if (shift2 != null) {
            "${shift.displayName} ${hours}h + ${shift2.displayName} ${hours2}h"
        } else {
            "${shift.displayName} ${hours}h"
        }

        return "Ulaz: $entryStr, Izlaz: $exitStr\nUpisano: $shiftStr"
    }
}

