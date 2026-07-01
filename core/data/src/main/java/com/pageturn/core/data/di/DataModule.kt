package com.pageturn.core.data.di

import com.pageturn.core.data.repository.BookRepositoryImpl
import com.pageturn.core.domain.repository.BookRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds
    @Singleton
    fun bindBookRepository(
        bookRepositoryImpl: BookRepositoryImpl
    ): BookRepository
}
