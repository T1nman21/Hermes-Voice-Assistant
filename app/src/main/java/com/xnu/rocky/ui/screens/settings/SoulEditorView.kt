//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-13
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.runtime.SoulDefinition
import com.xnu.rocky.runtime.SoulStore
import com.xnu.rocky.ui.theme.OpenRockyPalette
import com.xnu.rocky.ui.screens.providers.rockyTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoulEditorView(
    existingSoul: SoulDefinition?,
    soulStore: SoulStore,
    onBack: () -> Unit
) {
    val isNew = existingSoul == null
    var name by remember { mutableStateOf(existingSoul?.name ?: "") }
    var description by remember { mutableStateOf(existingSoul?.description ?: "") }
    var personality by remember { mutableStateOf(existingSoul?.personality ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Soul" else "Edit Soul", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                actions = {
                    if (existingSoul?.isBuiltIn == true) {
                        TextButton(onClick = {
                            soulStore.resetBuiltIn(existingSoul.id)
                            onBack()
                        }) {
                            Text("Reset", color = OpenRockyPalette.muted)
                        }
                    }
                    TextButton(
                        onClick = {
                            if (isNew) {
                                val soul = SoulDefinition(
                                    id = "soul-${System.currentTimeMillis()}",
                                    name = name, description = description,
                                    personality = personality, isBuiltIn = false
                                )
                                soulStore.add(soul)
                            } else {
                                soulStore.update(existingSoul!!.copy(
                                    name = name, description = description, personality = personality
                                ))
                            }
                            onBack()
                        },
                        enabled = name.isNotBlank() && personality.isNotBlank()
                    ) {
                        Text("Save", color = if (name.isNotBlank() && personality.isNotBlank()) OpenRockyPalette.accent else OpenRockyPalette.label)
                    }
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
            item {
                Text("Name", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    placeholder = { Text("Soul name", color = OpenRockyPalette.label) },
                    modifier = Modifier.fillMaxWidth(), colors = rockyTextFieldColors(),
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
            }
            item {
                Text("Description", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    placeholder = { Text("Brief description", color = OpenRockyPalette.label) },
                    modifier = Modifier.fillMaxWidth(), colors = rockyTextFieldColors(),
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )
            }
            item {
                Text("Personality (System Prompt)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = personality, onValueChange = { personality = it },
                    placeholder = { Text("Define the AI's personality, behavior rules, and communication style...", color = OpenRockyPalette.label) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                    colors = rockyTextFieldColors(), shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                Text("This text becomes the system prompt that shapes how the AI thinks, responds, and uses tools.", fontSize = 11.sp, color = OpenRockyPalette.label, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
