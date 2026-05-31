//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.voice

import kotlinx.coroutines.flow.Flow

interface RealtimeVoiceClient {
    suspend fun connect(): Flow<RealtimeEvent>
    suspend fun disconnect()
    suspend fun sendText(text: String)
    suspend fun sendAudioChunk(data: ByteArray)
    suspend fun finishAudioInput()
    suspend fun sendToolOutput(callID: String, output: String)
    suspend fun speakText(text: String)

    /** Cancel the in-flight assistant response (sent on user barge-in). Mirrors iOS `response.cancel`. */
    suspend fun cancelResponse() {}

    /** Truncate the assistant audio mid-playback so the conversation history matches what the user actually heard.
     *  [audioEndMs] is how much assistant audio has been played so far. Mirrors iOS `conversation.item.truncate`. */
    suspend fun truncateAssistantAudio(audioEndMs: Long) {}
}
