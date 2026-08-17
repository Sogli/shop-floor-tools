package com.zaduzenja.app

import android.content.Context
import android.util.Log
import com.zaduzenja.app.data.db.ArticleDao
import com.zaduzenja.app.data.db.ArticleEntity
import com.zaduzenja.app.data.db.HistoryEntryEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val TAG = "DataRepository"

enum class ArticleType(val displayName: String, val periodMonths: Int, val key: String) {
    RUKAVICE("Rukavice", 1, "rukavice"),
    MAXICUT("MaxiCut rukavice", 2, "maxicut"),
    MAJICA("Majica", 4, "majica"),
    CIPELE("Cipele", 12, "cipele"),
    ODELO("Odelo", 12, "odelo");

    val availableSizes: List<String>?
        get() = SizeOptions[this]

    val hasSizes: Boolean
        get() = availableSizes != null

    companion object {
        fun fromKey(key: String): ArticleType? {
            return values().firstOrNull { it.key == key }
        }
    }
}

private val SizeOptions = mapOf(
    ArticleType.CIPELE to listOf("39", "40", "41", "42", "43", "44", "45", "46"),
    ArticleType.ODELO to listOf("48", "50", "52", "54", "56", "58"),
    ArticleType.MAJICA to listOf("S", "M", "L", "XL", "XXL", "XXXL")
)

data class HistoryEntry(
    val date: LocalDate,
    val size: String? = null
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("date", date.format(DateUtils.isoFormatter))
        if (size != null) {
            obj.put("size", size)
        }
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): HistoryEntry {
            val date = DateUtils.parseDate(obj.getString("date")) ?: LocalDate.now()
            val size = obj.optString("size", "").ifBlank { null }
            return HistoryEntry(date = date, size = size)
        }
    }
}

data class ArticleRecord(
    val lastAssignment: LocalDate? = null,
    val history: List<HistoryEntry> = emptyList()
) {
    val lastSize: String?
        get() = history.maxByOrNull { it.date }?.size

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("last_assignment", lastAssignment?.format(DateUtils.isoFormatter))
        val historyArray = JSONArray()
        history.forEach { historyArray.put(it.toJson()) }
        obj.put("history", historyArray)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): ArticleRecord {
            val lastStr = obj.optString("last_assignment")
            val last = DateUtils.parseDate(lastStr)

            val historyArray = obj.optJSONArray("history") ?: JSONArray()
            val historyList = mutableListOf<HistoryEntry>()
            for (i in 0 until historyArray.length()) {
                val item = historyArray.get(i)
                if (item is JSONObject) {
                    historyList.add(HistoryEntry.fromJson(item))
                } else {
                    // Kompatibilnost sa starim formatom (niz stringova)
                    val dateStr = item.toString()
                    DateUtils.parseDate(dateStr)?.let {
                        historyList.add(HistoryEntry(date = it))
                    }
                }
            }

            return ArticleRecord(lastAssignment = last, history = historyList)
        }
    }
}

object DateUtils {
    private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun formatDate(date: LocalDate?): String {
        return date?.format(displayFormatter) ?: "-"
    }

    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank() || dateStr == "null") {
            return null
        }
        val formatters = listOf(isoFormatter, displayFormatter)
        for (formatter in formatters) {
            try {
                return LocalDate.parse(dateStr, formatter)
            } catch (_: DateTimeParseException) {
            }
        }
        return null
    }

    fun firstOfMonth(date: LocalDate): LocalDate {
        return date.withDayOfMonth(1)
    }

    fun addMonths(date: LocalDate, months: Int): LocalDate {
        return date.plusMonths(months.toLong()).withDayOfMonth(1)
    }

    fun calculateNextAllowed(last: LocalDate?, periodMonths: Int, today: LocalDate): LocalDate {
        return if (last == null) {
            firstOfMonth(today)
        } else {
            addMonths(last, periodMonths)
        }
    }
}

class DataRepository(private val context: Context, private val dao: ArticleDao) {
    private var onChangeCallback: (() -> Unit)? = null

    fun onChange(callback: () -> Unit) {
        onChangeCallback = callback
    }

    // --- Sinhroni metodi (za TrackingViewModel kompatibilnost) ---
    // TrackingViewModel poziva getRecord/getAllRecords/updateRecord sinhrono,
    // pa koristimo runBlocking za bridge.

