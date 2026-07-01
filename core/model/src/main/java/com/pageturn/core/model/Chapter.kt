package com.pageturn.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    val id: String,
    val bookId: String,
    val title: String,
    val content: String,
    val chapterNumber: Int,
    val imageUrl: String? = null
)
