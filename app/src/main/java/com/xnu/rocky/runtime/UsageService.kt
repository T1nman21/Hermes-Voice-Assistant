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
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class UsageRecord(
    val id: String = UUID.randomUUID().toString(),
    val provider: String,
    val model: String,
    val category: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class UsageService(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File get() = File(context.filesDir, "OpenRockyUsage/usage.json").also { it.parentFile?.mkdirs() }

    private val _records = MutableStateFlow<List<UsageRecord>>(emptyList())
    val records: StateFlow<List<UsageRecord>> = _records.asStateFlow()

    init {
        load()
        prune()
    }

    fun record(provider: String, model: String, category: String, promptTokens: Int, completionTokens: Int) {
        val record = UsageRecord(
            provider = provider,
            model = model,
            category = category,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = promptTokens + completionTokens
        )
        _records.value = _records.value + record
        save()
    }

    fun totalTokens(daysBack: Int = 30): Int {
        val cutoff = System.currentTimeMillis() - daysBack * 86400000L
        return _records.value.filter { it.timestamp >= cutoff }.sumOf { it.totalTokens }
    }

    fun totalRequests(daysBack: Int = 30): Int {
        val cutoff = System.currentTimeMillis() - daysBack * 86400000L
        return _records.value.count { it.timestamp >= cutoff }
    }

    fun dailyAverage(daysBack: Int = 30): Int {
        val total = totalTokens(daysBack)
        return if (daysBack > 0) total / daysBack else 0
    }

    data class DailySummary(val date: String, val promptTokens: Int, val completionTokens: Int)

    fun dailySummaries(daysBack: Int = 30): List<DailySummary> {
        val cutoff = System.currentTimeMillis() - daysBack * 86400000L
        val dateFormat = SimpleDateFormat("MM/dd", Locale.US)
        return _records.value
            .filter { it.timestamp >= cutoff }
            .groupBy { dateFormat.format(Date(it.timestamp)) }
            .map { (date, records) ->
                DailySummary(
                    date = date,
                    promptTokens = records.sumOf { it.promptTokens },
                    completionTokens = records.sumOf { it.completionTokens }
                )
            }
            .sortedBy { it.date }
    }

    data class ModelSummary(val model: String, val totalTokens: Int, val requestCount: Int)

    fun modelSummaries(daysBack: Int = 30): List<ModelSummary> {
        val cutoff = System.currentTimeMillis() - daysBack * 86400000L
        return _records.value
            .filter { it.timestamp >= cutoff }
            .groupBy { it.model }
            .map { (model, records) ->
                ModelSummary(
                    model = model,
                    totalTokens = records.sumOf { it.totalTokens },
                    requestCount = records.size
                )
            }
            .sortedByDescending { it.totalTokens }
    }

    fun clearAll() {
        _records.value = emptyList()
        save()
    }

    private fun prune() {
        val cutoff = System.currentTimeMillis() - 90 * 86400000L
        val pruned = _records.value.filter { it.timestamp >= cutoff }
        if (pruned.size != _records.value.size) {
            _records.value = pruned
            save()
        }
    }

    private fun load() {
        try {
            if (file.exists()) {
                _records.value = json.decodeFromString<List<UsageRecord>>(file.readText())
            }
        } catch (_: Exception) {}
    }

    private fun save() {
        file.writeText(json.encodeToString(_records.value))
    }
}
