package com.example.features.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.database.ReminderEntity
import java.util.Calendar

object ReminderScheduler {

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Check canScheduleExactAlarms on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Fallback to inexact or window alarm if exact permission not granted
                val intent = createPendingIntent(context, reminder)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerTimeEpochMs,
                    intent
                )
                return
            }
        }

        val intent = createPendingIntent(context, reminder)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerTimeEpochMs,
                intent
            )
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminder.triggerTimeEpochMs,
                intent
            )
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun createPendingIntent(context: Context, reminder: ReminderEntity): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_TITLE, reminder.title)
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun parseNaturalLanguageTimeToEpoch(text: String): Pair<String, Long> {
        val lower = text.lowercase()
        val calendar = Calendar.getInstance()

        // Check for relative days: "kal", "tomorrow"
        val isTomorrow = lower.contains("kal") || lower.contains("tomorrow")
        if (isTomorrow) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Check for hour: e.g. "8 baje", "8 pm", "10 am", "8:30"
        val hourRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(?:baje|am|pm|o'clock|hrs)?")
        val match = hourRegex.find(lower)

        var hour = 9
        var minute = 0

        if (match != null) {
            val h = match.groupValues[1].toIntOrNull() ?: 9
            val m = match.groupValues[2].toIntOrNull() ?: 0

            val isPm = lower.contains("pm") || lower.contains("shaam") || lower.contains("raat")
            val isAm = lower.contains("am") || lower.contains("subah")

            hour = when {
                isPm && h < 12 -> h + 12
                isAm && h == 12 -> 0
                h in 1..7 && !isAm -> h + 12 // Contextual afternoon/evening default for numbers like "5 baje"
                else -> h
            }
            minute = m
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If time is in the past today, push to tomorrow
        if (!isTomorrow && calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Clean reminder title
        var title = text
            .replace(Regex("(?i)\\b(?:kal|tomorrow|aaj|today)\\b"), "")
            .replace(Regex("(?i)\\b\\d{1,2}(?::\\d{2})?\\s*(?:baje|am|pm|shaam|subah|raat|ko)?\\b"), "")
            .replace(Regex("(?i)\\b(?:mujhe|yaad dilana|remind me|remind|karna hai|project|kaam|ke liye)\\b"), "")
            .trim()

        if (title.isBlank()) {
            title = "Voice Reminder"
        }

        return Pair(title, calendar.timeInMillis)
    }
}
