package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages Android SpeechRecognizer interface for voice input,
 * handling Android lifecycle transitions and resilient continuous listening mode.
 */
class SpeechManager(private val context: Context) : DefaultLifecycleObserver {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isContinuousMode = MutableStateFlow(false)
    val isContinuousMode: StateFlow<Boolean> = _isContinuousMode.asStateFlow()

    private val _rmsVolume = MutableStateFlow(0f)
    val rmsVolume: StateFlow<Float> = _rmsVolume.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private var isActivityResumed = false
    private var isPausedByTts = false
    private var currentLanguagePreference = "auto"
    private var retryCount = 0

    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null
    var onPartialSpeech: ((String) -> Unit)? = null

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            retryCount = 0
            Log.d("SpeechManager", "SpeechRecognizer: Ready for speech.")
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize -2dB to 10dB range into 0.0 .. 1.0
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _rmsVolume.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            _rmsVolume.value = 0f
            Log.d("SpeechManager", "SpeechRecognizer: End of speech detected.")
        }

        override fun onError(errorCode: Int) {
            _isListening.value = false
            _rmsVolume.value = 0f

            val (errorMessage, isTransient) = when (errorCode) {
                SpeechRecognizer.ERROR_AUDIO -> Pair("Audio recording hardware error.", false)
                SpeechRecognizer.ERROR_CLIENT -> Pair("Speech client error.", true)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Pair("Microphone permission required.", false)
                SpeechRecognizer.ERROR_NETWORK -> Pair("Network error during speech recognition.", true)
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Pair("Network timeout during recognition.", true)
                SpeechRecognizer.ERROR_NO_MATCH -> Pair("No speech recognized.", true)
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Pair("Speech service busy. Re-initializing...", true)
                SpeechRecognizer.ERROR_SERVER -> Pair("Speech recognition server error.", true)
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Pair("Speech input timeout.", true)
                else -> Pair("Speech recognition code $errorCode.", true)
            }

            Log.w("SpeechManager", "SpeechRecognizer error: $errorMessage (code: $errorCode, continuous: ${_isContinuousMode.value})")

            if (_isContinuousMode.value && isActivityResumed && !isPausedByTts && isTransient) {
                // In continuous mode, transient errors are absorbed and re-armed silently
                scheduleRestart(delayMillis = 400L)
            } else {
                if (!isTransient) {
                    _isContinuousMode.value = false
                }
                onSpeechError?.invoke(errorMessage)
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _rmsVolume.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim() ?: ""

            _partialText.value = recognizedText
            Log.d("SpeechManager", "SpeechRecognizer results: '$recognizedText'")

            if (recognizedText.isNotBlank()) {
                onSpeechResult?.invoke(recognizedText)
            }

            // In continuous mode, re-arm after the utterance has been processed
            if (_isContinuousMode.value && isActivityResumed && !isPausedByTts) {
                scheduleRestart(delayMillis = 600L)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull() ?: ""
            if (partial.isNotBlank()) {
                _partialText.value = partial
                onPartialSpeech?.invoke(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun initRecognizer() {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
        }
    }

    /**
     * Starts listening for a single or continuous utterance.
     */
    fun startListening(languagePreference: String = currentLanguagePreference) {
        currentLanguagePreference = languagePreference

        if (!isActivityResumed && speechRecognizer != null) {
            Log.d("SpeechManager", "Cannot start listening while activity is in background.")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onSpeechError?.invoke("Speech recognition service unavailable on this device.")
            return
        }

        // Clean any existing active recognition before starting anew
        cancelInternal()
        initRecognizer()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            val languageTag = when (languagePreference.lowercase()) {
                "te" -> "te-IN"
                "hi" -> "hi-IN"
                "tanglish" -> "te-IN"
                "en" -> "en-IN"
                else -> Locale.getDefault().toLanguageTag()
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "te-IN", "hi-IN"))
            // Prompt speech timeout tuning
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
            Log.e("SpeechManager", "Exception starting SpeechRecognizer: ${e.message}")
            if (_isContinuousMode.value && isActivityResumed) {
                scheduleRestart(1000L)
            } else {
                onSpeechError?.invoke("Could not start microphone: ${e.localizedMessage}")
            }
        }
    }

    fun setContinuousListening(enabled: Boolean, languagePreference: String = currentLanguagePreference) {
        _isContinuousMode.value = enabled
        if (enabled) {
            startListening(languagePreference)
        } else {
            stopListening()
        }
    }

    /**
     * Temporarily pause listening while JARVIS is speaking via TTS
     * to prevent audio echo loopback.
     */
    fun pauseForTts() {
        isPausedByTts = true
        cancelInternal()
        _isListening.value = false
        _rmsVolume.value = 0f
    }

    /**
     * Resumes listening once TTS speech playback completes.
     */
    fun resumeAfterTts() {
        isPausedByTts = false
        if (_isContinuousMode.value && isActivityResumed) {
            scheduleRestart(300L)
        }
    }

    private fun scheduleRestart(delayMillis: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (_isContinuousMode.value && isActivityResumed && !isPausedByTts) {
                startListening(currentLanguagePreference)
            }
        }, delayMillis)
    }

    fun stopListening() {
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
        _rmsVolume.value = 0f
    }

    private fun cancelInternal() {
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _isListening.value = false
        _rmsVolume.value = 0f
    }

    fun cancel() {
        _isContinuousMode.value = false
        cancelInternal()
    }

    fun destroy() {
        mainHandler.removeCallbacksAndMessages(null)
        _isContinuousMode.value = false
        cancelInternal()
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    // --- Android Lifecycle Handling ---

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        isActivityResumed = true
        Log.d("SpeechManager", "Lifecycle: onResume. ContinuousMode=${_isContinuousMode.value}")
        if (_isContinuousMode.value && !isPausedByTts) {
            startListening(currentLanguagePreference)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        isActivityResumed = false
        Log.d("SpeechManager", "Lifecycle: onPause. Halting active microphone.")
        cancelInternal()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d("SpeechManager", "Lifecycle: onDestroy. Tearing down SpeechRecognizer.")
        destroy()
    }
}
