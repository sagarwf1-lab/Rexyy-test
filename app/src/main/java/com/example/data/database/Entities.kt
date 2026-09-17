package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val speaker: String, // "USER" or "REXYY"
    val text: String,
    val actionType: String = "CHAT", // "AI_QUESTION", "APP_LAUNCH", "ANDROID_ACTION", "REMINDER", "MEDIA", "ROOT"
    val executionSuccess: Boolean = true
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerTimeEpochMs: Long,
    val isRecurring: Boolean = false,
    val recurringPattern: String = "ONCE", // "ONCE", "DAILY", "WEEKLY"
    val isCompleted: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
