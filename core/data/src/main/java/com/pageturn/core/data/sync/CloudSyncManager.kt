package com.pageturn.core.data.sync

import android.content.Context
import android.util.Log
import com.pageturn.core.common.preferences.UserPreferencesDataSource
import com.pageturn.core.domain.repository.BookRepository
import com.pageturn.core.network.api.BackendSyncService
import com.pageturn.core.network.api.HighlightDto
import com.pageturn.core.network.api.ProgressDto
import com.pageturn.core.network.api.SyncPushRequest
import kotlinx.coroutines.flow.firstOrNull
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
            val readerPrefs = context.getSharedPreferences("reader_progress", Context.MODE_PRIVATE)
            val progress = books.map { book ->
                ProgressDto(
                    bookHash = book.id,
                    chapterIdx = readerPrefs.getInt("last_chapter_${book.id}", 1),
                    scrollPct = book.progressPercent.coerceIn(0f, 1f)
                )
            }

            val highlightsSnapshot = bookRepository.getAllHighlightsSnapshot()
            val highlights = highlightsSnapshot.map { h ->
                HighlightDto(
                    id = h.id,
                    bookHash = h.bookId,
                    chapterIdx = h.chapterNumber,
                    startOffset = h.startOffset,
                    endOffset = h.endOffset,
                    textContent = h.selectedText ?: "",
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
                Log.i(tag, "Push success: ${books.size} books, ${highlights.size} highlights")
                SyncResult.Success(books.size, highlights.size)
            } else {
                SyncResult.Error(response.message ?: "Đẩy dữ liệu thất bại")
            }
        } catch (e: Exception) {
            Log.e(tag, "Push failed", e)
            SyncResult.Error(e.message ?: "Lỗi không xác định")
        }
    }

    // -------------------------------------------------------------------
    // PULL — cloud → local (merge, not overwrite)
    // -------------------------------------------------------------------
    suspend fun pullFromCloud(): SyncResult {
        if (!isUserSignedIn()) return SyncResult.Error("Người dùng chưa đăng nhập")

        return try {
            // Using a default/epoch timestamp or we could track lastSyncTimestamp in datastore
            val pullResponse = syncService.pullSyncChanges(since = "1970-01-01T00:00:00Z")
            val syncData = pullResponse.data ?: throw Exception(pullResponse.message ?: "Đồng bộ thất bại")

            var updatedBooks = 0
            syncData.progress.forEach { p ->
                bookRepository.updateProgressIfNewer(
                    bookId = p.bookHash,
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
                    id = id,
                    bookId = h.bookHash,
                    chapterNumber = h.chapterIdx,
                    startOffset = h.startOffset,
                    colorHex = h.color,
                    selectedText = h.textContent,
                    noteText = h.note
                )
                updatedHighlights++
            }

            Log.i(tag, "Pull success: $updatedBooks books, $updatedHighlights highlights")
            SyncResult.Success(updatedBooks, updatedHighlights)
        } catch (e: Exception) {
            Log.e(tag, "Pull failed", e)
            SyncResult.Error(e.message ?: "Lỗi không xác định")
        }
    }
}
