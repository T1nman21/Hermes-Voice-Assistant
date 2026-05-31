//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-13
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

@Serializable
data class SoulDefinition(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val personality: String = "",
    val isBuiltIn: Boolean = false
)

class SoulStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dir: File get() = File(context.filesDir, "OpenRockySouls").also { it.mkdirs() }
    private val manifestFile: File get() = File(dir, "manifest.json")

    private val _souls = MutableStateFlow<List<SoulDefinition>>(emptyList())
    val souls: StateFlow<List<SoulDefinition>> = _souls.asStateFlow()

    private val _activeSoulID = MutableStateFlow<String?>(null)
    val activeSoulID: StateFlow<String?> = _activeSoulID.asStateFlow()

    val activeSoul: SoulDefinition
        get() = _souls.value.find { it.id == _activeSoulID.value } ?: builtInSouls.first()

    init {
        seedBuiltIns()
        load()
    }

    fun add(soul: SoulDefinition) {
        val file = File(dir, "${soul.id}.json")
        file.writeText(json.encodeToString(soul))
        val list = _souls.value.toMutableList()
        list.add(soul)
        _souls.value = list
        _activeSoulID.value = soul.id
        saveManifest()
    }

    fun update(soul: SoulDefinition) {
        val file = File(dir, "${soul.id}.json")
        file.writeText(json.encodeToString(soul))
        val list = _souls.value.toMutableList()
        val idx = list.indexOfFirst { it.id == soul.id }
        if (idx >= 0) list[idx] = soul
        _souls.value = list
        saveManifest()
    }

    fun activate(id: String) {
        _activeSoulID.value = id
        saveManifest()
    }

    fun delete(id: String) {
        val soul = _souls.value.find { it.id == id } ?: return
        if (soul.isBuiltIn) return
        File(dir, "$id.json").delete()
        _souls.value = _souls.value.filter { it.id != id }
        if (_activeSoulID.value == id) {
            _activeSoulID.value = builtInSouls.first().id
        }
        saveManifest()
    }

    fun resetBuiltIn(id: String) {
        val original = builtInSouls.find { it.id == id } ?: return
        update(original)
    }

    private fun seedBuiltIns() {
        for (soul in builtInSouls) {
            val file = File(dir, "${soul.id}.json")
            file.parentFile?.mkdirs()
            // Always re-seed built-in souls to pick up code changes
            file.writeText(json.encodeToString(soul))
        }
    }

    private fun load() {
        val list = mutableListOf<SoulDefinition>()
        dir.listFiles()?.filter { it.extension == "json" && it.name != "manifest.json" }?.forEach { file ->
            try {
                list.add(json.decodeFromString<SoulDefinition>(file.readText()))
            } catch (_: Exception) {}
        }
        _souls.value = list.sortedByDescending { it.isBuiltIn }

        try {
            val manifest = json.decodeFromString<SoulManifest>(manifestFile.readText())
            _activeSoulID.value = manifest.activeSoulID
        } catch (_: Exception) {
            _activeSoulID.value = builtInSouls.first().id
        }
    }

    private fun saveManifest() {
        val data = SoulManifest(activeSoulID = _activeSoulID.value)
        manifestFile.writeText(json.encodeToString(data))
    }

    @Serializable
    private data class SoulManifest(val activeSoulID: String? = null)

    companion object {
        val builtInSouls = listOf(
            SoulDefinition(
                id = "soul-default",
                name = "Default",
                description = "Balanced, helpful assistant that uses all available tools",
                personality = """You are a helpful, friendly AI assistant running on the user's device. You have access to a wide range of tools including location, weather, calendar, reminders, contacts, web search, file operations, code execution, and more.

Key behaviors:
- Be conversational but concise. Get to the point quickly.
- When a task requires tools, use them proactively without asking unnecessary confirmation.
- Explain what you're doing briefly, then do it.
- If a task is complex or multi-step, use delegate-task to run subtasks in parallel.
- Present results clearly with relevant details.
- Respect the user's time — avoid unnecessary preamble or filler.""",
                isBuiltIn = true
            ),
            SoulDefinition(
                id = "soul-concise",
                name = "Concise",
                description = "Minimal, execution-focused — acts first, explains briefly",
                personality = """You are an ultra-efficient assistant. Rules:
- Answer in 1-3 sentences maximum unless the user asks for detail.
- Execute tools immediately without narrating your thought process.
- Present only the final result, not intermediate steps.
- Skip greetings, pleasantries, and filler words.
- Use bullet points and short phrases over full sentences.
- If the answer is a single fact, give just the fact.""",
                isBuiltIn = true
            ),
            SoulDefinition(
                id = "soul-creative",
                name = "Creative",
                description = "Warm, expressive, uses analogies and metaphors",
                personality = """You are a warm, creative, and expressive assistant. Your style:
- Use vivid analogies and metaphors to explain concepts.
- Be enthusiastic and genuinely curious about the user's questions.
- Add personality and warmth to your responses.
- Use storytelling elements when explaining complex topics.
- Sprinkle in gentle humor when appropriate.
- Still be accurate and helpful — creativity enhances clarity, not replaces it.
- When using tools, weave the results into a narrative rather than just listing data.""",
                isBuiltIn = true
            ),
        )
    }
}
