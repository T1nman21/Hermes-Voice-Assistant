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
import com.xnu.rocky.runtime.CharacterDefinition
import com.xnu.rocky.runtime.voice.OpenAIVoice
import com.xnu.rocky.ui.screens.providers.rockyTextFieldColors
import com.xnu.rocky.ui.theme.OpenRockyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditorView(
    character: CharacterDefinition?,
    onSave: (CharacterDefinition) -> Unit,
    onReset: ((String) -> Unit)?,
    onBack: () -> Unit
) {
    val isNew = character == null
    var name by remember { mutableStateOf(character?.name ?: "") }
    var description by remember { mutableStateOf(character?.description ?: "") }
    var personality by remember { mutableStateOf(character?.personality ?: "") }
    var greeting by remember { mutableStateOf(character?.greeting ?: "") }
    var speakingStyle by remember { mutableStateOf(character?.speakingStyle ?: "") }
    var openaiVoice by remember { mutableStateOf(character?.openaiVoice ?: "alloy") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Character" else "Edit Character", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                actions = {
                    if (character?.isBuiltIn == true && onReset != null) {
                        TextButton(onClick = { onReset(character.id); onBack() }) { Text("Reset", color = OpenRockyPalette.warning) }
                    }
                    TextButton(onClick = {
                        val c = (character ?: CharacterDefinition()).copy(
                            name = name, description = description, personality = personality,
                            greeting = greeting, speakingStyle = speakingStyle, openaiVoice = openaiVoice
                        )
                        onSave(c); onBack()
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
            item { EditorField("Greeting", greeting, { greeting = it }) }
            item { EditorField("Speaking Style", speakingStyle, { speakingStyle = it }) }
            item { EditorField("Personality (System Prompt)", personality, { personality = it }, minLines = 5) }
            item {
                Text("Voice", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OpenAIVoice.entries.forEach { voice ->
                        FilterChip(
                            selected = openaiVoice == voice.id, onClick = { openaiVoice = voice.id },
                            label = { Text(voice.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OpenRockyPalette.accent.copy(alpha = 0.2f), containerColor = OpenRockyPalette.cardElevated)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorField(label: String, value: String, onChange: (String) -> Unit, minLines: Int = 1) {
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            colors = rockyTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            minLines = minLines,
            maxLines = if (minLines > 1) 10 else 1
        )
    }
}
