package com.pageturn.core.domain.usecase

import com.pageturn.core.domain.repository.BookRepository
import javax.inject.Inject

class AddHighlightUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    suspend operator fun invoke(
        bookId: String,
        chapterNumber: Int,
        startOffset: Int,
        endOffset: Int,
        colorHex: String,
        noteText: String? = null,
        selectedText: String? = null
    ) {
        if (!selectedText.isNullOrEmpty()) {
            bookRepository.removeHighlightByText(bookId, chapterNumber, startOffset, selectedText)
        }
        bookRepository.addHighlight(bookId, chapterNumber, startOffset, endOffset, colorHex, noteText, selectedText)
    }
}
