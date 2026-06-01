package com.xnu.rocky

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

class VoiceForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
        }
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "Hermes Voice", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Hermes voice session active"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val stopIntent = Intent(this, VoiceForegroundService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_voice)
            .setContentTitle("Hermes Assistant")
            .setContentText("Listening...")
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(Notification.Action.Builder(null, "Stop", stopPending).build())
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "hermes_voice_session"
        private const val NOTIF_ID = 1001
        private const val ACTION_STOP = "com.xnu.rocky.action.STOP_VOICE"

        fun start(context: Context) {
            val intent = Intent(context, VoiceForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VoiceForegroundService::class.java))
        }
    }
}
