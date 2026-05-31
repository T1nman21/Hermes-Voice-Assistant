//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
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
import com.xnu.rocky.runtime.skills.CustomSkill
import com.xnu.rocky.ui.theme.OpenRockyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSkillsListView(
    skills: List<CustomSkill>,
    onToggle: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Skills", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                actions = {
                    IconButton(onClick = onImport) { Icon(Icons.Default.Download, "Import", tint = OpenRockyPalette.accent) }
                    IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add", tint = OpenRockyPalette.accent) }
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
            // Create custom skill at top
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onAdd() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.cardElevated)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Create Custom Skill", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                    }
                }
            }

            items(skills, key = { it.id }) { skill ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { onEdit(skill.id) }) {
                            Text(skill.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                            Text(skill.description, fontSize = 12.sp, color = OpenRockyPalette.muted)
                        }
                        Switch(
                            checked = skill.enabled,
                            onCheckedChange = { onToggle(skill.id) },
                            colors = SwitchDefaults.colors(checkedTrackColor = OpenRockyPalette.accent, uncheckedTrackColor = OpenRockyPalette.cardElevated)
                        )
                    }
                }
            }
        }
    }
}
