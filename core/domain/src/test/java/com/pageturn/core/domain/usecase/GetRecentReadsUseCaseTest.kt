package com.pageturn.core.domain.usecase

import com.pageturn.core.domain.repository.BookRepository
import com.pageturn.core.model.Book
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetRecentReadsUseCaseTest {

    private val bookRepository: BookRepository = mockk(relaxed = true)
    private val useCase = GetRecentReadsUseCase(bookRepository)

    @Test
    fun `invoke should sync and return books flow`() = runTest {
        // Arrange
        val expectedBooks = listOf(
            Book(
                id = "1",
                title = "Sherlock Holmes",
                author = "Arthur Conan Doyle",
                coverUrl = "url",
                progressPercent = 0.5f,
                totalPages = 100,
                currentPage = 50
            )
        )
        coEvery { bookRepository.getRecentReads() } returns flowOf(expectedBooks)

        // Act
        val result = useCase().toList()

        // Assert
        assertEquals(1, result.size)
        assertEquals(expectedBooks, result.first())
        coVerify(exactly = 1) { bookRepository.sync() }
    }
}
