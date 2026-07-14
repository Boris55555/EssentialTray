package com.boris55555.essentialtray

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationListener : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<Map<String, Int>>(emptyMap())
        val notifications: StateFlow<Map<String, Int>> = _notifications
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateNotifications()
    }

    override fun onListenerConnected() {
        updateNotifications()
    }

    private fun updateNotifications() {
        try {
            val active = activeNotifications
            val counts = active.groupBy { it.packageName }
                .mapValues { (_, notifications) -> notifications.size }
            _notifications.value = counts
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
