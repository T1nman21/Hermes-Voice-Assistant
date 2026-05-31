//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.providers

data class RealtimeProviderConfiguration(
    val provider: RealtimeProviderKind,
    val modelID: String,
    val credential: String,
    val customHost: String = "",
    val openaiVoice: String = "alloy",
    val advancedSettings: RealtimeAdvancedSettings = RealtimeAdvancedSettings.DEFAULT,
    val characterName: String = "",
    val characterSpeakingStyle: String = "",
    val characterGreeting: String = ""
) {
    val isValid: Boolean
        get() = credential.isNotBlank() && modelID.isNotBlank()

    val maskedCredential: String
        get() {
            if (credential.length <= 8) return "••••••••"
            return credential.take(4) + "••••" + credential.takeLast(4)
        }
}
