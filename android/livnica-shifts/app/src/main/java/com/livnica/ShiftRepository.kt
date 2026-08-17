package com.livnica

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.json.JSONObject
import java.io.File
import java.time.YearMonth
import java.util.TreeMap

data class BatchRecord(
    val year: Int,
    val month: Int,
    val day: Int,
    val shift: Shift,
    val hours: Int,
    val sickPayRate: Double? = null
)

class ShiftRepository(private val config: AppConfig = CONFIG) {
    private val payCalculator = PayCalculator(config.pay)

    // Thread safety: All access to these collections must be synchronized on `lock`
    private val lock = Any()
    private val records = mutableMapOf<DayKey, DayRecord>()
    private val byMonth = mutableMapOf<MonthKey, MutableSet<DayKey>>()
    private var basePay = config.pay.defaultBasePay
    private var foodPerDay = config.pay.foodPerDay
    private var initialCompBalance = 0
    private var yearsOfService = 0
    private var brigadeType = BrigadeType.CETVOROBRIGADA
    private var initialVacationBalance = 0.0
    private val summaries = mutableMapOf<MonthKey, MonthSummary>()
    private val dirtyMonths = mutableSetOf<MonthKey>()
    private val compBalanceCache = mutableMapOf<MonthKey, Int>()
    private var cachedSummaryCalc: PayCalculator? = null
    private var cachedSummaryCalcFoodPerDay: Double = Double.NaN

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val onChangeCallbacks = mutableMapOf<Int, () -> Unit>()
    private var nextCallbackId = 0

    companion object {
        private const val TAG = "ShiftRepository"
        private const val LOCAL_BACKUP_FILE = "livnica_local_data.json"
        const val BASE_PAY_KEY = "config_base_pay_hourly"
        const val INITIAL_COMP_BALANCE_KEY = "initial_comp_balance"
        const val YEARS_OF_SERVICE_KEY = "years_of_service"
        const val BRIGADE_TYPE_KEY = "brigade_type"
        const val FOOD_PER_DAY_KEY = "config_food_per_day"
        const val INITIAL_VACATION_BALANCE_KEY = "initial_vacation_balance"

        // Granice za validaciju unosa
        private const val MAX_BASE_PAY = 100_000.0
        private const val MAX_FOOD_PER_DAY = 10_000.0
        private const val MIN_COMP_BALANCE = -1000
        private const val MAX_COMP_BALANCE = 1000
    }

    /**
     * Register a callback to be called when data changes.
     * @return A token that can be used to remove the callback later.
     */
    fun onChange(callback: () -> Unit): Int {
        val id = nextCallbackId++
        onChangeCallbacks[id] = callback
        return id
    }

    /**
     * Remove a previously registered change callback.
     * @param token The token returned from onChange()
     */
    fun removeOnChange(token: Int) {
        onChangeCallbacks.remove(token)
    }

    /**
     * Cancel the coroutine scope and clean up resources.
     * Should be called from Activity.onDestroy()
     */
    fun close() {
        scope.cancel()
        onChangeCallbacks.clear()
    }

    private fun notifyChange() {
        for ((_, cb) in onChangeCallbacks) {
            try {
                cb()
            } catch (e: Exception) {
                Log.w(TAG, "onChange callback failed", e)
            }
        }
    }

    private fun buildDataMap(): TreeMap<String, Any> = synchronized(lock) {
        val data = TreeMap<String, Any>()
        data[BASE_PAY_KEY] = basePay
        data[FOOD_PER_DAY_KEY] = foodPerDay
        data[INITIAL_COMP_BALANCE_KEY] = initialCompBalance
        data[YEARS_OF_SERVICE_KEY] = yearsOfService
        data[BRIGADE_TYPE_KEY] = brigadeType.value
        data[INITIAL_VACATION_BALANCE_KEY] = initialVacationBalance

        for (record in records.values) {
            val yearKey = record.year.toString()
            val monthKey = "%02d".format(record.month)
            val dayKey = "%02d".format(record.day)

            @Suppress("UNCHECKED_CAST")
            val yearMap = data.getOrPut(yearKey) { TreeMap<String, Any>() } as TreeMap<String, Any>
            @Suppress("UNCHECKED_CAST")
            val monthMap = yearMap.getOrPut(monthKey) { TreeMap<String, Any>() } as TreeMap<String, Any>
            monthMap[dayKey] = record.toMap()
        }

        data
    }

    fun exportJson(): String {
        return JsonWriter.write(buildDataMap())
    }

