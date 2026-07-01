package com.pageturn.core.domain.usecase

import com.pageturn.core.domain.repository.BookRepository
import javax.inject.Inject

class UpdateProgressUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    suspend operator fun invoke(bookId: String, page: Int, progressPercent: Float) {
        bookRepository.updateReadingProgress(bookId, page, progressPercent)
    }
}
