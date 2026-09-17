package com.example.core.voice

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.RexyyApp
import com.example.core.commands.CommandRouter
import com.example.core.wakeword.WakeWordDetector
import com.example.features.root.RootManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RexyyVoiceService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var speechRecognizerManager: SpeechRecognizerManager? = null
    private var ttsManager: TtsManager? = null
    private var commandRouter: CommandRouter? = null

    override fun onCreate() {
        super.onCreate()

        ttsManager = TtsManager(this)
        commandRouter = CommandRouter(
            context = this,
            aiProvider = com.example.core.ai.OpenAiClient(),
            rootManager = RootManager.instance
        )

        speechRecognizerManager = SpeechRecognizerManager(
            context = this,
            onSpeechResult = { text ->
                handleVoiceText(text)
            },
            onErrorOccurred = { _ ->
                // In service background, loop quietly
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            RexyyApp.instance.userPreferences.setServiceActive(false)
            return START_NOT_STICKY
        }

        startAsForeground()

        if (RexyyApp.instance.userPreferences.wakeWordEnabled.value) {
            speechRecognizerManager?.speechLanguage = RexyyApp.instance.userPreferences.speechLanguage.value
            speechRecognizerManager?.startListening(continuous = true)
        }

        RexyyApp.instance.userPreferences.setServiceActive(true)
        return START_STICKY
    }

    private fun startAsForeground() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RexyyVoiceService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, RexyyApp.CHANNEL_VOICE_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("REXYY Voice Assistant Active")
            .setContentText("Listening for 'Hello REXYY'...")
            .setContentIntent(pendingOpen)
            .addAction(0, "Stop Assistant", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun handleVoiceText(text: String) {
        val match = WakeWordDetector.checkWakeWord(text)
        if (match.isWakeWordDetected) {
            serviceScope.launch {
                val command = match.extractedCommand
                if (command.isBlank()) {
                    ttsManager?.speak("Yes, Sagar Sir?")
                } else {
                    val result = commandRouter?.routeAndExecute(command, emptyList())
                    result?.let {
                        if (RexyyApp.instance.userPreferences.ttsVoiceResponses.value) {
                            ttsManager?.speak(it.spokenResponse)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        speechRecognizerManager?.destroy()
        ttsManager?.shutdown()
        RexyyApp.instance.userPreferences.setServiceActive(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 4040
        const val ACTION_START_SERVICE = "com.rexyy.ACTION_START_VOICE_SERVICE"
        const val ACTION_STOP_SERVICE = "com.rexyy.ACTION_STOP_VOICE_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, RexyyVoiceService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RexyyVoiceService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
