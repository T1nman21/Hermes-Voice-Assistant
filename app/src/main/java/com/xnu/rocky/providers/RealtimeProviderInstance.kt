//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.providers

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RealtimeProviderInstance(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val kind: RealtimeProviderKind = RealtimeProviderKind.OPENAI,
    val modelID: String = "",
    val customHost: String = "",
    val openaiVoice: String = "alloy",
    val advancedSettings: RealtimeAdvancedSettings? = null
) {
    val credentialKeychainKey: String
        get() = "realtime_provider_credential_$id"

    val effectiveAdvancedSettings: RealtimeAdvancedSettings
        get() = advancedSettings ?: RealtimeAdvancedSettings.DEFAULT

    fun toConfiguration(credential: String): RealtimeProviderConfiguration {
        val advanced = effectiveAdvancedSettings
        // Advanced settings are the source of truth for the realtime model — top-level modelID
        // is treated as a legacy hint, kept in sync below.
        val resolvedModel = advanced.realtimeModel.ifBlank { modelID.ifBlank { kind.defaultModel } }
        return RealtimeProviderConfiguration(
            provider = kind,
            modelID = resolvedModel,
            credential = credential,
            customHost = customHost,
            openaiVoice = openaiVoice,
            advancedSettings = advanced
        )
    }
}
