package com.pageturn.core.data.repository

import com.pageturn.core.database.dao.BookDao
import com.pageturn.core.database.dao.BookmarkDao
import com.pageturn.core.database.dao.HighlightDao
import com.pageturn.core.database.model.BookEntity
import com.pageturn.core.model.Book
import com.pageturn.core.network.PageTurnNetworkApi
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookRepositoryImplTest {

    private val bookDao: BookDao = mockk(relaxed = true)
    private val bookmarkDao: BookmarkDao = mockk(relaxed = true)
    private val highlightDao: HighlightDao = mockk(relaxed = true)
    private val networkApi: PageTurnNetworkApi = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val repository = BookRepositoryImpl(
        bookDao = bookDao,
        bookmarkDao = bookmarkDao,
        highlightDao = highlightDao,
        networkApi = networkApi,
        ioDispatcher = testDispatcher
    )

    @Test
    fun addLocalBook_shouldSaveToDatabase() = runTest(testDispatcher) {
        // Arrange
        val localBook = Book(
            id = "local_12345",
            title = "Test Local Book",
            author = "Test Author",
            coverUrl = "",
            progressPercent = 0f,
            totalPages = 100,
            currentPage = 0,
            description = "Test Description"
        )

        // Act
        repository.addLocalBook(localBook)

        // Assert
        coVerify(exactly = 1) {
            bookDao.insertOrReplaceBooks(any())
        }
    }

    @Test
    fun getChapter_forLocalBook_shouldReadFromDatabaseAndReturnLocalContent() = runTest(testDispatcher) {
        // Arrange
        val bookId = "local_12345"
        val localBookEntity = BookEntity(
            id = bookId,
            title = "Test Local Book",
            author = "Test Author",
            coverUrl = "",
            progressPercent = 0f,
            totalPages = 100,
            currentPage = 0,
            description = "Test Description"
        )
        every { bookDao.getBook(bookId) } returns flowOf(localBookEntity)

        // Act
        val chapter = repository.getChapter(bookId, 1)

        // Assert
        assertEquals("${bookId}_1", chapter.id)
        assertEquals(bookId, chapter.bookId)
        assertTrue(chapter.title.contains("Test Local Book"))
        assertTrue(chapter.content.contains("Test Description"))
        coVerify(exactly = 0) {
            networkApi.getChapter(any(), any())
        }
    }
}
