package com.xnu.rocky.hermes

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.xnu.rocky.MainActivity
import com.xnu.rocky.R

/**
 * Foreground service that listens for "Hey Hermes" wake word using Porcupine.
 *
 * When the wake word is detected, it fires the START_VOICE intent to open
 * the Hermes voice session. Falls back to manual activation if Porcupine
 * isn't configured (no access key).
 *
 * Required setup:
 * 1. Sign up at https://picovoice.ai/ for a free access key
 * 2. Train "Hey Hermes" wake word at https://console.picovoice.ai/
 * 3. Place the .ppn model file in app/src/main/assets/
 * 4. Update ACCESS_KEY below or store in app preferences
 */
class WakeWordService : Service() {

    companion object {
        private const val CHANNEL_ID = "hermes_wake_word"
        private const val NOTIF_ID = 2001
        private const val TAG = "WakeWordService"

        // Replace with your Picovoice access key or leave empty for manual activation
        private const val ACCESS_KEY = ""

        // Model file in assets/ folder (trained for "Hey Hermes")
        private const val KEYWORD_MODEL = "hey-hermes_en_android_v3_0_0.ppn"

        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordService::class.java))
        }

        fun isWakeWordConfigured(): Boolean = ACCESS_KEY.isNotBlank()
    }

    private var isListening = false
    // porcupineManager field would be here when Porcupine SDK is added

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP" -> {
                stopListening()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }

        if (!isListening && isWakeWordConfigured()) {
            startListening()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }

    private fun startListening() {
        if (!hasMicPermission()) {
            stopSelf()
            return
        }
        isListening = true

        // TODO: Initialize Porcupine here once the SDK dependency is added:
        //
        // porcupineManager = PorcupineManager.Builder()
        //     .setAccessKey(ACCESS_KEY)
        //     .setKeyword(KEYWORD_MODEL, onWakeWordDetected)
        //     .build(context)
        // porcupineManager?.start()
        //
        // For now, wake word activation works via:
        // - Long-press home button (digital assistant intent)
        // - Quick Settings tile
        // - Home screen widget
        // - App icon tap
    }

    private fun stopListening() {
        isListening = false
        // porcupineManager?.stop()
        // porcupineManager?.delete()
    }

    // Called when Porcupine detects "Hey Hermes"
    @Suppress("unused")
    private fun onWakeWordDetected() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = com.xnu.rocky.ACTION_START_VOICE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hermes Wake Word",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Listening for 'Hey Hermes' wake word"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, WakeWordService::class.java).apply {
            action = "STOP"
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val statusText = if (isWakeWordConfigured()) {
            "Listening for 'Hey Hermes'..."
        } else {
            "Wake word not configured — tap to talk"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_voice)
            .setContentTitle("Hermes Voice")
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, "Stop", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
