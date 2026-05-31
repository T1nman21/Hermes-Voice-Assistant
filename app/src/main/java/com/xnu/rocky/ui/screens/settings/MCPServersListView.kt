//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-05-04
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.runtime.tools.MCPServer
import com.xnu.rocky.ui.theme.OpenRockyPalette
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MCPServersListView(
    servers: List<MCPServer>,
    onToggle: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MCP Servers", color = OpenRockyPalette.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text)
                    }
                },
                actions = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, "Add", tint = OpenRockyPalette.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    "MCP servers expose tools to the chat sub-agent (called via delegate-task). Voice doesn't see them directly.",
                    fontSize = 12.sp,
                    color = OpenRockyPalette.muted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onAdd() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.cardElevated)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Add MCP Server",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = OpenRockyPalette.text
                        )
                    }
                }
            }

            items(servers, key = { it.id }) { server ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lan,
                            null,
                            tint = OpenRockyPalette.accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f).clickable { onEdit(server.id) }) {
                            Text(
                                server.label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OpenRockyPalette.text
                            )
                            val toolsLine = if (server.cachedTools.isEmpty()) {
                                "No tools cached — tap to refresh"
                            } else {
                                "${server.cachedTools.size} tool${if (server.cachedTools.size == 1) "" else "s"}"
                            }
                            val refreshLine = server.lastRefreshedAt?.let { ts ->
                                " · refreshed ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ts))}"
                            } ?: ""
                            Text(
                                toolsLine + refreshLine,
                                fontSize = 12.sp,
                                color = OpenRockyPalette.muted
                            )
                        }
                        Switch(
                            checked = server.isEnabled,
                            onCheckedChange = { onToggle(server.id) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = OpenRockyPalette.accent,
                                uncheckedTrackColor = OpenRockyPalette.cardElevated
                            )
                        )
                    }
                }
            }

            if (servers.isNotEmpty()) {
                item {
                    Text(
                        "${servers.count { it.isEnabled }} of ${servers.size} enabled",
                        fontSize = 12.sp,
                        color = OpenRockyPalette.muted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
