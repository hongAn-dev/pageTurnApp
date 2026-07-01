package com.pageturn.core.domain.usecase

import com.pageturn.core.domain.repository.BookRepository
import com.pageturn.core.model.Bookmark
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBookmarksUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    operator fun invoke(bookId: String): Flow<List<Bookmark>> {
        return bookRepository.getBookmarks(bookId)
    }
}
