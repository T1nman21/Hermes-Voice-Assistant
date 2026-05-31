//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.providers.ProviderInstance
import com.xnu.rocky.ui.theme.OpenRockyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderInstanceListView(
    instances: List<ProviderInstance>,
    activeInstanceId: String?,
    onSelect: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Providers", color = OpenRockyPalette.text) },
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
        if (instances.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No providers configured", color = OpenRockyPalette.muted, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Provider")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(instances, key = { it.id }) { instance ->
                    val isActive = instance.id == activeInstanceId
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(instance.id); true } else false
                            }
                        ),
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(OpenRockyPalette.error).padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) { Icon(Icons.Default.Delete, null, tint = OpenRockyPalette.text) }
                        }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(instance.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isActive) OpenRockyPalette.cardElevated else OpenRockyPalette.card)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isActive) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(OpenRockyPalette.accent))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(instance.name.ifBlank { instance.kind.displayName }, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                                    Text("${instance.kind.displayName} · ${instance.modelID.ifBlank { instance.kind.defaultModel }}", fontSize = 12.sp, color = OpenRockyPalette.muted)
                                }
                                IconButton(onClick = { onEdit(instance.id) }) {
                                    Icon(Icons.Default.Edit, "Edit", tint = OpenRockyPalette.label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
