//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationService(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "openrocky_notifications"
        private const val CHANNEL_NAME = "Rocky Notifications"
    }

    init {
        createChannel()
    }

    /**
     * Schedule a local notification. Supply either [delaySeconds] (relative) or [triggerDate]
     * (ISO-8601 absolute — matches iOS `notification-schedule`). If both are given, [triggerDate] wins.
     */
    suspend fun schedule(title: String, body: String, delaySeconds: Int, triggerDate: String? = null): String {
        val computedDelayMs = computeDelayMs(triggerDate, delaySeconds)
        if (computedDelayMs > 0) {
            delay(computedDelayMs)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
        return "Notification sent: $title"
    }

    private fun computeDelayMs(triggerDate: String?, delaySeconds: Int): Long {
        val iso = triggerDate?.trim().orEmpty()
        if (iso.isNotEmpty()) {
            val epochMs =
                runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()
                    ?: runCatching { ZonedDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()
                    ?: runCatching {
                        LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }.getOrNull()
            if (epochMs != null) {
                return (epochMs - System.currentTimeMillis()).coerceAtLeast(0)
            }
        }
        return delaySeconds.coerceAtLeast(0) * 1000L
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
