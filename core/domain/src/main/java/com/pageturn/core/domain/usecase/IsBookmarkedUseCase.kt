package com.pageturn.core.domain.usecase

import com.pageturn.core.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsBookmarkedUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    operator fun invoke(bookId: String, chapterNumber: Int, pageNumber: Int): Flow<Boolean> {
        val id = "${bookId}_${chapterNumber}_${pageNumber}"
        return bookRepository.isBookmarked(id)
    }
}
