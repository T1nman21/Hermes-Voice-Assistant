//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.settings

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
import com.xnu.rocky.runtime.CharacterDefinition
import com.xnu.rocky.ui.theme.OpenRockyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSettingsView(
    characters: List<CharacterDefinition>,
    activeCharacterId: String?,
    onActivate: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Characters", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                actions = { IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add", tint = OpenRockyPalette.accent) } },
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
            items(characters, key = { it.id }) { char ->
                val isActive = char.id == activeCharacterId
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onActivate(char.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isActive) OpenRockyPalette.cardElevated else OpenRockyPalette.card)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isActive) { Box(Modifier.size(8.dp).clip(CircleShape).background(OpenRockyPalette.accent)) }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(char.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                                if (char.isBuiltIn) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = OpenRockyPalette.accent.copy(alpha = 0.15f)) {
                                        Text("Built-in", fontSize = 9.sp, color = OpenRockyPalette.accent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text(char.description, fontSize = 12.sp, color = OpenRockyPalette.muted)
                        }
                        IconButton(onClick = { onEdit(char.id) }) { Icon(Icons.Default.Edit, "Edit", tint = OpenRockyPalette.label, modifier = Modifier.size(20.dp)) }
                        if (!char.isBuiltIn) {
                            IconButton(onClick = { onDelete(char.id) }) { Icon(Icons.Default.Delete, "Delete", tint = OpenRockyPalette.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}
