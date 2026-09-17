package com.example.features.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class RexyyNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isNotificationAccessGranted.value = true
        activeListener = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isNotificationAccessGranted.value = false
        if (activeListener == this) {
            activeListener = null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        // Ignore system/self notifications
        if (pkg == packageName || pkg == "android") return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val notifKey = "${pkg}_${sbn.id}_${sbn.postTime}"
        if (processedNotificationKeys.contains(notifKey)) return
        processedNotificationKeys.add(notifKey)

        // Keep set size manageable
        if (processedNotificationKeys.size > 500) {
            val toRemove = processedNotificationKeys.take(100)
            processedNotificationKeys.removeAll(toRemove.toSet())
        }

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkg
        }

        val appNotif = AppNotification(
            id = notifKey,
            packageName = pkg,
            appName = appName,
            title = title,
            text = text,
            timestamp = sbn.postTime
        )

        val currentList = _recentNotifications.value.toMutableList()
        currentList.add(0, appNotif)
        if (currentList.size > 50) currentList.removeAt(currentList.lastIndex)
        _recentNotifications.value = currentList

        onNotificationReceivedCallback?.invoke(appNotif)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    companion object {
        private val processedNotificationKeys = mutableSetOf<String>()

        private val _recentNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
        val recentNotifications: StateFlow<List<AppNotification>> = _recentNotifications.asStateFlow()

        private val _isNotificationAccessGranted = MutableStateFlow(false)
        val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

        var onNotificationReceivedCallback: ((AppNotification) -> Unit)? = null
        private var activeListener: RexyyNotificationListener? = null

        fun isPermissionGranted(context: Context): Boolean {
            val cn = ComponentName(context, RexyyNotificationListener::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            val granted = flat != null && flat.contains(cn.flattenToString())
            _isNotificationAccessGranted.value = granted
            return granted
        }

        fun openNotificationAccessSettings(context: Context) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        fun getLatestNotificationSummary(): String {
            val notifs = _recentNotifications.value
            if (notifs.isEmpty()) {
                return "Abhi koi naye notifications nahi hain, Sagar Sir."
            }
            val first = notifs.first()
            return "${first.appName} se notification: ${first.title} - ${first.text}"
        }

        fun clearNotifications() {
            _recentNotifications.value = emptyList()
            processedNotificationKeys.clear()
        }
    }
}
