package com.pageturn.core.domain.usecase

import com.pageturn.core.domain.repository.BookRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ToggleBookmarkUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    suspend operator fun invoke(bookId: String, chapterNumber: Int, pageNumber: Int) {
        val id = "${bookId}_${chapterNumber}_${pageNumber}"
        val exists = bookRepository.isBookmarked(id).first()
        if (exists) {
            bookRepository.removeBookmark(id)
        } else {
            bookRepository.addBookmark(bookId, chapterNumber, pageNumber)
        }
    }
}