    fun importFromJson(jsonText: String): Boolean {
        val parsed = try {
            parseJson(JSONObject(jsonText))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse import JSON", e)
            null
        } ?: return false

        synchronized(lock) {
            records.clear()
            byMonth.clear()
            summaries.clear()
            dirtyMonths.clear()
            compBalanceCache.clear()
            cachedSummaryCalc = null

            basePay = if (parsed.basePay.isFinite()) parsed.basePay.coerceIn(0.0, MAX_BASE_PAY) else config.pay.defaultBasePay
            foodPerDay = if (parsed.foodPerDay.isFinite()) parsed.foodPerDay.coerceIn(0.0, MAX_FOOD_PER_DAY) else config.pay.foodPerDay
            initialCompBalance = parsed.initialCompBalance.coerceIn(MIN_COMP_BALANCE, MAX_COMP_BALANCE)
            yearsOfService = parsed.yearsOfService
            brigadeType = parsed.brigadeType
            initialVacationBalance = parsed.initialVacationBalance

            for (record in parsed.records) {
                addToIndexUnsafe(record)
            }
        }
        notifyChange()
        return true
    }

    private fun parseJson(json: JSONObject): ParsedData {
        val parsedBasePay = json.optDouble(BASE_PAY_KEY, config.pay.defaultBasePay)
        val parsedFoodPerDay = json.optDouble(FOOD_PER_DAY_KEY, config.pay.foodPerDay)
        val parsedInitialComp = json.optInt(INITIAL_COMP_BALANCE_KEY, 0)
        val parsedYears = json.optInt(YEARS_OF_SERVICE_KEY, 0)
        val parsedBrigade = BrigadeType.fromValue(
            json.optString(BRIGADE_TYPE_KEY, BrigadeType.CETVOROBRIGADA.value)
        )
        val parsedVacationBalance = json.optDouble(INITIAL_VACATION_BALANCE_KEY, 0.0)

        val parsedRecords = mutableListOf<DayRecord>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val yearKey = keys.next()
            if (yearKey == BASE_PAY_KEY || yearKey == INITIAL_COMP_BALANCE_KEY ||
                yearKey == YEARS_OF_SERVICE_KEY || yearKey == BRIGADE_TYPE_KEY ||
                yearKey == FOOD_PER_DAY_KEY || yearKey == INITIAL_VACATION_BALANCE_KEY ||
                yearKey == "comp_time_balance"
            ) {
                continue
            }

            val year = yearKey.toIntOrNull() ?: continue
            // Skip invalid years (must be reasonable)
            if (year < 1900 || year > 2200) continue

            val monthsObj = json.optJSONObject(yearKey) ?: continue
            val monthKeys = monthsObj.keys()
            while (monthKeys.hasNext()) {
                val monthKey = monthKeys.next()
                val month = monthKey.toIntOrNull() ?: continue
                // Validate month is 1-12
                if (month < 1 || month > 12) {
                    Log.w(TAG, "Invalid month $month in import, skipping")
                    continue
                }

                val daysObj = monthsObj.optJSONObject(monthKey) ?: continue
                val dayKeys = daysObj.keys()

                // Get max days for this month
                val maxDays = try {
                    YearMonth.of(year, month).lengthOfMonth()
                } catch (e: Exception) {
                    31 // fallback to 31 if there's an issue
                }

                while (dayKeys.hasNext()) {
                    val dayKey = dayKeys.next()
                    val day = dayKey.toIntOrNull() ?: continue
                    // Validate day is within valid range for this month
                    if (day < 1 || day > maxDays) {
                        Log.w(TAG, "Invalid day $day for $year/$month (max: $maxDays), skipping")
                        continue
                    }
                    val recData = daysObj.optJSONObject(dayKey) ?: continue
                    parsedRecords.add(DayRecord.fromJson(year, month, day, recData))
                }
            }
        }

