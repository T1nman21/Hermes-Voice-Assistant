package com.xnu.rocky

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.xnu.rocky.hermes.HermesVoiceSession

class OpenRockyViewModel(application: Application) : AndroidViewModel(application) {

    val hermesVoiceSession = HermesVoiceSession(application)
    private val app = application as OpenRockyApp

    val needsOnboarding: Boolean get() = !app.isOnboarded
    val wakeWordEnabled: Boolean get() = app.wakeWordEnabled

    val relayState = app.relayClient.state

    init {
        hermesVoiceSession.startObserving()
    }

    fun completeOnboarding(relayKey: String) {
        val parts = relayKey.split("|")
        val relayUrl = parts.getOrElse(0) { "wss://localhost:8643" }
        val roomCode = parts.getOrElse(1) { "HERM" }
        val token = parts.getOrElse(2) { "" }

        app.saveOnboarding(relayUrl, roomCode, token)
    }

    fun toggleWakeWord(enabled: Boolean) {
        app.setWakeWordEnabled(enabled)
    }

    override fun onCleared() {
        super.onCleared()
        hermesVoiceSession.destroy()
    }
}
