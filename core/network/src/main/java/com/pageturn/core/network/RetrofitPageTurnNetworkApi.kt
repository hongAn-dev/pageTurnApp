package com.pageturn.core.network

import com.pageturn.core.model.Book
import com.pageturn.core.model.Chapter
import com.pageturn.core.network.api.BackendSyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitPageTurnNetworkApi @Inject constructor(
    private val syncService: BackendSyncService
) : PageTurnNetworkApi {

    override suspend fun getRecentReads(): List<Book> {
        return try {
            val response = syncService.getLibrary()
            val libraryList = response.data ?: emptyList()
            val remoteBooks = libraryList.map { dto ->
                Book(
                    id = dto.bookHash,
                    title = dto.title,
                    author = dto.author,
                    coverUrl = dto.coverUrl ?: "",
                    progressPercent = 0f,
                    totalPages = 100,
                    currentPage = 0,
                    description = dto.description ?: ""
                )
            }
            // Merge remote books with fake books so that the UI always has content to display
            val fakeBooks = FakePageTurnNetworkApi().getRecentReads()
            (remoteBooks + fakeBooks).distinctBy { it.id }
        } catch (e: Exception) {
            FakePageTurnNetworkApi().getRecentReads()
        }
    }

    override suspend fun getBookDetails(bookId: String): Book {
        try {
            val response = syncService.getLibrary()
            val libraryList = response.data ?: emptyList()
            val dto = libraryList.firstOrNull { it.bookHash == bookId }
            if (dto != null) {
                return Book(
                    id = dto.bookHash,
                    title = dto.title,
                    author = dto.author,
                    coverUrl = dto.coverUrl ?: "",
                    progressPercent = 0f,
                    totalPages = 100,
                    currentPage = 0,
                    description = dto.description ?: ""
                )
            }
        } catch (e: Exception) {
            // ignore network error and try fallback
        }

        // Fallback to fake books if not found on the server
        return try {
            FakePageTurnNetworkApi().getBookDetails(bookId)
        } catch (e: Exception) {
            throw NoSuchElementException("Book not found in library: $bookId")
        }
    }

    override suspend fun getChapter(bookId: String, chapterNumber: Int): Chapter {
        val book = getBookDetails(bookId)
        return Chapter(
            id = "${bookId}_$chapterNumber",
            bookId = bookId,
            title = "Chương $chapterNumber",
            chapterNumber = chapterNumber,
            content = """
                Nội dung Chương $chapterNumber của cuốn sách "${book.title}".
                
                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
            """.trimIndent(),
            imageUrl = ""
        )
    }
}
