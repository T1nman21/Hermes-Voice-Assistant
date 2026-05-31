//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.xnu.rocky.runtime.ConversationMessage
import com.xnu.rocky.ui.theme.OpenRockyPalette

@Composable
fun ChatScreen(
    messages: List<ConversationMessage>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (messages.isEmpty()) {
        EmptyStateView(modifier)
    } else {
        // Merge consecutive tool_call + tool_result into single items
        val displayItems = buildDisplayItems(messages)

        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .background(OpenRockyPalette.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayItems) { item ->
                when (item) {
                    is DisplayItem.Message -> MessageBubble(item.message)
                    is DisplayItem.ToolCall -> ToolCallCard(item.call, item.result)
                }
            }
        }
    }
}

private sealed class DisplayItem {
    data class Message(val message: ConversationMessage) : DisplayItem()
    data class ToolCall(val call: ConversationMessage, val result: ConversationMessage?) : DisplayItem()
}

private fun buildDisplayItems(messages: List<ConversationMessage>): List<DisplayItem> {
    val items = mutableListOf<DisplayItem>()
    var i = 0
    val filtered = messages.filter {
        it.role == "user" || it.role == "assistant" || it.role == "tool_call" || it.role == "tool_result"
    }
    while (i < filtered.size) {
        val msg = filtered[i]
        when (msg.role) {
            "tool_call" -> {
                // Merge with next tool_result if it matches
                val next = filtered.getOrNull(i + 1)
                if (next?.role == "tool_result" && next.toolName == msg.toolName) {
                    items.add(DisplayItem.ToolCall(msg, next))
                    i += 2
                } else {
                    items.add(DisplayItem.ToolCall(msg, null))
                    i++
                }
            }
            "tool_result" -> {
                // Orphan result (no preceding call) — show as merged with no call
                items.add(DisplayItem.ToolCall(msg, msg))
                i++
            }
            else -> {
                items.add(DisplayItem.Message(msg))
                i++
            }
        }
    }
    return items
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolCallCard(call: ConversationMessage, result: ConversationMessage?) {
    val hasResult = result != null
    var showSheet by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OpenRockyPalette.card,
        modifier = Modifier
            .fillMaxWidth(0.65f)
            .clickable { showSheet = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (hasResult) Icons.Default.CheckCircle else Icons.Default.Build,
                contentDescription = null,
                tint = if (hasResult) OpenRockyPalette.success else OpenRockyPalette.warning,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = call.toolName ?: "Tool",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = OpenRockyPalette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = OpenRockyPalette.mutedStatic,
                modifier = Modifier.size(14.dp)
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = OpenRockyPalette.card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (hasResult) Icons.Default.CheckCircle else Icons.Default.Build,
                        contentDescription = null,
                        tint = if (hasResult) OpenRockyPalette.success else OpenRockyPalette.warning,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = call.toolName ?: "Tool",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenRockyPalette.text
                    )
                    if (hasResult) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = OpenRockyPalette.success.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Succeeded",
                                fontSize = 11.sp,
                                color = OpenRockyPalette.success,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Parameters
                Text("Parameters", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.text)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OpenRockyPalette.cardElevated
                ) {
                    Text(
                        text = formatJson(call.content),
                        fontSize = 12.sp,
                        color = OpenRockyPalette.text.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Result
                if (result != null && result.content.isNotBlank()) {
                    Text("Result", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.text)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OpenRockyPalette.cardElevated
                    ) {
                        Text(
                            text = formatJson(result.content),
                            fontSize = 12.sp,
                            color = OpenRockyPalette.text.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatJson(text: String): String {
    return try {
        val element = kotlinx.serialization.json.Json.parseToJsonElement(text)
        kotlinx.serialization.json.Json { prettyPrint = true }.encodeToString(
            kotlinx.serialization.json.JsonElement.serializer(), element
        )
    } catch (_: Exception) {
        text
    }
}

@Composable
private fun MessageBubble(message: ConversationMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) OpenRockyPalette.accent.copy(alpha = 0.15f) else OpenRockyPalette.cardElevated,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "You" else "Rocky",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUser) OpenRockyPalette.accent else OpenRockyPalette.secondary
                )
                Spacer(Modifier.height(4.dp))

                if (isUser) {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = OpenRockyPalette.text,
                        lineHeight = 20.sp
                    )
                } else {
                    Markdown(
                        content = message.content,
                        colors = markdownColor(
                            text = OpenRockyPalette.text,
                            codeText = OpenRockyPalette.accent,
                            codeBackground = OpenRockyPalette.card,
                            dividerColor = OpenRockyPalette.separator,
                            linkText = OpenRockyPalette.accent,
                        ),
                        typography = markdownTypography(
                            h1 = MaterialTheme.typography.titleLarge.copy(color = OpenRockyPalette.text),
                            h2 = MaterialTheme.typography.titleMedium.copy(color = OpenRockyPalette.text),
                            h3 = MaterialTheme.typography.titleSmall.copy(color = OpenRockyPalette.text),
                            paragraph = MaterialTheme.typography.bodyMedium.copy(
                                color = OpenRockyPalette.text,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            ),
                            code = MaterialTheme.typography.bodySmall.copy(
                                color = OpenRockyPalette.accent,
                                fontSize = 13.sp
                            ),
                        ),
                    )
                }
            }
        }
    }
}
