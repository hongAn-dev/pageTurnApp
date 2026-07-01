package com.pageturn.core.domain.usecase

import com.pageturn.core.domain.repository.BookRepository
import com.pageturn.core.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class GetRecentReadsUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    operator fun invoke(): Flow<List<Book>> {
        return bookRepository.getRecentReads()
            .onStart {
                try {
                    bookRepository.sync()
                } catch (e: Exception) {
                    // Ignore network issues during sync, fallback to local DB flow
                }
            }
    }
}
