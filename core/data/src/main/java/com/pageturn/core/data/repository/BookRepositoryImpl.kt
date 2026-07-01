package com.pageturn.core.data.repository

import com.pageturn.core.common.network.Dispatcher
import com.pageturn.core.common.network.PtDispatchers
import com.pageturn.core.database.dao.BookDao
import com.pageturn.core.database.dao.BookmarkDao
import com.pageturn.core.database.dao.HighlightDao
import com.pageturn.core.database.model.BookEntity
import com.pageturn.core.database.model.BookmarkEntity
import com.pageturn.core.database.model.HighlightEntity
import com.pageturn.core.database.model.asEntity
import com.pageturn.core.database.model.asExternalModel
import com.pageturn.core.domain.repository.BookRepository
import com.pageturn.core.model.Book
import com.pageturn.core.model.Bookmark
import com.pageturn.core.model.Chapter
import com.pageturn.core.model.Highlight
import com.pageturn.core.network.PageTurnNetworkApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao,
    private val highlightDao: HighlightDao,
    private val networkApi: PageTurnNetworkApi,
    @Dispatcher(PtDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : BookRepository {

    override fun getRecentReads(): Flow<List<Book>> {
        return bookDao.getBooks()
            .map { entities -> entities.map(BookEntity::asExternalModel) }
            .flowOn(ioDispatcher)
    }

    override fun getBookDetails(bookId: String): Flow<Book?> {
        return bookDao.getBook(bookId)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)
    }

    override suspend fun getChapter(bookId: String, chapterNumber: Int): Chapter = withContext(ioDispatcher) {
        if (bookId.startsWith("local_")) {
            val localBook = bookDao.getBook(bookId).firstOrNull()
            Chapter(
                id = "${bookId}_$chapterNumber",
                bookId = bookId,
                title = "Tài liệu: ${localBook?.title ?: "Sách Local"}",
                chapterNumber = chapterNumber,
                content = localBook?.description ?: "Không có nội dung.",
                imageUrl = ""
            )
        } else {
            networkApi.getChapter(bookId, chapterNumber)
        }
    }

    override suspend fun updateReadingProgress(bookId: String, page: Int, progressPercent: Float) = withContext(ioDispatcher) {
        bookDao.updateBookProgress(bookId, page, progressPercent)
    }

    override suspend fun sync() = withContext(ioDispatcher) {
        val remoteBooks = networkApi.getRecentReads()
        bookDao.insertOrReplaceBooks(remoteBooks.map(Book::asEntity))
    }

    override fun getBookmarks(bookId: String): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksForBook(bookId)
            .map { entities -> entities.map(BookmarkEntity::asExternalModel) }
            .flowOn(ioDispatcher)
    }

    override suspend fun addBookmark(bookId: String, chapterNumber: Int, pageNumber: Int) = withContext(ioDispatcher) {
        val id = "${bookId}_${chapterNumber}_${pageNumber}"
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                id = id,
                bookId = bookId,
                chapterNumber = chapterNumber,
                pageNumber = pageNumber,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeBookmark(bookmarkId: String) = withContext(ioDispatcher) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }

    override fun isBookmarked(bookmarkId: String): Flow<Boolean> {
        return bookmarkDao.isBookmarked(bookmarkId)
            .flowOn(ioDispatcher)
    }

    override fun getHighlights(bookId: String, chapterNumber: Int): Flow<List<Highlight>> {
        return highlightDao.getHighlightsForChapter(bookId, chapterNumber)
            .map { entities -> entities.map(HighlightEntity::asExternalModel) }
            .flowOn(ioDispatcher)
    }

    override fun getAllHighlights(): Flow<List<Highlight>> {
        return highlightDao.getAllHighlights()
            .map { entities -> entities.map(HighlightEntity::asExternalModel) }
            .flowOn(ioDispatcher)
    }

    override suspend fun addHighlight(
        bookId: String,
        chapterNumber: Int,
        startOffset: Int,
        endOffset: Int,
        colorHex: String,
        noteText: String?,
        selectedText: String?
    ) = withContext(ioDispatcher) {
        // Use a unique ID with timestamp so multiple highlights in same paragraph do not overwrite each other
        val id = "${bookId}_${chapterNumber}_${startOffset}_${endOffset}_${System.currentTimeMillis()}"
        highlightDao.insertHighlight(
            HighlightEntity(
                id = id,
                bookId = bookId,
                chapterNumber = chapterNumber,
                startOffset = startOffset,
                endOffset = endOffset,
                colorHex = colorHex,
                noteText = noteText,
                timestamp = System.currentTimeMillis(),
                selectedText = selectedText
            )
        )
    }

    override suspend fun removeHighlight(highlightId: String) = withContext(ioDispatcher) {
        highlightDao.deleteHighlightById(highlightId)
    }

    override suspend fun removeHighlightsByParagraph(bookId: String, chapterNumber: Int, startOffset: Int) = withContext(ioDispatcher) {
        highlightDao.deleteHighlightsByParagraph(bookId, chapterNumber, startOffset)
    }

    override suspend fun removeHighlightByText(bookId: String, chapterNumber: Int, startOffset: Int, selectedText: String) = withContext(ioDispatcher) {
        highlightDao.deleteHighlightByText(bookId, chapterNumber, startOffset, selectedText)
    }

    override suspend fun addLocalBook(book: Book) = withContext(ioDispatcher) {
        bookDao.insertOrReplaceBooks(listOf(book.asEntity()))
    }

    override fun getBookmarkedBookIds(): Flow<List<String>> {
        return bookmarkDao.getBookmarkedBookIds()
            .flowOn(ioDispatcher)
    }

    override suspend fun deleteLocalBook(bookId: String) = withContext(ioDispatcher) {
        bookDao.deleteBookById(bookId)
        bookmarkDao.deleteBookmarksByBookId(bookId)
        highlightDao.deleteHighlightsByBookId(bookId)
    }

    // --- Cloud sync helpers ---

    override suspend fun getAllBooksSnapshot(): List<Book> = withContext(ioDispatcher) {
        bookDao.getBooksSnapshot().map(BookEntity::asExternalModel)
    }

    override suspend fun getAllHighlightsSnapshot(): List<Highlight> = withContext(ioDispatcher) {
        highlightDao.getAllHighlightsSnapshot().map(HighlightEntity::asExternalModel)
    }

    override suspend fun updateProgressIfNewer(
        bookId: String, cloudPage: Int, cloudTotal: Int, cloudProgress: Float
    ) = withContext(ioDispatcher) {
        val local = bookDao.getBookSnapshot(bookId) ?: return@withContext
        if (cloudPage > local.currentPage) {
            bookDao.updateBookProgress(bookId, cloudPage, cloudProgress)
        }
    }

    override suspend fun upsertHighlight(
        id: String, bookId: String, chapterNumber: Int, startOffset: Int,
        colorHex: String, selectedText: String, noteText: String
    ) = withContext(ioDispatcher) {
        val existing = highlightDao.getHighlightById(id)
        highlightDao.insertHighlight(
            HighlightEntity(
                id = id,
                bookId = bookId,
                chapterNumber = chapterNumber,
                startOffset = startOffset,
                endOffset = existing?.endOffset ?: startOffset,
                colorHex = colorHex,
                selectedText = selectedText,
                noteText = noteText,
                timestamp = existing?.timestamp ?: System.currentTimeMillis()
            )
        )
    }
}
