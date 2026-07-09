package com.pageturn.core.network

import com.pageturn.core.model.Book
import com.pageturn.core.model.Chapter

interface PageTurnNetworkApi {
    suspend fun getRecentReads(): List<Book>
    suspend fun getBookDetails(bookId: String): Book
    suspend fun getChapter(bookId: String, chapterNumber: Int): Chapter
    suspend fun downloadBook(bookId: String): ByteArray
    suspend fun deleteCloudBook(bookId: String)
}

