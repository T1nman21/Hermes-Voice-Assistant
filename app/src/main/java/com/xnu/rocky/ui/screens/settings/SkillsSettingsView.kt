//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.settings

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
import com.xnu.rocky.runtime.tools.BuiltInToolStore
import com.xnu.rocky.ui.theme.OpenRockyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsSettingsView(
    toolStore: BuiltInToolStore,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tools", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
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
            toolStore.groups.forEach { group ->
                item { Text(group, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted, modifier = Modifier.padding(top = 8.dp)) }
                items(toolStore.toolsInGroup(group)) { tool ->
                    var enabled by remember { mutableStateOf(toolStore.isEnabled(tool.name)) }
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, null, tint = OpenRockyPalette.muted, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tool.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                                Text(tool.description, fontSize = 11.sp, color = OpenRockyPalette.muted)
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { enabled = it; toolStore.setEnabled(tool.name, it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = OpenRockyPalette.accent, uncheckedTrackColor = OpenRockyPalette.cardElevated)
                            )
                        }
                    }
                }
            }
        }
    }
}
