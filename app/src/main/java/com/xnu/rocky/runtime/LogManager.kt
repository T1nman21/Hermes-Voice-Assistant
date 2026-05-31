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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

enum class LogLevel { DEBUG, INFO, WARNING, ERROR }

@Serializable
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val message: String,
    val source: String = ""
)

object LogManager {
    private val buffer = ConcurrentLinkedQueue<LogEntry>()
    private var logDir: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH", Locale.US)
    private val json = Json { ignoreUnknownKeys = true }
    private val timer = Timer()

    fun init(context: Context) {
        logDir = File(context.filesDir, "OpenRockyLogs").also { it.mkdirs() }
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { flush() }
        }, 30000, 30000)
        pruneOldLogs()
    }

    fun log(level: LogLevel, message: String, source: String = "") {
        val entry = LogEntry(level = level.name, message = message, source = source)
        buffer.add(entry)
        if (buffer.size >= 50) flush()
    }

    fun debug(message: String, source: String = "") = log(LogLevel.DEBUG, message, source)
    fun info(message: String, source: String = "") = log(LogLevel.INFO, message, source)
    fun warning(message: String, source: String = "") = log(LogLevel.WARNING, message, source)
    fun error(message: String, source: String = "") = log(LogLevel.ERROR, message, source)

    fun flush() {
        val dir = logDir ?: return
        if (buffer.isEmpty()) return
        val entries = mutableListOf<LogEntry>()
        while (buffer.isNotEmpty()) {
            buffer.poll()?.let { entries.add(it) }
        }
        val filename = "log_${dateFormat.format(Date())}.jsonl"
        val file = File(dir, filename)
        val text = entries.joinToString("\n") { json.encodeToString(it) }
        file.appendText(text + "\n")
    }

    fun listLogFiles(): List<File> {
        val dir = logDir ?: return emptyList()
        return dir.listFiles()?.filter { it.extension == "jsonl" }?.sortedByDescending { it.name } ?: emptyList()
    }

    fun readLogFile(file: File): List<LogEntry> {
        return try {
            file.readLines().filter { it.isNotBlank() }.mapNotNull {
                try { json.decodeFromString<LogEntry>(it) } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun clearAll() {
        logDir?.listFiles()?.forEach { it.delete() }
    }

    private fun pruneOldLogs() {
        val dir = logDir ?: return
        val cutoff = System.currentTimeMillis() - 7 * 86400000L
        dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }
}
