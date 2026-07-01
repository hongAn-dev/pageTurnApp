package com.pageturn.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterNumber: Int,
    val pageNumber: Int,
    val timestamp: Long
)

fun BookmarkEntity.asExternalModel() = com.pageturn.core.model.Bookmark(
    id = id,
    bookId = bookId,
    chapterNumber = chapterNumber,
    pageNumber = pageNumber,
    timestamp = timestamp
)

fun com.pageturn.core.model.Bookmark.asEntity() = BookmarkEntity(
    id = id,
    bookId = bookId,
    chapterNumber = chapterNumber,
    pageNumber = pageNumber,
    timestamp = timestamp
)