    fun getRecord(articleType: ArticleType): ArticleRecord {
        return kotlinx.coroutines.runBlocking {
            val entity = dao.getArticle(articleType.key)
            val historyEntities = dao.getHistory(articleType.key)
            toArticleRecord(entity, historyEntities)
        }
    }

    fun getAllRecords(): Map<ArticleType, ArticleRecord> {
        return kotlinx.coroutines.runBlocking {
            val result = mutableMapOf<ArticleType, ArticleRecord>()
            for (type in ArticleType.values()) {
                val entity = dao.getArticle(type.key)
                val historyEntities = dao.getHistory(type.key)
                result[type] = toArticleRecord(entity, historyEntities)
            }
            result
        }
    }

    fun updateRecord(articleType: ArticleType, record: ArticleRecord) {
        kotlinx.coroutines.runBlocking {
            val lastStr = record.lastAssignment?.format(DateUtils.isoFormatter)
            dao.updateLastAssignment(articleType.key, lastStr)

            // Obriši stare history entries za ovaj tip i ubaci nove
            val oldEntries = dao.getHistory(articleType.key)
            for (old in oldEntries) {
                dao.deleteHistoryEntry(old.id)
            }

            val newEntries = record.history.map { entry ->
                HistoryEntryEntity(
                    articleType = articleType.key,
                    date = entry.date.format(DateUtils.isoFormatter),
                    size = entry.size
                )
            }
            if (newEntries.isNotEmpty()) {
                dao.insertHistoryEntries(newEntries)
            }
        }
        onChangeCallback?.invoke()
    }

    // save() i saveIfDirty() više nisu potrebni jer Room automatski čuva,
    // ali zadržavamo ih kao prazne metode za kompatibilnost
    fun save() { /* Room auto-persist */ }
    fun saveIfDirty() { /* Room auto-persist */ }

    // --- JSON export/import za Google Drive backup ---

    fun exportJson(pretty: Boolean = true): String {
        return kotlinx.coroutines.runBlocking {
            val root = JSONObject()
            for (type in ArticleType.values()) {
                val entity = dao.getArticle(type.key)
                val historyEntities = dao.getHistory(type.key)
                val record = toArticleRecord(entity, historyEntities)
                root.put(type.key, record.toJson())
            }
            if (pretty) root.toString(2) else root.toString()
        }
    }

    fun importJson(raw: String) {
        kotlinx.coroutines.runBlocking {
            val json = JSONObject(raw)
            val recordsByType = ArticleType.values().associateWith { type ->
                val obj = json.optJSONObject(type.key)
                if (obj != null) {
                    ArticleRecord.fromJson(obj)
                } else {
                    ArticleRecord()
                }
            }

            for (type in ArticleType.values()) {
                val record = recordsByType.getValue(type)

                // Ažuriraj article
                val lastStr = record.lastAssignment?.format(DateUtils.isoFormatter)
                dao.insertArticle(ArticleEntity(
                    articleType = type.key,
                    lastAssignment = lastStr
                ))

                // Zameni history
                val oldEntries = dao.getHistory(type.key)
                for (old in oldEntries) {
                    dao.deleteHistoryEntry(old.id)
                }

                val newEntries = record.history.map { entry ->
                    HistoryEntryEntity(
                        articleType = type.key,
                        date = entry.date.format(DateUtils.isoFormatter),
                        size = entry.size
                    )
                }
                if (newEntries.isNotEmpty()) {
                    dao.insertHistoryEntries(newEntries)
                }
            }
        }
        onChangeCallback?.invoke()
    }

    // --- Pomoćna funkcija ---

    private fun toArticleRecord(
        entity: ArticleEntity?,
        historyEntities: List<HistoryEntryEntity>
    ): ArticleRecord {
        val lastAssignment = entity?.lastAssignment?.let { DateUtils.parseDate(it) }
        val history = historyEntities.map { e ->
            HistoryEntry(
                date = DateUtils.parseDate(e.date) ?: LocalDate.now(),
                size = e.size
            )
        }
        return ArticleRecord(lastAssignment = lastAssignment, history = history)
    }
}

data class ArticleStatus(
    val articleType: ArticleType,
    val lastAssignment: LocalDate?,
    val nextAllowed: LocalDate,
    val canAssignNow: Boolean,
    val history: List<HistoryEntry>,
    val lastSize: String? = null
)

