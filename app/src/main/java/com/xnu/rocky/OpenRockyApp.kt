package com.xnu.rocky

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.getSystemService
import com.xnu.rocky.hermes.RelayClient
import com.xnu.rocky.hermes.WakeWordService

/**
 * Application singleton — owns long-lived state:
 * - RelayClient WebSocket (survives Activity lifecycle)
 * - SharedPreferences for onboarding + wake word config
 * - Auto-starts WakeWordService if already onboarded
 */
class OpenRockyApp : Application() {

    lateinit var relayClient: RelayClient
        private set

    lateinit var prefs: SharedPreferences
        private set

    var isOnboarded: Boolean = false
        private set

    var wakeWordEnabled: Boolean = true
        private set

    companion object {
        const val PREFS_NAME = "hermes_prefs"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_RELAY_URL = "relay_url"
        const val KEY_ROOM_CODE = "room_code"
        const val KEY_TOKEN = "token"
        const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        const val KEY_PICOVOICE_KEY = "picovoice_access_key"

        private lateinit var instance: OpenRockyApp

        /** Convenience accessor for WakeWordService and other components. */
        fun get(context: Context): OpenRockyApp = context.applicationContext as OpenRockyApp
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isOnboarded = prefs.getBoolean(KEY_ONBOARDED, false)
        wakeWordEnabled = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)

        // Create relay client — stays connected even when Activity goes away
        relayClient = RelayClient()

        // Auto-connect if we have saved relay config
        if (isOnboarded) {
            val url = prefs.getString(KEY_RELAY_URL, "") ?: ""
            val room = prefs.getString(KEY_ROOM_CODE, "HERM") ?: "HERM"
            val token = prefs.getString(KEY_TOKEN, "") ?: ""
            if (url.isNotBlank()) {
                relayClient.configure(url, room, token)
                relayClient.connect()
            }
        }

        // Auto-start wake word service if onboarded and enabled
        if (isOnboarded && wakeWordEnabled) {
            WakeWordService.start(this)
        }
    }

    fun saveOnboarding(url: String, room: String, token: String) {
        prefs.edit()
            .putString(KEY_RELAY_URL, url)
            .putString(KEY_ROOM_CODE, room)
            .putString(KEY_TOKEN, token)
            .putBoolean(KEY_ONBOARDED, true)
            .apply()
        isOnboarded = true

        relayClient.configure(url, room, token)
        relayClient.connect()

        // Start wake word after onboarding
        if (wakeWordEnabled) {
            WakeWordService.start(this)
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        wakeWordEnabled = enabled
        prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
        if (enabled && isOnboarded) {
            WakeWordService.start(this)
        } else {
            WakeWordService.stop(this)
        }
    }

    fun getPicovoiceKey(): String =
        prefs.getString(KEY_PICOVOICE_KEY, BuildConfig.PICOVOICE_ACCESS_KEY)?.takeIf { it.isNotBlank() } ?: ""

    fun setPicovoiceKey(key: String) {
        prefs.edit().putString(KEY_PICOVOICE_KEY, key).apply()
        // Restart wake word service to pick up new key
        if (wakeWordEnabled && isOnboarded) {
            WakeWordService.stop(this)
            WakeWordService.start(this)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService<NotificationManager>() ?: return
            val wakeChannel = NotificationChannel(
                "hermes_wake_word",
                "Hermes Wake Word",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Listening for 'Hey Hermes' wake word"
                setShowBadge(false)
            }
            val voiceChannel = NotificationChannel(
                "hermes_voice_session",
                "Hermes Voice",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hermes voice session active"
                setShowBadge(false)
            }
            nm.createNotificationChannel(wakeChannel)
            nm.createNotificationChannel(voiceChannel)
        }
    }

}
