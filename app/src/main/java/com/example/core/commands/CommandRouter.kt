package com.example.core.commands

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import com.example.RexyyApp
import com.example.core.ai.AiProvider
import com.example.core.ai.ChatMessage
import com.example.features.notifications.RexyyNotificationListener
import com.example.features.reminders.ReminderScheduler
import com.example.features.root.RootAction
import com.example.features.root.RootManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

enum class CommandType {
    APP_LAUNCH,
    WEB_SEARCH,
    ANDROID_ACTION,
    MEDIA_CONTROL,
    REMINDER,
    NOTIFICATION,
    ROOT_ACTION,
    MULTI_STEP_TASK,
    AI_QUESTION,
    UNKNOWN
}

data class ExecutionResult(
    val type: CommandType,
    val spokenResponse: String,
    val displayText: String = spokenResponse,
    val isSuccess: Boolean = true,
    val requiresConfirmation: Boolean = false,
    val pendingAction: (() -> Unit)? = null
)

class CommandRouter(
    private val context: Context,
    private val aiProvider: AiProvider,
    private val rootManager: RootManager
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    suspend fun routeAndExecute(rawCommand: String, conversationHistory: List<ChatMessage>): ExecutionResult {
        val command = rawCommand.trim()
        if (command.isBlank()) {
            return ExecutionResult(CommandType.UNKNOWN, "Kahiye Sagar Sir, main aapki kya madad karoon?")
        }

        val lower = command.lowercase()

        // 1. Check for Multi-step task: "kholo aur", "open and", or multiple clauses
        if (isMultiStep(lower)) {
            return executeMultiStep(command, conversationHistory)
        }

        // 2. Check for App Launching
        val appLaunchMatch = matchAppLaunch(lower)
        if (appLaunchMatch != null) {
            return executeAppLaunch(appLaunchMatch)
        }

        // 3. Check for Web & YouTube Search
        val webSearchMatch = matchWebSearch(lower)
        if (webSearchMatch != null) {
            return executeWebSearch(webSearchMatch)
        }

        // 4. Check for Android Settings / System Actions
        val settingsMatch = matchSettingsAction(lower)
        if (settingsMatch != null) {
            return executeSettingsAction(settingsMatch)
        }

        // 5. Check for Media & Volume Control
        val mediaMatch = matchMediaControl(lower)
        if (mediaMatch != null) {
            return executeMediaControl(mediaMatch, lower)
        }

        // 6. Check for Reminders
        if (isReminderCommand(lower)) {
            return executeReminder(command)
        }

        // 7. Check for Notification Reading
        if (isNotificationCommand(lower)) {
            return executeNotificationRead()
        }

        // 8. Check for Root Actions
        val rootMatch = matchRootAction(lower)
        if (rootMatch != null) {
            return executeRootAction(rootMatch)
        }

        // 9. Otherwise, route to AI Brain (General questions, coding, explanations, reasoning)
        return executeAiQuestion(command, conversationHistory)
    }

    private fun isMultiStep(lower: String): Boolean {
        return (lower.contains(" aur ") || lower.contains(" and ") || lower.contains(" then ")) &&
               (lower.contains("kholo") || lower.contains("open") || lower.contains("search"))
    }

    private suspend fun executeMultiStep(
        command: String,
        history: List<ChatMessage>
    ): ExecutionResult = withContext(Dispatchers.Main) {
        val delimiters = arrayOf(" aur ", " and ", " then ", ", ")
        var steps = listOf(command)
        for (delimiter in delimiters) {
            steps = steps.flatMap { it.split(delimiter) }
        }
        val cleanSteps = steps.map { it.trim() }.filter { it.isNotBlank() }

        if (cleanSteps.size <= 1) {
            return@withContext executeAiQuestion(command, history)
        }

        val executedReports = mutableListOf<String>()
        var allSucceeded = true

        for ((index, step) in cleanSteps.withIndex()) {
            val result = routeAndExecute(step, history)
            if (result.isSuccess) {
                executedReports.add("Step ${index + 1}: Complete")
            } else {
                allSucceeded = false
                executedReports.add("Step ${index + 1} par rukawat aayi.")
                break
            }
        }

        val summary = if (allSucceeded) {
            "Sabhi tasks execute kar diye hain, Sagar Sir."
        } else {
            "Tasks execute ho rahe the par kuch steps mein dikkat aayi, Sagar Sir."
        }

        ExecutionResult(
            type = CommandType.MULTI_STEP_TASK,
            spokenResponse = summary,
            displayText = "$summary\n" + executedReports.joinToString("\n"),
            isSuccess = allSucceeded
        )
    }

    private fun matchAppLaunch(lower: String): String? {
        val appTriggers = listOf("kholo", "open", "chalao", "start", "launch")
        val hasTrigger = appTriggers.any { lower.contains(it) }
        if (!hasTrigger) return null

        val apps = listOf(
            "youtube", "chrome", "whatsapp", "instagram", "camera", "settings",
            "calculator", "maps", "gmail", "spotify", "telegram", "play store", "files", "gallery"
        )
        return apps.find { lower.contains(it) }
    }

    private fun executeAppLaunch(appName: String): ExecutionResult {
        val pm = context.packageManager

        val intent: Intent? = when (appName) {
            "youtube" -> pm.getLaunchIntentForPackage("com.google.android.youtube")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            "chrome" -> pm.getLaunchIntentForPackage("com.android.chrome")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            "whatsapp" -> pm.getLaunchIntentForPackage("com.whatsapp")
            "instagram" -> pm.getLaunchIntentForPackage("com.instagram.android")
            "camera" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            "settings" -> Intent(Settings.ACTION_SETTINGS)
            "maps" -> pm.getLaunchIntentForPackage("com.google.android.apps.maps")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
            "gmail" -> pm.getLaunchIntentForPackage("com.google.android.gm")
            "spotify" -> pm.getLaunchIntentForPackage("com.spotify.music")
            "telegram" -> pm.getLaunchIntentForPackage("org.telegram.messenger")
            "play store" -> Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q="))
            "files" -> Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media"))
            else -> null
        }

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                context.startActivity(intent)
                val appLabel = appName.replaceFirstChar { it.uppercase() }
                ExecutionResult(
                    type = CommandType.APP_LAUNCH,
                    spokenResponse = "Sure, Sagar Sir. $appLabel open kar raha hoon.",
                    isSuccess = true
                )
            } catch (e: Exception) {
                ExecutionResult(
                    type = CommandType.APP_LAUNCH,
                    spokenResponse = "$appName open karne mein error aaya, Sagar Sir.",
                    isSuccess = false
                )
            }
        }

        return ExecutionResult(
            type = CommandType.APP_LAUNCH,
            spokenResponse = "$appName aapke device par install nahi mila, Sagar Sir.",
            isSuccess = false
        )
    }

    private fun matchWebSearch(lower: String): Pair<String, String>? {
        if (lower.contains("search")) {
            if (lower.contains("youtube par") || lower.contains("on youtube")) {
                val query = lower.substringAfter("search").replace("on youtube", "").replace("youtube par", "").replace("karo", "").trim()
                return Pair("youtube", query.ifBlank { "trending" })
            }
            val query = lower.substringAfter("search").replace("on google", "").replace("google par", "").replace("karo", "").trim()
            return Pair("google", query.ifBlank { "google" })
        }
        if (lower.contains("google par")) {
            val query = lower.substringAfter("google par").replace("karo", "").replace("search", "").trim()
            return Pair("google", query)
        }
        return null
    }

    private fun executeWebSearch(search: Pair<String, String>): ExecutionResult {
        val (engine, query) = search
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = if (engine == "youtube") {
            "https://www.youtube.com/results?search_query=$encodedQuery"
        } else {
            "https://www.google.com/search?q=$encodedQuery"
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val platform = if (engine == "youtube") "YouTube" else "Google"
            ExecutionResult(
                type = CommandType.WEB_SEARCH,
                spokenResponse = "$platform par $query search kar raha hoon, Sagar Sir.",
                isSuccess = true
            )
        } catch (_: Exception) {
            ExecutionResult(
                type = CommandType.WEB_SEARCH,
                spokenResponse = "Browser open karne mein dikkat aayi, Sagar Sir.",
                isSuccess = false
            )
        }
    }

    private fun matchSettingsAction(lower: String): String? {
        val keywords = listOf(
            "wifi" to Settings.ACTION_WIFI_SETTINGS,
            "wi-fi" to Settings.ACTION_WIFI_SETTINGS,
            "bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
            "display" to Settings.ACTION_DISPLAY_SETTINGS,
            "brightness" to Settings.ACTION_DISPLAY_SETTINGS,
            "sound settings" to Settings.ACTION_SOUND_SETTINGS,
            "volume settings" to Settings.ACTION_SOUND_SETTINGS,
            "battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
            "apps settings" to Settings.ACTION_APPLICATION_SETTINGS,
            "notification settings" to Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            "accessibility" to Settings.ACTION_ACCESSIBILITY_SETTINGS
        )

        for ((key, intentAction) in keywords) {
            if (lower.contains(key) && (lower.contains("setting") || lower.contains("kholo") || lower.contains("open"))) {
                return intentAction
            }
        }
        return null
    }

    private fun executeSettingsAction(intentAction: String): ExecutionResult {
        return try {
            val intent = Intent(intentAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExecutionResult(
                type = CommandType.ANDROID_ACTION,
                spokenResponse = "Settings open kar raha hoon, Sagar Sir.",
                isSuccess = true
            )
        } catch (_: Exception) {
            ExecutionResult(
                type = CommandType.ANDROID_ACTION,
                spokenResponse = "Ye settings screen directly available nahi hai, Sagar Sir.",
                isSuccess = false
            )
        }
    }

    private fun matchMediaControl(lower: String): String? {
        if (lower.contains("volume")) return "volume"
        if (lower.contains("pause") || lower.contains("rok do") || lower.contains("stop")) return "pause"
        if (lower.contains("play") || lower.contains("chalao") || lower.contains("resume")) return "play"
        if (lower.contains("next song") || lower.contains("agla gaana")) return "next"
        if (lower.contains("previous song") || lower.contains("pichla gaana")) return "previous"
        if (lower.contains("mute")) return "mute"
        return null
    }

    private fun executeMediaControl(type: String, lower: String): ExecutionResult {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        when (type) {
            "volume" -> {
                // Check if user specified percentage (e.g. "50 percent", "70%", "full")
                val percentRegex = Regex("(\\d{1,3})\\s*(?:percent|%|pratishat)")
                val match = percentRegex.find(lower)
                val targetVol: Int = if (match != null) {
                    val p = match.groupValues[1].toIntOrNull() ?: 50
                    ((p / 100f) * maxVol).toInt().coerceIn(0, maxVol)
                } else if (lower.contains("badhao") || lower.contains("up") || lower.contains("increase")) {
                    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    (current + 2).coerceAtMost(maxVol)
                } else if (lower.contains("kam") || lower.contains("down") || lower.contains("decrease")) {
                    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    (current - 2).coerceAtLeast(0)
                } else if (lower.contains("full") || lower.contains("100")) {
                    maxVol
                } else {
                    maxVol / 2
                }

                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                val percentage = ((targetVol.toFloat() / maxVol) * 100).toInt()
                return ExecutionResult(
                    type = CommandType.MEDIA_CONTROL,
                    spokenResponse = "Volume $percentage percent set kar diya hai, Sagar Sir.",
                    isSuccess = true
                )
            }
            "mute" -> {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                return ExecutionResult(
                    type = CommandType.MEDIA_CONTROL,
                    spokenResponse = "Media mute kar diya hai, Sagar Sir.",
                    isSuccess = true
                )
            }
            "pause" -> {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)
                return ExecutionResult(
                    type = CommandType.MEDIA_CONTROL,
                    spokenResponse = "Media paused, Sagar Sir.",
                    isSuccess = true
                )
            }
            "play" -> {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)
                return ExecutionResult(
                    type = CommandType.MEDIA_CONTROL,
                    spokenResponse = "Media playing, Sagar Sir.",
                    isSuccess = true
                )
            }
            "next" -> {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
                return ExecutionResult(
                    type = CommandType.MEDIA_CONTROL,
                    spokenResponse = "Playing next track, Sagar Sir.",
                    isSuccess = true
                )
            }
            "previous" -> {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                return ExecutionResult(
                    type = CommandType.MEDIA_CONTROL,
                    spokenResponse = "Playing previous track, Sagar Sir.",
                    isSuccess = true
                )
            }
        }

        return ExecutionResult(
            type = CommandType.MEDIA_CONTROL,
            spokenResponse = "Media command execute kar diya hai, Sagar Sir.",
            isSuccess = true
        )
    }

    private fun sendMediaKeyEvent(keyCode: Int) {
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun isReminderCommand(lower: String): Boolean {
        return lower.contains("yaad dilana") || lower.contains("remind me") || lower.contains("reminder lagao")
    }

    private suspend fun executeReminder(command: String): ExecutionResult {
        val (title, triggerEpoch) = ReminderScheduler.parseNaturalLanguageTimeToEpoch(command)
        return try {
            val id = RexyyApp.instance.repository.addReminder(
                title = title,
                triggerTimeEpochMs = triggerEpoch
            )
            val reminder = RexyyApp.instance.database.reminderDao().getReminderById(id)
            if (reminder != null) {
                ReminderScheduler.scheduleReminder(context, reminder)
            }
            ExecutionResult(
                type = CommandType.REMINDER,
                spokenResponse = "Aapka reminder set ho gaya hai, Sagar Sir: $title",
                displayText = "Reminder set: $title",
                isSuccess = true
            )
        } catch (_: Exception) {
            ExecutionResult(
                type = CommandType.REMINDER,
                spokenResponse = "Reminder schedule karne mein issue aayi, Sagar Sir.",
                isSuccess = false
            )
        }
    }

    private fun isNotificationCommand(lower: String): Boolean {
        return lower.contains("notification") || lower.contains("messages padho") || lower.contains("kiska message")
    }

    private fun executeNotificationRead(): ExecutionResult {
        if (!RexyyNotificationListener.isPermissionGranted(context)) {
            return ExecutionResult(
                type = CommandType.NOTIFICATION,
                spokenResponse = "Notification Access granted nahi hai. Settings mein access allow karein, Sagar Sir.",
                isSuccess = false
            )
        }

        val summary = RexyyNotificationListener.getLatestNotificationSummary()
        return ExecutionResult(
            type = CommandType.NOTIFICATION,
            spokenResponse = summary,
            isSuccess = true
        )
    }

    private fun matchRootAction(lower: String): RootAction? {
        if (lower.contains("recovery") && (lower.contains("reboot") || lower.contains("restart"))) {
            return RootAction.REBOOT_RECOVERY
        }
        if (lower.contains("bootloader") && (lower.contains("reboot") || lower.contains("restart"))) {
            return RootAction.REBOOT_BOOTLOADER
        }
        if (lower.contains("reboot") || lower.contains("restart karo")) {
            return RootAction.REBOOT_NORMAL
        }
        if (lower.contains("cache clear") || lower.contains("ram clear")) {
            return RootAction.CLEAR_APP_CACHE
        }
        if (lower.contains("selinux")) {
            return RootAction.GET_SELINUX_STATUS
        }
        return null
    }

    private suspend fun executeRootAction(action: RootAction): ExecutionResult {
        if (!rootManager.isRootAvailable()) {
            return ExecutionResult(
                type = CommandType.ROOT_ACTION,
                spokenResponse = "Ye action normal Android mode mein available nahi hai. Root access required hai.",
                isSuccess = false
            )
        }

        val isDestructive = action == RootAction.REBOOT_NORMAL ||
                            action == RootAction.REBOOT_RECOVERY ||
                            action == RootAction.REBOOT_BOOTLOADER

        if (isDestructive) {
            val targetName = when (action) {
                RootAction.REBOOT_RECOVERY -> "Recovery mode"
                RootAction.REBOOT_BOOTLOADER -> "Bootloader mode"
                else -> "System"
            }
            return ExecutionResult(
                type = CommandType.ROOT_ACTION,
                spokenResponse = "$targetName mein reboot karna hai. Confirm?",
                displayText = "$targetName reboot requires your confirmation.",
                isSuccess = true,
                requiresConfirmation = true,
                pendingAction = {
                    // Pending action executed on user confirmation
                }
            )
        }

        val output = rootManager.executeAllowedAction(action)
        return ExecutionResult(
            type = CommandType.ROOT_ACTION,
            spokenResponse = output,
            isSuccess = true
        )
    }

    private suspend fun executeAiQuestion(
        prompt: String,
        history: List<ChatMessage>
    ): ExecutionResult {
        val apiKey = RexyyApp.instance.keyStoreManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return ExecutionResult(
                type = CommandType.AI_QUESTION,
                spokenResponse = "API key missing hai, Sagar Sir. Settings mein jaakar API key daalo.",
                isSuccess = false
            )
        }

        val model = RexyyApp.instance.userPreferences.aiModel.value
        val result = aiProvider.generateResponse(
            apiKey = apiKey,
            model = model,
            conversationHistory = history,
            userPrompt = prompt
        )

        return if (result.isSuccess) {
            val responseText = result.getOrNull() ?: "Khama kijiye, main samajh nahi paya."
            ExecutionResult(
                type = CommandType.AI_QUESTION,
                spokenResponse = responseText,
                displayText = responseText,
                isSuccess = true
            )
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "AI request failed."
            ExecutionResult(
                type = CommandType.AI_QUESTION,
                spokenResponse = errorMsg,
                displayText = errorMsg,
                isSuccess = false
            )
        }
    }
}
