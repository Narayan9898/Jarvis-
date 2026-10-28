package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsVolume = MutableStateFlow(0f)
    val rmsVolume: StateFlow<Float> = _rmsVolume.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null

    private fun initRecognizer() {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize -2 to 10 dB to 0.0 .. 1.0
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _rmsVolume.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _rmsVolume.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _rmsVolume.value = 0f
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during recognition"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                            SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Recognition error ($error)"
                        }
                        Log.w("SpeechManager", message)
                        onSpeechError?.invoke(message)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _rmsVolume.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _partialText.value = text
                        if (text.isNotBlank()) {
                            onSpeechResult?.invoke(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            _partialText.value = it
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun startListening(languagePreference: String = "auto") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onSpeechError?.invoke("Speech recognition is not available on this device.")
            return
        }

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
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
            onSpeechError?.invoke("Could not start microphone: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
        _rmsVolume.value = 0f
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _isListening.value = false
        _rmsVolume.value = 0f
    }

    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
