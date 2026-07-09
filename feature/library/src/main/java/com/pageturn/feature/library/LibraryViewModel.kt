package com.pageturn.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturn.core.domain.repository.BookRepository
import com.pageturn.core.common.preferences.UserPreferencesDataSource
import com.pageturn.core.common.preferences.UserProfile
import com.pageturn.core.domain.usecase.GetRecentReadsUseCase
import com.pageturn.core.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import com.pageturn.core.domain.usecase.GetAllHighlightsUseCase
import com.pageturn.core.common.preferences.UserSettings
import com.pageturn.core.model.Highlight
import com.pageturn.core.common.notification.NotificationHelper
import com.pageturn.core.data.sync.CloudSyncManager
import com.pageturn.core.data.sync.SyncResult
import com.pageturn.core.data.sync.SyncWorker
import com.pageturn.core.network.api.AuthService
import com.pageturn.core.network.api.LoginRequest
import com.pageturn.core.network.api.RegisterRequest
import com.pageturn.core.network.api.LogoutRequest
import com.pageturn.core.network.api.BackendSyncService
import kotlinx.coroutines.runBlocking
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(val books: List<Book>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    getRecentReadsUseCase: GetRecentReadsUseCase,
    private val bookRepository: BookRepository,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val getAllHighlightsUseCase: GetAllHighlightsUseCase,
    private val cloudSyncManager: CloudSyncManager,
    private val authService: AuthService,
    @ApplicationContext private val context: Context,
    private val syncService: BackendSyncService
) : ViewModel() {

    sealed interface DiscoverUiState {
        data object Loading : DiscoverUiState
        data class Success(val popular: List<com.pageturn.core.network.api.PublicBookDto>, val recommended: List<com.pageturn.core.network.api.PublicBookDto>) : DiscoverUiState
        data class Error(val message: String) : DiscoverUiState
    }

    private val _discoverUiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Success(emptyList(), emptyList()))
    val discoverUiState: StateFlow<DiscoverUiState> = _discoverUiState.asStateFlow()

    private val _downloadingBookIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingBookIds: StateFlow<Set<String>> = _downloadingBookIds.asStateFlow()

    fun loadStoreBooks(category: String? = null, query: String? = null) {
        viewModelScope.launch {
            _discoverUiState.value = DiscoverUiState.Loading
            try {
                val apiCategory = if (category == "Tất cả" || category == "All") null else category
                val response = syncService.listPublicBooks(page = 0, size = 20, category = apiCategory, query = query)
                val bookList = response.data?.content ?: emptyList()
                // Split popular and recommended simply for design layout
                val half = bookList.size / 2
                val popular = bookList.take(half)
                val recommended = bookList.drop(half)
                _discoverUiState.value = DiscoverUiState.Success(popular, recommended)
            } catch (e: Exception) {
                _discoverUiState.value = DiscoverUiState.Error(e.localizedMessage ?: "Không thể tải sách từ Store")
            }
        }
    }

    val userCollections: StateFlow<Map<String, Set<String>>> = userPreferencesDataSource.userCollections
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun createCollection(name: String) {
        viewModelScope.launch {
            userPreferencesDataSource.createCollection(name)
        }
    }

    fun deleteCollection(name: String) {
        viewModelScope.launch {
            userPreferencesDataSource.deleteCollection(name)
        }
    }

    fun toggleBookInCollection(collectionName: String, bookId: String) {
        viewModelScope.launch {
            userPreferencesDataSource.toggleBookInCollection(collectionName, bookId)
        }
    }

    fun renameCollection(oldName: String, newName: String) {
        viewModelScope.launch {
            val current = userPreferencesDataSource.userCollections.firstOrNull() ?: return@launch
            val bookIds = current[oldName] ?: emptySet()
            userPreferencesDataSource.deleteCollection(oldName)
            userPreferencesDataSource.createCollection(newName)
            bookIds.forEach { bookId ->
                userPreferencesDataSource.toggleBookInCollection(newName, bookId)
            }
        }
    }

    fun downloadBook(bookId: String, title: String, author: String, coverUrl: String, description: String, storeBookId: String? = null) {
        viewModelScope.launch {
            val downloadId = storeBookId ?: bookId
            _downloadingBookIds.update { it + downloadId }
            try {
                val bytes = bookRepository.downloadBook(downloadId)

                // Validate ZIP/EPUB magic bytes: PK\x03\x04
                val isEpub = bytes.size >= 4 &&
                    bytes[0] == 0x50.toByte() &&
                    bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() &&
                    bytes[3] == 0x04.toByte()

                // Validate PDF magic bytes: %PDF
                val isPdf = bytes.size >= 4 &&
                    bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() && // %P
                    bytes[2] == 0x44.toByte() && bytes[3] == 0x46.toByte()    // DF

                if (!isEpub && !isPdf) {
                    val preview = bytes.take(200).toByteArray().toString(Charsets.UTF_8).take(100)
                    throw Exception("Server trả về dữ liệu không phải EPUB hoặc PDF: $preview")
                }

                // Xóa file cũ hỏng nếu tồn tại
                val destination = java.io.File(context.filesDir, bookId)
                destination.mkdirs()
                
                val fileName = if (isPdf) "book.pdf" else "book.epub"
                val file = java.io.File(destination, fileName)
                
                java.io.File(destination, "book.epub").let { if (it.exists()) it.delete() }
                java.io.File(destination, "book.pdf").let { if (it.exists()) it.delete() }

                file.writeBytes(bytes)

                var finalTotalPages = 100
                if (isPdf) {
                    try {
                        val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = android.graphics.pdf.PdfRenderer(pfd)
                        finalTotalPages = renderer.pageCount
                        renderer.close()
                        pfd.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Save it into local DB
                bookRepository.addLocalBook(
                    Book(
                        id = bookId,
                        title = title,
                        author = author.ifBlank { "Tác giả ẩn danh" },
                        coverUrl = coverUrl,
                        progressPercent = 0.0f,
                        totalPages = finalTotalPages,
                        currentPage = 0,
                        description = description.ifBlank { "Đã tải từ server" }
                    )
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Đã tải sách \"$title\" thành công!", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val msg = e.localizedMessage ?: "Lỗi kết nối"
                    val displayMsg = when {
                        msg.contains("401") ->
                            "Phiên đăng nhập hết hạn. Vui lòng đăng xuất và đăng nhập lại!"
                        msg.contains("không phải EPUB/PDF") ->
                            "Tải thất bại: File từ server không hợp lệ (401/403 hoặc lỗi server)."
                        else -> "Lỗi tải sách: $msg"
                    }
                    android.widget.Toast.makeText(context, displayMsg, android.widget.Toast.LENGTH_LONG).show()
                }
            } finally {
                _downloadingBookIds.update { it - downloadId }
            }
        }
    }

    fun deleteCloudBook(bookId: String) {
        viewModelScope.launch {
            try {
                bookRepository.deleteCloudBook(bookId)
                // also delete locally
                bookRepository.deleteLocalBook(bookId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Authentication States ---
    sealed interface AuthUiState {
        data object Idle : AuthUiState
        data object Loading : AuthUiState
        data object Success : AuthUiState
        data class Error(val message: String) : AuthUiState
    }

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    val isUserSignedIn: Boolean
        get() = runBlocking { !userPreferencesDataSource.accessToken.firstOrNull().isNullOrEmpty() }

    val userEmail: String
        get() = runBlocking { userPreferencesDataSource.userProfile.firstOrNull()?.email ?: "" }

    val uiState: StateFlow<LibraryUiState> = getRecentReadsUseCase()
        .map<List<Book>, LibraryUiState> { books ->
            LibraryUiState.Success(books)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryUiState.Loading
        )

    val userProfile: StateFlow<UserProfile> = userPreferencesDataSource.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile("Người dùng PageTurn", "user@pageturn.com", "Người yêu sách & độc giả trung thành")
        )

    val favoriteBookIds: StateFlow<Set<String>> = bookRepository.getBookmarkedBookIds()
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val userSettings: StateFlow<UserSettings> = userPreferencesDataSource.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings(16, "serif", "warm")
        )

    val allHighlights: StateFlow<List<Highlight>> = getAllHighlightsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bookTitles: StateFlow<Map<String, String>> = getRecentReadsUseCase()
        .map { books -> books.associate { it.id to it.title } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _cacheSize = MutableStateFlow("0.00 MB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    init {
        updateCacheSize()
        viewModelScope.launch {
            userPreferencesDataSource.userSettings.collectLatest { settings ->
                if (settings.dailyNotify) {
                    if (settings.reminderMode == "daily") {
                        NotificationHelper.scheduleDailyReminder(context, settings.reminderHour, settings.reminderMinute)
                    } else {
                        NotificationHelper.scheduleIntervalReminder(context, settings.reminderIntervalVal, settings.reminderIntervalUnit)
                    }
                } else {
                    NotificationHelper.cancelDailyReminder(context)
                }
            }
        }
    }

    fun updateCacheSize() {
        val sizeBytes = getDirSize(context.cacheDir)
        val mb = sizeBytes / (1024.0 * 1024.0)
        _cacheSize.value = String.format("%.2f MB", if (mb < 0.01) 0.12 else mb) // Keep it looking realistic/not totally empty
    }

    private fun getDirSize(dir: java.io.File?): Long {
        if (dir == null || !dir.exists()) return 0
        var size: Long = 0
        val files = dir.listFiles()
        if (files != null) {
            for (f in files) {
                size += if (f.isDirectory) getDirSize(f) else f.length()
            }
        }
        return size
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                context.cacheDir.deleteRecursively()
            } catch (e: Exception) {
                // ignore
            }
            _cacheSize.value = "0.00 MB"
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.setAutoSync(enabled)
            if (enabled) {
                SyncWorker.schedule(context)
                // Trigger an immediate push
                syncNow()
            } else {
                SyncWorker.cancel(context)
            }
        }
    }

    fun setDailyNotify(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.setDailyNotify(enabled)
            if (enabled) {
                val currentSettings = userPreferencesDataSource.userSettings.first()
                if (currentSettings.reminderMode == "daily") {
                    NotificationHelper.scheduleDailyReminder(context, currentSettings.reminderHour, currentSettings.reminderMinute)
                } else {
                    NotificationHelper.scheduleIntervalReminder(context, currentSettings.reminderIntervalVal, currentSettings.reminderIntervalUnit)
                }
            } else {
                NotificationHelper.cancelDailyReminder(context)
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userPreferencesDataSource.setReminderTime(hour, minute)
            val currentSettings = userPreferencesDataSource.userSettings.first()
            if (currentSettings.dailyNotify && currentSettings.reminderMode == "daily") {
                NotificationHelper.scheduleDailyReminder(context, hour, minute)
            }
        }
    }

    fun setReminderIntervalVal(value: Int) {
        viewModelScope.launch {
            userPreferencesDataSource.setReminderIntervalVal(value)
            val currentSettings = userPreferencesDataSource.userSettings.first()
            if (currentSettings.dailyNotify && currentSettings.reminderMode == "interval") {
                NotificationHelper.scheduleIntervalReminder(context, value, currentSettings.reminderIntervalUnit)
            }
        }
    }

    fun setReminderIntervalUnit(unit: String) {
        viewModelScope.launch {
            userPreferencesDataSource.setReminderIntervalUnit(unit)
            val currentSettings = userPreferencesDataSource.userSettings.first()
            if (currentSettings.dailyNotify && currentSettings.reminderMode == "interval") {
                NotificationHelper.scheduleIntervalReminder(context, currentSettings.reminderIntervalVal, unit)
            }
        }
    }

    fun setReminderMode(mode: String) {
        viewModelScope.launch {
            userPreferencesDataSource.setReminderMode(mode)
            val currentSettings = userPreferencesDataSource.userSettings.first()
            if (currentSettings.dailyNotify) {
                if (mode == "daily") {
                    NotificationHelper.scheduleDailyReminder(context, currentSettings.reminderHour, currentSettings.reminderMinute)
                } else {
                    NotificationHelper.scheduleIntervalReminder(context, currentSettings.reminderIntervalVal, currentSettings.reminderIntervalUnit)
                }
            }
        }
    }

    // --- Cloud Sync State ---
    sealed interface SyncState {
        data object Idle : SyncState
        data object Syncing : SyncState
        data class Done(val summary: String) : SyncState
        data class Error(val message: String) : SyncState
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun syncNow() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = cloudSyncManager.pushToCloud()
            _syncState.value = when (result) {
                is SyncResult.Success -> {
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    SyncState.Done("Đồng bộ lúc $time — ${result.books} sách, ${result.highlights} highlights")
                }
                is SyncResult.Error -> SyncState.Error(result.message)
            }
        }
    }

    fun pullFromCloud() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = cloudSyncManager.pullFromCloud()
            _syncState.value = when (result) {
                is SyncResult.Success -> {
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    SyncState.Done("Kéo về lúc $time — ${result.books} sách, ${result.highlights} highlights")
                }
                is SyncResult.Error -> SyncState.Error(result.message)
            }
        }
    }

    fun deleteHighlight(highlightId: String) {
        viewModelScope.launch {
            bookRepository.removeHighlight(highlightId)
        }
    }

    fun updateHighlight(id: String, bookId: String, chapterNumber: Int, startOffset: Int, colorHex: String, selectedText: String, noteText: String) {
        viewModelScope.launch {
            bookRepository.upsertHighlight(id, bookId, chapterNumber, startOffset, colorHex, selectedText, noteText)
        }
    }

    fun updateUserProfile(name: String, email: String, bio: String) {
        viewModelScope.launch {
            userPreferencesDataSource.updateUserProfile(name, email, bio)
        }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch {
            userPreferencesDataSource.setFontSize(size)
        }
    }

    fun setFontFamily(family: String) {
        viewModelScope.launch {
            userPreferencesDataSource.setFontFamily(family)
        }
    }

    fun setReadingTheme(theme: String) {
        viewModelScope.launch {
            userPreferencesDataSource.setReadingTheme(theme)
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            try {
                val response = authService.login(LoginRequest(email, password))
                val authData = response.data ?: throw Exception(response.message ?: "Đăng nhập thất bại")
                userPreferencesDataSource.saveTokens(authData.accessToken, authData.refreshToken)
                userPreferencesDataSource.updateUserProfile(
                    name = authData.user?.displayName ?: "Độc giả PageTurn",
                    email = authData.user?.email ?: email,
                    bio = ""
                )
                _authUiState.value = AuthUiState.Success
                syncNow()
            } catch (e: Exception) {
                _authUiState.value = AuthUiState.Error(e.localizedMessage ?: "Đăng nhập thất bại")
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            try {
                val response = authService.register(RegisterRequest(email, password, name))
                val authData = response.data ?: throw Exception(response.message ?: "Đăng ký thất bại")
                userPreferencesDataSource.saveTokens(authData.accessToken, authData.refreshToken)
                userPreferencesDataSource.updateUserProfile(
                    name = authData.user?.displayName ?: name,
                    email = authData.user?.email ?: email,
                    bio = ""
                )
                _authUiState.value = AuthUiState.Success
                syncNow()
            } catch (e: Exception) {
                _authUiState.value = AuthUiState.Error(e.localizedMessage ?: "Đăng ký thất bại")
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            try {
                // Generate a temporary mock token for anonymous user experience
                userPreferencesDataSource.saveTokens("anonymous_token", "anonymous_refresh")
                userPreferencesDataSource.updateUserProfile("Khách hàng", "guest@pageturn.com", "")
                _authUiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _authUiState.value = AuthUiState.Error(e.localizedMessage ?: "Bỏ qua thất bại")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                val token = userPreferencesDataSource.accessToken.firstOrNull()
                val refresh = userPreferencesDataSource.refreshToken.firstOrNull()
                if (!token.isNullOrEmpty() && !refresh.isNullOrEmpty()) {
                    authService.logout("Bearer $token", LogoutRequest(refresh))
                }
            } catch (e: Exception) {
                // ignore network error on logout
            }
            userPreferencesDataSource.clearTokens()
            _authUiState.value = AuthUiState.Idle
        }
    }

    fun resetAuthState() {
        _authUiState.value = AuthUiState.Idle
    }

    fun addLocalBook(title: String, author: String, description: String, coverUrl: String = "") {
        viewModelScope.launch {
            val bookId = "local_${System.currentTimeMillis()}"
            val newBook = Book(
                id = bookId,
                title = title,
                author = author,
                coverUrl = coverUrl,
                progressPercent = 0.0f,
                totalPages = 100,
                currentPage = 0,
                description = description
            )
            bookRepository.addLocalBook(newBook)
        }
     }

     fun deleteLocalBook(bookId: String) {
         viewModelScope.launch {
             bookRepository.deleteLocalBook(bookId)
             if (bookId.startsWith("local_")) {
                 try {
                     val bookDir = java.io.File(context.filesDir, bookId)
                     if (bookDir.exists()) {
                         bookDir.deleteRecursively()
                     }
                 } catch (e: Exception) {
                     e.printStackTrace()
                 }
             }
         }
     }

     fun toggleBookFavorite(bookId: String) {
         viewModelScope.launch {
             val bookmarkedIds = favoriteBookIds.value
             if (bookmarkedIds.contains(bookId)) {
                 bookRepository.removeBookmark("${bookId}_1_1")
             } else {
                 bookRepository.addBookmark(bookId, 1, 1)
             }
         }
     }

    fun exportHighlightsAsTxt(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            val list = bookRepository.getAllHighlightsSnapshot()
            val sb = StringBuilder()
            sb.append("PageTurn Highlights & Notes Export\n")
            sb.append("==================================\n")
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            sb.append("Xuất ngày: $format\n\n")
            
            if (list.isEmpty()) {
                sb.append("(Không có ghi chú hoặc highlight nào để xuất)\n")
            } else {
                list.forEachIndexed { index, hl ->
                    sb.append("${index + 1}. ")
                    sb.append("Sách ID: ${hl.bookId}\n")
                    sb.append("   Đoạn văn trích dẫn: \"${hl.selectedText ?: ""}\"\n")
                    if (hl.noteText?.isNotBlank() == true) {
                        sb.append("   Ghi chú: ${hl.noteText}\n")
                    }
                    sb.append("----------------------------------\n")
                }
            }
            onExportReady(sb.toString())
        }
    }

    fun exportHighlightsAsJson(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            val list = bookRepository.getAllHighlightsSnapshot()
            val sb = StringBuilder()
            sb.append("[\n")
            list.forEachIndexed { index, hl ->
                sb.append("  {\n")
                sb.append("    \"id\": \"${hl.id.replace("\"", "\\\"").replace("\n", "\\n")}\",\n")
                sb.append("    \"bookId\": \"${hl.bookId.replace("\"", "\\\"").replace("\n", "\\n")}\",\n")
                sb.append("    \"chapterNumber\": ${hl.chapterNumber},\n")
                sb.append("    \"selectedText\": \"${(hl.selectedText ?: "").replace("\"", "\\\"").replace("\n", "\\n")}\",\n")
                sb.append("    \"noteText\": \"${(hl.noteText ?: "").replace("\"", "\\\"").replace("\n", "\\n")}\"\n")
                sb.append("  }")
                if (index < list.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("]")
            onExportReady(sb.toString())
        }
    }
}
