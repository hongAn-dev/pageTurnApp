package com.pageturn.core.domain.repository

import com.pageturn.core.model.Book
import com.pageturn.core.model.Bookmark
import com.pageturn.core.model.Chapter
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getRecentReads(): Flow<List<Book>>
    fun getBookDetails(bookId: String): Flow<Book?>
    suspend fun getChapter(bookId: String, chapterNumber: Int): Chapter
    suspend fun updateReadingProgress(bookId: String, page: Int, progressPercent: Float)
    suspend fun sync()

    fun getBookmarks(bookId: String): Flow<List<Bookmark>>
    suspend fun addBookmark(bookId: String, chapterNumber: Int, pageNumber: Int)
    suspend fun removeBookmark(bookmarkId: String)
    fun isBookmarked(bookmarkId: String): Flow<Boolean>

    fun getHighlights(bookId: String, chapterNumber: Int): Flow<List<com.pageturn.core.model.Highlight>>
    fun getAllHighlights(): Flow<List<com.pageturn.core.model.Highlight>>
    suspend fun addHighlight(bookId: String, chapterNumber: Int, startOffset: Int, endOffset: Int, colorHex: String, noteText: String?, selectedText: String?)
    suspend fun removeHighlight(highlightId: String)
    suspend fun removeHighlightsByParagraph(bookId: String, chapterNumber: Int, startOffset: Int)
    suspend fun removeHighlightByText(bookId: String, chapterNumber: Int, startOffset: Int, selectedText: String)
    suspend fun addLocalBook(book: Book)
    fun getBookmarkedBookIds(): Flow<List<String>>
    suspend fun deleteLocalBook(bookId: String)

    // --- Cloud sync helpers ---
    /** One-shot snapshot of all books (not a Flow) */
    suspend fun getAllBooksSnapshot(): List<Book>
    /** One-shot snapshot of all highlights */
    suspend fun getAllHighlightsSnapshot(): List<com.pageturn.core.model.Highlight>
    /** Update local progress only if cloud page is ahead */
    suspend fun updateProgressIfNewer(bookId: String, cloudPage: Int, cloudTotal: Int, cloudProgress: Float)
    /** Insert or update highlight by id */
    suspend fun upsertHighlight(id: String, bookId: String, chapterNumber: Int, startOffset: Int, colorHex: String, selectedText: String, noteText: String)
    suspend fun downloadBook(bookId: String): ByteArray
    suspend fun deleteCloudBook(bookId: String)
    suspend fun clearAllLocalData()
}

