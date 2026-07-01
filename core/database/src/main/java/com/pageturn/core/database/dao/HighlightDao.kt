package com.pageturn.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pageturn.core.database.model.HighlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE bookId = :bookId AND chapterNumber = :chapterNumber ORDER BY timestamp DESC")
    fun getHighlightsForChapter(bookId: String, chapterNumber: Int): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY timestamp DESC")
    fun getAllHighlights(): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :highlightId")
    suspend fun deleteHighlightById(highlightId: String)

    @Query("DELETE FROM highlights WHERE bookId = :bookId AND chapterNumber = :chapterNumber AND startOffset = :startOffset")
    suspend fun deleteHighlightsByParagraph(bookId: String, chapterNumber: Int, startOffset: Int)

    @Query("DELETE FROM highlights WHERE bookId = :bookId AND chapterNumber = :chapterNumber AND startOffset = :startOffset AND selectedText = :selectedText")
    suspend fun deleteHighlightByText(bookId: String, chapterNumber: Int, startOffset: Int, selectedText: String)

    @Query("DELETE FROM highlights WHERE bookId = :bookId")
    suspend fun deleteHighlightsByBookId(bookId: String)

    // --- Snapshot & lookup for cloud sync ---
    @Query("SELECT * FROM highlights ORDER BY timestamp DESC")
    suspend fun getAllHighlightsSnapshot(): List<HighlightEntity>

    @Query("SELECT * FROM highlights WHERE id = :highlightId LIMIT 1")
    suspend fun getHighlightById(highlightId: String): HighlightEntity?
}
