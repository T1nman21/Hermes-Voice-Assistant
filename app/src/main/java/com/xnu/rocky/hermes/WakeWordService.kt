package com.xnu.rocky.hermes

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ai.picovoice.porcupine.*
import com.xnu.rocky.MainActivity
import com.xnu.rocky.OpenRockyApp
import com.xnu.rocky.R

/**
 * Foreground service that listens for "Hey Hermes" wake word using Porcupine.
 *
 * When the wake word is detected:
 * 1. A chime plays to confirm detection
 * 2. MainActivity opens (or comes to foreground) with START_VOICE action
 * 3. The app auto-starts listening for the user's command
 *
 * Falls back to notification-only mode if Porcupine isn't configured
 * (no access key, no model file). In that case, users activate voice
 * by tapping the notification or app icon.
 */
class WakeWordService : Service() {

    private var porcupineManager: PorcupineManager? = null
    private var isListening = false
    private var toneGenerator: ToneGenerator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }

        if (!isListening) {
            startListening()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopListening()
        toneGenerator?.release()
        toneGenerator = null
        super.onDestroy()
    }

    // ── Wake word engine ────────────────────────────────────────────

    private fun startListening() {
        if (!hasMicPermission()) {
            updateNotification("Microphone permission required")
            return
        }

        val accessKey = OpenRockyApp.get(this).getPicovoiceKey()
        if (accessKey.isBlank()) {
            updateNotification("Wake word not configured — tap to talk")
            return
        }

        // Check if model file exists in assets
        val modelPath = keywordModelPath()
        if (modelPath == null) {
            updateNotification("Wake word model missing — tap to talk")
            return
        }

        isListening = true
        updateNotification("Listening for 'Hey Hermes'...")

        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeywordPath(modelPath)
                .build(applicationContext) { keywordIndex ->
                    onWakeWordDetected()
                }
            porcupineManager?.start()
        } catch (e: PorcupineException) {
            isListening = false
            updateNotification("Wake word error: ${e.message?.take(30)}")
        }
    }

    private fun stopListening() {
        isListening = false
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
        } catch (_: Exception) {}
        porcupineManager = null
    }

    private fun onWakeWordDetected() {
        playWakeChime()

        // Fire intent to open MainActivity in voice mode
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_START_VOICE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    // ── Model file extraction ───────────────────────────────────────

    private fun keywordModelPath(): String? {
        // Porcupine reads from assets directly — just check it exists
        return try {
            assets.open(KEYWORD_MODEL).close()
            KEYWORD_MODEL  // relative path from assets/
        } catch (e: Exception) {
            null
        }
    }

    // ── Audio feedback ──────────────────────────────────────────────

    private fun playWakeChime() {
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(
                AudioManager.STREAM_NOTIFICATION, 80
            )
        }
        // Rising two-tone chime: "ding-DING"
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    // ── Permissions ─────────────────────────────────────────────────

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    // ── Notification ────────────────────────────────────────────────

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun buildNotification(text: String? = null): Notification {
        val statusText = text ?: if (isListening) {
            "Listening for 'Hey Hermes'..."
        } else {
            "Tap to talk to Hermes"
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, WakeWordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_voice)
            .setContentTitle("Hermes Assistant")
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, "Stop", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "hermes_wake_word"
        private const val NOTIF_ID = 2001
        private const val KEYWORD_MODEL = "hey-hermes_en_android_v3_0_0.ppn"

        const val ACTION_STOP = "com.xnu.rocky.action.STOP_WAKE_WORD"
        const val ACTION_START_VOICE = "com.xnu.rocky.action.START_VOICE"

        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WakeWordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
