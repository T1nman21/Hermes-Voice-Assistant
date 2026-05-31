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

@Serializable
enum class RealtimeProviderKind(
    val displayName: String,
    val summary: String,
    val defaultModel: String,
    val suggestedModels: List<String>,
    val credentialTitle: String,
    val credentialPlaceholder: String,
    val guideUrl: String
) {
    OPENAI(
        displayName = "OpenAI Realtime",
        summary = "gpt-realtime-2, tool calls + streaming (WebRTC)",
        defaultModel = "gpt-realtime-2",
        suggestedModels = listOf("gpt-realtime-2"),
        credentialTitle = "API Key",
        credentialPlaceholder = "sk-...",
        guideUrl = "https://platform.openai.com/api-keys"
    );
}
