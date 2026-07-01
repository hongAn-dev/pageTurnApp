package com.pageturn.core.database.di

import android.content.Context
import androidx.room.Room
import com.pageturn.core.database.dao.BookDao
import com.pageturn.core.database.dao.BookmarkDao
import com.pageturn.core.database.dao.HighlightDao
import com.pageturn.core.database.PageTurnDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providePageTurnDatabase(
        @ApplicationContext context: Context
    ): PageTurnDatabase = Room.databaseBuilder(
        context,
        PageTurnDatabase::class.java,
        "pageturn-database"
    )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideBookDao(database: PageTurnDatabase) = database.bookDao()

    @Provides
    fun provideBookmarkDao(database: PageTurnDatabase) = database.bookmarkDao()

    @Provides
    fun provideHighlightDao(database: PageTurnDatabase) = database.highlightDao()
}
