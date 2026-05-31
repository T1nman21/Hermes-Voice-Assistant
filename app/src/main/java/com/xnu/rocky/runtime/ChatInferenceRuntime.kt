//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime

import com.xnu.rocky.providers.*
import com.xnu.rocky.runtime.tools.Toolbox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ChatInferenceRuntime(
    private val toolbox: Toolbox
) {
    companion object {
        private const val TAG = "ChatInference"
    }

    suspend fun runInference(
        config: ProviderConfiguration,
        messages: MutableList<ChatMessage>,
        tools: List<ToolDefinition>,
        onDelta: (String) -> Unit,
        onToolCall: (String, String) -> Unit,
        onToolResult: (String, String) -> Unit,
        onUsage: (UsageData) -> Unit
    ): String {
        val client = ChatClient(config)
        var fullResponse = ""
        var maxIterations = 10
        LogManager.info("[INFER] runInference start, messages=${messages.size} tools=${tools.size} provider=${config.provider.displayName}", TAG)

        while (maxIterations > 0) {
            maxIterations--
            val accumulatedToolCalls = mutableMapOf<Int, ToolCallData>()
            var hasToolCalls = false
            var contentBuffer = StringBuilder()
            var streamDeltaCount = 0

            LogManager.info("[INFER] iteration ${10 - maxIterations}, calling streamChat...", TAG)

            client.streamChat(messages, tools).collect { delta ->
                streamDeltaCount++
                delta.content?.let { c ->
                    contentBuffer.append(c)
                    onDelta(c)
                }
                delta.toolCalls?.forEach { tc ->
                    hasToolCalls = true
                    val existing = accumulatedToolCalls.getOrPut(tc.index) {
                        ToolCallData(id = tc.id ?: "", function = ToolCallFunction())
                    }
                    val updatedFn = existing.function.copy(
                        name = tc.function?.name ?: existing.function.name,
                        arguments = existing.function.arguments + (tc.function?.arguments ?: "")
                    )
                    accumulatedToolCalls[tc.index] = existing.copy(
                        id = tc.id ?: existing.id,
                        function = updatedFn
                    )
                }
                delta.usage?.let { onUsage(it) }
            }

            LogManager.info("[INFER] stream done, deltaCount=$streamDeltaCount contentLen=${contentBuffer.length} hasToolCalls=$hasToolCalls", TAG)

            if (hasToolCalls) {
                val toolCallsList = accumulatedToolCalls.values.toList()
                LogManager.info("[INFER] toolCalls: ${toolCallsList.map { it.function.name }}", TAG)
                messages.add(ChatMessage(
                    role = "assistant",
                    content = contentBuffer.toString().ifBlank { null },
                    tool_calls = toolCallsList
                ))

                for (toolCall in toolCallsList) {
                    onToolCall(toolCall.function.name, toolCall.function.arguments)
                    val result = toolbox.execute(toolCall.function.name, toolCall.function.arguments)
                    onToolResult(toolCall.function.name, result)
                    messages.add(ChatMessage(
                        role = "tool",
                        content = result,
                        tool_call_id = toolCall.id,
                        name = toolCall.function.name
                    ))
                }
                fullResponse = contentBuffer.toString()
            } else {
                fullResponse = contentBuffer.toString()
                LogManager.info("[INFER] final response len=${fullResponse.length} content='${fullResponse.take(100)}'", TAG)
                messages.add(ChatMessage(role = "assistant", content = fullResponse))
                break
            }
        }

        if (maxIterations == 0) {
            LogManager.warning("[INFER] hit max iterations (10), returning partial response", TAG)
        }

        return fullResponse
    }

    /**
     * Auto-compact conversation history by summarizing older messages when the list grows beyond
     * [threshold]. Keeps the most recent messages verbatim and replaces the older portion with a
     * single system message containing a compact summary. Mirrors iOS auto-compression.
     */
    suspend fun compactHistoryIfNeeded(
        history: MutableList<ChatMessage>,
        config: ProviderConfiguration,
        threshold: Int = 60
    ) {
        val nonTool = history.filter { it.role != "tool" }
        if (nonTool.size <= threshold) return

        val recentCount = (threshold / 2).coerceAtLeast(10)
        val toKeep = history.takeLast(recentCount)
        val toCompact = history.dropLast(recentCount)
        if (toCompact.size < 5) return

        LogManager.info("[INFER] auto-compacting ${toCompact.size} messages (keeping $recentCount recent)", TAG)

        val conversationText = toCompact.mapNotNull { msg ->
            val content = msg.content?.take(500) ?: return@mapNotNull null
            "<message role=\"${msg.role}\">$content</message>"
        }.joinToString("\n")

        val summaryPrompt = """
            Summarize this conversation history concisely. Preserve key facts, user preferences,
            decisions, tool results, and context needed to continue naturally. Output only the summary.

            <conversation>
            $conversationText
            </conversation>
        """.trimIndent()

        val summaryMessages = listOf(
            ChatMessage(role = "system", content = "You are a conversation summarizer. Output a concise summary."),
            ChatMessage(role = "user", content = summaryPrompt)
        )

        val client = ChatClient(config)
        val buffer = StringBuilder()
        runCatching {
            client.streamChat(summaryMessages).collect { delta -> delta.content?.let { buffer.append(it) } }
        }.onFailure {
            LogManager.warning("[INFER] auto-compaction failed: ${it.message}", TAG)
            return
        }
        val summary = buffer.toString().trim()
        if (summary.isEmpty()) {
            LogManager.warning("[INFER] auto-compaction produced empty summary", TAG)
            return
        }

        history.clear()
        history.add(ChatMessage(role = "system", content = "[Previous conversation summary]\n$summary"))
        history.addAll(toKeep)
        LogManager.info("[INFER] auto-compacted to ${history.size} messages (summary: ${summary.length} chars)", TAG)
    }
}
