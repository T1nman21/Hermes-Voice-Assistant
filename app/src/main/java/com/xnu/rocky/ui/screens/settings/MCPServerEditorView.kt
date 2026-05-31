//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-05-04
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.runtime.tools.MCPClient
import com.xnu.rocky.runtime.tools.MCPServer
import com.xnu.rocky.ui.theme.OpenRockyPalette
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MCPServerEditorView(
    existing: MCPServer?,
    onSave: (MCPServer) -> Unit,
    onCacheTools: (String, List<MCPServer.CachedTool>) -> Unit,
    onBack: () -> Unit
) {
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var endpointURL by remember { mutableStateOf(existing?.endpointURL ?: "") }
    var bearerToken by remember { mutableStateOf(existing?.bearerToken ?: "") }
    var isEnabled by remember { mutableStateOf(existing?.isEnabled ?: true) }
    var cachedTools by remember { mutableStateOf(existing?.cachedTools ?: emptyList()) }
    var lastRefreshedAt by remember { mutableStateOf(existing?.lastRefreshedAt) }
    var refreshing by remember { mutableStateOf(false) }
    var refreshError by remember { mutableStateOf<String?>(null) }
    var bearerVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existing == null) "New MCP Server" else "Edit MCP Server",
                        color = OpenRockyPalette.text
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text)
                    }
                },
                actions = {
                    val canSave = label.isNotBlank() && endpointURL.isNotBlank()
                    TextButton(
                        onClick = {
                            val saved = (existing ?: MCPServer(
                                label = label.trim(),
                                endpointURL = endpointURL.trim()
                            )).copy(
                                label = label.trim(),
                                endpointURL = endpointURL.trim(),
                                bearerToken = bearerToken.trim().ifEmpty { null },
                                isEnabled = isEnabled,
                                cachedTools = cachedTools,
                                lastRefreshedAt = lastRefreshedAt
                            )
                            onSave(saved)
                            onBack()
                        },
                        enabled = canSave
                    ) {
                        Text("Save", color = if (canSave) OpenRockyPalette.accent else OpenRockyPalette.muted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = OpenRockyPalette.text,
                                unfocusedTextColor = OpenRockyPalette.text,
                                focusedBorderColor = OpenRockyPalette.accent,
                                unfocusedBorderColor = OpenRockyPalette.muted,
                                focusedLabelColor = OpenRockyPalette.accent,
                                unfocusedLabelColor = OpenRockyPalette.muted
                            )
                        )
                        OutlinedTextField(
                            value = endpointURL,
                            onValueChange = { endpointURL = it },
                            label = { Text("Endpoint URL") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Uri
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = OpenRockyPalette.text,
                                unfocusedTextColor = OpenRockyPalette.text,
                                focusedBorderColor = OpenRockyPalette.accent,
                                unfocusedBorderColor = OpenRockyPalette.muted,
                                focusedLabelColor = OpenRockyPalette.accent,
                                unfocusedLabelColor = OpenRockyPalette.muted
                            )
                        )
                        OutlinedTextField(
                            value = bearerToken,
                            onValueChange = { bearerToken = it },
                            label = { Text("Bearer token (optional)") },
                            singleLine = true,
                            visualTransformation = if (bearerVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { bearerVisible = !bearerVisible }) {
                                    Icon(
                                        if (bearerVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null,
                                        tint = OpenRockyPalette.muted
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = OpenRockyPalette.text,
                                unfocusedTextColor = OpenRockyPalette.text,
                                focusedBorderColor = OpenRockyPalette.accent,
                                unfocusedBorderColor = OpenRockyPalette.muted,
                                focusedLabelColor = OpenRockyPalette.accent,
                                unfocusedLabelColor = OpenRockyPalette.muted
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Enabled", color = OpenRockyPalette.text, modifier = Modifier.weight(1f))
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = OpenRockyPalette.accent,
                                    uncheckedTrackColor = OpenRockyPalette.cardElevated
                                )
                            )
                        }
                        Text(
                            "Streamable-HTTP MCP transport. The label is the namespace prefix shown to the model as `mcp-{label}-{tool}`.",
                            fontSize = 11.sp,
                            color = OpenRockyPalette.muted
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Tools (${cachedTools.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OpenRockyPalette.muted
                        )
                        Button(
                            onClick = {
                                if (refreshing || endpointURL.isBlank()) return@Button
                                refreshing = true
                                refreshError = null
                                scope.launch {
                                    try {
                                        // Build a temporary server snapshot from the form state so the
                                        // user can refresh before saving — otherwise first-time setup
                                        // would force a save → reload → refresh dance.
                                        val snapshot = MCPServer(
                                            label = label.trim().ifEmpty { "preview" },
                                            endpointURL = endpointURL.trim(),
                                            bearerToken = bearerToken.trim().ifEmpty { null }
                                        )
                                        val tools = MCPClient(snapshot).listTools()
                                        cachedTools = tools
                                        lastRefreshedAt = System.currentTimeMillis()
                                        // Persist immediately for existing servers so other parts of
                                        // the app see the new tools without waiting for Save.
                                        existing?.id?.let { onCacheTools(it, tools) }
                                    } catch (e: Exception) {
                                        refreshError = e.message ?: e.javaClass.simpleName
                                    } finally {
                                        refreshing = false
                                    }
                                }
                            },
                            enabled = !refreshing && endpointURL.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (refreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = OpenRockyPalette.text,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Refreshing…")
                            } else {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Refresh tool catalog")
                            }
                        }
                        lastRefreshedAt?.let { ts ->
                            Text(
                                "Last refresh: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ts))}",
                                fontSize = 11.sp,
                                color = OpenRockyPalette.muted
                            )
                        }
                        refreshError?.let { err ->
                            Text(err, fontSize = 11.sp, color = OpenRockyPalette.warning)
                        }
                    }
                }
            }

            if (cachedTools.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            cachedTools.forEach { tool ->
                                Column {
                                    Text(
                                        tool.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OpenRockyPalette.text
                                    )
                                    if (tool.description.isNotEmpty()) {
                                        Text(tool.description, fontSize = 11.sp, color = OpenRockyPalette.muted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
