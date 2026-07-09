package com.pageturn.core.network

import android.util.Log
import com.pageturn.core.common.preferences.UserPreferencesDataSource
import com.pageturn.core.model.Book
import com.pageturn.core.model.Chapter
import com.pageturn.core.network.api.BackendSyncService
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitPageTurnNetworkApi @Inject constructor(
    private val syncService: BackendSyncService,
    private val preferences: UserPreferencesDataSource
) : PageTurnNetworkApi {

    override suspend fun getRecentReads(): List<Book> {
        return try {
            val response = syncService.getLibrary()
            val libraryList = response.data ?: emptyList()
            libraryList.map { dto ->
                Book(
                    id = dto.id.toString(),
                    title = dto.title,
                    author = dto.author.ifBlank { "Tác giả ẩn danh" },
                    coverUrl = dto.coverUrl ?: "",
                    progressPercent = 0f,
                    totalPages = 100,
                    currentPage = 0,
                    description = "Đã đồng bộ từ server"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getBookDetails(bookId: String): Book {
        val response = syncService.getLibrary()
        val libraryList = response.data ?: emptyList()
        val dto = libraryList.firstOrNull { it.id.toString() == bookId }
        if (dto != null) {
            return Book(
                id = dto.id.toString(),
                title = dto.title,
                author = dto.author.ifBlank { "Tác giả ẩn danh" },
                coverUrl = dto.coverUrl ?: "",
                progressPercent = 0f,
                totalPages = 100,
                currentPage = 0,
                description = "Đã đồng bộ từ server"
            )
        }
        throw NoSuchElementException("Book not found in library: $bookId")
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

    override suspend fun downloadBook(bookId: String): ByteArray {
        // Read the current access token from preferences and pass explicitly
        // OkHttp Authenticator does NOT retry @Streaming endpoints, so we must inject the token manually
        val token = preferences.accessToken.firstOrNull() ?: ""
        val bearerToken = if (token.isNotEmpty()) "Bearer $token" else ""

        val isStoreId = bookId.all { it.isDigit() }
        val response = if (isStoreId) {
            syncService.downloadPublicBook(bookId.toInt(), bearerToken)
        } else {
            syncService.downloadBook(bookId, bearerToken)
        }

        // Log Content-Type for diagnostics (check Logcat tag "PageTurnDownload")
        val contentType = response.headers()["Content-Type"] ?: "unknown"
        Log.d("PageTurnDownload", "HTTP ${response.code()} | Content-Type: $contentType | bookId: $bookId | tokenEmpty: ${token.isEmpty()}")

        if (response.isSuccessful) {
            val bytes = response.body()?.bytes()
                ?: throw Exception("Tải thất bại: Server trả về body rỗng (HTTP ${response.code()})")
            return bytes
        }

        // Parse error body to give better diagnostic info
        val errorBody = try { response.errorBody()?.string()?.take(300) } catch (_: Exception) { null }
        throw Exception("Tải thất bại: HTTP ${response.code()} | CT: $contentType${if (errorBody != null) " | $errorBody" else ""}")
    }

    override suspend fun deleteCloudBook(bookId: String) {
        val response = syncService.deleteBook(bookId, deletePhysicalFile = true)
        if (!response.isSuccessful) {
            throw Exception("Failed to delete book on cloud: HTTP ${response.code()}")
        }
    }
}
