//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-21
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import com.xnu.rocky.runtime.LogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class OpenRockyTimer(
    val id: String,
    val label: String,
    val createdAt: Long,
    val fireAt: Long
)

/**
 * Countdown timers backed by AlarmManager + local notification.
 * Parallel of OpenRockyTimerService on iOS.
 */
class TimerService(private val context: Context) {
    companion object {
        private const val TAG = "TimerService"
        private const val CHANNEL_ID = "openrocky_timers"
        const val ACTION_FIRE = "com.xnu.rocky.TIMER_FIRE"
        const val EXTRA_ID = "timer_id"
        const val EXTRA_LABEL = "timer_label"
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val storeFile: File get() = File(context.filesDir, "openrocky_timers.json")
    private val _timers = MutableStateFlow<List<OpenRockyTimer>>(emptyList())
    val timers: StateFlow<List<OpenRockyTimer>> = _timers.asStateFlow()

    init {
        load()
        pruneExpired()
        createChannel()
        registerReceiver()
    }

    fun schedule(label: String, seconds: Long): OpenRockyTimer {
        require(seconds > 0) { "Timer duration must be > 0 seconds" }
        val cleanLabel = label.trim().ifEmpty { "Timer" }
        val now = System.currentTimeMillis()
        val timer = OpenRockyTimer(
            id = UUID.randomUUID().toString(),
            label = cleanLabel,
            createdAt = now,
            fireAt = now + seconds * 1000
        )
        scheduleAlarm(timer)
        _timers.value = _timers.value + timer
        save()
        LogManager.info("Timer scheduled: ${timer.label} in ${seconds}s", TAG)
        return timer
    }

    fun cancel(id: String): Boolean {
        val existing = _timers.value.find { it.id == id } ?: return false
        cancelAlarm(existing)
        _timers.value = _timers.value.filter { it.id != id }
        save()
        return true
    }

    fun cancelAll(): Int {
        val n = _timers.value.size
        _timers.value.forEach { cancelAlarm(it) }
        _timers.value = emptyList()
        save()
        return n
    }

    fun list(): List<OpenRockyTimer> {
        pruneExpired()
        return _timers.value.sortedBy { it.fireAt }
    }

    private fun scheduleAlarm(timer: OpenRockyTimer) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_FIRE).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_ID, timer.id)
            putExtra(EXTRA_LABEL, timer.label)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, timer.id.hashCode(), intent, flags)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.fireAt, pi)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timer.fireAt, pi)
        }
    }

    private fun cancelAlarm(timer: OpenRockyTimer) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_FIRE).setPackage(context.packageName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, timer.id.hashCode(), intent, flags)
        alarmManager.cancel(pi)
    }

    private fun pruneExpired() {
        val now = System.currentTimeMillis()
        val kept = _timers.value.filter { it.fireAt > now }
        if (kept.size != _timers.value.size) {
            _timers.value = kept
            save()
        }
    }

    private fun load() {
        if (!storeFile.exists()) return
        runCatching {
            _timers.value = json.decodeFromString(storeFile.readText())
        }.onFailure { LogManager.error("Timer load failed: ${it.message}", TAG) }
    }

    private fun save() {
        runCatching {
            storeFile.writeText(json.encodeToString(_timers.value))
        }.onFailure { LogManager.error("Timer save failed: ${it.message}", TAG) }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Timers", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private var receiverRegistered = false
    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(ACTION_FIRE)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val id = intent.getStringExtra(EXTRA_ID) ?: return
                val label = intent.getStringExtra(EXTRA_LABEL) ?: "Timer"
                fireTimer(c, id, label)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        receiverRegistered = true
    }

    private fun fireTimer(c: Context, id: String, label: String) {
        val manager = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(c, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(label)
            .setContentText("Timer finished.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        manager.notify(id.hashCode(), notification)
        _timers.value = _timers.value.filter { it.id != id }
        save()
    }
}
