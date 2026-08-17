package com.livnica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShiftRepositoryTest {

    private lateinit var repo: ShiftRepository

    @Before
    fun setUp() {
        repo = ShiftRepository()
    }

    // ============= Osnovne CRUD operacije =============

    @Test
    fun `set i get vraćaju isti zapis`() {
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        val record = repo.get(2025, 1, 6)
        assertNotNull(record)
        assertEquals(Shift.FIRST, record!!.shift)
        assertEquals(8, record.hours)
    }

    @Test
    fun `get vraca null za nepostojeci zapis`() {
        val record = repo.get(2025, 1, 6)
        assertNull(record)
    }

    @Test
    fun `set prepisuje postojeći zapis`() {
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        repo.set(2025, 1, 6, Shift.SECOND, 6)
        val record = repo.get(2025, 1, 6)
        assertNotNull(record)
        assertEquals(Shift.SECOND, record!!.shift)
        assertEquals(6, record.hours)
    }

    @Test
    fun `delete briše postojeći zapis`() {
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        val deleted = repo.delete(2025, 1, 6)
        assertTrue(deleted)
        assertNull(repo.get(2025, 1, 6))
    }

    @Test
    fun `delete vraca false za nepostojeci zapis`() {
        val deleted = repo.delete(2025, 1, 6)
        assertFalse(deleted)
    }

    // ============= Mesečni zapisi =============

    @Test
    fun `getMonthRecords vraca sve zapise za mesec`() {
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        repo.set(2025, 1, 7, Shift.SECOND, 8)
        repo.set(2025, 2, 3, Shift.THIRD, 8)

        val janRecords = repo.getMonthRecords(2025, 1)
        assertEquals(2, janRecords.size)

        val febRecords = repo.getMonthRecords(2025, 2)
        assertEquals(1, febRecords.size)
    }

    @Test
    fun `getMonthRecords vraca praznu listu za mesec bez zapisa`() {
        val records = repo.getMonthRecords(2025, 3)
        assertTrue(records.isEmpty())
    }

    @Test
    fun `deleteMonth briše sve zapise za mesec`() {
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        repo.set(2025, 1, 7, Shift.SECOND, 8)
        repo.set(2025, 2, 3, Shift.THIRD, 8)

        val count = repo.deleteMonth(2025, 1)
        assertEquals(2, count)
        assertTrue(repo.getMonthRecords(2025, 1).isEmpty())
        assertEquals(1, repo.getMonthRecords(2025, 2).size)
    }

    @Test
    fun `deleteAllData briše sve zapise`() {
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        repo.set(2025, 2, 3, Shift.THIRD, 8)

        val count = repo.deleteAllData()
        assertEquals(2, count)
        assertTrue(repo.getMonthRecords(2025, 1).isEmpty())
        assertTrue(repo.getMonthRecords(2025, 2).isEmpty())
    }

    // ============= setBatch testovi =============

    @Test
    fun `setBatch dodaje više zapisa odjednom`() {
        val batch = listOf(
            BatchRecord(2025, 1, 6, Shift.FIRST, 8),
            BatchRecord(2025, 1, 7, Shift.SECOND, 8),
            BatchRecord(2025, 1, 8, Shift.THIRD, 8)
        )
        repo.setBatch(batch)

        assertEquals(3, repo.getMonthRecords(2025, 1).size)
        assertEquals(Shift.FIRST, repo.get(2025, 1, 6)!!.shift)
        assertEquals(Shift.SECOND, repo.get(2025, 1, 7)!!.shift)
        assertEquals(Shift.THIRD, repo.get(2025, 1, 8)!!.shift)
    }

    @Test
    fun `setBatch prepisuje postojeće zapise`() {
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        val batch = listOf(
            BatchRecord(2025, 1, 6, Shift.THIRD, 8)
        )
        repo.setBatch(batch)
        assertEquals(Shift.THIRD, repo.get(2025, 1, 6)!!.shift)
    }

    // ============= Cache invalidation testovi =============

    @Test
    fun `getSummary se ažurira posle set poziva`() {
        repo.set(2025, 1, 6, Shift.FIRST, 8) // Ponedeljak
        val summary1 = repo.getSummary(2025, 1)
        assertEquals(8, summary1.workHours)

        repo.set(2025, 1, 7, Shift.FIRST, 8) // Utorak
        val summary2 = repo.getSummary(2025, 1)
        assertEquals(16, summary2.workHours)
    }

    @Test
    fun `getSummary se ažurira posle delete poziva`() {
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        repo.set(2025, 1, 7, Shift.SECOND, 8)
        assertEquals(16, repo.getSummary(2025, 1).workHours)

        repo.delete(2025, 1, 7)
        assertEquals(8, repo.getSummary(2025, 1).workHours)
    }

    @Test
    fun `getSummary prazan mesec vraca nula sate`() {
        val summary = repo.getSummary(2025, 3)
        assertEquals(0, summary.workHours)
        assertEquals(0.0, summary.totalPay, 0.01)
    }

    // ============= Comp balance keš testovi =============

    @Test
    fun `getCompBalanceAtMonthStart koristi initialCompBalance`() {
        repo.initialCompBalanceValue = 20
        val balance = repo.getCompBalanceAtMonthStart(2025, 1)
        assertEquals(20, balance)
    }

    @Test
    fun `getCompBalanceAtMonthStart akumulira iz prethodnih meseci`() {
        repo.initialCompBalanceValue = 10

        // Januar: zaradimo 8 sati comp time (vikend)
        repo.set(
            2025, 1, 4, Shift.FIRST, 8,
            weekendOvertimeType = OvertimeType.COMP_TIME
        )

        // Februar: počinjemo sa 10 + 8 = 18
        val febBalance = repo.getCompBalanceAtMonthStart(2025, 2)
        assertEquals(18, febBalance)
    }

    @Test
    fun `comp balance keš se invalidira posle promene zapisa`() {
        repo.initialCompBalanceValue = 10
        repo.set(
            2025, 1, 4, Shift.FIRST, 8,
            weekendOvertimeType = OvertimeType.COMP_TIME
        )
        // Puni keš
        assertEquals(18, repo.getCompBalanceAtMonthStart(2025, 2))

        // Brišemo januar zapis - keš treba da se invalidira
        repo.delete(2025, 1, 4)
        assertEquals(10, repo.getCompBalanceAtMonthStart(2025, 2))
    }

    // ============= Konfiguracija testovi =============

    @Test
    fun `basePayValue podrazumevana vrednost`() {
        assertEquals(CONFIG.pay.defaultBasePay, repo.basePayValue, 0.01)
    }

    @Test
    fun `basePayValue promena invalidira sumare`() {
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        val summary1 = repo.getSummary(2025, 1)

        repo.basePayValue = 500.0
        val summary2 = repo.getSummary(2025, 1)

        // Plata mora biti različita jer se bazna satnica promenila
        assertTrue(
            "Plata se mora promeniti posle promene base pay",
            summary1.totalPay != summary2.totalPay
        )
    }

    @Test
    fun `basePayValue odbija negativnu vrednost`() {
        val original = repo.basePayValue
        repo.basePayValue = -100.0
        assertEquals(original, repo.basePayValue, 0.01)
    }

    @Test
    fun `basePayValue odbija NaN`() {
        val original = repo.basePayValue
        repo.basePayValue = Double.NaN
        assertEquals(original, repo.basePayValue, 0.01)
    }

    @Test
    fun `basePayValue odbija Infinity`() {
        val original = repo.basePayValue
        repo.basePayValue = Double.POSITIVE_INFINITY
        assertEquals(original, repo.basePayValue, 0.01)
    }

    @Test
    fun `foodPerDayValue prihvata nulu`() {
        repo.foodPerDayValue = 0.0
        assertEquals(0.0, repo.foodPerDayValue, 0.01)
    }

    @Test
    fun `foodPerDayValue odbija negativnu vrednost`() {
        val original = repo.foodPerDayValue
        repo.foodPerDayValue = -50.0
        assertEquals(original, repo.foodPerDayValue, 0.01)
    }

    @Test
    fun `initialCompBalanceValue klampuje na minimum 0`() {
        repo.initialCompBalanceValue = -10
        assertEquals(0, repo.initialCompBalanceValue)
    }

    @Test
    fun `yearsOfServiceValue klampuje na minimum 0`() {
        repo.yearsOfServiceValue = -5
        assertEquals(0, repo.yearsOfServiceValue)
    }

    @Test
    fun `brigadeTypeValue promena invalidira sumare`() {
        repo.set(2025, 1, 4, Shift.FIRST, 8) // Subota
        val summaryTrob = repo.getSummary(2025, 1)

        repo.brigadeTypeValue = BrigadeType.CETVOROBRIGADA
        val summaryCetv = repo.getSummary(2025, 1)

        // Overtime se razlikuje između brigada na vikendu
        assertTrue(
            "Overtime se mora razlikovati po tipu brigade",
            summaryTrob.overtimeHours != summaryCetv.overtimeHours
        )
    }

    // ============= getAllMonthsSorted testovi =============

    @Test
    fun `getAllMonthsSorted vraća sortiranu listu`() {
        repo.set(2025, 3, 3, Shift.FIRST, 8)
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        repo.set(2024, 12, 1, Shift.FIRST, 8)

        val months = repo.getAllMonthsSorted()
        assertEquals(3, months.size)
        assertEquals(MonthKey(2024, 12), months[0])
        assertEquals(MonthKey(2025, 1), months[1])
        assertEquals(MonthKey(2025, 3), months[2])
    }

    @Test
    fun `getAllMonthsSorted prazna lista za prazan repo`() {
        assertTrue(repo.getAllMonthsSorted().isEmpty())
    }

    // ============= onChange callback testovi =============

    @Test
    fun `onChange callback se poziva posle set`() {
        var called = false
        repo.onChange { called = true }
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        assertTrue("Callback mora biti pozvan posle set()", called)
    }

    @Test
    fun `onChange callback se poziva posle delete`() {
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        var called = false
        repo.onChange { called = true }
        repo.delete(2025, 1, 6)
        assertTrue("Callback mora biti pozvan posle delete()", called)
    }

    @Test
    fun `removeOnChange uklanja callback`() {
        var callCount = 0
        val token = repo.onChange { callCount++ }
        repo.set(2025, 1, 6, Shift.FIRST, 8)
        assertEquals(1, callCount)

        repo.removeOnChange(token)
        repo.set(2025, 1, 7, Shift.SECOND, 8)
        assertEquals(1, callCount) // Nije pozvano ponovo
    }

    // ============= Konkurentni pristup testovi =============

    @Test
    fun `konkurentni set pozivi ne gube zapise`() {
        val threads = (1..10).map { threadId ->
            Thread {
                for (day in 1..28) {
                    repo.set(2025, threadId.coerceIn(1, 12), day, Shift.FIRST, 8)
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Svaki thread upisuje 28 zapisa u svoj mesec
        for (month in 1..10) {
            val records = repo.getMonthRecords(2025, month)
            assertEquals(
                "Mesec $month mora imati 28 zapisa",
                28,
                records.size
            )
        }
    }

    @Test
    fun `konkurentni set i getSummary ne prave crash`() {
        // Popuni inicijalne podatke
        for (day in listOf(6, 7, 8, 9, 10)) { // Pon-Pet
            repo.set(2025, 1, day, Shift.FIRST, 8)
        }

        val writerThread = Thread {
            for (i in 1..100) {
                repo.set(2025, 1, 6, Shift.values()[i % 3 + 1], 8)
            }
        }
        val readerThread = Thread {
            for (i in 1..100) {
                repo.getSummary(2025, 1) // Ne sme da baci exception
            }
        }

        writerThread.start()
        readerThread.start()
        writerThread.join()
        readerThread.join()
        // Ako smo stigli dovde bez exception-a, test prolazi
    }

    // ============= JSON import/export testovi =============

    @Test
    fun `exportJson i importFromJson roundtrip`() {
        repo.basePayValue = 500.0
        repo.foodPerDayValue = 300.0
        repo.yearsOfServiceValue = 5
        repo.initialCompBalanceValue = 10
        repo.brigadeTypeValue = BrigadeType.CETVOROBRIGADA

        repo.set(2025, 1, 6, Shift.FIRST, 8)
        repo.set(2025, 1, 7, Shift.SICK, 8, sickPayRate = 0.80)
        repo.set(
            2025, 1, 8, Shift.SECOND, 8,
            shift2 = Shift.THIRD,
            hours2 = 4,
            overtimeType = OvertimeType.COMP_TIME
        )

        val json = repo.exportJson()
        assertNotNull(json)
        assertTrue(json.isNotEmpty())

        // Uvezi u novi repo
        val repo2 = ShiftRepository()
        val success = repo2.importFromJson(json)
        assertTrue("Import mora uspeti", success)

        assertEquals(500.0, repo2.basePayValue, 0.01)
        assertEquals(300.0, repo2.foodPerDayValue, 0.01)
        assertEquals(5, repo2.yearsOfServiceValue)
        assertEquals(10, repo2.initialCompBalanceValue)
        assertEquals(BrigadeType.CETVOROBRIGADA, repo2.brigadeTypeValue)

        val rec1 = repo2.get(2025, 1, 6)
        assertNotNull(rec1)
        assertEquals(Shift.FIRST, rec1!!.shift)

        val rec2 = repo2.get(2025, 1, 7)
        assertNotNull(rec2)
        assertEquals(Shift.SICK, rec2!!.shift)

        val rec3 = repo2.get(2025, 1, 8)
        assertNotNull(rec3)
        assertEquals(Shift.SECOND, rec3!!.shift)
        assertEquals(Shift.THIRD, rec3.shift2)
        assertEquals(4, rec3.hours2)
    }

    @Test
    fun `importFromJson sa nevalidnim JSON vraća false`() {
        val result = repo.importFromJson("ovo nije JSON")
        assertFalse(result)
    }

    @Test
    fun `importFromJson sa praznim objektom ne puca`() {
        val result = repo.importFromJson("{}")
        assertTrue(result)
        assertTrue(repo.getMonthRecords(2025, 1).isEmpty())
    }

    // ============= Vacation balance testovi =============

    @Test
    fun `vacation balance prati korišćene dane`() {
        repo.initialVacationBalanceValue = 20.0
        repo.set(2025, 1, 6, Shift.VACATION, 8)
        repo.set(2025, 1, 7, Shift.VACATION, 8)

        assertEquals(2, repo.getVacationDaysUsed())
        assertEquals(18.0, repo.getVacationDaysRemaining(), 0.01)
        assertFalse(repo.isVacationExceeded())
    }

    @Test
    fun `vacation exceeded kad se prekorači limit`() {
        repo.initialVacationBalanceValue = 1.0
        repo.set(2025, 1, 6, Shift.VACATION, 8)
        repo.set(2025, 1, 7, Shift.VACATION, 8)

        assertTrue(repo.isVacationExceeded())
        assertEquals(0.0, repo.getVacationDaysRemaining(), 0.01)
    }

    @Test
    fun `initialVacationBalanceValue klampuje na minimum 0`() {
        repo.initialVacationBalanceValue = -5.0
        assertEquals(0.0, repo.initialVacationBalanceValue, 0.01)
    }

    // ============= set sa dupla smena testovi =============

    @Test
    fun `set sa duplom smenom čuva sve podatke`() {
        repo.set(
            2025, 1, 6, Shift.FIRST, 8,
            shift2 = Shift.SECOND,
            hours2 = 4,
            overtimeType = OvertimeType.COMP_TIME
        )
        val record = repo.get(2025, 1, 6)
        assertNotNull(record)
        assertEquals(Shift.FIRST, record!!.shift)
        assertEquals(8, record.hours)
        assertEquals(Shift.SECOND, record.shift2)
        assertEquals(4, record.hours2)
        assertEquals(OvertimeType.COMP_TIME, record.overtimeType)
    }

    @Test
    fun `set sick pay rate se čuva samo za bolovanje`() {
        repo.set(2025, 1, 6, Shift.SICK, 8, sickPayRate = 0.80)
        val sickRecord = repo.get(2025, 1, 6)
        assertNotNull(sickRecord!!.sickPayRate)

        repo.set(2025, 1, 7, Shift.FIRST, 8, sickPayRate = 0.80)
        val workRecord = repo.get(2025, 1, 7)
        assertNull(workRecord!!.sickPayRate)
    }
}

