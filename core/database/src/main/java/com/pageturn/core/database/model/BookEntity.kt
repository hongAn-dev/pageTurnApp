package com.pageturn.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pageturn.core.model.Book

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val progressPercent: Float,
    val totalPages: Int,
    val currentPage: Int,
    val description: String
)

fun BookEntity.asExternalModel() = Book(
    id = id,
    title = title,
    author = author,
    coverUrl = coverUrl,
    progressPercent = progressPercent,
    totalPages = totalPages,
    currentPage = currentPage,
    description = description
)

fun Book.asEntity() = BookEntity(
    id = id,
    title = title,
    author = author,
    coverUrl = coverUrl,
    progressPercent = progressPercent,
    totalPages = totalPages,
    currentPage = currentPage,
    description = description
)
