package com.pageturn.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pageturn.core.database.dao.BookDao
import com.pageturn.core.database.dao.BookmarkDao
import com.pageturn.core.database.dao.HighlightDao
import com.pageturn.core.database.model.BookEntity
import com.pageturn.core.database.model.BookmarkEntity
import com.pageturn.core.database.model.HighlightEntity

@Database(entities = [BookEntity::class, BookmarkEntity::class, HighlightEntity::class], version = 3, exportSchema = false)
abstract class PageTurnDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
}
