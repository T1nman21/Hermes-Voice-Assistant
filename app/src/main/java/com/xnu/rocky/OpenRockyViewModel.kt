//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.xnu.rocky.hermes.HermesVoiceSession
import com.xnu.rocky.providers.ProviderStore
import com.xnu.rocky.providers.RealtimeAdvancedSettings
import com.xnu.rocky.providers.RealtimeProviderStore
import com.xnu.rocky.runtime.*
import com.xnu.rocky.runtime.skills.CustomSkillStore
import com.xnu.rocky.runtime.tools.BuiltInToolStore
import com.xnu.rocky.runtime.tools.Toolbox

class OpenRockyViewModel(application: Application) : AndroidViewModel(application) {
    val providerStore = ProviderStore(application)
    val realtimeProviderStore = RealtimeProviderStore(application)
    val characterStore = CharacterStore(application)
    val soulStore = SoulStore(application)
    val memoryService = MemoryService(application)
    val usageService = UsageService(application)
    val storageProvider = PersistentStorageProvider(application)
    val customSkillStore = CustomSkillStore(application)
    val builtInToolStore = BuiltInToolStore(application)
    val preferences = Preferences(application)
    val toolbox = Toolbox(application, memoryService)

    /** Hermes voice session — handles TTS, STT, and Hermes API communication */
    val hermesVoiceSession = HermesVoiceSession(application)

    val sessionRuntime = SessionRuntime(
        context = application,
        providerStore = providerStore,
        realtimeProviderStore = realtimeProviderStore,
        characterStore = characterStore,
        memoryService = memoryService,
        usageService = usageService,
        storageProvider = storageProvider,
        toolbox = toolbox,
        preferences = preferences
    )

    private var hasOnboarded: Boolean
        get() = getApplication<Application>().getSharedPreferences("openrocky_prefs", 0).getBoolean("onboarded", false)
        set(value) = getApplication<Application>().getSharedPreferences("openrocky_prefs", 0).edit().putBoolean("onboarded", value).apply()

    val needsOnboarding: Boolean get() = !hasOnboarded || providerStore.instances.value.isEmpty()

    fun completeOnboarding(hostOrApiKey: String) {
        if (hostOrApiKey.startsWith("ws://") || hostOrApiKey.startsWith("wss://")) {
            // Relay-based connection: "ws://ip:port|ROOMCODE"
            val parts = hostOrApiKey.split("|")
            val relayUrl = parts[0]
            val roomCode = parts.getOrElse(1) { "HERM" }
            val token = parts.getOrElse(2) { "" }
            hermesVoiceSession.configure(relayUrl, roomCode, token)
            hermesVoiceSession.connect()
        } else if (hostOrApiKey.startsWith("http")) {
            // Hermes desktop connection — host URL from onboarding
            val displayHost = hostOrApiKey
                .removePrefix("http://")
                .removeSuffix("/v1")
                .removeSuffix("/")
            val chatInstance = com.xnu.rocky.providers.ProviderInstance(
                name = "Hermes ($displayHost)",
                kind = com.xnu.rocky.providers.ProviderKind.HERMES,
                modelID = "hermes",
                customHost = hostOrApiKey
            )
            providerStore.save(chatInstance, "hermes-local")
        } else if (hostOrApiKey.isNotBlank()) {
            // Legacy: OpenAI API key (fallback for non-Hermes users)
            val chatInstance = com.xnu.rocky.providers.ProviderInstance(
                name = "OpenAI",
                kind = com.xnu.rocky.providers.ProviderKind.OPENAI,
                modelID = "gpt-4o"
            )
            providerStore.save(chatInstance, hostOrApiKey)

            val voiceInstance = com.xnu.rocky.providers.RealtimeProviderInstance(
                name = "OpenAI Realtime",
                kind = com.xnu.rocky.providers.RealtimeProviderKind.OPENAI,
                modelID = RealtimeAdvancedSettings.DEFAULT_REALTIME_MODEL
            )
            realtimeProviderStore.save(voiceInstance, hostOrApiKey)
        }
        hasOnboarded = true
    }

    override fun onCleared() {
        super.onCleared()
        sessionRuntime.destroy()
    }
}
