package com.pageturn.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Highlight(
    val id: String,
    val bookId: String,
    val chapterNumber: Int,
    val startOffset: Int,
    val endOffset: Int,
    val colorHex: String,
    val noteText: String?,
    val timestamp: Long,
    val selectedText: String? = ""
)
