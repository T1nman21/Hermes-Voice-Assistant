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
data class MemoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class MemoryService(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File get() = File(context.filesDir, "OpenRockyMemory/memory.json").also { it.parentFile?.mkdirs() }

    private val _entries = MutableStateFlow<List<MemoryEntry>>(emptyList())
    val entries: StateFlow<List<MemoryEntry>> = _entries.asStateFlow()

    init {
        load()
    }

    fun get(key: String): String? {
        return _entries.value.find { it.key.equals(key, ignoreCase = true) }?.value
    }

    fun set(key: String, value: String) {
        val normalizedKey = key.lowercase().trim()
        val list = _entries.value.toMutableList()
        val idx = list.indexOfFirst { it.key == normalizedKey }
        if (idx >= 0) {
            list[idx] = list[idx].copy(value = value, updatedAt = System.currentTimeMillis())
        } else {
            list.add(MemoryEntry(key = normalizedKey, value = value))
        }
        _entries.value = list
        save()
    }

    fun delete(key: String) {
        _entries.value = _entries.value.filter { !it.key.equals(key, ignoreCase = true) }
        save()
    }

    fun allEntries(): Map<String, String> {
        return _entries.value.associate { it.key to it.value }
    }

    private fun load() {
        try {
            if (file.exists()) {
                _entries.value = json.decodeFromString<List<MemoryEntry>>(file.readText())
            }
        } catch (_: Exception) {}
    }

    private fun save() {
        file.writeText(json.encodeToString(_entries.value))
    }
}
