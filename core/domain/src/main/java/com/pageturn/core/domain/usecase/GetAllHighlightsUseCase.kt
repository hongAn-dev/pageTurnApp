package com.pageturn.core.domain.usecase

import com.pageturn.core.domain.repository.BookRepository
import com.pageturn.core.model.Highlight
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllHighlightsUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    operator fun invoke(): Flow<List<Highlight>> {
        return bookRepository.getAllHighlights()
    }
}
