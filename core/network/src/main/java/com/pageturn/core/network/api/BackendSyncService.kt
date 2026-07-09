package com.pageturn.core.network.api

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

@Serializable
data class BookMetadataDto(
    val id: Long? = null,
    val title: String,
    val authors: List<String> = emptyList(),
    val fileFormat: String? = null
)

@Serializable
data class BookResponseDto(
    val id: Long,
    val title: String,
    val authors: List<String> = emptyList(),
    val fileFormat: String? = null,
    val coverUrl: String? = null,
    val fileUrl: String? = null,
    val fileSize: Long = 0,
    val cloudSynced: Boolean = false,
    val addedAt: String? = null,
    val updatedAt: String? = null
) {
    val author: String get() = authors.joinToString(", ")
}

@Serializable
data class UploadBookResponseDto(
    val id: Long,
    val fileUrl: String? = null,
    val publicId: String? = null,
    val fileFormat: String? = null,
    val fileSize: Long = 0,
    val cloudSynced: Boolean = false
)

@Serializable
data class ProgressDto(
    val bookId: Long,
    val chapterIdx: Int,
    val scrollPct: Float,
    val updatedAt: String? = null
)

@Serializable
data class BookmarkDto(
    val id: Long? = null,
    val bookId: Long,
    val chapterIdx: Int,
    val scrollPct: Float,
    val snippet: String,
    val updatedAt: String? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class HighlightDto(
    val id: Long? = null,
    val bookId: Long,
    val chapterIdx: Int,
    val startOffset: Int,
    val endOffset: Int,
    val textContent: String,
    val color: String,
    val note: String,
    val updatedAt: String? = null,
    val isDeleted: Boolean = false
)

@Serializable
data class CollectionBookItemDto(
    val bookId: Long,
    val position: Int = 0
)

@Serializable
data class CollectionDto(
    val id: Long? = null,
    val name: String,
    val description: String,
    val books: List<CollectionBookItemDto> = emptyList(),
    val updatedAt: String? = null
)

@Serializable
data class CollectionCreateRequest(
    val name: String,
    val description: String
)

@Serializable
data class CollectionAddBookRequest(
    val bookHash: String,
    val position: Int = 0
)

@Serializable
data class SyncPushRequest(
    val progress: List<ProgressDto> = emptyList(),
    val bookmarks: List<BookmarkDto> = emptyList(),
    val highlights: List<HighlightDto> = emptyList(),
    val collections: List<CollectionDto> = emptyList()
)

@Serializable
data class SyncPushResponse(
    val success: Boolean,
    val message: String? = null
)

@Serializable
data class SyncPullResponse(
    val progress: List<ProgressDto> = emptyList(),
    val bookmarks: List<BookmarkDto> = emptyList(),
    val highlights: List<HighlightDto> = emptyList(),
    val collections: List<CollectionDto> = emptyList(),
    val serverTime: String
)

@Serializable
data class SyncDeletesRequest(
    val bookmarkIds: List<Long> = emptyList(),
    val highlightIds: List<Long> = emptyList(),
    val collectionIds: List<Long> = emptyList()
)

@Serializable
data class TransferSendRequest(
    val receiverEmail: String,
    val bookHash: String
)

@Serializable
data class TransferDto(
    val id: String,
    val senderEmail: String,
    val receiverEmail: String,
    val bookHash: String,
    val status: String,
    val bookTitle: String? = null
)

@Serializable
data class PublicBookDto(
    val id: Int,
    val title: String,
    val authors: List<String> = emptyList(),
    val description: String? = null,
    val language: String? = null,
    val category: String? = null,
    val categoryId: Int? = null,
    val coverUrl: String? = null,
    val fileFormat: String? = null,
    val fileSize: Long = 0,
    val downloadCount: Int = 0
) {
    // Convenience helpers to match existing usages
    val author: String get() = authors.joinToString(", ")
    val bookHash: String get() = id.toString()
}

@Serializable
data class StoreResponse(
    val content: List<PublicBookDto>,
    val totalElements: Int = 0,
    val totalPages: Int = 0
)

interface BackendSyncService {
    // --- Library ---
    @GET("api/library")
    suspend fun getLibrary(): ApiResponse<List<BookResponseDto>>

    @POST("api/library")
    suspend fun upsertBookMetadata(@Body request: BookMetadataDto): ApiResponse<BookResponseDto>

    @Multipart
    @POST("api/library/{bookId}/upload")
    suspend fun uploadBookFile(
        @Path("bookId") bookId: Long,
        @Part file: MultipartBody.Part
    ): ApiResponse<UploadBookResponseDto>

    @DELETE("api/library/{bookHash}")
    suspend fun deleteBook(
        @Path("bookHash") bookHash: String,
        @Query("deletePhysicalFile") deletePhysicalFile: Boolean = true
    ): Response<Unit>

    // --- Progress ---
    @PUT("api/progress/{bookHash}")
    suspend fun saveProgress(
        @Path("bookHash") bookHash: String,
        @Body progress: ProgressDto
    ): Response<Unit>

    @GET("api/progress/{bookHash}")
    suspend fun getProgress(@Path("bookHash") bookHash: String): ApiResponse<ProgressDto>

    // --- Bookmarks ---
    @GET("api/books/{bookHash}/bookmarks")
    suspend fun getBookmarks(@Path("bookHash") bookHash: String): ApiResponse<List<BookmarkDto>>

    @POST("api/books/{bookHash}/bookmarks")
    suspend fun createBookmark(
        @Path("bookHash") bookHash: String,
        @Body request: BookmarkDto
    ): ApiResponse<BookmarkDto>

    @PUT("api/bookmarks/{bookmarkId}")
    suspend fun updateBookmark(
        @Path("bookmarkId") bookmarkId: String,
        @Body request: BookmarkDto
    ): ApiResponse<BookmarkDto>

    @DELETE("api/bookmarks/{bookmarkId}")
    suspend fun deleteBookmark(@Path("bookmarkId") bookmarkId: String): Response<Unit>

    // --- Highlights ---
    @GET("api/books/{bookHash}/highlights")
    suspend fun getHighlights(@Path("bookHash") bookHash: String): ApiResponse<List<HighlightDto>>

    @POST("api/books/{bookHash}/highlights")
    suspend fun createHighlight(
        @Path("bookHash") bookHash: String,
        @Body request: HighlightDto
    ): ApiResponse<HighlightDto>

    @PUT("api/highlights/{highlightId}")
    suspend fun updateHighlight(
        @Path("highlightId") highlightId: String,
        @Body request: HighlightDto
    ): ApiResponse<HighlightDto>

    @DELETE("api/highlights/{highlightId}")
    suspend fun deleteHighlight(@Path("highlightId") highlightId: String): Response<Unit>

    // --- Collections ---
    @GET("api/collections")
    suspend fun getCollections(): ApiResponse<List<CollectionDto>>

    @POST("api/collections")
    suspend fun createCollection(@Body request: CollectionCreateRequest): ApiResponse<CollectionDto>

    @PUT("api/collections/{collectionId}")
    suspend fun updateCollection(
        @Path("collectionId") collectionId: String,
        @Body request: CollectionCreateRequest
    ): ApiResponse<CollectionDto>

    @DELETE("api/collections/{collectionId}")
    suspend fun deleteCollection(@Path("collectionId") collectionId: String): Response<Unit>

    @POST("api/collections/{collectionId}/books")
    suspend fun addBookToCollection(
        @Path("collectionId") collectionId: String,
        @Body request: CollectionAddBookRequest
    ): Response<Unit>

    @DELETE("api/collections/{collectionId}/books/{bookHash}")
    suspend fun removeBookFromCollection(
        @Path("collectionId") collectionId: String,
        @Path("bookHash") bookHash: String
    ): Response<Unit>

    // --- Sync ---
    @POST("api/sync/push")
    suspend fun pushSyncChanges(@Body request: SyncPushRequest): ApiResponse<SyncPushResponse>

    @GET("api/sync/pull")
    suspend fun pullSyncChanges(@Query("since") since: String): ApiResponse<SyncPullResponse>

    @POST("api/sync/deletes")
    suspend fun pushDeletedRecords(@Body request: SyncDeletesRequest): Response<Unit>

    // --- Transfers ---
    @POST("api/transfers")
    suspend fun sendBook(@Body request: TransferSendRequest): Response<Unit>

    @GET("api/transfers/inbox")
    suspend fun getInbox(): ApiResponse<List<TransferDto>>

    @PUT("api/transfers/{transferId}/accept")
    suspend fun acceptTransfer(@Path("transferId") transferId: String): Response<Unit>

    @PUT("api/transfers/{transferId}/decline")
    suspend fun declineTransfer(@Path("transferId") transferId: String): Response<Unit>

    // --- Store ---
    @GET("api/store")
    suspend fun listPublicBooks(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("category") category: String?,
        @Query("q") query: String?
    ): ApiResponse<StoreResponse>

    @GET("api/store/{storeBookId}")
    suspend fun getPublicBookDetail(@Path("storeBookId") storeBookId: Int): ApiResponse<PublicBookDto>

    @Streaming
    @GET("api/store/{storeBookId}/download")
    suspend fun downloadPublicBook(
        @Path("storeBookId") storeBookId: Int,
        @Header("Authorization") authToken: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    @Streaming
    @GET("api/library/{bookId}/download")
    suspend fun downloadBook(
        @Path("bookId") bookId: String,
        @Header("Authorization") authToken: String
    ): retrofit2.Response<okhttp3.ResponseBody>
}
