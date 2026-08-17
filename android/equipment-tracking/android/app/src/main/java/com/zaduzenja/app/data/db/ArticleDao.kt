package com.zaduzenja.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles WHERE article_type = :type")
    suspend fun getArticle(type: String): ArticleEntity?

    @Query("SELECT * FROM articles")
    suspend fun getAllArticles(): List<ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("UPDATE articles SET last_assignment = :lastAssignment WHERE article_type = :type")
    suspend fun updateLastAssignment(type: String, lastAssignment: String?)

    // History entries

    @Query("SELECT * FROM history_entries WHERE article_type = :type ORDER BY date ASC")
    suspend fun getHistory(type: String): List<HistoryEntryEntity>

    @Query("SELECT * FROM history_entries ORDER BY date ASC")
    suspend fun getAllHistory(): List<HistoryEntryEntity>

    @Insert
    suspend fun insertHistoryEntry(entry: HistoryEntryEntity): Long

    @Insert
    suspend fun insertHistoryEntries(entries: List<HistoryEntryEntity>)

    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteHistoryEntry(id: Long)

    @Query("DELETE FROM history_entries WHERE article_type = :type AND date = :date AND (size = :size OR (size IS NULL AND :size IS NULL))")
    suspend fun deleteHistoryEntryByContent(type: String, date: String, size: String?)

    @Query("SELECT * FROM history_entries WHERE article_type = :type ORDER BY date DESC LIMIT 1")
    suspend fun getLastHistoryEntry(type: String): HistoryEntryEntity?

    // Za migraciju - provera da li baza već ima podatke
    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getArticleCount(): Int
}