enum class AssignmentResult {
    SUCCESS,
    NOT_ALLOWED_YET,
    INVALID_DATE
}

data class AssignmentResponse(
    val result: AssignmentResult,
    val message: String,
    val nextAllowed: LocalDate? = null
)

class TrackingViewModel(private val repository: DataRepository) {
    private var today: LocalDate = LocalDate.now()

    val todayDate: LocalDate
        get() = today

    fun refreshToday() {
        today = LocalDate.now()
    }

    fun getAllStatuses(): List<ArticleStatus> {
        val statuses = mutableListOf<ArticleStatus>()
        val records = repository.getAllRecords()
        for ((type, record) in records) {
            val nextAllowed = DateUtils.calculateNextAllowed(
                record.lastAssignment,
                type.periodMonths,
                today
            )
            statuses.add(
                ArticleStatus(
                    articleType = type,
                    lastAssignment = record.lastAssignment,
                    nextAllowed = nextAllowed,
                    canAssignNow = today >= nextAllowed,
                    history = record.history.toList(),
                    lastSize = record.lastSize
                )
            )
        }
        return statuses
    }

    fun getStatus(articleType: ArticleType): ArticleStatus {
        val record = repository.getRecord(articleType)
        val nextAllowed = DateUtils.calculateNextAllowed(
            record.lastAssignment,
            articleType.periodMonths,
            today
        )
        return ArticleStatus(
            articleType = articleType,
            lastAssignment = record.lastAssignment,
            nextAllowed = nextAllowed,
            canAssignNow = today >= nextAllowed,
            history = record.history.toList(),
            lastSize = record.lastSize
        )
    }

    fun attemptAssignment(articleType: ArticleType, assignmentDate: LocalDate? = null, size: String? = null): AssignmentResponse {
        refreshToday()
        val dateToUse = assignmentDate ?: today

        if (dateToUse > today) {
            return AssignmentResponse(
                result = AssignmentResult.INVALID_DATE,
                message = "Datum u budućnosti nije dozvoljen."
            )
        }

        val record = repository.getRecord(articleType)
        val nextAllowed = DateUtils.calculateNextAllowed(
            record.lastAssignment,
            articleType.periodMonths,
            dateToUse
        )

        if (today < nextAllowed || dateToUse < nextAllowed) {
            return AssignmentResponse(
                result = AssignmentResult.NOT_ALLOWED_YET,
                message = "Zaduženje za ${articleType.displayName} nije moguće.",
                nextAllowed = nextAllowed
            )
        }

        val entry = HistoryEntry(date = dateToUse, size = size)
        val newRecord = ArticleRecord(
            lastAssignment = dateToUse,
            history = record.history + entry
        )
        repository.updateRecord(articleType, newRecord)
        repository.save()

        return AssignmentResponse(
            result = AssignmentResult.SUCCESS,
            message = "Uspešno zaduženo: ${articleType.displayName}"
        )
    }

    fun getHistory(articleType: ArticleType): List<HistoryEntry> {
        val record = repository.getRecord(articleType)
        return record.history.sortedByDescending { it.date }
    }

    fun deleteHistoryEntry(articleType: ArticleType, entry: HistoryEntry): Boolean {
        val record = repository.getRecord(articleType)
        val history = record.history.toMutableList()
        val removed = history.remove(entry)
        if (!removed) {
            return false
        }

        val newLast = history.maxByOrNull { it.date }?.date
        val newRecord = ArticleRecord(
            lastAssignment = newLast,
            history = history
        )
        repository.updateRecord(articleType, newRecord)
        repository.save()
        return true
    }

    fun editHistoryEntry(
        articleType: ArticleType,
        oldEntry: HistoryEntry,
        newDate: LocalDate,
        newSize: String?
    ): Boolean {
        val record = repository.getRecord(articleType)
        val history = record.history.toMutableList()
        val index = history.indexOf(oldEntry)
        if (index == -1) {
            return false
        }

        history[index] = HistoryEntry(date = newDate, size = newSize)
        val newLast = history.maxByOrNull { it.date }?.date
        val newRecord = ArticleRecord(
            lastAssignment = newLast,
            history = history
        )
        repository.updateRecord(articleType, newRecord)
        repository.save()
        return true
    }
}
