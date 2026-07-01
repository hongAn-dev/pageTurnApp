package com.pageturn.core.domain.usecase

import com.pageturn.core.domain.repository.BookRepository
import javax.inject.Inject

class RemoveHighlightsByParagraphUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    suspend operator fun invoke(bookId: String, chapterNumber: Int, startOffset: Int) {
        bookRepository.removeHighlightsByParagraph(bookId, chapterNumber, startOffset)
    }
}
