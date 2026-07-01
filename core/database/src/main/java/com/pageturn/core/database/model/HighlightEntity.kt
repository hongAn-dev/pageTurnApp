package com.pageturn.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pageturn.core.model.Highlight

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterNumber: Int,
    val startOffset: Int,
    val endOffset: Int,
    val colorHex: String,
    val noteText: String?,
    val timestamp: Long,
    val selectedText: String? = ""
)

fun HighlightEntity.asExternalModel() = Highlight(
    id = id,
    bookId = bookId,
    chapterNumber = chapterNumber,
    startOffset = startOffset,
    endOffset = endOffset,
    colorHex = colorHex,
    noteText = noteText,
    timestamp = timestamp,
    selectedText = selectedText
)

fun Highlight.asEntity() = HighlightEntity(
    id = id,
    bookId = bookId,
    chapterNumber = chapterNumber,
    startOffset = startOffset,
    endOffset = endOffset,
    colorHex = colorHex,
    noteText = noteText,
    timestamp = timestamp,
    selectedText = selectedText
)
