//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-05-04
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.content.Context
import com.xnu.rocky.runtime.LogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persistent registry of user-configured Model Context Protocol servers.
 * Stored as one JSON file per server under `filesDir/OpenRockyMCPServers/`,
 * mirroring [com.xnu.rocky.runtime.skills.CustomSkillStore]'s shape so the
 * settings UI can stay consistent. Mirrors iOS `OpenRockyMCPStore` — the
 * iOS side uses UserDefaults (single blob) which works for that platform but
 * Android prefers per-file storage to keep IO predictable.
 */
class MCPStore(private val context: Context) {
    companion object {
        private const val TAG = "MCPStore"
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dir: File get() = File(context.filesDir, "OpenRockyMCPServers").also { it.mkdirs() }

    private val _servers = MutableStateFlow<List<MCPServer>>(emptyList())
    val servers: StateFlow<List<MCPServer>> = _servers.asStateFlow()

    init {
        load()
    }

    // MARK: - CRUD

    fun add(server: MCPServer) {
        LogManager.info("MCP server added: ${server.label}", TAG)
        save(server)
    }

    fun update(server: MCPServer) {
        save(server)
    }

    fun delete(id: String) {
        val label = _servers.value.firstOrNull { it.id == id }?.label ?: id
        LogManager.info("MCP server deleted: $label", TAG)
        File(dir, "$id.json").delete()
        _servers.value = _servers.value.filter { it.id != id }
    }

    fun toggle(id: String) {
        val server = _servers.value.firstOrNull { it.id == id } ?: return
        save(server.copy(isEnabled = !server.isEnabled))
    }

    fun server(id: String): MCPServer? = _servers.value.firstOrNull { it.id == id }

    fun serverForSanitizedLabel(label: String): MCPServer? =
        _servers.value.firstOrNull { MCPServer.sanitizedLabel(it.label) == label }

    /**
     * Update the cached tool catalog after a successful `tools/list`. Called
     * from the refresh action in the settings UI.
     */
    fun updateCachedTools(serverID: String, tools: List<MCPServer.CachedTool>) {
        val server = _servers.value.firstOrNull { it.id == serverID } ?: return
        save(server.copy(cachedTools = tools, lastRefreshedAt = System.currentTimeMillis()))
    }

    // MARK: - Tool lookup

    /**
     * Resolves a namespaced tool name back to its server. Returns null for
     * non-MCP tools or unknown servers (the toolbox's default branch will
     * then bubble up an error so the caller sees a real failure rather than
     * the call hanging). Mirrors iOS `resolveTool`.
     */
    fun resolveTool(namespacedName: String): Pair<MCPServer, String>? {
        val parsed = MCPServer.parseNamespacedToolName(namespacedName) ?: return null
        val (sanitizedLabel, toolName) = parsed
        val server = serverForSanitizedLabel(sanitizedLabel) ?: return null
        // The cached tool name might have been stored verbatim or with `_`
        // substitutions from sanitization; accept either form, then map back
        // to the real (un-scrubbed) name so the JSON-RPC call carries the
        // canonical identifier the server expects.
        val match = server.cachedTools.firstOrNull { tool ->
            tool.name == toolName ||
                MCPServer.sanitizedToolName(server.label, tool.name).endsWith("-$toolName")
        } ?: return null
        return server to match.name
    }

    // MARK: - Persistence

    private fun save(server: MCPServer) {
        val file = File(dir, "${server.id}.json")
        file.writeText(json.encodeToString(server))
        val list = _servers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == server.id }
        if (idx >= 0) list[idx] = server else list.add(server)
        _servers.value = list
    }

    private fun load() {
        val files = dir.listFiles { f -> f.extension == "json" } ?: return
        val loaded = files.mapNotNull { f ->
            try {
                json.decodeFromString<MCPServer>(f.readText())
            } catch (e: Exception) {
                LogManager.error("MCP store decode failed for ${f.name}: ${e.message}", TAG)
                null
            }
        }
        _servers.value = loaded
    }
}
