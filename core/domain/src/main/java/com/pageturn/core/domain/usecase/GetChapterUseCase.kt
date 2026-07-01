package com.pageturn.core.domain.usecase

import com.pageturn.core.domain.repository.BookRepository
import com.pageturn.core.model.Chapter
import javax.inject.Inject

class GetChapterUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    suspend operator fun invoke(bookId: String, chapterNumber: Int): Chapter {
        return bookRepository.getChapter(bookId, chapterNumber)
    }
}
