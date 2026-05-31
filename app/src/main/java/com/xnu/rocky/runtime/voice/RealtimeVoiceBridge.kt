//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.voice

import android.content.Context
import com.xnu.rocky.providers.RealtimeProviderConfiguration
import com.xnu.rocky.providers.RealtimeProviderKind
import com.xnu.rocky.runtime.CharacterStore
import com.xnu.rocky.runtime.LogManager
import com.xnu.rocky.runtime.tools.Toolbox
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class RealtimeVoiceBridge(
    private val context: Context,
    private val config: RealtimeProviderConfiguration,
    private val toolbox: Toolbox,
    private val characterStore: CharacterStore,
    /**
     * Optional replay-into-the-realtime-session items. Captured at bridge
     * construction so reconnect-on-drop pushes the same priming forward and a
     * Wi-Fi blip mid-conversation doesn't reset the model's view of what the
     * user already said. Empty list means a fresh session. Mirrors iOS
     * `lastPrimingItems`.
     */
    private val primingItems: List<VoicePrimingItem> = emptyList()
) {
    companion object {
        private const val TAG = "VoiceBridge"

        /**
         * Backoff schedule for unexpected drops. Three short tries cover the
         * common Wi-Fi → cell handoff and brief server hiccups. After that we
         * give up and ask the user to tap, so we don't burn battery looping.
         * Mirrors iOS reconnectBackoffSeconds.
         */
        private val RECONNECT_BACKOFF_MS = listOf(1_000L, 3_000L, 9_000L)

        /**
         * Hard cap on transcript turns kept for reconnect-replay. A marathon
         * session shouldn't grow unbounded — VoicePriming.capped() then trims
         * the combined list to its own budget before the new session sees it.
         * Mirrors iOS inSessionTurnsHardCap.
         */
        private const val IN_SESSION_TURNS_HARD_CAP = 100
    }

    private var client: RealtimeVoiceClient? = null

    /**
     * Final-transcript turns observed since this session started. Combined with
     * [primingItems] on reconnect so the new session sees both the pre-session
     * context AND whatever was said after — otherwise reconnect "forgets" the
     * live conversation. Reset after a successful reconnect, since the new
     * session has freshly replayed them. Mirrors iOS `inSessionTurns`.
     */
    private val inSessionTurns: MutableList<VoicePrimingItem> = mutableListOf()

    private fun makeClient(replayItems: List<VoicePrimingItem>): RealtimeVoiceClient =
        when (config.provider) {
            RealtimeProviderKind.OPENAI -> OpenAIRealtimeVoiceClient(
                config, toolbox, characterStore, context, replayItems
            )
        }

    private fun appendInSessionTurn(role: VoicePrimingItem.Role, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        inSessionTurns.add(VoicePrimingItem(role, trimmed))
        if (inSessionTurns.size > IN_SESSION_TURNS_HARD_CAP) {
            // Drop the oldest overflow; the priming budget further trims later.
            val overflow = inSessionTurns.size - IN_SESSION_TURNS_HARD_CAP
            repeat(overflow) { inSessionTurns.removeAt(0) }
        }
    }

    /**
     * Streams events for one logical voice session, reconnecting transparently
     * across transport drops. The inner loop creates a fresh client each time
     * the previous one terminates with `Disconnected`; cancellation (user tap)
     * propagates out cleanly so the upstream collector sees the stop. After
     * RECONNECT_BACKOFF_MS is exhausted the session ends with a Status entry
     * inviting the user to tap reconnect.
     */
    fun start(): Flow<RealtimeEvent> = channelFlow {
        var attempts = 0
        // Per-attempt replay window: initial priming on the first pass, then
        // (priming + accumulated in-session turns) on each reconnect. Trimmed
        // back to budget so a long live conversation doesn't balloon the replay.
        var replayWindow: List<VoicePrimingItem> = VoicePriming.capped(primingItems)
        while (true) {
            val voiceClient = makeClient(replayWindow)
            client = voiceClient
            var dropped = false
            var lastError: String? = null
            try {
                voiceClient.connect().collect { event ->
                    when (event) {
                        is RealtimeEvent.Disconnected -> {
                            // Sentinel: client's internal teardown is in progress.
                            // The collect() call will return shortly; we'll fall
                            // through to the reconnect decision below.
                            dropped = true
                            lastError = event.lastError
                        }
                        is RealtimeEvent.SessionReady -> {
                            // A successful (re)connect resets the backoff so a
                            // user with a flaky connection still gets the full
                            // budget on the next drop. The new session has
                            // replayed every captured turn, so the in-session
                            // log starts clean for the next drop.
                            attempts = 0
                            inSessionTurns.clear()
                            send(event)
                        }
                        is RealtimeEvent.UserTranscriptFinal -> {
                            appendInSessionTurn(VoicePrimingItem.Role.USER, event.text)
                            send(event)
                        }
                        is RealtimeEvent.AssistantTranscriptFinal -> {
                            appendInSessionTurn(VoicePrimingItem.Role.ASSISTANT, event.text)
                            send(event)
                        }
                        is RealtimeEvent.ToolCallRequested -> {
                            send(event)
                            try {
                                val result = toolbox.execute(event.name, event.arguments)
                                voiceClient.sendToolOutput(event.callID, result)
                            } catch (e: Exception) {
                                LogManager.error("Tool execution failed: ${e.message}", TAG)
                                voiceClient.sendToolOutput(event.callID, "Error: ${e.message}")
                            }
                        }
                        else -> send(event)
                    }
                }
            } catch (e: CancellationException) {
                throw e // user-initiated stop — propagate cleanly
            } catch (e: Exception) {
                LogManager.error("Voice client error: ${e.message}", TAG)
                dropped = true
                lastError = e.message
            }

            client = null
            if (!dropped) break // clean termination, nothing to reconnect to

            // Skip the backoff loop for unrecoverable errors — retrying a 401
            // three times only produces three 401s, and meanwhile the user is
            // staring at "reconnecting...". Surface a one-line hint instead.
            val unrecoverable = lastError?.let { VoiceErrorTriage.isUnrecoverable(it) } == true
            if (unrecoverable) {
                LogManager.warning("Voice drop is unrecoverable — skipping retry: $lastError", TAG)
                val hint = lastError?.let { VoiceErrorTriage.hint(it) }
                    ?: "Voice session disconnected. Tap to reconnect."
                send(RealtimeEvent.Status(hint))
                break
            }

            if (attempts >= RECONNECT_BACKOFF_MS.size) {
                // All attempts exhausted. Surface the reason via `Error` (not just
                // `Status`) so it lands in the timeline and the user sees *why* the
                // session went quiet, not just that it did. Mirrors iOS.
                val total = RECONNECT_BACKOFF_MS.size
                val reason = lastError?.let { VoiceErrorTriage.hint(it) ?: it }
                val message = if (reason != null) {
                    "Voice reconnect failed after $total attempts: $reason Tap to reconnect."
                } else {
                    "Voice session disconnected after $total reconnect attempts. Tap to reconnect."
                }
                LogManager.warning("Voice reconnect exhausted after $total attempts: ${lastError ?: "transport drop"}", TAG)
                send(RealtimeEvent.Error(message))
                break
            }
            send(RealtimeEvent.MicrophoneActive(false))
            send(RealtimeEvent.Status("Voice session dropped — reconnecting..."))
            delay(RECONNECT_BACKOFF_MS[attempts])
            attempts++
            send(RealtimeEvent.Status("Reconnecting (attempt $attempts/${RECONNECT_BACKOFF_MS.size})..."))

            // Combine pre-session priming with anything captured during the
            // live session; trim back to budget so the replay stays small.
            replayWindow = VoicePriming.capped(primingItems + inSessionTurns)
        }
    }

    fun stop() {
        LogManager.info("[BRIDGE] Stopping voice bridge", TAG)
        client = null
    }
}
