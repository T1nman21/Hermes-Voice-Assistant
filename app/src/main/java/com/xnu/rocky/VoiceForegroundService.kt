//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//

package com.xnu.rocky

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Keeps the voice session alive when the user leaves the app or locks the screen.
 *
 * iOS only lets backgrounded audio apps *play* — they cannot sustain a full mic-in voice session.
 * Android's foreground-service-with-microphone lets Rocky stay in an active conversation while
 * the user walks around, switches apps, or turns the screen off. Users see a persistent
 * notification with a "Stop" button they can tap from the shade or lock screen.
 */
class VoiceForegroundService : Service() {

    private var mediaSession: VoiceMediaSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                requestSessionStop()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startAsForeground()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    /**
     * Hand off to MainActivity so SessionRuntime (the single source of truth for voice
     * lifecycle) can tear down the bridge cleanly; it then calls stop() on this service.
     */
    private fun requestSessionStop() {
        val stopIntent = Intent(this, MainActivity::class.java)
        stopIntent.action = ACTION_STOP_VOICE
        stopIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(stopIntent)
    }

    private fun startAsForeground() {
        ensureChannel()
        // MediaSession makes the notification show rich media controls on the lock screen + Bluetooth
        // headsets, and routes headset play/pause buttons to stop the voice session.
        if (mediaSession == null) {
            mediaSession = VoiceMediaSession(this) { requestSessionStop() }
        }
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, VoiceForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopAction = Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_tile_voice),
            getString(R.string.voice_service_stop),
            stopPending
        ).build()

        // Framework Notification.Builder so we can attach Notification.MediaStyle (lock-screen
        // media card) without pulling in androidx.media just for the compat shim.
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_voice)
            .setContentTitle(getString(R.string.voice_service_title))
            .setContentText(getString(R.string.voice_service_text))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setContentIntent(openPending)
            .addAction(stopAction)
            // Silence default notification sound — the channel is IMPORTANCE_LOW so this is mostly belt-and-braces.
            .setOnlyAlertOnce(true)

        mediaSession?.token?.let { token ->
            val style = Notification.MediaStyle()
                .setMediaSession(token)
                .setShowActionsInCompactView(0)
            builder.style = style
        }

        return builder.build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.voice_service_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.voice_service_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "openrocky_voice_session"
        private const val NOTIF_ID = 1001
        private const val ACTION_STOP = "com.xnu.rocky.action.STOP_VOICE_SERVICE"

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
