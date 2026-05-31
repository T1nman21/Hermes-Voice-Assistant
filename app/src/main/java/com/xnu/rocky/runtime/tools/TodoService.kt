//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

class TodoService(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File get() = File(context.filesDir, "OpenRockyTodo/todos.json").also { it.parentFile?.mkdirs() }
    private var items: MutableList<TodoItem> = mutableListOf()

    init { load() }

    fun add(title: String): String {
        val item = TodoItem(title = title)
        items.add(item)
        save()
        return "Added: ${item.title} (id: ${item.id.take(8)})"
    }

    fun list(): String {
        if (items.isEmpty()) return "No todos."
        val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.US)
        return items.joinToString("\n") { item ->
            val status = if (item.isCompleted) "✓" else "○"
            val date = dateFormat.format(Date(item.createdAt))
            "$status ${item.title} (${item.id.take(8)}, $date)"
        }
    }

    fun complete(id: String): String {
        val idx = items.indexOfFirst { it.id.startsWith(id) }
        if (idx < 0) return "Todo not found: $id"
        items[idx] = items[idx].copy(isCompleted = true, completedAt = System.currentTimeMillis())
        save()
        return "Completed: ${items[idx].title}"
    }

    fun delete(id: String): String {
        val item = items.find { it.id.startsWith(id) } ?: return "Todo not found: $id"
        items.remove(item)
        save()
        return "Deleted: ${item.title}"
    }

    private fun load() {
        try {
            if (file.exists()) {
                items = json.decodeFromString<MutableList<TodoItem>>(file.readText())
            }
        } catch (_: Exception) {}
    }

    private fun save() {
        file.writeText(json.encodeToString(items.toList()))
    }
}
