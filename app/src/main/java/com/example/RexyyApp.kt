package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.database.RexyyDatabase
import com.example.data.database.RexyyRepository
import com.example.data.preferences.UserPreferences
import com.example.data.secure.KeyStoreManager

class RexyyApp : Application() {

    lateinit var database: RexyyDatabase
        private set

    lateinit var repository: RexyyRepository
        private set

    lateinit var userPreferences: UserPreferences
        private set

    lateinit var keyStoreManager: KeyStoreManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize secure storage and preferences
        keyStoreManager = KeyStoreManager(this)
        userPreferences = UserPreferences(this)

        // Initialize Room Database
        database = RexyyDatabase.getDatabase(this)
        repository = RexyyRepository(database.conversationDao(), database.reminderDao())

        // Create Notification Channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Voice Foreground Service Channel
            val voiceChannel = NotificationChannel(
                CHANNEL_VOICE_SERVICE,
                "REXYY Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows continuous voice assistant and wake word status"
                setShowBadge(false)
            }

            // Reminders Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "REXYY Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for scheduled voice reminders"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(voiceChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    companion object {
        const val CHANNEL_VOICE_SERVICE = "rexyy_voice_service_channel"
        const val CHANNEL_REMINDERS = "rexyy_reminders_channel"

        lateinit var instance: RexyyApp
            private set
    }
}
