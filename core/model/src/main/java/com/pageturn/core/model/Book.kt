package com.pageturn.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val progressPercent: Float,
    val totalPages: Int,
    val currentPage: Int,
    val description: String = ""
)
