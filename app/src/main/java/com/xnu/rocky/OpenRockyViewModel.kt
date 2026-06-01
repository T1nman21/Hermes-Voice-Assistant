package com.xnu.rocky

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.xnu.rocky.hermes.HermesVoiceSession
import com.xnu.rocky.hermes.WakeWordService

class OpenRockyViewModel(application: Application) : AndroidViewModel(application) {

    val hermesVoiceSession = HermesVoiceSession(application)

    private val prefs = getApplication<Application>()
        .getSharedPreferences("hermes_prefs", 0)

    private var hasOnboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) = prefs.edit().putBoolean("onboarded", value).apply()

    val needsOnboarding: Boolean get() = !hasOnboarded

    init {
        // Auto-connect on app restart using saved relay config
        if (hasOnboarded) {
            val savedUrl = prefs.getString("relay_url", "") ?: ""
            val savedRoom = prefs.getString("room_code", "HERM") ?: "HERM"
            val savedToken = prefs.getString("token", "") ?: ""
            hermesVoiceSession.configure(savedUrl, savedRoom, savedToken)
            hermesVoiceSession.connect()
        }
    }

    fun completeOnboarding(relayKey: String) {
        val parts = relayKey.split("|")
        val relayUrl = parts.getOrElse(0) { "wss://localhost:8643" }
        val roomCode = parts.getOrElse(1) { "HERM" }
        val token = parts.getOrElse(2) { "" }

        // Persist for auto-connect on next launch
        prefs.edit()
            .putString("relay_url", relayUrl)
            .putString("room_code", roomCode)
            .putString("token", token)
            .apply()

        hermesVoiceSession.configure(relayUrl, roomCode, token)
        hermesVoiceSession.connect()
        hasOnboarded = true
    }

    override fun onCleared() {
        super.onCleared()
        hermesVoiceSession.destroy()
    }
}
