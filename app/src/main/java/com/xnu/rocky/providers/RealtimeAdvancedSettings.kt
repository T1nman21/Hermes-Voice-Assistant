//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.providers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors iOS `OpenRockyRealtimeAdvancedSettings`. Persisted only when it differs from
 *  [DEFAULT] — null on the instance means "use defaults".
 *
 *  Defaults target `gpt-realtime-2` on the GA session schema: semantic VAD,
 *  audio output (transcript events come over the data channel automatically),
 *  and `low` reasoning effort (OpenAI's recommended starting point — keeps
 *  turn latency low for typical back-and-forth). */
@Serializable
data class RealtimeAdvancedSettings(
    val realtimeModel: String = DEFAULT_REALTIME_MODEL,
    val transcriptionModel: String = DEFAULT_TRANSCRIPTION_MODEL,
    val inputLanguage: String? = null,
    val reasoningEffort: ReasoningEffort = ReasoningEffort.LOW,
    val turnDetection: TurnDetection = TurnDetection.Semantic(),
    val maxOutputTokens: Int = 1024,
    val speed: Double = 1.0,
    val allowTextOnly: Boolean = false,
    val bargeInRMSThreshold: Int = 3500
) {
    companion object {
        const val DEFAULT_REALTIME_MODEL = "gpt-realtime-2"
        const val DEFAULT_TRANSCRIPTION_MODEL = "gpt-4o-mini-transcribe"
        val DEFAULT = RealtimeAdvancedSettings()

        val realtimeModelOptions = listOf(
            "gpt-realtime-2"
        )
        val transcriptionModelOptions = listOf(
            "gpt-4o-mini-transcribe",
            "gpt-4o-transcribe",
            "whisper-1"
        )
    }
}

/** GA realtime models surface a `reasoning.effort` knob in place of the legacy
 *  `temperature` slider. `LOW` is OpenAI's recommended starting point — best
 *  latency / quality balance for everyday voice. */
@Serializable
enum class ReasoningEffort(val wireValue: String, val displayName: String, val summary: String) {
    @SerialName("minimal")
    MINIMAL("minimal", "Minimal", "Snappiest. Skip reasoning when the model is barely thinking anyway."),

    @SerialName("low")
    LOW("low", "Low (recommended)", "OpenAI's default. Best latency / quality balance for everyday voice."),

    @SerialName("medium")
    MEDIUM("medium", "Medium", "More deliberate. Helps with multi-step tool chains."),

    @SerialName("high")
    HIGH("high", "High", "Strong reasoning. Adds noticeable latency on each turn."),

    @SerialName("xhigh")
    XHIGH("xhigh", "Extra High", "Maximum thinking. Voice will feel sluggish — use only when needed.")
}

@Serializable
sealed class TurnDetection {
    @Serializable
    @SerialName("semantic")
    data class Semantic(val eagerness: String = "auto") : TurnDetection()

    @Serializable
    @SerialName("server")
    data class Server(
        val prefixPaddingMs: Int = 300,
        val silenceDurationMs: Int = 500,
        val threshold: Double = 0.5
    ) : TurnDetection()
}
