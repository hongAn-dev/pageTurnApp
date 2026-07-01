package com.pageturn.core.domain.usecase

import com.pageturn.core.domain.repository.BookRepository
import javax.inject.Inject

class RemoveHighlightUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    suspend operator fun invoke(highlightId: String) {
        bookRepository.removeHighlight(highlightId)
    }
}
