package com.example.features.assistant

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RexyyApp
import com.example.core.ai.ChatMessage
import com.example.core.ai.OpenAiClient
import com.example.core.commands.CommandRouter
import com.example.core.commands.CommandType
import com.example.core.commands.ExecutionResult
import com.example.core.security.RootState
import com.example.core.voice.AssistantState
import com.example.core.voice.RexyyVoiceService
import com.example.core.voice.SpeechRecognizerManager
import com.example.core.voice.TtsManager
import com.example.core.wakeword.WakeWordDetector
import com.example.data.database.ConversationEntity
import com.example.data.database.ReminderEntity
import com.example.features.reminders.ReminderScheduler
import com.example.features.root.RootAction
import com.example.features.root.RootManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConfirmationDialogState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val onConfirm: () -> Unit = {}
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RexyyApp
    private val rootManager = RootManager.instance
    private val openAiClient = OpenAiClient()
    private val commandRouter = CommandRouter(app, openAiClient, rootManager)

    // Speech Recognizer & TTS
    private var speechManager: SpeechRecognizerManager? = null
    val ttsManager = TtsManager(app)

    // State Flows
    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Waiting for 'Hello REXYY'...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _lastCommand = MutableStateFlow<String?>(null)
    val lastCommand: StateFlow<String?> = _lastCommand.asStateFlow()

    private val _lastResponse = MutableStateFlow<String?>(null)
    val lastResponse: StateFlow<String?> = _lastResponse.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _confirmationState = MutableStateFlow(ConfirmationDialogState())
    val confirmationState: StateFlow<ConfirmationDialogState> = _confirmationState.asStateFlow()

    // Root State
    val rootState: StateFlow<RootState> = rootManager.rootState

    // User preferences & Secure Key
    val userPrefs = app.userPreferences
    val keyStoreManager = app.keyStoreManager

    // Setup state
    private val _isTestingKey = MutableStateFlow(false)
    val isTestingKey: StateFlow<Boolean> = _isTestingKey.asStateFlow()

    private val _testKeyResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val testKeyResult: StateFlow<Pair<Boolean, String>?> = _testKeyResult.asStateFlow()

    // History and Reminders from DB
    val conversations: StateFlow<List<ConversationEntity>> = app.repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = app.repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Internal conversation memory for context window
    private val conversationMemory = mutableListOf<ChatMessage>()

    init {
        initSpeechRecognizer()
        observeTtsSpeaking()
    }

    private fun initSpeechRecognizer() {
        speechManager = SpeechRecognizerManager(
            context = app,
            onSpeechResult = { recognizedText ->
                handleSpeechResult(recognizedText)
            },
            onSpeechPartial = { partial ->
                _statusMessage.value = partial
            },
            onErrorOccurred = { error ->
                if (_assistantState.value == AssistantState.LISTENING) {
                    _assistantState.value = AssistantState.IDLE
                    _statusMessage.value = "Waiting for 'Hello REXYY'..."
                }
            },
            onReadyForSpeech = {
                _statusMessage.value = "Listening..."
            }
        )

        // Observe RMS audio decibels for dynamic waveform
        viewModelScope.launch {
            speechManager?.rmsDb?.collect { level ->
                _rmsDb.value = level
            }
        }
    }

    private fun observeTtsSpeaking() {
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                if (isSpeaking) {
                    _assistantState.value = AssistantState.SPEAKING
                    _statusMessage.value = "Speaking..."
                } else if (_assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                    _statusMessage.value = "Waiting for 'Hello REXYY'..."
                }
            }
        }
    }

    fun startListeningForWakeWordOrCommand() {
        if (!keyStoreManager.isApiKeyConfigured() && !userPrefs.isFirstLaunchDone.value) {
            return
        }

        _assistantState.value = AssistantState.LISTENING
        _statusMessage.value = "Listening..."
        speechManager?.speechLanguage = userPrefs.speechLanguage.value
        speechManager?.startListening(continuous = false)
    }

    fun stopListening() {
        speechManager?.stopListening()
        _assistantState.value = AssistantState.IDLE
        _statusMessage.value = "Waiting for 'Hello REXYY'..."
    }

    private fun handleSpeechResult(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            _assistantState.value = AssistantState.IDLE
            _statusMessage.value = "Waiting for 'Hello REXYY'..."
            return
        }

        // Check if wake word present
        val wakeMatch = WakeWordDetector.checkWakeWord(trimmed)
        if (wakeMatch.isWakeWordDetected) {
            val extractedCommand = wakeMatch.extractedCommand
            if (extractedCommand.isBlank()) {
                // User just said "Hello REXYY"
                _assistantState.value = AssistantState.SPEAKING
                speakAndThenListen("Yes, Sagar Sir?")
            } else {
                // User said "Hello REXYY <command>" in one breath!
                executeCommand(extractedCommand)
            }
        } else {
            // Direct command
            executeCommand(trimmed)
        }
    }

    fun executeCommand(command: String) {
        viewModelScope.launch {
            _lastCommand.value = command
            _assistantState.value = AssistantState.THINKING
            _statusMessage.value = "Thinking..."

            val result: ExecutionResult = commandRouter.routeAndExecute(command, conversationMemory)

            _lastResponse.value = result.spokenResponse
            _statusMessage.value = result.displayText

            // Log to conversation history if enabled
            if (userPrefs.historyEnabled.value) {
                app.repository.logConversation(
                    speaker = "USER",
                    text = command,
                    actionType = result.type.name,
                    executionSuccess = result.isSuccess
                )
                app.repository.logConversation(
                    speaker = "REXYY",
                    text = result.spokenResponse,
                    actionType = result.type.name,
                    executionSuccess = result.isSuccess
                )
            }

            // Update memory for multi-turn AI context
            conversationMemory.add(ChatMessage("user", command))
            conversationMemory.add(ChatMessage("assistant", result.spokenResponse))
            if (conversationMemory.size > 12) {
                conversationMemory.removeAt(0)
                conversationMemory.removeAt(0)
            }

            if (result.requiresConfirmation) {
                _confirmationState.value = ConfirmationDialogState(
                    isVisible = true,
                    title = "Confirmation Required",
                    message = result.displayText,
                    onConfirm = {
                        result.pendingAction?.invoke()
                        _confirmationState.value = ConfirmationDialogState(isVisible = false)
                    }
                )
            }

            // Speak response if voice responses are enabled
            if (userPrefs.ttsVoiceResponses.value) {
                _assistantState.value = AssistantState.SPEAKING
                ttsManager.setRate(userPrefs.ttsSpeed.value)
                ttsManager.setPitch(userPrefs.ttsPitch.value)
                ttsManager.speak(result.spokenResponse) {
                    _assistantState.value = AssistantState.IDLE
                    _statusMessage.value = "Waiting for 'Hello REXYY'..."
                }
            } else {
                _assistantState.value = AssistantState.IDLE
                _statusMessage.value = "Waiting for 'Hello REXYY'..."
            }
        }
    }

    private fun speakAndThenListen(speechText: String) {
        if (userPrefs.ttsVoiceResponses.value) {
            ttsManager.speak(speechText) {
                _assistantState.value = AssistantState.LISTENING
                _statusMessage.value = "Listening..."
                speechManager?.startListening(continuous = false)
            }
        } else {
            _assistantState.value = AssistantState.LISTENING
            _statusMessage.value = "Listening..."
            speechManager?.startListening(continuous = false)
        }
    }

    fun dismissConfirmationDialog() {
        _confirmationState.value = ConfirmationDialogState(isVisible = false)
        _assistantState.value = AssistantState.IDLE
        _statusMessage.value = "Waiting for 'Hello REXYY'..."
    }

    // First Launch Setup
    fun testApiKey(apiKey: String) {
        viewModelScope.launch {
            _isTestingKey.value = true
            _testKeyResult.value = null
            val result = openAiClient.testConnection(apiKey)
            _isTestingKey.value = false
            if (result.isSuccess) {
                _testKeyResult.value = Pair(true, "Connection successful! Key is valid.")
            } else {
                _testKeyResult.value = Pair(false, result.exceptionOrNull()?.message ?: "Connection failed.")
            }
        }
    }

    fun completeSetup(apiKey: String) {
        val saved = keyStoreManager.saveApiKey(apiKey)
        if (saved) {
            userPrefs.setFirstLaunchDone(true)
            viewModelScope.launch(Dispatchers.Main) {
                ttsManager.speak("REXYY is ready, Sagar Sir.")
            }
        }
    }

    fun toggleVoiceService(context: Context) {
        val currentActive = userPrefs.serviceActive.value
        if (currentActive) {
            RexyyVoiceService.stopService(context)
        } else {
            RexyyVoiceService.startService(context)
        }
    }

    fun requestRootPermission() {
        viewModelScope.launch {
            val granted = rootManager.requestRootPermission()
            if (granted) {
                userPrefs.setRootModeAllowed(true)
                ttsManager.speak("Root permission granted. Advanced system mode enabled, Sagar Sir.")
            } else {
                ttsManager.speak("Root permission deny kar di gayi hai, Sagar Sir.")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            app.repository.clearHistory()
            conversationMemory.clear()
        }
    }

    fun addManualReminder(title: String, epochMs: Long) {
        viewModelScope.launch {
            val id = app.repository.addReminder(title, epochMs)
            val reminder = app.database.reminderDao().getReminderById(id)
            if (reminder != null) {
                ReminderScheduler.scheduleReminder(app, reminder)
            }
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(app, id)
            app.repository.deleteReminder(id)
        }
    }

    fun markReminderCompleted(id: Long) {
        viewModelScope.launch {
            app.repository.markReminderCompleted(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager.shutdown()
    }
}
