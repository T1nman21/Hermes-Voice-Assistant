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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.runtime.skills.CustomSkill
import com.xnu.rocky.ui.screens.providers.rockyTextFieldColors
import com.xnu.rocky.ui.theme.OpenRockyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSkillEditorView(
    skill: CustomSkill?,
    onSave: (CustomSkill) -> Unit,
    onBack: () -> Unit
) {
    val isNew = skill == null
    var name by remember { mutableStateOf(skill?.name ?: "") }
    var description by remember { mutableStateOf(skill?.description ?: "") }
    var trigger by remember { mutableStateOf(skill?.trigger ?: "") }
    var prompt by remember { mutableStateOf(skill?.prompt ?: "") }
    var enabled by remember { mutableStateOf(skill?.enabled ?: true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Skill" else "Edit Skill", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                actions = {
                    TextButton(onClick = {
                        val s = (skill ?: CustomSkill()).copy(name = name, description = description, trigger = trigger, prompt = prompt, enabled = enabled)
                        onSave(s); onBack()
                    }, enabled = name.isNotBlank()) { Text("Save", color = if (name.isNotBlank()) OpenRockyPalette.accent else OpenRockyPalette.label) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { EditorField("Name", name, { name = it }) }
            item { EditorField("Description", description, { description = it }) }
            item { EditorField("Trigger", trigger, { trigger = it }) }
            item { EditorField("Prompt", prompt, { prompt = it }, minLines = 6) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Enabled", fontSize = 15.sp, color = OpenRockyPalette.text)
                    Switch(checked = enabled, onCheckedChange = { enabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = OpenRockyPalette.accent))
                }
            }
        }
    }
}
