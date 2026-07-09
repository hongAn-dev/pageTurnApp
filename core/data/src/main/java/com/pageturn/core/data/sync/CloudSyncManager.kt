package com.pageturn.core.data.sync

import android.content.Context
import android.util.Log
import com.pageturn.core.common.preferences.UserPreferencesDataSource
import com.pageturn.core.domain.repository.BookRepository
import com.pageturn.core.network.api.BackendSyncService
import com.pageturn.core.network.api.BookMetadataDto
import com.pageturn.core.network.api.HighlightDto
import com.pageturn.core.network.api.ProgressDto
import com.pageturn.core.network.api.SyncPushRequest
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

sealed class SyncResult {
    data class Success(val books: Int, val highlights: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

@Singleton
class CloudSyncManager @Inject constructor(
    private val bookRepository: BookRepository,
    private val syncService: BackendSyncService,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    @ApplicationContext private val context: Context
) {
    private val tag = "CloudSyncManager"

    suspend fun isUserSignedIn(): Boolean {
        val token = userPreferencesDataSource.accessToken.firstOrNull()
        return !token.isNullOrEmpty()
    }

    suspend fun getFirebaseUserEmail(): String? {
        return userPreferencesDataSource.userProfile.firstOrNull()?.email
    }

    fun isAnonymous(): Boolean {
        return false
    }

    // -------------------------------------------------------------------
    // PUSH — local → cloud
    // -------------------------------------------------------------------
    suspend fun pushToCloud(): SyncResult {
        if (!isUserSignedIn()) return SyncResult.Error("Người dùng chưa đăng nhập")

        return try {
            val books = bookRepository.getAllBooksSnapshot()
            val cloudBookIds = syncBookMetadata(books)
            val readerPrefs = context.getSharedPreferences("reader_progress", Context.MODE_PRIVATE)
            val progress = books.mapNotNull { book ->
                val serverBookId = cloudBookIds[book.id] ?: return@mapNotNull null
                ProgressDto(
                    bookId = serverBookId,
                    chapterIdx = readerPrefs.getInt("last_chapter_${book.id}", 1),
                    scrollPct = book.progressPercent.coerceIn(0f, 1f)
                )
            }

            val highlightsSnapshot = bookRepository.getAllHighlightsSnapshot()
            val highlights = highlightsSnapshot.mapNotNull { h ->
                val serverBookId = cloudBookIds[h.bookId] ?: return@mapNotNull null
                HighlightDto(
                    id = h.id.toLongOrNull(),
                    bookId = serverBookId,
                    chapterIdx = h.chapterNumber,
                    startOffset = h.startOffset,
                    endOffset = h.endOffset,
                    textContent = h.selectedText?.takeIf { it.isNotBlank() } ?: h.noteText?.takeIf { it.isNotBlank() } ?: "Highlight",
                    color = h.colorHex,
                    note = h.noteText ?: ""
                )
            }

            val request = SyncPushRequest(
                progress = progress,
                highlights = highlights
            )

            val response = syncService.pushSyncChanges(request)
            if (response.success) {
                Log.i(tag, "Push success: ${cloudBookIds.size} books, ${highlights.size} highlights")
                SyncResult.Success(cloudBookIds.size, highlights.size)
            } else {
                SyncResult.Error(response.message ?: "Đẩy dữ liệu thất bại")
            }
        } catch (e: Exception) {
            Log.e(tag, "Push failed", e)
            SyncResult.Error(e.message ?: "Lỗi không xác định")
        }
    }

    private suspend fun syncBookMetadata(books: List<com.pageturn.core.model.Book>): Map<String, Long> {
        val cloudBookIds = mutableMapOf<String, Long>()
        books.forEach { book ->
            try {
                val response = syncService.upsertBookMetadata(
                    BookMetadataDto(
                        id = book.id.toLongOrNull(),
                        title = book.title,
                        authors = listOf(book.author).filter { it.isNotBlank() },
                        fileFormat = inferFileFormat(book.id)
                    )
                )
                val serverBook = response.data
                if (response.success && serverBook != null) {
                    cloudBookIds[book.id] = serverBook.id
                    if (!serverBook.cloudSynced) {
                        uploadLocalBookFile(book.id, serverBook.id)
                    }
                } else {
                    Log.w(tag, "Book metadata sync skipped for ${book.id}: ${response.message}")
                }
            } catch (e: Exception) {
                Log.w(tag, "Book metadata sync failed for ${book.id}", e)
            }
        }
        return cloudBookIds
    }

    private suspend fun uploadLocalBookFile(localBookId: String, serverBookId: Long) {
        val file = localBookFile(localBookId) ?: return
        val mediaType = when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "epub" -> "application/epub+zip"
            else -> "application/octet-stream"
        }.toMediaTypeOrNull()
        val body = file.asRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        val response = syncService.uploadBookFile(serverBookId, part)
        if (!response.success) {
            Log.w(tag, "Book file upload failed for $localBookId: ${response.message}")
        }
    }

    private fun localBookFile(bookId: String): java.io.File? {
        val dir = java.io.File(context.filesDir, bookId)
        return listOf("book.epub", "book.pdf")
            .map { java.io.File(dir, it) }
            .firstOrNull { it.exists() && it.length() > 0 }
    }

    private fun inferFileFormat(bookId: String): String? {
        return localBookFile(bookId)?.extension?.lowercase()
    }

    // -------------------------------------------------------------------
    // PULL — cloud → local (merge, not overwrite)
    // -------------------------------------------------------------------
    suspend fun pullFromCloud(): SyncResult {
        if (!isUserSignedIn()) return SyncResult.Error("Người dùng chưa đăng nhập")

        return try {
            bookRepository.sync()

            // Using a default/epoch timestamp or we could track lastSyncTimestamp in datastore
            val pullResponse = syncService.pullSyncChanges(since = "1970-01-01T00:00:00Z")
            val syncData = pullResponse.data ?: throw Exception(pullResponse.message ?: "Đồng bộ thất bại")

            var updatedBooks = 0
            syncData.progress.forEach { p ->
                bookRepository.updateProgressIfNewer(
                    bookId = p.bookId.toString(),
                    cloudPage = p.chapterIdx,
                    cloudTotal = 100,
                    cloudProgress = p.scrollPct
                )
                updatedBooks++
            }

            var updatedHighlights = 0
            syncData.highlights.forEach { h ->
                val id = h.id ?: return@forEach
                bookRepository.upsertHighlight(
                    id = id.toString(),
                    bookId = h.bookId.toString(),
                    chapterNumber = h.chapterIdx,
                    startOffset = h.startOffset,
                    colorHex = h.color,
                    selectedText = h.textContent,
                    noteText = h.note
                )
                updatedHighlights++
            }

            Log.i(tag, "Pull success: $updatedBooks progress records, $updatedHighlights highlights")
            SyncResult.Success(updatedBooks, updatedHighlights)
        } catch (e: Exception) {
            Log.e(tag, "Pull failed", e)
            SyncResult.Error(e.message ?: "Lỗi không xác định")
        }
    }
}
