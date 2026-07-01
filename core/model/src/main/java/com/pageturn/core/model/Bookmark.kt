package com.pageturn.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Bookmark(
    val id: String,
    val bookId: String,
    val chapterNumber: Int,
    val pageNumber: Int,
    val timestamp: Long
)
