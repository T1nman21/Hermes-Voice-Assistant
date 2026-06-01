package com.xnu.rocky

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.xnu.rocky.hermes.HermesVoiceSession
import com.xnu.rocky.hermes.WakeWordService

class OpenRockyViewModel(application: Application) : AndroidViewModel(application) {

    val hermesVoiceSession = HermesVoiceSession(application)

    private var hasOnboarded: Boolean
        get() = getApplication<Application>()
            .getSharedPreferences("hermes_prefs", 0)
            .getBoolean("onboarded", false)
        set(value) = getApplication<Application>()
            .getSharedPreferences("hermes_prefs", 0)
            .edit().putBoolean("onboarded", value).apply()

    val needsOnboarding: Boolean get() = !hasOnboarded

    fun completeOnboarding(relayKey: String) {
        val parts = relayKey.split("|")
        val relayUrl = parts.getOrElse(0) { "wss://localhost:8643" }
        val roomCode = parts.getOrElse(1) { "HERM" }
        val token = parts.getOrElse(2) { "" }
        hermesVoiceSession.configure(relayUrl, roomCode, token)
        hermesVoiceSession.connect()
        hasOnboarded = true
    }

    override fun onCleared() {
        super.onCleared()
        hermesVoiceSession.destroy()
    }
}
