//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.voice

data class RealtimeVoiceFeatures(
    val supportsTextInput: Boolean = false,
    val supportsAssistantStreaming: Boolean = false,
    val supportsToolCalls: Boolean = false,
    val supportsAudioOutput: Boolean = false,
    val needsMicSuspension: Boolean = false
)
