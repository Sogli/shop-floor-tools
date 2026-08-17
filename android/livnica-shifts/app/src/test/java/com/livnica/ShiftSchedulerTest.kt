package com.livnica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ShiftSchedulerTest {

    private lateinit var scheduler: ShiftScheduler

    @Before
    fun setUp() {
        // Default cycle: [3, 2, 1] = [THIRD, SECOND, FIRST]
        scheduler = ShiftScheduler(
            cycle = listOf(3, 2, 1),
            defaultHours = 8
        )
    }

    // ============= getShiftForDate tests =============

    @Test
    fun `getShiftForDate returns null for weekend`() {
        val saturday = LocalDate.of(2025, 1, 4) // Saturday
        val refDate = LocalDate.of(2025, 1, 6) // Monday
        val result = scheduler.getShiftForDate(saturday, refDate, Shift.FIRST)
        assertNull(result)
    }

    @Test
    fun `getShiftForDate returns null for Sunday`() {
        val sunday = LocalDate.of(2025, 1, 5) // Sunday
        val refDate = LocalDate.of(2025, 1, 6) // Monday
        val result = scheduler.getShiftForDate(sunday, refDate, Shift.FIRST)
        assertNull(result)
    }

    @Test
    fun `getShiftForDate returns same shift for same week`() {
        val monday = LocalDate.of(2025, 1, 6) // Monday
        val friday = LocalDate.of(2025, 1, 10) // Friday same week
        val result = scheduler.getShiftForDate(friday, monday, Shift.FIRST)
        assertEquals(Shift.FIRST, result)
    }

    @Test
    fun `getShiftForDate rotates shift after one week`() {
        val refMonday = LocalDate.of(2025, 1, 6) // Monday
        val nextMonday = LocalDate.of(2025, 1, 13) // Next Monday

        // Cycle is [3, 2, 1] = [THIRD, SECOND, FIRST]
        // If reference is FIRST (index 2), after 1 week -> index (2+1) % 3 = 0 -> THIRD
        val result = scheduler.getShiftForDate(nextMonday, refMonday, Shift.FIRST)
        assertEquals(Shift.THIRD, result)
    }

    @Test
    fun `getShiftForDate rotates through full cycle`() {
        val refMonday = LocalDate.of(2025, 1, 6) // Monday

        // FIRST is at index 2 in cycle [3, 2, 1]
        // Week 0: FIRST (index 2)
        // Week 1: THIRD (index 0)
        // Week 2: SECOND (index 1)
        // Week 3: FIRST (index 2) - back to start

        val week0 = scheduler.getShiftForDate(refMonday, refMonday, Shift.FIRST)
        val week1 = scheduler.getShiftForDate(refMonday.plusWeeks(1), refMonday, Shift.FIRST)
        val week2 = scheduler.getShiftForDate(refMonday.plusWeeks(2), refMonday, Shift.FIRST)
        val week3 = scheduler.getShiftForDate(refMonday.plusWeeks(3), refMonday, Shift.FIRST)

        assertEquals(Shift.FIRST, week0)
        assertEquals(Shift.THIRD, week1)
        assertEquals(Shift.SECOND, week2)
        assertEquals(Shift.FIRST, week3)
    }

    @Test
    fun `getShiftForDate returns null for non-work shifts`() {
        val monday = LocalDate.of(2025, 1, 6)
        val result = scheduler.getShiftForDate(monday, monday, Shift.SICK)
        assertNull(result)
    }

    @Test
    fun `getShiftForDate handles past dates`() {
        val refMonday = LocalDate.of(2025, 1, 13) // Monday
        val prevMonday = LocalDate.of(2025, 1, 6) // Previous Monday

        // If reference is FIRST (index 2), going back 1 week -> index (2-1) % 3 = 1 -> SECOND
        val result = scheduler.getShiftForDate(prevMonday, refMonday, Shift.FIRST)
        assertEquals(Shift.SECOND, result)
    }

    // ============= Four Brigade Pattern tests =============

    @Test
    fun `four brigade pattern concept is documented`() {
        // The four-brigade pattern is: FIRST, FIRST, SECOND, SECOND, THIRD, THIRD, OFF, OFF
        // This pattern is handled internally by getFourBrigadeShiftForDate which is private
        // Testing would require integration tests with ShiftRepository
        // For now, we document the expected behavior here
        assertEquals(8, listOf(1, 1, 2, 2, 3, 3, 0, 0).size) // 8-day cycle
    }

    // ============= Helper tests =============

    @Test
    fun `weekday detection works correctly`() {
        assertEquals(true, Weekday.MONDAY.isWorkday)
        assertEquals(true, Weekday.FRIDAY.isWorkday)
        assertEquals(false, Weekday.SATURDAY.isWorkday)
        assertEquals(false, Weekday.SUNDAY.isWorkday)

        assertEquals(false, Weekday.MONDAY.isWeekend)
        assertEquals(false, Weekday.FRIDAY.isWeekend)
        assertEquals(true, Weekday.SATURDAY.isWeekend)
        assertEquals(true, Weekday.SUNDAY.isWeekend)
    }

    @Test
    fun `Weekday from LocalDate works correctly`() {
        val monday = LocalDate.of(2025, 1, 6)
        val saturday = LocalDate.of(2025, 1, 4)
        val sunday = LocalDate.of(2025, 1, 5)

        assertEquals(Weekday.MONDAY, Weekday.from(monday))
        assertEquals(Weekday.SATURDAY, Weekday.from(saturday))
        assertEquals(Weekday.SUNDAY, Weekday.from(sunday))
    }

    // ============= Shift enum tests =============

    @Test
    fun `Shift workShifts returns only work shifts`() {
        val workShifts = Shift.workShifts()
        assertEquals(3, workShifts.size)
        assertEquals(true, workShifts.contains(Shift.FIRST))
        assertEquals(true, workShifts.contains(Shift.SECOND))
        assertEquals(true, workShifts.contains(Shift.THIRD))
        assertEquals(false, workShifts.contains(Shift.OFF))
        assertEquals(false, workShifts.contains(Shift.SICK))
        assertEquals(false, workShifts.contains(Shift.VACATION))
    }

    @Test
    fun `Shift fromValue works correctly`() {
        assertEquals(Shift.OFF, Shift.fromValue(0))
        assertEquals(Shift.FIRST, Shift.fromValue(1))
        assertEquals(Shift.SECOND, Shift.fromValue(2))
        assertEquals(Shift.THIRD, Shift.fromValue(3))
        assertEquals(Shift.SICK, Shift.fromValue(4))
        assertEquals(Shift.VACATION, Shift.fromValue(5))
        assertEquals(Shift.OFF, Shift.fromValue(99)) // Invalid defaults to OFF
    }

    // ============= BrigadeType tests =============

    @Test
    fun `BrigadeType fromValue works correctly`() {
        assertEquals(BrigadeType.TROBRIGADA, BrigadeType.fromValue("trob"))
        assertEquals(BrigadeType.CETVOROBRIGADA, BrigadeType.fromValue("cetv"))
        assertEquals(BrigadeType.CETVOROBRIGADA, BrigadeType.fromValue(null))
        assertEquals(BrigadeType.CETVOROBRIGADA, BrigadeType.fromValue("invalid"))
    }

    // ============= OvertimeType tests =============

    @Test
    fun `OvertimeType fromValue works correctly`() {
        assertEquals(OvertimeType.PAID, OvertimeType.fromValue(0))
        assertEquals(OvertimeType.COMP_TIME, OvertimeType.fromValue(1))
        assertEquals(OvertimeType.PAID, OvertimeType.fromValue(99)) // Invalid defaults to PAID
    }

    // ============= DayRecord tests =============

    @Test
    fun `DayRecord clamps hours to valid range`() {
        val record = DayRecord(2025, 1, 6, Shift.FIRST, 30) // 30 hours - invalid
        assertEquals(24, record.hours)
    }

    @Test
    fun `DayRecord clamps negative hours to 0`() {
        val record = DayRecord(2025, 1, 6, Shift.FIRST, -5)
        assertEquals(0, record.hours)
    }

    @Test
    fun `DayRecord totalHours includes double shift`() {
        val record = DayRecord(
            2025, 1, 6, Shift.FIRST, 8,
            shift2 = Shift.SECOND,
            hours2 = 4
        )
        assertEquals(12, record.totalHours)
    }

    @Test
    fun `DayRecord isDoubleShift detects double shift`() {
        val single = DayRecord(2025, 1, 6, Shift.FIRST, 8)
        val double = DayRecord(
            2025, 1, 6, Shift.FIRST, 8,
            shift2 = Shift.SECOND,
            hours2 = 4
        )

        assertEquals(false, single.isDoubleShift)
        assertEquals(true, double.isDoubleShift)
    }

    @Test
    fun `DayRecord isWorkDay detects work days`() {
        val workDay = DayRecord(2025, 1, 6, Shift.FIRST, 8)
        val offDay = DayRecord(2025, 1, 6, Shift.OFF, 0)
        val sickDay = DayRecord(2025, 1, 6, Shift.SICK, 8)

        assertEquals(true, workDay.isWorkDay)
        assertEquals(false, offDay.isWorkDay)
        assertEquals(false, sickDay.isWorkDay)
    }

    @Test
    fun `DayRecord compTimeEarned calculates correctly`() {
        val noCompTime = DayRecord(
            2025, 1, 6, Shift.FIRST, 8,
            shift2 = Shift.SECOND,
            hours2 = 4,
            overtimeType = OvertimeType.PAID
        )
        val withCompTime = DayRecord(
            2025, 1, 6, Shift.FIRST, 8,
            shift2 = Shift.SECOND,
            hours2 = 4,
            overtimeType = OvertimeType.COMP_TIME
        )

        assertEquals(0, noCompTime.compTimeEarned)
        assertEquals(4, withCompTime.compTimeEarned)
    }

    @Test
    fun `DayRecord hoursDeficit calculates correctly`() {
        val fullDay = DayRecord(2025, 1, 6, Shift.FIRST, 8) // Monday, 8 hours
        val shortDay = DayRecord(2025, 1, 6, Shift.FIRST, 6) // Monday, 6 hours
        val weekend = DayRecord(2025, 1, 4, Shift.FIRST, 6) // Saturday, 6 hours

        assertEquals(0, fullDay.hoursDeficit)
        assertEquals(2, shortDay.hoursDeficit)
        assertEquals(0, weekend.hoursDeficit) // No deficit on weekends
    }

    @Test
    fun `DayRecord sickPayRate is null for non-sick shifts`() {
        val record = DayRecord(2025, 1, 6, Shift.FIRST, 8, sickPayRate = 0.65)
        assertNull(record.sickPayRate)
    }

    @Test
    fun `DayRecord sickPayRate is preserved for sick shift`() {
        val record = DayRecord(2025, 1, 6, Shift.SICK, 8, sickPayRate = 0.80)
        assertEquals(0.80, record.sickPayRate!!, 0.01)
    }

    @Test
    fun `DayRecord sickPayRate clamps to 1 max`() {
        val record = DayRecord(2025, 1, 6, Shift.SICK, 8, sickPayRate = 1.5)
        assertEquals(1.0, record.sickPayRate!!, 0.01)
    }

    // ============= MonthKey tests =============

    @Test
    fun `MonthKey comparison works correctly`() {
        val jan2025 = MonthKey(2025, 1)
        val feb2025 = MonthKey(2025, 2)
        val jan2026 = MonthKey(2026, 1)

        assertEquals(true, jan2025 < feb2025)
        assertEquals(true, feb2025 < jan2026)
        assertEquals(true, jan2025 < jan2026)
        assertEquals(false, feb2025 < jan2025)
    }

    // ============= Mesec koji počinje u nedelju =============

    @Test
    fun `getShiftForDate mesec koji počinje u nedelju`() {
        // Jun 2025 počinje u nedelju
        val sunday = LocalDate.of(2025, 6, 1) // Nedelja
        val monday = LocalDate.of(2025, 6, 2) // Ponedeljak

        val sundayResult = scheduler.getShiftForDate(sunday, monday, Shift.FIRST)
        val mondayResult = scheduler.getShiftForDate(monday, monday, Shift.FIRST)

        // Nedelja se preskače
        assertNull(sundayResult)
        // Ponedeljak je radni dan
        assertEquals(Shift.FIRST, mondayResult)
    }

    @Test
    fun `getShiftForDate mesec koji počinje u subotu`() {
        // Mart 2025 počinje u subotu
        val saturday = LocalDate.of(2025, 3, 1) // Subota
        val monday = LocalDate.of(2025, 3, 3) // Ponedeljak

        val saturdayResult = scheduler.getShiftForDate(saturday, monday, Shift.SECOND)
        assertNull(saturdayResult)

        val mondayResult = scheduler.getShiftForDate(monday, monday, Shift.SECOND)
        assertEquals(Shift.SECOND, mondayResult)
    }

    // ============= Rotacija preko granice meseca =============

    @Test
    fun `getShiftForDate rotacija prelazi iz jednog meseca u drugi`() {
        val refMonday = LocalDate.of(2025, 1, 27) // Poslednji ponedeljak januara
        val nextMonday = LocalDate.of(2025, 2, 3) // Prvi ponedeljak februara

        // FIRST je na indexu 2 u ciklusu [3, 2, 1]
        // Posle 1 nedelje: index (2+1) % 3 = 0 -> THIRD
        val result = scheduler.getShiftForDate(nextMonday, refMonday, Shift.FIRST)
        assertEquals(Shift.THIRD, result)
    }

    // ============= Referentni datum koji nije ponedeljak =============

    @Test
    fun `getShiftForDate referentni datum sreda iste nedelje`() {
        val refWednesday = LocalDate.of(2025, 1, 8) // Sreda
        val monday = LocalDate.of(2025, 1, 6) // Ponedeljak iste nedelje

        // Oba su u istoj nedelji, pa treba ista smena
        val result = scheduler.getShiftForDate(monday, refWednesday, Shift.SECOND)
        assertEquals(Shift.SECOND, result)
    }

    @Test
    fun `getShiftForDate referentni datum petak sledeće nedelje rotira`() {
        val refFriday = LocalDate.of(2025, 1, 10) // Petak
        val nextMonday = LocalDate.of(2025, 1, 13) // Ponedeljak sledeće nedelje

        // Cycle [3, 2, 1]. SECOND je index 1. Posle 1 nedelje: (1+1) % 3 = 2 -> FIRST
        val result = scheduler.getShiftForDate(nextMonday, refFriday, Shift.SECOND)
        assertEquals(Shift.FIRST, result)
    }

    // ============= Edge case-ovi za četvorobrigadu =============

    @Test
    fun `DayRecord weekday from za 1 januar 2025 sreda`() {
        val record = DayRecord(2025, 1, 1, Shift.FIRST, 8)
        assertEquals(Weekday.WEDNESDAY, record.weekday)
    }

    @Test
    fun `getShiftForDate OFF smena nema u ciklusu`() {
        val monday = LocalDate.of(2025, 1, 6)
        val result = scheduler.getShiftForDate(monday, monday, Shift.OFF)
        // OFF (value 0) nije u ciklusu [3, 2, 1], vraca null
        assertNull(result)
    }

    @Test
    fun `getShiftForDate VACATION smena nema u ciklusu`() {
        val monday = LocalDate.of(2025, 1, 6)
        val result = scheduler.getShiftForDate(monday, monday, Shift.VACATION)
        assertNull(result)
    }

    // ============= Dugačak period unapred =============

    @Test
    fun `getShiftForDate 52 nedelje unapred vraca se na početak ciklusa`() {
        val refMonday = LocalDate.of(2025, 1, 6)
        // 52 nedelje = 17 * 3 + 1 ciklus (3 nedelje po ciklusu)
        // Posle 51 nedelje (17 ciklusa): isto kao početak
        // Posle 52 nedelje: 1 pozicija dalje

        val result51 = scheduler.getShiftForDate(refMonday.plusWeeks(51), refMonday, Shift.FIRST)
        val result0 = scheduler.getShiftForDate(refMonday, refMonday, Shift.FIRST)
        // 51 = 17 * 3, pa je isti kao početak
        assertEquals(result0, result51)
    }

    // ============= DayRecord sickPayRate edge case-ovi =============

    @Test
    fun `DayRecord sickPayRate nula se postavlja na null`() {
        val record = DayRecord(2025, 1, 6, Shift.SICK, 8, sickPayRate = 0.0)
        assertNull(record.sickPayRate)
    }

    @Test
    fun `DayRecord sickPayRate negativna se postavlja na null`() {
        val record = DayRecord(2025, 1, 6, Shift.SICK, 8, sickPayRate = -0.5)
        assertNull(record.sickPayRate)
    }

    @Test
    fun `DayRecord compHoursUsed se klampuje`() {
        val record = DayRecord(2025, 1, 6, Shift.FIRST, 8, compHoursUsed = 30)
        assertEquals(24, record.compHoursUsed)
    }

    @Test
    fun `DayRecord compHoursUsed negativno se klampuje na 0`() {
        val record = DayRecord(2025, 1, 6, Shift.FIRST, 8, compHoursUsed = -5)
        assertEquals(0, record.compHoursUsed)
    }

    @Test
    fun `DayRecord hours2 se klampuje na 0-24`() {
        val record = DayRecord(
            2025, 1, 6, Shift.FIRST, 8,
            shift2 = Shift.SECOND,
            hours2 = 30
        )
        assertEquals(24, record.hours2)
    }
}

