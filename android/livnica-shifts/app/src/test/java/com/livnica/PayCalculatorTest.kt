package com.livnica

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PayCalculatorTest {

    private lateinit var calculator: PayCalculator
    private lateinit var config: PayConfig

    @Before
    fun setUp() {
        config = PayConfig(
            defaultBasePay = 300.0,
            defaultHours = 8,
            shift3Premium = 1.41,
            overtimePremium = 1.41,
            sundayBonus = 0.25,
            sickPayRate = 0.65,
            vacationPayRate = 1.0,
            holidayBonus = 1.23,
            foodPerDay = 280.0,
            minuliRadPerYear = 0.005
        )
        calculator = PayCalculator(config)
    }

    // ============= getSundayHoursForShift tests =============

    @Test
    fun `getSundayHoursForShift returns 0 for CETVOROBRIGADA`() {
        val result = calculator.getSundayHoursForShift(
            Weekday.SUNDAY,
            Shift.FIRST,
            8,
            BrigadeType.CETVOROBRIGADA
        )
        assertEquals(0, result)
    }

    @Test
    fun `getSundayHoursForShift returns 0 for sick shift`() {
        val result = calculator.getSundayHoursForShift(
            Weekday.SUNDAY,
            Shift.SICK,
            8,
            BrigadeType.TROBRIGADA
        )
        assertEquals(0, result)
    }

    @Test
    fun `getSundayHoursForShift returns 0 for vacation shift`() {
        val result = calculator.getSundayHoursForShift(
            Weekday.SUNDAY,
            Shift.VACATION,
            8,
            BrigadeType.TROBRIGADA
        )
        assertEquals(0, result)
    }

    @Test
    fun `getSundayHoursForShift returns full hours for first shift on Sunday`() {
        val result = calculator.getSundayHoursForShift(
            Weekday.SUNDAY,
            Shift.FIRST,
            8,
            BrigadeType.TROBRIGADA
        )
        assertEquals(8, result)
    }

    @Test
    fun `getSundayHoursForShift returns full hours for second shift on Sunday`() {
        val result = calculator.getSundayHoursForShift(
            Weekday.SUNDAY,
            Shift.SECOND,
            8,
            BrigadeType.TROBRIGADA
        )
        assertEquals(8, result)
    }

    @Test
    fun `getSundayHoursForShift returns 2 hours max for third shift on Sunday`() {
        val result = calculator.getSundayHoursForShift(
            Weekday.SUNDAY,
            Shift.THIRD,
            8,
            BrigadeType.TROBRIGADA
        )
        assertEquals(2, result)
    }

    @Test
    fun `getSundayHoursForShift returns hours minus 2 for third shift on Saturday`() {
        val result = calculator.getSundayHoursForShift(
            Weekday.SATURDAY,
            Shift.THIRD,
            8,
            BrigadeType.TROBRIGADA
        )
        assertEquals(6, result)
    }

    @Test
    fun `getSundayHoursForShift returns 0 for first shift on Saturday`() {
        val result = calculator.getSundayHoursForShift(
            Weekday.SATURDAY,
            Shift.FIRST,
            8,
            BrigadeType.TROBRIGADA
        )
        assertEquals(0, result)
    }

    @Test
    fun `getSundayHoursForShift returns 0 for weekday`() {
        val result = calculator.getSundayHoursForShift(
            Weekday.MONDAY,
            Shift.FIRST,
            8,
            BrigadeType.TROBRIGADA
        )
        assertEquals(0, result)
    }

    // ============= getOvertimeHours tests =============

    @Test
    fun `getOvertimeHours returns 0 for sick shift`() {
        val record = DayRecord(2025, 1, 6, Shift.SICK, 8) // Monday
        val result = calculator.getOvertimeHours(record, BrigadeType.TROBRIGADA)
        assertEquals(0, result)
    }

    @Test
    fun `getOvertimeHours returns 0 for vacation shift`() {
        val record = DayRecord(2025, 1, 6, Shift.VACATION, 8) // Monday
        val result = calculator.getOvertimeHours(record, BrigadeType.TROBRIGADA)
        assertEquals(0, result)
    }

    @Test
    fun `getOvertimeHours returns 0 for off shift`() {
        val record = DayRecord(2025, 1, 6, Shift.OFF, 0) // Monday
        val result = calculator.getOvertimeHours(record, BrigadeType.TROBRIGADA)
        assertEquals(0, result)
    }

    @Test
    fun `getOvertimeHours returns 0 for regular 8 hour weekday`() {
        val record = DayRecord(2025, 1, 6, Shift.FIRST, 8) // Monday
        val result = calculator.getOvertimeHours(record, BrigadeType.TROBRIGADA)
        assertEquals(0, result)
    }

    @Test
    fun `getOvertimeHours returns 2 for 10 hour weekday`() {
        val record = DayRecord(2025, 1, 6, Shift.FIRST, 10) // Monday
        val result = calculator.getOvertimeHours(record, BrigadeType.TROBRIGADA)
        assertEquals(2, result)
    }

    @Test
    fun `getOvertimeHours returns full hours for weekend work TROBRIGADA`() {
        val record = DayRecord(2025, 1, 4, Shift.FIRST, 8) // Saturday
        val result = calculator.getOvertimeHours(record, BrigadeType.TROBRIGADA)
        assertEquals(8, result)
    }

    @Test
    fun `getOvertimeHours returns 0 for weekend work CETVOROBRIGADA when under 8 hours`() {
        val record = DayRecord(2025, 1, 4, Shift.FIRST, 8) // Saturday
        val result = calculator.getOvertimeHours(record, BrigadeType.CETVOROBRIGADA)
        assertEquals(0, result)
    }

    // ============= getFoodAllowance tests =============

    @Test
    fun `getFoodAllowance returns 0 for sick shift`() {
        val record = DayRecord(2025, 1, 6, Shift.SICK, 8)
        val result = calculator.getFoodAllowance(record)
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `getFoodAllowance returns 0 for vacation shift`() {
        val record = DayRecord(2025, 1, 6, Shift.VACATION, 8)
        val result = calculator.getFoodAllowance(record)
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `getFoodAllowance returns 0 for off shift`() {
        val record = DayRecord(2025, 1, 6, Shift.OFF, 0)
        val result = calculator.getFoodAllowance(record)
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `getFoodAllowance returns foodPerDay for work shift`() {
        val record = DayRecord(2025, 1, 6, Shift.FIRST, 8)
        val result = calculator.getFoodAllowance(record)
        assertEquals(280.0, result, 0.01)
    }

    // ============= getNightHours tests =============

    @Test
    fun `getNightHours returns hours for third shift`() {
        val record = DayRecord(2025, 1, 6, Shift.THIRD, 8)
        val result = calculator.getNightHours(record)
        assertEquals(8, result)
    }

    @Test
    fun `getNightHours returns 0 for first shift`() {
        val record = DayRecord(2025, 1, 6, Shift.FIRST, 8)
        val result = calculator.getNightHours(record)
        assertEquals(0, result)
    }

    @Test
    fun `getNightHours returns 0 for second shift`() {
        val record = DayRecord(2025, 1, 6, Shift.SECOND, 8)
        val result = calculator.getNightHours(record)
        assertEquals(0, result)
    }

    // ============= calculateDailyPay tests =============

    @Test
    fun `calculateDailyPay returns 0 for off day on weekend`() {
        val record = DayRecord(2025, 1, 4, Shift.OFF, 0) // Saturday
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay returns sick pay for sick weekday`() {
        val record = DayRecord(2025, 1, 6, Shift.SICK, 8) // Monday
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // 8 * 300 * 0.65 = 1560
        assertEquals(1560.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay returns 0 for sick weekend`() {
        val record = DayRecord(2025, 1, 4, Shift.SICK, 8) // Saturday
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay returns vacation pay for vacation weekday`() {
        val record = DayRecord(2025, 1, 6, Shift.VACATION, 8) // Monday
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // 8 * 300 * 1.0 = 2400
        assertEquals(2400.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay calculates regular first shift weekday`() {
        val record = DayRecord(2025, 1, 6, Shift.FIRST, 8) // Monday
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // 8 * 300 = 2400
        assertEquals(2400.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay calculates third shift with night premium`() {
        val record = DayRecord(2025, 1, 6, Shift.THIRD, 8) // Monday
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // 8 * 300 * 1.41 = 3384
        assertEquals(3384.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay calculates overtime on weekday`() {
        val record = DayRecord(2025, 1, 6, Shift.FIRST, 10) // Monday, 10 hours
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // Regular: 8 * 300 = 2400
        // Overtime: 2 * 300 * 1.41 = 846
        // Total: 3246
        assertEquals(3246.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay calculates weekend work with overtime premium`() {
        val record = DayRecord(2025, 1, 4, Shift.FIRST, 8) // Saturday
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // 8 * 300 * 1.41 = 3384
        assertEquals(3384.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay calculates Sunday with Sunday bonus`() {
        val record = DayRecord(2025, 1, 5, Shift.FIRST, 8) // Sunday
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // Base overtime: 8 * 300 * 1.41 = 3384
        // Sunday bonus: 8 * 300 * 1.41 * 0.25 = 846
        // Total: 4230
        assertEquals(4230.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay uses custom sick pay rate`() {
        val record = DayRecord(2025, 1, 6, Shift.SICK, 8, sickPayRate = 1.0) // Monday
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // 8 * 300 * 1.0 = 2400
        assertEquals(2400.0, result, 0.01)
    }

    // ============= calculateMonthSummary tests =============

    @Test
    fun `calculateMonthSummary returns empty summary for empty records`() {
        val result = calculator.calculateMonthSummary(emptyList(), 300.0)
        assertEquals(0, result.workHours)
        assertEquals(0.0, result.totalPay, 0.01)
    }

    @Test
    fun `calculateMonthSummary calculates work hours correctly`() {
        val records = listOf(
            DayRecord(2025, 1, 6, Shift.FIRST, 8), // Monday
            DayRecord(2025, 1, 7, Shift.FIRST, 8), // Tuesday
            DayRecord(2025, 1, 8, Shift.FIRST, 8)  // Wednesday
        )
        val result = calculator.calculateMonthSummary(records, 300.0)
        assertEquals(24, result.workHours)
        assertEquals(3, result.workDays)
    }

    @Test
    fun `calculateMonthSummary calculates sick hours correctly`() {
        val records = listOf(
            DayRecord(2025, 1, 6, Shift.SICK, 8), // Monday
            DayRecord(2025, 1, 7, Shift.SICK, 8)  // Tuesday
        )
        val result = calculator.calculateMonthSummary(records, 300.0)
        assertEquals(16, result.sickHours)
        assertEquals(0, result.workHours)
    }

    @Test
    fun `calculateMonthSummary calculates vacation hours correctly`() {
        val records = listOf(
            DayRecord(2025, 1, 6, Shift.VACATION, 8), // Monday
            DayRecord(2025, 1, 7, Shift.VACATION, 8)  // Tuesday
        )
        val result = calculator.calculateMonthSummary(records, 300.0)
        assertEquals(16, result.vacationHours)
        assertEquals(0, result.workHours)
    }

    @Test
    fun `calculateMonthSummary calculates food allowance correctly`() {
        val records = listOf(
            DayRecord(2025, 1, 6, Shift.FIRST, 8), // Monday
            DayRecord(2025, 1, 7, Shift.FIRST, 8), // Tuesday
            DayRecord(2025, 1, 8, Shift.SICK, 8)   // Wednesday - no food
        )
        val result = calculator.calculateMonthSummary(records, 300.0)
        // 2 work days * 280 = 560
        assertEquals(560.0, result.foodAllowance, 0.01)
    }

    @Test
    fun `calculateMonthSummary calculates minuli bonus correctly`() {
        val records = listOf(
            DayRecord(2025, 1, 6, Shift.FIRST, 8) // Monday
        )
        val result = calculator.calculateMonthSummary(
            records,
            300.0,
            yearsOfService = 10
        )
        // Pay: 2400, minuliRate: 10 * 0.005 = 0.05
        // Bonus: 2400 * 0.05 = 120
        assertEquals(120.0, result.minuliRadBonus, 0.01)
    }

    @Test
    fun `calculateMonthSummary calculates night hours correctly`() {
        val records = listOf(
            DayRecord(2025, 1, 6, Shift.THIRD, 8), // Monday night
            DayRecord(2025, 1, 7, Shift.FIRST, 8)  // Tuesday day
        )
        val result = calculator.calculateMonthSummary(records, 300.0)
        assertEquals(8, result.nightHours)
    }

    @Test
    fun `calculateMonthSummary tracks comp time balance`() {
        val records = listOf(
            DayRecord(
                2025, 1, 6, Shift.FIRST, 8,
                shift2 = Shift.SECOND,
                hours2 = 4,
                overtimeType = OvertimeType.COMP_TIME
            )
        )
        val result = calculator.calculateMonthSummary(
            records,
            300.0,
            startingCompBalance = 10
        )
        assertEquals(10, result.compTimeStarting)
        assertEquals(4, result.compTimeEarned)
        assertEquals(14, result.compTimeEnding)
    }

    // ============= CETVOROBRIGADA Sunday comp time kombinacija =============

    @Test
    fun `calculateDailyPay CETVOROBRIGADA nedelja nema overtime premium`() {
        // Za CETVOROBRIGADA, vikend se računa kao normalan radni dan
        val record = DayRecord(2025, 1, 5, Shift.FIRST, 8) // Nedelja
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.CETVOROBRIGADA)
        // CETVOROBRIGADA: vikend je normalan dan, 8 * 300 = 2400
        assertEquals(2400.0, result, 0.01)
    }

    @Test
    fun `getOvertimeHours CETVOROBRIGADA vikend prekovremeni samo preko 8 sati`() {
        val record = DayRecord(2025, 1, 4, Shift.FIRST, 10) // Subota, 10 sati
        val result = calculator.getOvertimeHours(record, BrigadeType.CETVOROBRIGADA)
        // CETVOROBRIGADA: vikend se tretira kao radni dan, overtime = 10 - 8 = 2
        assertEquals(2, result)
    }

    @Test
    fun `getSundayHours CETVOROBRIGADA uvek vraca 0`() {
        val record = DayRecord(2025, 1, 5, Shift.FIRST, 8) // Nedelja
        val result = calculator.getSundayHours(record, BrigadeType.CETVOROBRIGADA)
        assertEquals(0, result)
    }

    @Test
    fun `calculateMonthSummary CETVOROBRIGADA nedelja comp time`() {
        val records = listOf(
            DayRecord(
                2025, 1, 5, Shift.FIRST, 8, // Nedelja
                weekendOvertimeType = OvertimeType.COMP_TIME
            )
        )
        val result = calculator.calculateMonthSummary(
            records,
            300.0,
            brigadeType = BrigadeType.CETVOROBRIGADA
        )
        // CETVOROBRIGADA + vikend comp time: sati se zarađuju kao comp time
        assertEquals(8, result.compTimeEarned)
    }

    // ============= Holiday pay sa različitim brigade tipovima =============

    @Test
    fun `calculateDailyPay praznik TROBRIGADA radni dan`() {
        // 7. januar 2025 je utorak (Božić)
        val record = DayRecord(2025, 1, 7, Shift.FIRST, 8)
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // Regularno: 8 * 300 = 2400
        // Holiday bonus: 2400 * 1.23 = 2952
        // Ukupno: 2400 + 2952 = 5352
        assertEquals(5352.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay praznik CETVOROBRIGADA radni dan`() {
        // 7. januar 2025 je utorak (Božić)
        val record = DayRecord(2025, 1, 7, Shift.FIRST, 8)
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.CETVOROBRIGADA)
        // Isto kao TROBRIGADA za radne dane
        assertEquals(5352.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay slobodan dan na praznik radnog dana`() {
        // 7. januar 2025 je utorak (Božić), OFF smena
        val record = DayRecord(2025, 1, 7, Shift.OFF, 0)
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // OFF na praznik radnog dana: 8 * 300 = 2400
        assertEquals(2400.0, result, 0.01)
    }

    // ============= Treća smena + nedelja + praznik kombinacija =============

    @Test
    fun `calculateDailyPay treća smena nedelja TROBRIGADA`() {
        val record = DayRecord(2025, 1, 5, Shift.THIRD, 8) // Nedelja
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // Vikend + noćna: 8 * 300 * 1.41 * 1.41 = 4751.28
        // Sunday bonus: 2 sata nedelje (THIRD na nedelju = min(8,2) = 2)
        // Sunday: 2 * 300 * 1.41 * 1.41 * 0.25 = 296.955
        // Ukupno: 4751.28 + 296.955 = 5048.235
        assertEquals(5048.235, result, 0.01)
    }

    @Test
    fun `calculateDailyPay treća smena subota TROBRIGADA`() {
        val record = DayRecord(2025, 1, 4, Shift.THIRD, 8) // Subota
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // Vikend + noćna: 8 * 300 * 1.41 * 1.41 = 4751.28
        // Sunday bonus: 6 sati nedelje (THIRD na subotu = max(0, 8-2) = 6)
        // Sunday: 6 * 300 * 1.41 * 1.41 * 0.25 = 890.865
        // Ukupno: 4751.28 + 890.865 = 5642.145
        assertEquals(5642.145, result, 0.01)
    }

    // ============= COMP_TIME + Holiday kombinacija =============

    @Test
    fun `calculateMonthSummary comp time vikend sa praznikom`() {
        // 1. januar 2025 je sreda - koristimo 2. februar koji je nedelja u 2025
        // Zapravo, koristimo zapis sa holidayOverride za testiranje
        val records = listOf(
            DayRecord(
                2025, 1, 5, Shift.FIRST, 8, // Nedelja
                weekendOvertimeType = OvertimeType.COMP_TIME,
                holidayOverride = true
            )
        )
        val result = calculator.calculateMonthSummary(
            records,
            300.0,
            brigadeType = BrigadeType.TROBRIGADA
        )
        // Vikend comp time: sati idu u comp time, ne u platu
        assertEquals(8, result.compTimeEarned)
    }

    // ============= Double shift plata testovi =============

    @Test
    fun `calculateDailyPay dupla smena radni dan`() {
        val record = DayRecord(
            2025, 1, 6, Shift.FIRST, 8, // Ponedeljak
            shift2 = Shift.SECOND,
            hours2 = 4,
            overtimeType = OvertimeType.PAID
        )
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // Regularno: 8 * 300 = 2400 (prva smena)
        // Overtime: 4 * 300 * 1.41 = 1692 (druga smena, svi sati prekovremeni)
        // Ukupno: 4092
        assertEquals(4092.0, result, 0.01)
    }

    @Test
    fun `calculateDailyPay dupla smena comp time ne ide u platu`() {
        val record = DayRecord(
            2025, 1, 6, Shift.FIRST, 8, // Ponedeljak
            shift2 = Shift.SECOND,
            hours2 = 4,
            overtimeType = OvertimeType.COMP_TIME
        )
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // Samo prva smena: 8 * 300 = 2400
        assertEquals(2400.0, result, 0.01)
    }

    // ============= Neupisani praznici =============

    @Test
    fun `calculateUnrecordedHolidayPay računa neupisane praznike`() {
        // Januar 2025: 1. i 2. su praznici (sreda i četvrtak), 7. je Božić (utorak)
        val recordedDays = setOf(7) // Samo Božić je upisan
        val result = calculator.calculateUnrecordedHolidayPay(
            2025, 1, recordedDays, 300.0, BrigadeType.TROBRIGADA
        )
        // Neupisani praznici na radnim danima: 1. jan (sreda), 2. jan (četvrtak)
        // 2 * 8 * 300 = 4800
        assertEquals(4800.0, result, 0.01)
    }

    @Test
    fun `calculateUnrecordedHolidayPay CETVOROBRIGADA vikend praznik se ne plaća`() {
        // Februar 2025: 15. je subota (Dan državnosti)
        val recordedDays = emptySet<Int>()
        val result = calculator.calculateUnrecordedHolidayPay(
            2025, 2, recordedDays, 300.0, BrigadeType.CETVOROBRIGADA
        )
        // 15. feb je subota - CETVOROBRIGADA vikend praznici se ne plaćaju
        // 16. feb je nedelja - isto se ne plaća
        // 17. feb je ponedeljak - plaća se: 8 * 300 = 2400
        assertEquals(2400.0, result, 0.01)
    }

    @Test
    fun `calculateUnrecordedHolidayPay ne računa vikend praznike za TROBRIGADA`() {
        // Februar 2025: 15. je subota, 16. nedelja
        val recordedDays = emptySet<Int>()
        val result = calculator.calculateUnrecordedHolidayPay(
            2025, 2, recordedDays, 300.0, BrigadeType.TROBRIGADA
        )
        // 15. feb subota - nije radni dan, preskače se
        // 16. feb nedelja - nije radni dan, preskače se
        // 17. feb ponedeljak - jeste radni dan: 8 * 300 = 2400
        assertEquals(2400.0, result, 0.01)
    }

    // ============= Weekend comp time testovi =============

    @Test
    fun `getOvertimeHours vikend comp time ne broji prvu smenu`() {
        val record = DayRecord(
            2025, 1, 4, Shift.FIRST, 8, // Subota
            weekendOvertimeType = OvertimeType.COMP_TIME
        )
        val result = calculator.getOvertimeHours(record, BrigadeType.TROBRIGADA)
        // Weekend comp time: prva smena ne ide u overtime
        assertEquals(0, result)
    }

    @Test
    fun `getSundayHours vikend comp time ne broji prvu smenu`() {
        val record = DayRecord(
            2025, 1, 5, Shift.FIRST, 8, // Nedelja
            weekendOvertimeType = OvertimeType.COMP_TIME
        )
        val result = calculator.getSundayHours(record, BrigadeType.TROBRIGADA)
        assertEquals(0, result)
    }

    @Test
    fun `calculateDailyPay vikend comp time ne uključuje prvu smenu`() {
        val record = DayRecord(
            2025, 1, 4, Shift.FIRST, 8, // Subota
            weekendOvertimeType = OvertimeType.COMP_TIME
        )
        val result = calculator.calculateDailyPay(record, 300.0, BrigadeType.TROBRIGADA)
        // Weekend comp time: prva smena ne ide u platu
        assertEquals(0.0, result, 0.01)
    }
}

