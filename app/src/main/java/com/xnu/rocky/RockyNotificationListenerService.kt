//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//

package com.xnu.rocky

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.xnu.rocky.runtime.tools.NotificationInboxStore

/**
 * System notification listener.
 *
 * iOS cannot grant apps read access to notifications from other apps; on Android the user
 * can enable Rocky as a NotificationListener once in Settings and then ask things like
 * "What did I miss?" or "Summarize my unread WhatsApp" — the signature example of Android's
 * greater user-agency trust model.
 *
 * We only keep notifications in an in-memory ring buffer ([NotificationInboxStore]) and never
 * persist them to disk.
 */
class RockyNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationInboxStore.setGranted(true)
        // Seed the inbox with whatever is currently posted so the AI sees existing notifications
        // immediately rather than having to wait for the next post event.
        runCatching { activeNotifications }.getOrNull()?.forEach { record(it) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationInboxStore.setGranted(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        record(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotificationInboxStore.remove(sbn.key)
    }

    private fun record(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        // Skip our own notifications (voice-session foreground notification isn't useful to surface back).
        if (sbn.packageName == packageName) return

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val appLabel = runCatching {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrNull()

        NotificationInboxStore.record(
            NotificationInboxStore.Entry(
                key = sbn.key,
                packageName = sbn.packageName,
                appLabel = appLabel,
                title = title,
                text = text,
                subText = subText,
                postedAtMs = sbn.postTime
            )
        )
    }
}
