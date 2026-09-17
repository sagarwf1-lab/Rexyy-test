package com.example.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognizerManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onSpeechPartial: ((String) -> Unit)? = null,
    private val onErrorOccurred: ((String) -> Unit)? = null,
    private val onReadyForSpeech: (() -> Unit)? = null
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    var speechLanguage: String = "en-IN"
    private var isContinuousWakeMode: Boolean = false
    private var isDestroyed: Boolean = false

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorOccurred?.invoke("Speech recognition is not available on this device.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }
        } catch (e: Exception) {
            onErrorOccurred?.invoke("Could not initialize SpeechRecognizer: ${e.localizedMessage}")
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            onReadyForSpeech?.invoke()
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize roughly between 0f and 10f
            _rmsDb.value = (rmsdB.coerceAtLeast(0f) / 1.2f).coerceAtMost(10f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            _rmsDb.value = 0f
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _rmsDb.value = 0f

            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                SpeechRecognizer.ERROR_CLIENT -> "Client speech error."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required hai."
                SpeechRecognizer.ERROR_NETWORK -> "Network issue in voice recognition."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy."
                SpeechRecognizer.ERROR_SERVER -> "Voice server error."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout."
                else -> "Speech recognition error ($error)."
            }

            if (isContinuousWakeMode && !isDestroyed) {
                // In continuous wake mode, silently re-arm after brief delay
                mainHandler.postDelayed({
                    if (isContinuousWakeMode && !isDestroyed) {
                        startListeningInternal()
                    }
                }, 1000)
            } else {
                onErrorOccurred?.invoke(errorMsg)
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _rmsDb.value = 0f

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim() ?: ""

            if (recognizedText.isNotBlank()) {
                onSpeechResult(recognizedText)
            }

            if (isContinuousWakeMode && !isDestroyed) {
                mainHandler.postDelayed({
                    if (isContinuousWakeMode && !isDestroyed) {
                        startListeningInternal()
                    }
                }, 500)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { onSpeechPartial?.invoke(it) }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening(continuous: Boolean = false) {
        isContinuousWakeMode = continuous
        mainHandler.post {
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        if (isDestroyed) return
        if (speechRecognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLanguage)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, speechLanguage)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, speechLanguage)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (_: Exception) {
            initRecognizer()
            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e2: Exception) {
                onErrorOccurred?.invoke("Cannot start voice recognition: ${e2.localizedMessage}")
            }
        }
    }

    fun stopListening() {
        isContinuousWakeMode = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
            _rmsDb.value = 0f
        }
    }

    fun destroy() {
        isDestroyed = true
        isContinuousWakeMode = false
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            _isListening.value = false
            _rmsDb.value = 0f
        }
    }
}
