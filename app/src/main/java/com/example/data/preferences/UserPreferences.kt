package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isFirstLaunchDone = MutableStateFlow(prefs.getBoolean(KEY_FIRST_LAUNCH, false))
    val isFirstLaunchDone: StateFlow<Boolean> = _isFirstLaunchDone.asStateFlow()

    private val _aiModel = MutableStateFlow(prefs.getString(KEY_AI_MODEL, DEFAULT_AI_MODEL) ?: DEFAULT_AI_MODEL)
    val aiModel: StateFlow<String> = _aiModel.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true))
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    private val _speechLanguage = MutableStateFlow(prefs.getString(KEY_SPEECH_LANGUAGE, "en-IN") ?: "en-IN")
    val speechLanguage: StateFlow<String> = _speechLanguage.asStateFlow()

    private val _ttsVoiceResponses = MutableStateFlow(prefs.getBoolean(KEY_TTS_RESPONSES, true))
    val ttsVoiceResponses: StateFlow<Boolean> = _ttsVoiceResponses.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(prefs.getFloat(KEY_TTS_SPEED, 1.05f))
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    private val _ttsPitch = MutableStateFlow(prefs.getFloat(KEY_TTS_PITCH, 0.95f))
    val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

    private val _notificationReadingEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATION_READING, false))
    val notificationReadingEnabled: StateFlow<Boolean> = _notificationReadingEnabled.asStateFlow()

    private val _rootModeAllowed = MutableStateFlow(prefs.getBoolean(KEY_ROOT_MODE_ALLOWED, false))
    val rootModeAllowed: StateFlow<Boolean> = _rootModeAllowed.asStateFlow()

    private val _historyEnabled = MutableStateFlow(prefs.getBoolean(KEY_HISTORY_ENABLED, true))
    val historyEnabled: StateFlow<Boolean> = _historyEnabled.asStateFlow()

    private val _serviceActive = MutableStateFlow(prefs.getBoolean(KEY_SERVICE_ACTIVE, false))
    val serviceActive: StateFlow<Boolean> = _serviceActive.asStateFlow()

    fun setFirstLaunchDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, done).apply()
        _isFirstLaunchDone.value = done
    }

    fun setAiModel(model: String) {
        prefs.edit().putString(KEY_AI_MODEL, model).apply()
        _aiModel.value = model
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
        _wakeWordEnabled.value = enabled
    }

    fun setSpeechLanguage(lang: String) {
        prefs.edit().putString(KEY_SPEECH_LANGUAGE, lang).apply()
        _speechLanguage.value = lang
    }

    fun setTtsVoiceResponses(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_RESPONSES, enabled).apply()
        _ttsVoiceResponses.value = enabled
    }

    fun setTtsSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_TTS_SPEED, speed).apply()
        _ttsSpeed.value = speed
    }

    fun setTtsPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_TTS_PITCH, pitch).apply()
        _ttsPitch.value = pitch
    }

    fun setNotificationReadingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_READING, enabled).apply()
        _notificationReadingEnabled.value = enabled
    }

    fun setRootModeAllowed(allowed: Boolean) {
        prefs.edit().putBoolean(KEY_ROOT_MODE_ALLOWED, allowed).apply()
        _rootModeAllowed.value = allowed
    }

    fun setHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HISTORY_ENABLED, enabled).apply()
        _historyEnabled.value = enabled
    }

    fun setServiceActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ACTIVE, active).apply()
        _serviceActive.value = active
    }

    fun getAllowedNotificationApps(): Set<String> {
        return prefs.getStringSet(
            KEY_ALLOWED_NOTIFICATION_APPS,
            setOf("com.whatsapp", "com.google.android.apps.messaging", "org.telegram.messenger", "com.instagram.android")
        ) ?: setOf("com.whatsapp")
    }

    fun setAllowedNotificationApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_ALLOWED_NOTIFICATION_APPS, apps).apply()
    }

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
        _isFirstLaunchDone.value = false
        _aiModel.value = DEFAULT_AI_MODEL
        _wakeWordEnabled.value = true
        _speechLanguage.value = "en-IN"
        _ttsVoiceResponses.value = true
        _notificationReadingEnabled.value = false
        _rootModeAllowed.value = false
        _historyEnabled.value = true
        _serviceActive.value = false
    }

    companion object {
        private const val PREFS_NAME = "rexyy_user_settings"
        private const val KEY_FIRST_LAUNCH = "key_first_launch"
        private const val KEY_AI_MODEL = "key_ai_model"
        private const val KEY_WAKE_WORD_ENABLED = "key_wake_word_enabled"
        private const val KEY_SPEECH_LANGUAGE = "key_speech_language"
        private const val KEY_TTS_RESPONSES = "key_tts_responses"
        private const val KEY_TTS_SPEED = "key_tts_speed"
        private const val KEY_TTS_PITCH = "key_tts_pitch"
        private const val KEY_NOTIFICATION_READING = "key_notification_reading"
        private const val KEY_ALLOWED_NOTIFICATION_APPS = "key_allowed_notification_apps"
        private const val KEY_ROOT_MODE_ALLOWED = "key_root_mode_allowed"
        private const val KEY_HISTORY_ENABLED = "key_history_enabled"
        private const val KEY_SERVICE_ACTIVE = "key_service_active"

        const val DEFAULT_AI_MODEL = "gpt-4o-mini"
    }
}
