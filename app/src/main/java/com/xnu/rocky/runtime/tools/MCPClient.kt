//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-05-04
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import com.xnu.rocky.runtime.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Minimal MCP client implementing the Streamable-HTTP transport (the modern
 * transport — stdio is moot on Android, and the legacy SSE GET path was
 * deprecated in 2025-06-18). Each [MCPClient] is short-lived: it spans the
 * initialize handshake plus one tools/list or tools/call. We re-init per call
 * to stay simple — MCP servers are typically stateless across the
 * initialize→call boundary, and any session token returned by the server is
 * carried through within the one client. Mirrors iOS `OpenRockyMCPClient`.
 */
class MCPClient(private val server: MCPServer) {
    companion object {
        private const val TAG = "MCPClient"

        /**
         * Mirrors the MCP protocol revision we target. Servers that advertise
         * an older version on initialize will still typically work for
         * tools/list and tools/call — the spec keeps these stable.
         */
        const val PROTOCOL_VERSION = "2025-06-18"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private var sessionID: String? = null
    private var nextRequestID: Int = 1

    // MARK: - Public API

    /**
     * Run the initialize → tools/list dance and return the advertised tool
     * catalog as the cache-shape struct the rest of the runtime understands.
     */
    suspend fun listTools(): List<MCPServer.CachedTool> = withContext(Dispatchers.IO) {
        initializeIfNeeded()
        val result = send(method = "tools/list", params = null)
        val tools = result?.jsonObject?.get("tools")?.jsonArray ?: JsonArray(emptyList())
        tools.map { entry ->
            val obj = entry.jsonObject
            MCPServer.CachedTool(
                name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "",
                description = obj["description"]?.jsonPrimitive?.contentOrNull ?: "",
                inputSchemaJSON = obj["inputSchema"]?.toString() ?: "{}"
            )
        }.filter { it.name.isNotEmpty() }
    }

    /**
     * Invoke a tool by name, with model-supplied JSON arguments. Returns the
     * concatenated text content from the MCP response, or "{}" if no text was
     * provided. Server-reported errors (HTTP, JSON-RPC, or tool-side isError)
     * are thrown unchanged so the subagent loop records `succeeded=false` and
     * the user sees the ✗ marker.
     */
    suspend fun callTool(name: String, argumentsJSON: String): String = withContext(Dispatchers.IO) {
        initializeIfNeeded()
        val argumentsValue: JsonElement = try {
            json.parseToJsonElement(argumentsJSON)
        } catch (_: Exception) {
            JsonObject(emptyMap())
        }
        val params = buildJsonObject {
            put("name", JsonPrimitive(name))
            put("arguments", argumentsValue)
        }
        val result = send(method = "tools/call", params = params)
            ?: throw MCPClientException("Empty result from tools/call")
        val obj = result.jsonObject
        val isError = obj["isError"]?.jsonPrimitive?.booleanOrNull ?: false
        val text = flattenText(obj["content"])
        if (isError) {
            // Surface the model-visible failure as a thrown error so the
            // subagent loop records succeeded=false.
            throw MCPClientException(if (text.isEmpty()) "MCP tool reported an error." else text)
        }
        if (text.isEmpty()) "{}" else text
    }

    // MARK: - Initialize

    private suspend fun initializeIfNeeded() {
        if (sessionID != null) return
        val params = buildJsonObject {
            put("protocolVersion", JsonPrimitive(PROTOCOL_VERSION))
            put("capabilities", JsonObject(emptyMap()))
            putJsonObject("clientInfo") {
                put("name", JsonPrimitive("OpenRocky"))
                put("version", JsonPrimitive("1.0"))
            }
        }
        // We don't inspect the result — initialize returns free-form server
        // capabilities we don't model. We only need the call to succeed and
        // (optionally) populate sessionID via the response header.
        send(method = "initialize", params = params)
    }

    // MARK: - Transport

    /**
     * Encode + POST a JSON-RPC request, decode the matching response. Handles
     * both `application/json` and `text/event-stream` response bodies (the
     * two encodings the Streamable-HTTP transport supports).
     */
    private fun send(method: String, params: JsonElement?): JsonElement? {
        val id = nextRequestID++
        val envelope = buildJsonObject {
            put("jsonrpc", JsonPrimitive("2.0"))
            put("id", JsonPrimitive(id))
            put("method", JsonPrimitive(method))
            if (params != null) put("params", params)
        }
        val builder = Request.Builder()
            .url(server.endpointURL)
            .post(envelope.toString().toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .header("User-Agent", "OpenRocky/1.0")
        server.bearerToken?.takeIf { it.isNotEmpty() }?.let {
            builder.header("Authorization", "Bearer $it")
        }
        for ((k, v) in server.extraHeaders) builder.header(k, v)
        sessionID?.let { builder.header("Mcp-Session-Id", it) }

        httpClient.newCall(builder.build()).execute().use { response ->
            // Capture session id if the server set one on this round-trip — we
            // need to echo it on subsequent calls within the same client.
            response.header("Mcp-Session-Id")?.takeIf { it.isNotEmpty() }?.let { sessionID = it }

            val bodyText = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw MCPClientException(
                    "MCP HTTP ${response.code}: ${bodyText.take(400)}"
                )
            }
            // 202 Accepted with no body is the spec'd response for plain
            // notifications — no result expected. Not used in our flows
            // (initialize / tools/list / tools/call all return a result).
            if (response.code == 202) return null

            val contentType = (response.header("Content-Type") ?: "").lowercase()
            val payload = if (contentType.contains("text/event-stream")) {
                extractFirstSSEData(bodyText, expectingID = id)
            } else {
                bodyText
            }
            val parsed = json.parseToJsonElement(payload).jsonObject
            parsed["error"]?.jsonObject?.let { err ->
                val message = err["message"]?.jsonPrimitive?.contentOrNull ?: "MCP server error"
                throw MCPClientException("MCP server error: $message")
            }
            return parsed["result"]
        }
    }

    /**
     * Streamable-HTTP can return SSE: a stream of `event: …` / `data: …`
     * blocks separated by blank lines. The first `data:` payload that matches
     * our request id is the JSON-RPC response — we don't need any later
     * events for non-streaming methods.
     */
    private fun extractFirstSSEData(body: String, expectingID: Int): String {
        for (line in body.lineSequence()) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("data:")) continue
            val payload = trimmed.removePrefix("data:").trim()
            if (payload.isEmpty() || payload == "[DONE]") continue
            // Verify id matches before returning so we don't pick up an
            // unrelated progress / ping event.
            try {
                val parsed = json.parseToJsonElement(payload).jsonObject
                val id = parsed["id"]?.jsonPrimitive?.intOrNull
                if (id == expectingID || parsed["error"] != null) {
                    return payload
                }
            } catch (_: Exception) {}
        }
        throw MCPClientException("No matching JSON-RPC data event in SSE stream")
    }

    private fun flattenText(content: JsonElement?): String {
        // MCP responses are an array of typed content blocks. For our use the
        // chat model only consumes text, so we concatenate the text blocks.
        // Non-text blocks (images, embedded resources) are dropped — the chat
        // model wouldn't know what to do with them at this layer anyway.
        val arr = (content as? JsonArray) ?: return ""
        return arr.mapNotNull { block ->
            val obj = block.jsonObject
            val type = obj["type"]?.jsonPrimitive?.contentOrNull
            if (type == "text") obj["text"]?.jsonPrimitive?.contentOrNull else null
        }.joinToString("\n")
    }
}

class MCPClientException(message: String) : Exception(message)
