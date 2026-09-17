package com.example.data.database

import kotlinx.coroutines.flow.Flow

class RexyyRepository(
    private val conversationDao: ConversationDao,
    private val reminderDao: ReminderDao
) {

    // Conversations
    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    suspend fun logConversation(
        speaker: String,
        text: String,
        actionType: String = "CHAT",
        executionSuccess: Boolean = true
    ): Long {
        return conversationDao.insert(
            ConversationEntity(
                timestamp = System.currentTimeMillis(),
                speaker = speaker,
                text = text,
                actionType = actionType,
                executionSuccess = executionSuccess
            )
        )
    }

    suspend fun clearHistory() {
        conversationDao.clearAll()
    }

    suspend fun deleteConversation(id: Long) {
        conversationDao.deleteById(id)
    }

    // Reminders
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    val activeReminders: Flow<List<ReminderEntity>> = reminderDao.getActiveReminders()

    suspend fun addReminder(
        title: String,
        triggerTimeEpochMs: Long,
        isRecurring: Boolean = false,
        recurringPattern: String = "ONCE"
    ): Long {
        return reminderDao.insert(
            ReminderEntity(
                title = title,
                triggerTimeEpochMs = triggerTimeEpochMs,
                isRecurring = isRecurring,
                recurringPattern = recurringPattern,
                isCompleted = false
            )
        )
    }

    suspend fun getActiveRemindersList(): List<ReminderEntity> {
        return reminderDao.getActiveRemindersList()
    }

    suspend fun markReminderCompleted(id: Long) {
        reminderDao.markCompleted(id)
    }

    suspend fun deleteReminder(id: Long) {
        reminderDao.deleteById(id)
    }

    suspend fun clearAllReminders() {
        reminderDao.clearAll()
    }
}
