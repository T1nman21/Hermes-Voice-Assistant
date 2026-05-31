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
data class ConversationMetadata(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Conversation",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ConversationMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCallId: String? = null,
    val toolName: String? = null,
    val toolCalls: String? = null
)

class PersistentStorageProvider(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dir: File get() = File(context.filesDir, "OpenRockyConversations").also { it.mkdirs() }
    private val indexFile: File get() = File(dir, "index.json")

    private val _conversations = MutableStateFlow<List<ConversationMetadata>>(emptyList())
    val conversations: StateFlow<List<ConversationMetadata>> = _conversations.asStateFlow()

    private val messageCache = mutableMapOf<String, MutableList<ConversationMessage>>()

    init {
        loadIndex()
    }

    fun createConversation(title: String = "New Conversation"): String {
        val meta = ConversationMetadata(title = title)
        val convDir = File(dir, meta.id).also { it.mkdirs() }
        File(convDir, "messages.jsonl").createNewFile()
        _conversations.value = listOf(meta) + _conversations.value
        saveIndex()
        return meta.id
    }

    fun deleteConversation(id: String) {
        File(dir, id).deleteRecursively()
        messageCache.remove(id)
        _conversations.value = _conversations.value.filter { it.id != id }
        saveIndex()
    }

    fun updateTitle(id: String, title: String) {
        _conversations.value = _conversations.value.map {
            if (it.id == id) it.copy(title = title, updatedAt = System.currentTimeMillis()) else it
        }
        saveIndex()
    }

    fun appendMessage(conversationId: String, message: ConversationMessage) {
        val file = File(dir, "$conversationId/messages.jsonl")
        file.parentFile?.mkdirs()
        file.appendText(json.encodeToString(message) + "\n")

        val cached = messageCache.getOrPut(conversationId) { mutableListOf() }
        cached.add(message)

        _conversations.value = _conversations.value.map {
            if (it.id == conversationId) it.copy(updatedAt = System.currentTimeMillis()) else it
        }
        saveIndex()
    }

    fun loadMessages(conversationId: String): List<ConversationMessage> {
        messageCache[conversationId]?.let { return it.toList() }

        val file = File(dir, "$conversationId/messages.jsonl")
        if (!file.exists()) return emptyList()

        val messages = file.readLines().filter { it.isNotBlank() }.mapNotNull {
            try { json.decodeFromString<ConversationMessage>(it) } catch (_: Exception) { null }
        }.toMutableList()

        messageCache[conversationId] = messages
        return messages.toList()
    }

    fun clearConversation(conversationId: String) {
        val file = File(dir, "$conversationId/messages.jsonl")
        file.writeText("")
        messageCache.remove(conversationId)
    }

    private fun loadIndex() {
        try {
            if (indexFile.exists()) {
                _conversations.value = json.decodeFromString<List<ConversationMetadata>>(indexFile.readText())
            }
        } catch (_: Exception) {}
    }

    private fun saveIndex() {
        indexFile.writeText(json.encodeToString(_conversations.value))
    }
}
