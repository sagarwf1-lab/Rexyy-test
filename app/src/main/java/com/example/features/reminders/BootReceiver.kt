package com.example.features.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.RexyyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activeReminders = RexyyApp.instance.repository.getActiveRemindersList()
                    val now = System.currentTimeMillis()
                    for (reminder in activeReminders) {
                        if (reminder.triggerTimeEpochMs > now) {
                            ReminderScheduler.scheduleReminder(context, reminder)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