        return ParsedData(
            basePay = parsedBasePay,
            foodPerDay = parsedFoodPerDay,
            initialCompBalance = parsedInitialComp,
            yearsOfService = parsedYears,
            brigadeType = parsedBrigade,
            initialVacationBalance = parsedVacationBalance,
            records = parsedRecords
        )
    }

    // Unsafe versions - caller must hold lock
    private fun addToIndexUnsafe(record: DayRecord) {
        records[record.key] = record
        byMonth.getOrPut(record.monthKey) { mutableSetOf() }.add(record.key)
        invalidateSummaryUnsafe(record.monthKey)
    }

    private fun removeFromIndexUnsafe(record: DayRecord) {
        records.remove(record.key)
        val monthSet = byMonth[record.monthKey]
        monthSet?.remove(record.key)
        if (monthSet != null && monthSet.isEmpty()) {
            byMonth.remove(record.monthKey)
        }
        invalidateSummaryUnsafe(record.monthKey)
    }

    private fun invalidateSummaryUnsafe(monthKey: MonthKey) {
        dirtyMonths.add(monthKey)
        summaries.remove(monthKey)
        // Invalidate comp balance cache for all FUTURE months (> not >=)
        // because the starting balance of month X only depends on records in months BEFORE X
        // So when a record in month X changes, only months X+1 and beyond are affected
        compBalanceCache.entries.removeIf { it.key > monthKey }
    }

    fun get(year: Int, month: Int, day: Int): DayRecord? = synchronized(lock) {
        records[DayKey(year, month, day)]
    }

    fun set(
        year: Int,
        month: Int,
        day: Int,
        shift: Shift,
        hours: Int,
        shift2: Shift? = null,
        hours2: Int = 0,
        overtimeType: OvertimeType = OvertimeType.PAID,
        weekendOvertimeType: OvertimeType = OvertimeType.PAID,
        compHoursUsed: Int = 0,
        holidayWorkOverride: Boolean? = null,
        holidayOverride: Boolean? = null,
        sickPayRate: Double? = null
    ): Boolean {
        synchronized(lock) {
            val key = DayKey(year, month, day)
            records[key]?.let { removeFromIndexUnsafe(it) }

            val finalSickRate = if (shift != Shift.SICK) {
                null
            } else {
                sickPayRate ?: config.pay.sickPayRate
            }

            val record = DayRecord(
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
                sickPayRate = finalSickRate
            )
            addToIndexUnsafe(record)
        }

        notifyChange()
        return true
    }

    fun delete(year: Int, month: Int, day: Int): Boolean {
        synchronized(lock) {
            val key = DayKey(year, month, day)
            val record = records[key] ?: return false
            removeFromIndexUnsafe(record)
        }
        notifyChange()
        return true
    }

    fun deleteMonth(year: Int, month: Int): Int {
        val keysToDelete: List<DayKey>
        synchronized(lock) {
            val monthKey = MonthKey(year, month)
            keysToDelete = byMonth[monthKey]?.toList().orEmpty()
            for (key in keysToDelete) {
                records[key]?.let { removeFromIndexUnsafe(it) }
            }
        }
        if (keysToDelete.isNotEmpty()) {
            notifyChange()
        }
        return keysToDelete.size
    }

    fun deleteAllData(): Int {
        val count: Int
        synchronized(lock) {
            count = records.size
            records.clear()
            byMonth.clear()
            summaries.clear()
            dirtyMonths.clear()
            compBalanceCache.clear()
        }
        if (count > 0) {
            notifyChange()
        }
        return count
    }

    fun setBatch(batchRecords: List<BatchRecord>) {
        synchronized(lock) {
            for (rec in batchRecords) {
                val key = DayKey(rec.year, rec.month, rec.day)
                this.records[key]?.let { removeFromIndexUnsafe(it) }
                val finalSickRate = if (rec.shift != Shift.SICK) {
                    null
                } else {
                    rec.sickPayRate ?: config.pay.sickPayRate
                }
                val record = DayRecord(
                    year = rec.year,
                    month = rec.month,
                    day = rec.day,
                    shift = rec.shift,
                    hours = rec.hours,
                    sickPayRate = finalSickRate
                )
                addToIndexUnsafe(record)
            }
        }
        notifyChange()
    }

    fun getMonthRecords(year: Int, month: Int): List<DayRecord> = synchronized(lock) {
        val monthKey = MonthKey(year, month)
        val keys = byMonth[monthKey].orEmpty()
        keys.mapNotNull { records[it] }
    }

    fun getAllMonthsSorted(): List<MonthKey> = synchronized(lock) {
        byMonth.keys.sorted()
    }

    fun getCompBalanceAtMonthStart(year: Int, month: Int): Int = synchronized(lock) {
        calculateCompBalanceUnsafe(year, month)
    }

    /**
     * Vraća keširanu PayCalculator instancu za summary kalkulacije.
     * Kreira novu samo ako se foodPerDay promenio. Pozivalac MORA držati lock.
     */
    private fun getSummaryCalculator(): PayCalculator {
        val currentFood = foodPerDay
        val cached = cachedSummaryCalc
        if (cached != null && cachedSummaryCalcFoodPerDay == currentFood) {
            return cached
        }
        val calc = PayCalculator(config.pay.copy(foodPerDay = currentFood))
        cachedSummaryCalc = calc
        cachedSummaryCalcFoodPerDay = currentFood
        return calc
    }

    fun getSummary(year: Int, month: Int): MonthSummary = synchronized(lock) {
        val monthKey = MonthKey(year, month)
        summaries[monthKey]?.let { return it }

        val startingBalance = getCompBalanceAtMonthStartUnsafe(year, month)
        val keys = byMonth[monthKey].orEmpty()
        val monthRecords = keys.mapNotNull { records[it] }
        val summary = getSummaryCalculator().calculateMonthSummary(
            monthRecords,
            basePay,
            startingBalance,
            yearsOfService,
            brigadeType,
            summaryYear = year,
            summaryMonth = month
        )
        summaries[monthKey] = summary
        dirtyMonths.remove(monthKey)
        summary
    }

    // Interna verzija koja pretpostavlja da je lock zauzet
    private fun getCompBalanceAtMonthStartUnsafe(year: Int, month: Int): Int {
        return calculateCompBalanceUnsafe(year, month)
    }

    /**
     * Zajednička logika za izračunavanje comp balance-a na početku meseca.
     * Pozivalac MORA držati lock. Koristi keš za ubrzanje - počinje od
     * najbližeg keširanog meseca umesto od početka.
     */
    private fun calculateCompBalanceUnsafe(year: Int, month: Int): Int {
        val target = MonthKey(year, month)
        compBalanceCache[target]?.let { return it }

        val priorMonths = byMonth.keys.sorted().filter { it < target }
        if (priorMonths.isEmpty()) {
            compBalanceCache[target] = initialCompBalance
            return initialCompBalance
        }

        // Pronađi najbliži keširani mesec da ne iteriramo od početka.
        // Keš[M] = balans na POČETKU meseca M (pre zapisa iz M).
        // Da bismo dobili target, trebamo primeniti zapise od keširanog meseca nadalje.
        var balance = initialCompBalance
        var startIndex = 0
        for (i in priorMonths.indices.reversed()) {
            val cached = compBalanceCache[priorMonths[i]]
            if (cached != null) {
                balance = cached
                startIndex = i
                break
            }
        }

        for (i in startIndex until priorMonths.size) {
            val monthKey = priorMonths[i]
            val keys = byMonth[monthKey].orEmpty()
            val monthRecords = keys.mapNotNull { records[it] }
            val earned = monthRecords.sumOf { it.compTimeEarned }
            val deficit = monthRecords.sumOf { it.hoursDeficit }
            val used = monthRecords.sumOf { it.compHoursUsed }
            balance += earned - deficit - used
            if (balance < 0) balance = 0
        }

        compBalanceCache[target] = balance
        return balance
    }

    fun calculateBasePayFromNet(year: Int, month: Int, netPay: Double): Double? = synchronized(lock) {
        if (netPay < 0) return null
        val monthKey = MonthKey(year, month)
        val keys = byMonth[monthKey].orEmpty()
        val monthRecords = keys.mapNotNull { records[it] }
        val summary = payCalculator.calculateMonthSummary(
            monthRecords,
            1.0,
            getCompBalanceAtMonthStartUnsafe(year, month),
            yearsOfService,
            brigadeType,
            summaryYear = year,
            summaryMonth = month
        )
        val coefficient = summary.totalWithMinuli
        if (coefficient <= 0.0) return null
        netPay / coefficient
    }

    fun getCurrentCompBalance(year: Int, month: Int): Int {
        return getSummary(year, month).compTimeEnding
    }

    fun calculateDailyPay(record: DayRecord): Double = synchronized(lock) {
        payCalculator.calculateDailyPay(record, basePay, brigadeType)
    }

    var basePayValue: Double
        get() = synchronized(lock) { basePay }
        set(value) {
            synchronized(lock) {
                if (!value.isFinite()) {
                    Log.w(TAG, "Odbijena nevažeća basePay vrednost: $value")
                    return
                }
                basePay = value.coerceIn(0.0, MAX_BASE_PAY)
                summaries.clear()
            }
            notifyChange()
        }

    var foodPerDayValue: Double
        get() = synchronized(lock) { foodPerDay }
        set(value) {
            synchronized(lock) {
                if (!value.isFinite()) {
                    Log.w(TAG, "Odbijena nevažeća foodPerDay vrednost: $value")
                    return
                }
                foodPerDay = value.coerceIn(0.0, MAX_FOOD_PER_DAY)
                summaries.clear()
            }
            notifyChange()
        }

    var initialCompBalanceValue: Int
        get() = synchronized(lock) { initialCompBalance }
        set(value) {
            synchronized(lock) {
                initialCompBalance = value.coerceIn(MIN_COMP_BALANCE, MAX_COMP_BALANCE)
                summaries.clear()
                compBalanceCache.clear()
            }
            notifyChange()
        }

    var yearsOfServiceValue: Int
        get() = synchronized(lock) { yearsOfService }
        set(value) {
            synchronized(lock) {
                yearsOfService = value.coerceAtLeast(0)
                summaries.clear()
            }
            notifyChange()
        }

    var brigadeTypeValue: BrigadeType
        get() = synchronized(lock) { brigadeType }
        set(value) {
            synchronized(lock) {
                if (brigadeType != value) {
                    brigadeType = value
                    summaries.clear()
                } else {
                    return
                }
            }
            notifyChange()
        }

    var initialVacationBalanceValue: Double
        get() = synchronized(lock) { initialVacationBalance }
        set(value) {
            synchronized(lock) {
                initialVacationBalance = value.coerceAtLeast(0.0)
            }
            notifyChange()
        }

    private fun getVacationDaysUsedUnsafe(): Int {
        return records.values.count { it.shift == Shift.VACATION }
    }

    fun getVacationDaysUsed(): Int = synchronized(lock) {
        getVacationDaysUsedUnsafe()
    }

    fun getVacationDaysRemaining(): Double = synchronized(lock) {
        maxOf(0.0, initialVacationBalance - getVacationDaysUsedUnsafe())
    }

    fun isVacationExceeded(): Boolean = synchronized(lock) {
        getVacationDaysUsedUnsafe() > initialVacationBalance
    }

    // =========================================================================
    // LOCAL STORAGE
    // =========================================================================

    fun saveToLocal(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, LOCAL_BACKUP_FILE)
            file.writeText(exportJson())
            Log.d(TAG, "Saved to local storage")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to local storage", e)
            false
        }
    }

    fun loadFromLocal(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, LOCAL_BACKUP_FILE)
            if (!file.exists()) {
                Log.d(TAG, "No local backup file found")
                return false
            }
            val json = file.readText()
            if (json.isBlank()) {
                Log.d(TAG, "Local backup file is empty")
                return false
            }
            importFromJson(json)
            Log.d(TAG, "Loaded from local storage")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load from local storage", e)
            false
        }
    }

    fun hasLocalData(context: Context): Boolean {
        val file = File(context.filesDir, LOCAL_BACKUP_FILE)
        return file.exists() && file.length() > 0
    }
}

