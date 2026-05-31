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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.runtime.ConversationMetadata
import com.xnu.rocky.ui.theme.OpenRockyPalette
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListView(
    conversations: List<ConversationMetadata>,
    activeConversationId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OpenRockyPalette.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Conversations",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpenRockyPalette.text
                )
                TextButton(onClick = onNewConversation) {
                    Icon(Icons.Default.Add, null, tint = OpenRockyPalette.accent)
                    Spacer(Modifier.width(4.dp))
                    Text("New", color = OpenRockyPalette.accent)
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    val isActive = conv.id == activeConversationId
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    onDelete(conv.id)
                                    true
                                } else false
                            }
                        ),
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(OpenRockyPalette.error)
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, null, tint = OpenRockyPalette.text)
                            }
                        },
                        content = {
                            ListItem(
                                headlineContent = {
                                    Text(conv.title, color = OpenRockyPalette.text, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal)
                                },
                                supportingContent = {
                                    Text(dateFormat.format(Date(conv.updatedAt)), color = OpenRockyPalette.muted, fontSize = 12.sp)
                                },
                                leadingContent = {
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(OpenRockyPalette.accent)
                                        )
                                    }
                                },
                                modifier = Modifier.clickable { onSelect(conv.id) },
                                colors = ListItemDefaults.colors(containerColor = OpenRockyPalette.card)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
