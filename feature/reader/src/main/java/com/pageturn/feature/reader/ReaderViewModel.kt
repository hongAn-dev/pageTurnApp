package com.pageturn.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pageturn.core.common.preferences.UserPreferencesDataSource
import com.pageturn.core.common.preferences.UserSettings
import com.pageturn.core.domain.usecase.GetChapterUseCase
import com.pageturn.core.domain.usecase.IsBookmarkedUseCase
import com.pageturn.core.domain.usecase.ToggleBookmarkUseCase
import com.pageturn.core.domain.usecase.UpdateProgressUseCase
import com.pageturn.core.domain.usecase.GetHighlightsUseCase
import com.pageturn.core.domain.usecase.AddHighlightUseCase
import com.pageturn.core.domain.usecase.RemoveHighlightUseCase
import com.pageturn.core.domain.usecase.RemoveHighlightsByParagraphUseCase
import com.pageturn.core.common.tts.PageTurnTtsHelper
import com.pageturn.core.model.Chapter
import com.pageturn.core.model.Highlight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.pageturn.core.domain.repository.BookRepository

import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Success(val chapter: Chapter, val bookId: String, val bookTitle: String, val bookAuthor: String) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getChapterUseCase: GetChapterUseCase,
    private val updateProgressUseCase: UpdateProgressUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val isBookmarkedUseCase: IsBookmarkedUseCase,
    private val getHighlightsUseCase: GetHighlightsUseCase,
    private val addHighlightUseCase: AddHighlightUseCase,
    private val removeHighlightUseCase: RemoveHighlightUseCase,
    private val removeHighlightsByParagraphUseCase: RemoveHighlightsByParagraphUseCase,
    private val ttsHelper: PageTurnTtsHelper,
    private val preferencesDataSource: UserPreferencesDataSource,
    private val bookRepository: BookRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])
    val initialParagraph: Int
        get() {
            val fromSavedState: Int? = savedStateHandle["paragraph"]
            if (fromSavedState != null && fromSavedState != -1) return fromSavedState
            val sharedPrefs = context.getSharedPreferences("reader_progress", Context.MODE_PRIVATE)
            return sharedPrefs.getInt("last_paragraph_$bookId", 0)
        }
    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val settings: StateFlow<UserSettings> = preferencesDataSource.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings(16, "serif", "light")
        )

    init {
        loadChapter(1)
    }

    fun loadChapter(chapterNumber: Int) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                val chapter = getChapterUseCase(bookId, chapterNumber)
                val book = bookRepository.getBookDetails(bookId).firstOrNull()
                val bookTitle = book?.title ?: when (bookId) {
                    "1" -> "THE ADVENTURES OF SHERLOCK HOLMES"
                    "2" -> "THE GREAT GATSBY"
                    "3" -> "PRIDE AND PREJUDICE"
                    "4" -> "MOBY DICK"
                    else -> "WAR AND PEACE"
                }
                val bookAuthor = book?.author ?: when (bookId) {
                    "1" -> "Arthur Conan Doyle"
                    "2" -> "F. Scott Fitzgerald"
                    "3" -> "Jane Austen"
                    "4" -> "Herman Melville"
                    else -> "Leo Tolstoy"
                }
                _uiState.value = ReaderUiState.Success(chapter, bookId, bookTitle, bookAuthor)
            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error(e.message ?: "Failed to load chapter")
            }
        }
    }

    fun updateProgress(page: Int, totalPages: Int) {
        viewModelScope.launch {
            val progressPercent = page.toFloat() / totalPages
            updateProgressUseCase(bookId, page, progressPercent)
        }
    }

    fun saveLastReadPosition(chapter: Int, paragraph: Int) {
        viewModelScope.launch {
            val sharedPrefs = context.getSharedPreferences("reader_progress", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putInt("last_chapter_$bookId", chapter)
                .putInt("last_paragraph_$bookId", paragraph)
                .apply()
        }
    }

    fun isBookmarked(chapterNumber: Int, pageNumber: Int): Flow<Boolean> {
        return isBookmarkedUseCase(bookId, chapterNumber, pageNumber)
    }

    fun toggleBookmark(chapterNumber: Int, pageNumber: Int) {
        viewModelScope.launch {
            toggleBookmarkUseCase(bookId, chapterNumber, pageNumber)
        }
    }

    fun changeFontSize(sizeSp: Int) {
        viewModelScope.launch {
            preferencesDataSource.setFontSize(sizeSp)
        }
    }

    fun changeFontFamily(family: String) {
        viewModelScope.launch {
            preferencesDataSource.setFontFamily(family)
        }
    }

    fun changeReadingTheme(theme: String) {
        viewModelScope.launch {
            preferencesDataSource.setReadingTheme(theme)
        }
    }

    fun getHighlights(chapterNumber: Int): Flow<List<Highlight>> {
        return getHighlightsUseCase(bookId, chapterNumber)
    }

    fun addHighlight(chapterNumber: Int, startOffset: Int, endOffset: Int, colorHex: String, noteText: String? = null, selectedText: String? = null) {
        viewModelScope.launch {
            addHighlightUseCase(bookId, chapterNumber, startOffset, endOffset, colorHex, noteText, selectedText)
        }
    }

    fun removeHighlight(highlightId: String) {
        viewModelScope.launch {
            removeHighlightUseCase(highlightId)
        }
    }

    fun removeHighlightForParagraph(chapterNumber: Int, paragraphIndex: Int) {
        viewModelScope.launch {
            try {
                // Remove ALL highlights for this paragraph position from the database
                removeHighlightsByParagraphUseCase(bookId, chapterNumber, paragraphIndex)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val isTtsPlaying: StateFlow<Boolean> = ttsHelper.isPlaying
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun speak(text: String) {
        ttsHelper.speak(text)
    }

    fun stopSpeaking() {
        ttsHelper.stop()
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }
}
