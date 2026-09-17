package com.example.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

interface VoiceOutputEngine {
    val isReady: Boolean
    val isSpeaking: StateFlow<Boolean>
    fun speak(text: String, onComplete: (() -> Unit)? = null)
    fun stop()
    fun setRate(rate: Float)
    fun setPitch(pitch: Float)
    fun shutdown()
}

class TtsManager(
    private val context: Context,
    private val onInitComplete: ((Boolean) -> Unit)? = null
) : VoiceOutputEngine {

    private var tts: TextToSpeech? = null
    override var isReady: Boolean = false
        private set

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var pendingCompletion: (() -> Unit)? = null

    private var currentRate = 1.05f
    private var currentPitch = 0.95f

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                setupVoice()
                tts?.setSpeechRate(currentRate)
                tts?.setPitch(currentPitch)
                setupProgressListener()
                onInitComplete?.invoke(true)
            } else {
                isReady = false
                onInitComplete?.invoke(false)
            }
        }
    }

    private fun setupVoice() {
        val t = tts ?: return
        // Try to set Indian English or Hindi locale for natural Hinglish articulation
        val hindiLocale = Locale("hi", "IN")
        val englishIndiaLocale = Locale("en", "IN")

        val result = t.setLanguage(englishIndiaLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            t.setLanguage(hindiLocale)
        }

        // Try selecting a natural sounding voice if available
        try {
            val voices = t.voices
            if (!voices.isNullOrEmpty()) {
                val maleOrJarvisVoice = voices.find { voice ->
                    (voice.locale.language == "en" || voice.locale.language == "hi") &&
                    (voice.name.lowercase().contains("male") ||
                     voice.quality >= Voice.QUALITY_HIGH)
                } ?: voices.firstOrNull()

                maleOrJarvisVoice?.let { t.voice = it }
            }
        } catch (_: Exception) {
            // Voice selection fallback
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                pendingCompletion?.invoke()
                pendingCompletion = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                pendingCompletion?.invoke()
                pendingCompletion = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                pendingCompletion?.invoke()
                pendingCompletion = null
            }
        })
    }

    override fun speak(text: String, onComplete: (() -> Unit)?) {
        if (!isReady || text.isBlank()) {
            onComplete?.invoke()
            return
        }

        pendingCompletion = onComplete
        val utteranceId = "rexyy_${System.currentTimeMillis()}"

        // Filter text to keep it pleasant for speech (strip markdown asterisks, urls)
        val cleanText = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`{1,3}.*?`{1,3}"), "code")
            .replace(Regex("https?://\\S+"), "link")
            .trim()

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        pendingCompletion = null
    }

    override fun setRate(rate: Float) {
        currentRate = rate
        tts?.setSpeechRate(rate)
    }

    override fun setPitch(pitch: Float) {
        currentPitch = pitch
        tts?.setPitch(pitch)
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        _isSpeaking.value = false
    }
}
