//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class CharacterDefinition(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val personality: String = "",
    val greeting: String = "",
    val speakingStyle: String = "",
    val openaiVoice: String? = null,
    val isBuiltIn: Boolean = false
)

class CharacterStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dir: File get() = File(context.filesDir, "OpenRockyCharacters").also { it.mkdirs() }
    private val manifestFile: File get() = File(dir, "manifest.json")

    private val _characters = MutableStateFlow<List<CharacterDefinition>>(emptyList())
    val characters: StateFlow<List<CharacterDefinition>> = _characters.asStateFlow()

    private val _activeCharacterID = MutableStateFlow<String?>(null)
    val activeCharacterID: StateFlow<String?> = _activeCharacterID.asStateFlow()

    val activeCharacter: CharacterDefinition
        get() = _characters.value.find { it.id == _activeCharacterID.value } ?: builtInCharacters.first()

    init {
        seedBuiltIns()
        load()
    }

    fun save(character: CharacterDefinition) {
        val file = File(dir, "${character.id}.json")
        file.writeText(json.encodeToString(character))
        val list = _characters.value.toMutableList()
        val idx = list.indexOfFirst { it.id == character.id }
        if (idx >= 0) list[idx] = character else list.add(character)
        _characters.value = list
        saveManifest()
    }

    fun activate(id: String) {
        _activeCharacterID.value = id
        saveManifest()
    }

    fun delete(id: String) {
        File(dir, "$id.json").delete()
        _characters.value = _characters.value.filter { it.id != id }
        if (_activeCharacterID.value == id) {
            _activeCharacterID.value = _characters.value.firstOrNull()?.id
        }
        saveManifest()
    }

    fun resetBuiltIn(id: String) {
        val original = builtInCharacters.find { it.id == id } ?: return
        save(original)
    }

    fun systemPrompt(toolDescriptions: String = ""): String {
        val char = activeCharacter
        return buildString {
            appendLine("You are ${char.name}.")
            if (char.personality.isNotBlank()) {
                appendLine()
                appendLine(char.personality)
            }
            if (toolDescriptions.isNotBlank()) {
                appendLine()
                appendLine("## Available Tools")
                appendLine(toolDescriptions)
                if (toolDescriptions.contains("delegate-task")) {
                    appendLine()
                    appendLine("When a task is complex or multi-step, prefer using `delegate-task` to run focused background subtasks in parallel. For simple single-tool tasks, call the tool directly.")
                }
            }
        }
    }

    /**
     * System prompt for the OpenAI Realtime voice session.
     *
     * The realtime model has a deliberately small tool surface — about 15 fast, single-shot
     * local actions plus `delegate-task`. Anything beyond that (web search, multi-step
     * composition, file/shell/python work, custom skills, reasoning that combines multiple
     * data sources) must go through `delegate-task`, which runs on the chat-provider sub-agent
     * and has the full tool set. Mirrors iOS `OpenRockyCharacterStore.voiceSystemPrompt`.
     */
    fun voiceSystemPrompt(): String {
        val char = activeCharacter
        val greeting = char.greeting.ifBlank { "Hey, what can I do for you?" }
        return buildString {
            appendLine("You are ${char.name}, an AI voice assistant on Android.")
            if (char.speakingStyle.isNotBlank()) appendLine(char.speakingStyle)
            if (char.personality.isNotBlank()) appendLine(char.personality)
            appendLine("Keep replies short and conversational — one to three sentences unless the user asks for detail.")
            appendLine("Do NOT narrate tool calls. Call tools silently and speak only the final result.")
            appendLine("When greeting the user, just say something brief like \"$greeting\"")
            appendLine()
            appendLine("## Tool Routing")
            appendLine("You have two tiers of tools.")
            appendLine()
            appendLine("**Fast tools (call these directly):** android-location, timer, android-alarm, " +
                "android-calendar-list, android-calendar-create, android-reminder-list, " +
                "android-reminder-create, notification-schedule, open-url, memory_get, memory_write, " +
                "camera-capture, photo-pick, file-pick.")
            appendLine("These are single-shot local actions that complete in milliseconds.")
            appendLine()
            appendLine("**delegate-task (use for everything else):** weather, web search, news, " +
                "research, multi-step composition, file/shell/python work, contacts lookup, " +
                "health analysis, comparing data from multiple sources, any task that requires " +
                "reasoning or combining information.")
            appendLine()
            appendLine("Default to `delegate-task` when in doubt. The sub-agent has a much larger " +
                "toolbox and is better at multi-step reasoning than the realtime voice model.")
            appendLine()
            appendLine("## Speaking Before Delegation")
            appendLine("`delegate-task` may take 3–30 seconds. Before calling it, **briefly say one " +
                "line out loud** so the user knows you're working — e.g. \"Let me look that up\", " +
                "\"Hold on, I'll check\", \"稍等，我查一下\". Keep it natural and short.")
            appendLine("Then call delegate-task. When the result comes back, summarize it " +
                "conversationally — do not read the raw response verbatim.")
        }
    }

    private fun seedBuiltIns() {
        for (char in builtInCharacters) {
            val file = File(dir, "${char.id}.json")
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.writeText(json.encodeToString(char))
            }
        }
    }

    private fun load() {
        val list = mutableListOf<CharacterDefinition>()
        dir.listFiles()?.filter { it.extension == "json" && it.name != "manifest.json" }?.forEach { file ->
            try {
                list.add(json.decodeFromString<CharacterDefinition>(file.readText()))
            } catch (_: Exception) {}
        }
        _characters.value = list.sortedByDescending { it.isBuiltIn }

        try {
            val manifest = json.decodeFromString<ManifestData>(manifestFile.readText())
            _activeCharacterID.value = manifest.activeCharacterID
        } catch (_: Exception) {
            _activeCharacterID.value = builtInCharacters.first().id
        }
    }

    private fun saveManifest() {
        val data = ManifestData(activeCharacterID = _activeCharacterID.value)
        manifestFile.writeText(json.encodeToString(data))
    }

    @kotlinx.serialization.Serializable
    private data class ManifestData(val activeCharacterID: String? = null)

    companion object {
        val builtInCharacters = listOf(
            CharacterDefinition(
                id = "rocky-default",
                name = "Rocky",
                description = "Friendly, efficient AI assistant",
                personality = "You are Rocky, a friendly and efficient AI assistant. You help users accomplish tasks quickly and clearly. You're conversational but concise, and you always try to be helpful. When using tools, explain what you're doing briefly.",
                greeting = "Hey! What can I do for you?",
                speakingStyle = "Conversational, concise, and friendly. Use natural language.",
                openaiVoice = "alloy",
                isBuiltIn = true
            ),
            CharacterDefinition(
                id = "english-teacher",
                name = "English Teacher",
                description = "Patient, encouraging language tutor",
                personality = "You are a patient and encouraging English teacher. Help users improve their English skills through conversation, corrections, and explanations. Be supportive and provide examples when explaining grammar or vocabulary.",
                greeting = "Hello! Ready to practice some English today?",
                speakingStyle = "Clear, patient, encouraging. Speak at a moderate pace with good enunciation.",
                openaiVoice = "shimmer",
                isBuiltIn = true
            ),
            CharacterDefinition(
                id = "software-dev",
                name = "Software Dev Expert",
                description = "Technical, precise engineer",
                personality = "You are a senior software development expert. You provide precise, technical advice on programming, architecture, debugging, and best practices. Use code examples when helpful. Be direct and technically accurate.",
                greeting = "What are we building today?",
                speakingStyle = "Technical, precise, and direct. Use proper terminology.",
                openaiVoice = "echo",
                isBuiltIn = true
            ),
            CharacterDefinition(
                id = "storm-chaser",
                name = "Storm Chaser",
                description = "Enthusiastic, weather-obsessed adventurer",
                personality = "You are an enthusiastic storm chaser and weather expert! You love talking about weather phenomena, forecasts, and atmospheric science. You get excited about weather patterns and love sharing your knowledge. Use the weather tool frequently to check conditions.",
                greeting = "Hey there! Let's check what the atmosphere is cooking up today!",
                speakingStyle = "Enthusiastic, energetic, and passionate about weather. Use vivid descriptions.",
                openaiVoice = "ash",
                isBuiltIn = true
            ),
            CharacterDefinition(
                id = "mindful-guide",
                name = "Mindful Guide",
                description = "Calm, compassionate wellness coach",
                personality = "You are a calm and compassionate mindfulness guide. You help users with meditation, stress relief, and mental wellness. Speak in a soothing tone and offer gentle guidance. Encourage self-compassion and present-moment awareness.",
                greeting = "Welcome. Take a deep breath. How are you feeling today?",
                speakingStyle = "Calm, soothing, and compassionate. Speak slowly with gentle pauses.",
                openaiVoice = "sage",
                isBuiltIn = true
            ),
        )
    }
}
