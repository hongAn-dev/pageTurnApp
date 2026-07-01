package com.pageturn.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pageturn.core.database.model.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    fun getBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBook(bookId: String): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceBooks(books: List<BookEntity>)

    @Query("UPDATE books SET currentPage = :page, progressPercent = :progress WHERE id = :bookId")
    suspend fun updateBookProgress(bookId: String, page: Int, progress: Float)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: String)

    // --- Snapshot queries for cloud sync (suspend, not Flow) ---
    @Query("SELECT * FROM books")
    suspend fun getBooksSnapshot(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getBookSnapshot(bookId: String): BookEntity?
}
