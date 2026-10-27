package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var speechRate = 1.0f
    private var speechPitch = 1.0f

    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechCompleted: (() -> Unit)? = null

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                onSpeechStarted?.invoke()
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                onSpeechCompleted?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                onSpeechCompleted?.invoke()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                Log.w("TtsManager", "TTS error code: $errorCode")
                onSpeechCompleted?.invoke()
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.US
            tts?.setPitch(speechPitch)
            tts?.setSpeechRate(speechRate)
        } else {
            Log.e("TtsManager", "TTS initialization failed status: $status")
        }
    }

    fun speak(text: String, languageCode: String = "en") {
        if (!isInitialized || text.isBlank()) return

        val locale = when (languageCode.lowercase()) {
            "te", "tanglish" -> Locale.forLanguageTag("te-IN")
            "hi" -> Locale.forLanguageTag("hi-IN")
            else -> Locale.US
        }

        try {
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default
                tts?.language = Locale.US
            }
        } catch (e: Exception) {
            tts?.language = Locale.US
        }

        tts?.setPitch(speechPitch)
        tts?.setSpeechRate(speechRate)

        val utteranceId = "JARVIS_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun setSpeedAndPitch(rate: Float, pitch: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        speechPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
