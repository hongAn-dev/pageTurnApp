package com.pageturn.core.common.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PageTurnTtsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("vi", "VN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.getDefault()
            }
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isPlaying.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                }
                
                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isPlaying.value = false
                }
            })
            
            _isInitialized.value = true
            
            pendingText?.let {
                speak(it)
                pendingText = null
            }
        } else {
            _isInitialized.value = false
        }
    }

    fun speak(text: String) {
        if (tts == null) {
            tts = TextToSpeech(context, this)
        }
        if (_isInitialized.value) {
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "PageTurnUtterance")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "PageTurnUtterance")
            _isPlaying.value = true
        } else {
            pendingText = text
        }
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
    }

    fun shutdown() {
        tts?.shutdown()
        _isInitialized.value = false
        _isPlaying.value = false
    }
}