private data class ParsedData(
    val basePay: Double,
    val foodPerDay: Double,
    val initialCompBalance: Int,
    val yearsOfService: Int,
    val brigadeType: BrigadeType,
    val initialVacationBalance: Double,
    val records: List<DayRecord>
)

object JsonWriter {
    fun write(value: Any?): String {
        return writeValue(value, 0)
    }

    private fun writeValue(value: Any?, level: Int): String {
        return when (value) {
            null -> "null"
            is String -> "\"${escape(value)}\""
            is Number, is Boolean -> value.toString()
            is Map<*, *> -> writeMap(@Suppress("UNCHECKED_CAST") (value as Map<String, Any?>), level)
            is List<*> -> writeList(value, level)
            else -> "\"${escape(value.toString())}\""
        }
    }

    private fun writeMap(map: Map<String, Any?>, level: Int): String {
        if (map.isEmpty()) return "{}"
        val indent = "  "
        val pad = indent.repeat(level)
        val sb = StringBuilder()
        sb.append("{\n")
        val entries = map.entries.toList()
        for (i in entries.indices) {
            val entry = entries[i]
            sb.append(pad).append(indent)
            sb.append("\"").append(escape(entry.key)).append("\": ")
            sb.append(writeValue(entry.value, level + 1))
            if (i < entries.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append(pad).append("}")
        return sb.toString()
    }

    private fun writeList(list: List<*>, level: Int): String {
        if (list.isEmpty()) return "[]"
        val indent = "  "
        val pad = indent.repeat(level)
        val sb = StringBuilder()
        sb.append("[\n")
        for (i in list.indices) {
            sb.append(pad).append(indent)
            sb.append(writeValue(list[i], level + 1))
            if (i < list.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append(pad).append("]")
        return sb.toString()
    }

    private fun escape(value: String): String {
        return buildString {
            for (ch in value) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }
}

