//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.providers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    val tool_calls: List<ToolCallData>? = null,
    val tool_call_id: String? = null,
    val name: String? = null
)

@Serializable
data class ToolCallData(
    val id: String = "",
    val type: String = "function",
    val function: ToolCallFunction = ToolCallFunction()
)

@Serializable
data class ToolCallFunction(
    val name: String = "",
    val arguments: String = ""
)

@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: ToolFunctionDef
)

@Serializable
data class ToolFunctionDef(
    val name: String,
    val description: String,
    val parameters: JsonObject = JsonObject(emptyMap())
)

data class ChatStreamDelta(
    val content: String? = null,
    val toolCalls: List<ToolCallDelta>? = null,
    val finishReason: String? = null,
    val usage: UsageData? = null
)

data class ToolCallDelta(
    val index: Int = 0,
    val id: String? = null,
    val function: ToolCallFunctionDelta? = null
)

data class ToolCallFunctionDelta(
    val name: String? = null,
    val arguments: String? = null
)

data class UsageData(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

class ChatClient(private val config: ProviderConfiguration) {
    companion object {
        private const val TAG = "ChatClient"
    }
    private val client = OpenAIServiceFactory.createClient(config)
    private val json = Json { ignoreUnknownKeys = true }

    private val isAnthropic: Boolean
        get() = config.provider == ProviderKind.ANTHROPIC

    fun streamChat(
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>? = null
    ): Flow<ChatStreamDelta> = flow {
        val url = OpenAIServiceFactory.chatCompletionUrl(config)
        android.util.Log.d(TAG, "[API] streamChat url=$url messages=${messages.size} tools=${tools?.size ?: 0} isAnthropic=$isAnthropic")

        val bodyMap = if (isAnthropic) buildAnthropicBody(messages, tools) else buildOpenAIBody(messages, tools)
        val bodyStr = bodyMap.toString()
        android.util.Log.d(TAG, "[API] request body length=${bodyStr.length}")

        val request = Request.Builder()
            .url(url)
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        android.util.Log.d(TAG, "[API] response code=${response.code} isSuccessful=${response.isSuccessful}")
        if (!response.isSuccessful) {
            val body = response.body?.string() ?: "HTTP ${response.code}"
            android.util.Log.e(TAG, "[API] error body: ${body.take(500)}")
            throw IOException("API error: $body")
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        try {
            val deltas = if (isAnthropic) parseAnthropicStream(reader) else parseOpenAIStream(reader)
            android.util.Log.d(TAG, "[API] parsed ${deltas.size} deltas")
            for (delta in deltas) {
                emit(delta)
            }
        } finally {
            reader.close()
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun parseOpenAIStream(reader: BufferedReader): List<ChatStreamDelta> {
                val results = mutableListOf<ChatStreamDelta>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: continue
                    if (!l.startsWith("data: ")) continue
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") break

                    try {
                        val obj = json.parseToJsonElement(data).jsonObject
                        val choices = obj["choices"]?.jsonArray
                        val choiceObj = choices?.firstOrNull()?.jsonObject
                        val deltaElement = choiceObj?.get("delta")
                        val delta = if (deltaElement is JsonObject) deltaElement else null
                        val finishReason = choiceObj?.get("finish_reason")?.let {
                            if (it is JsonNull) null else it.jsonPrimitive.contentOrNull
                        }
                        val usageElement = obj["usage"]
                        val usage = if (usageElement is JsonObject) usageElement else null

                        val contentDelta = delta?.get("content")?.let { if (it is JsonNull) null else it.jsonPrimitive.contentOrNull }
                        val toolCallsElement = delta?.get("tool_calls")
                        val toolCallDeltas = (toolCallsElement as? kotlinx.serialization.json.JsonArray)?.map { tc ->
                            val tcObj = tc.jsonObject
                            val fnElement = tcObj["function"]
                            val fn = if (fnElement is JsonObject) fnElement else null
                            ToolCallDelta(
                                index = tcObj["index"]?.jsonPrimitive?.int ?: 0,
                                id = tcObj["id"]?.jsonPrimitive?.contentOrNull,
                                function = fn?.let {
                                    ToolCallFunctionDelta(
                                        name = it["name"]?.jsonPrimitive?.contentOrNull,
                                        arguments = it["arguments"]?.jsonPrimitive?.contentOrNull
                                    )
                                }
                            )
                        }
                        val usageData = usage?.let {
                            UsageData(
                                promptTokens = it["prompt_tokens"]?.jsonPrimitive?.int ?: 0,
                                completionTokens = it["completion_tokens"]?.jsonPrimitive?.int ?: 0,
                                totalTokens = it["total_tokens"]?.jsonPrimitive?.int ?: 0
                            )
                        }

                        results.add(ChatStreamDelta(
                            content = contentDelta,
                            toolCalls = toolCallDeltas,
                            finishReason = finishReason,
                            usage = usageData
                        ))
                    } catch (e: Exception) {
                        android.util.Log.e("ChatClient", "OpenAI parse error: ${e.message} for data: ${data.take(100)}", e)
                    }
                }
                return results
    }

    private fun parseAnthropicStream(reader: BufferedReader): List<ChatStreamDelta> {
                val results = mutableListOf<ChatStreamDelta>()
                var currentToolIndex = 0
                var currentToolId = ""
                var currentToolName = ""
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: continue
                    if (!l.startsWith("data: ")) continue
                    val data = l.removePrefix("data: ").trim()

                    try {
                        val obj = json.parseToJsonElement(data).jsonObject
                        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: continue

                        when (type) {
                            "content_block_start" -> {
                                val block = obj["content_block"]?.jsonObject
                                val blockType = block?.get("type")?.jsonPrimitive?.contentOrNull
                                if (blockType == "tool_use") {
                                    currentToolId = block["id"]?.jsonPrimitive?.contentOrNull ?: ""
                                    currentToolName = block["name"]?.jsonPrimitive?.contentOrNull ?: ""
                                    currentToolIndex = obj["index"]?.jsonPrimitive?.int ?: 0
                                    results.add(ChatStreamDelta(toolCalls = listOf(
                                        ToolCallDelta(
                                            index = currentToolIndex,
                                            id = currentToolId,
                                            function = ToolCallFunctionDelta(name = currentToolName, arguments = "")
                                        )
                                    )))
                                }
                            }
                            "content_block_delta" -> {
                                val delta = obj["delta"]?.jsonObject
                                val deltaType = delta?.get("type")?.jsonPrimitive?.contentOrNull
                                when (deltaType) {
                                    "text_delta" -> {
                                        val text = delta["text"]?.jsonPrimitive?.contentOrNull
                                        if (text != null) results.add(ChatStreamDelta(content = text))
                                    }
                                    "input_json_delta" -> {
                                        val partialJson = delta["partial_json"]?.jsonPrimitive?.contentOrNull
                                        if (partialJson != null) {
                                            results.add(ChatStreamDelta(toolCalls = listOf(
                                                ToolCallDelta(
                                                    index = currentToolIndex,
                                                    function = ToolCallFunctionDelta(arguments = partialJson)
                                                )
                                            )))
                                        }
                                    }
                                }
                            }
                            "content_block_stop" -> { }
                            "message_delta" -> {
                                val delta = obj["delta"]?.jsonObject
                                val stopReason = delta?.get("stop_reason")?.jsonPrimitive?.contentOrNull
                                val usage = obj["usage"]?.jsonObject
                                val usageData = usage?.let {
                                    UsageData(completionTokens = it["output_tokens"]?.jsonPrimitive?.int ?: 0)
                                }
                                results.add(ChatStreamDelta(
                                    finishReason = when (stopReason) { "tool_use" -> "tool_calls"; "end_turn" -> "stop"; else -> stopReason },
                                    usage = usageData
                                ))
                            }
                            "message_start" -> {
                                val usage = obj["message"]?.jsonObject?.get("usage")?.jsonObject
                                if (usage != null) {
                                    results.add(ChatStreamDelta(usage = UsageData(
                                        promptTokens = usage["input_tokens"]?.jsonPrimitive?.int ?: 0
                                    )))
                                }
                            }
                            "message_stop" -> break
                        }
                    } catch (_: Exception) {}
                }
                return results
    }

    private fun buildOpenAIBody(messages: List<ChatMessage>, tools: List<ToolDefinition>?): JsonObject {
        return buildJsonObject {
            put("model", JsonPrimitive(config.modelID))
            putJsonArray("messages") {
                for (msg in messages) {
                    addJsonObject {
                        put("role", JsonPrimitive(msg.role))
                        if (msg.content != null) put("content", JsonPrimitive(msg.content))
                        if (msg.tool_call_id != null) put("tool_call_id", JsonPrimitive(msg.tool_call_id))
                        if (msg.name != null) put("name", JsonPrimitive(msg.name))
                        if (msg.tool_calls != null) {
                            putJsonArray("tool_calls") {
                                for (tc in msg.tool_calls) {
                                    addJsonObject {
                                        put("id", JsonPrimitive(tc.id))
                                        put("type", JsonPrimitive(tc.type))
                                        putJsonObject("function") {
                                            put("name", JsonPrimitive(tc.function.name))
                                            put("arguments", JsonPrimitive(tc.function.arguments))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            put("stream", JsonPrimitive(true))
            put("stream_options", buildJsonObject { put("include_usage", JsonPrimitive(true)) })
            if (tools != null && tools.isNotEmpty()) {
                putJsonArray("tools") {
                    for (tool in tools) {
                        addJsonObject {
                            put("type", JsonPrimitive("function"))
                            putJsonObject("function") {
                                put("name", JsonPrimitive(tool.function.name))
                                put("description", JsonPrimitive(tool.function.description))
                                put("parameters", tool.function.parameters)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun buildAnthropicBody(messages: List<ChatMessage>, tools: List<ToolDefinition>?): JsonObject {
        return buildJsonObject {
            put("model", JsonPrimitive(config.modelID))
            put("max_tokens", JsonPrimitive(4096))
            put("stream", JsonPrimitive(true))

            // Extract system message
            val systemMsg = messages.firstOrNull { it.role == "system" }
            if (systemMsg?.content != null) {
                put("system", JsonPrimitive(systemMsg.content))
            }

            // Convert messages — Anthropic doesn't support "system" role in messages array
            // and uses different format for tool results
            putJsonArray("messages") {
                for (msg in messages) {
                    if (msg.role == "system") continue // handled above

                    when {
                        // Tool result message → Anthropic format
                        msg.role == "tool" -> {
                            addJsonObject {
                                put("role", JsonPrimitive("user"))
                                putJsonArray("content") {
                                    addJsonObject {
                                        put("type", JsonPrimitive("tool_result"))
                                        put("tool_use_id", JsonPrimitive(msg.tool_call_id ?: ""))
                                        put("content", JsonPrimitive(msg.content ?: ""))
                                    }
                                }
                            }
                        }
                        // Assistant with tool calls → Anthropic format
                        msg.role == "assistant" && msg.tool_calls != null -> {
                            addJsonObject {
                                put("role", JsonPrimitive("assistant"))
                                putJsonArray("content") {
                                    if (msg.content != null && msg.content.isNotBlank()) {
                                        addJsonObject {
                                            put("type", JsonPrimitive("text"))
                                            put("text", JsonPrimitive(msg.content))
                                        }
                                    }
                                    for (tc in msg.tool_calls) {
                                        addJsonObject {
                                            put("type", JsonPrimitive("tool_use"))
                                            put("id", JsonPrimitive(tc.id))
                                            put("name", JsonPrimitive(tc.function.name))
                                            val inputJson = try {
                                                json.parseToJsonElement(tc.function.arguments)
                                            } catch (_: Exception) {
                                                JsonObject(emptyMap())
                                            }
                                            put("input", inputJson)
                                        }
                                    }
                                }
                            }
                        }
                        // Regular user/assistant message
                        else -> {
                            addJsonObject {
                                put("role", JsonPrimitive(msg.role))
                                put("content", JsonPrimitive(msg.content ?: ""))
                            }
                        }
                    }
                }
            }

            // Tools — Anthropic format
            if (tools != null && tools.isNotEmpty()) {
                putJsonArray("tools") {
                    for (tool in tools) {
                        addJsonObject {
                            put("name", JsonPrimitive(tool.function.name))
                            put("description", JsonPrimitive(tool.function.description))
                            put("input_schema", tool.function.parameters)
                        }
                    }
                }
            }
        }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = OpenAIServiceFactory.chatCompletionUrl(config)
            val bodyMap = if (isAnthropic) {
                buildJsonObject {
                    put("model", JsonPrimitive(config.modelID))
                    put("max_tokens", JsonPrimitive(5))
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive("Hi"))
                        }
                    }
                }
            } else {
                // OpenAI-flavor health probe. `max_tokens` is rejected by gpt-5 / o-series; use
                // `max_completion_tokens: 1`. Mirrors iOS `OpenRockyProviderHealthService` after
                // the 2026-04 settings polish commit.
                buildJsonObject {
                    put("model", JsonPrimitive(config.modelID))
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive("Hi"))
                        }
                    }
                    put("max_completion_tokens", JsonPrimitive(1))
                }
            }
            val request = Request.Builder()
                .url(url)
                .post(bodyMap.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Connected to ${config.provider.displayName} (${config.modelID})")
            } else {
                val body = response.body?.string() ?: "Unknown error"
                Result.failure(IOException("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
