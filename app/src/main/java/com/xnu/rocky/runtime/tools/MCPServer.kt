//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-05-04
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * User-configured Model Context Protocol server. Persisted via [MCPStore].
 * Tools advertised by the server are surfaced through the chat toolbox under
 * the `mcp-{label}-{tool}` namespace, so any chat provider (not just OpenAI)
 * can call them. Mirrors iOS `OpenRockyMCPServer`.
 */
@Serializable
data class MCPServer(
    val id: String = UUID.randomUUID().toString(),
    /**
     * Stable short label used as a namespace prefix in tool names. Lower-case,
     * alphanumeric + dashes. Disambiguates tools when two servers expose tools
     * with overlapping names.
     */
    val label: String,
    /** Streamable-HTTP endpoint (modern MCP transport). Stdio is not applicable on Android. */
    val endpointURL: String,
    /** Optional bearer token sent as `Authorization: Bearer <token>`. */
    val bearerToken: String? = null,
    /** Extra headers (e.g. multi-tenant identifiers, vendor-specific keys). */
    val extraHeaders: Map<String, String> = emptyMap(),
    val isEnabled: Boolean = true,
    /**
     * Optional allowlist of tool names. When empty the full advertised set is
     * exposed; when non-empty only matching names pass through.
     */
    val allowedToolNames: List<String> = emptyList(),
    /**
     * Cached tool catalog from the most recent successful `tools/list`. Lets
     * the chat sub-agent build its tool definitions cold without paying the
     * network round-trip every turn.
     */
    val cachedTools: List<CachedTool> = emptyList(),
    /** Time of the last successful `tools/list` refresh, for the settings UI. */
    val lastRefreshedAt: Long? = null
) {
    /**
     * One advertised tool, captured in the form we need to round-trip through
     * the chat-tool catalog. The schema is held as raw JSON because MCP servers
     * can advertise inputSchemas that our local schema model doesn't cover —
     * we pass them through verbatim and rely on the chat provider to translate.
     */
    @Serializable
    data class CachedTool(
        val name: String,
        val description: String,
        /**
         * JSON-encoded input schema as advertised by the MCP server. Stored as
         * String so we keep the raw shape (additionalProperties, oneOf, etc.)
         * without modeling every JSON Schema feature locally.
         */
        val inputSchemaJSON: String
    )

    companion object {
        /**
         * Tool name as exposed to the chat model: `mcp-{label}-{tool}`. The
         * underlying server label is sanitized so the resulting identifier
         * matches OpenAI's allowed-character rules (a-z, A-Z, 0-9, _, -, ≤64).
         */
        fun sanitizedLabel(raw: String): String {
            val lower = raw.lowercase()
            val scrubbed = lower.map { c ->
                if (c in 'a'..'z' || c in '0'..'9' || c == '-') c else '-'
            }.joinToString("")
            var result = scrubbed
            while (result.contains("--")) {
                result = result.replace("--", "-")
            }
            result = result.trim('-')
            return if (result.isEmpty()) "server" else result.take(20)
        }

        fun sanitizedToolName(serverLabel: String, toolName: String): String {
            val safeLabel = sanitizedLabel(serverLabel)
            val safeTool = toolName.map { c ->
                if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_' || c == '-') c else '_'
            }.joinToString("")
            val combined = "mcp-$safeLabel-$safeTool"
            // OpenAI's hard limit on tool names is 64 chars; truncate from the
            // tool side so the namespace prefix stays readable.
            return combined.take(64)
        }

        /**
         * Reverse of [sanitizedToolName]. Returns `(sanitizedLabel, toolName)`
         * when the name has the `mcp-` prefix, else null. The caller still has
         * to look up the server by label; sanitized labels can collide on edge
         * inputs.
         */
        fun parseNamespacedToolName(name: String): Pair<String, String>? {
            if (!name.startsWith("mcp-")) return null
            val body = name.removePrefix("mcp-")
            val dashIdx = body.indexOf('-')
            if (dashIdx <= 0) return null
            val label = body.substring(0, dashIdx)
            val tool = body.substring(dashIdx + 1)
            if (label.isEmpty() || tool.isEmpty()) return null
            return label to tool
        }
    }
}
