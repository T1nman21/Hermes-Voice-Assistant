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
import com.xnu.rocky.runtime.skills.CustomSkillStore
import com.xnu.rocky.ui.screens.providers.rockyTextFieldColors
import com.xnu.rocky.ui.theme.OpenRockyPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillImportView(
    skillStore: CustomSkillStore,
    onBack: () -> Unit
) {
    var repoUrl by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var importedCount by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Skills", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("GitHub Repository URL", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
            OutlinedTextField(
                value = repoUrl, onValueChange = { repoUrl = it; errorMessage = null; importedCount = null },
                placeholder = { Text("https://github.com/owner/skills-repo", color = OpenRockyPalette.label) },
                modifier = Modifier.fillMaxWidth(), colors = rockyTextFieldColors(),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
            Text(
                "Each skill should be in a subdirectory with a SKILL.md file. Already imported skills will be skipped.",
                fontSize = 11.sp, color = OpenRockyPalette.label
            )

            Button(
                onClick = {
                    scope.launch {
                        isImporting = true
                        errorMessage = null
                        importedCount = null
                        try {
                            val count = skillStore.importFromGitHubRepo(repoUrl)
                            importedCount = count
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Import failed"
                        }
                        isImporting = false
                    }
                },
                enabled = repoUrl.isNotBlank() && !isImporting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OpenRockyPalette.text, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Importing...")
                } else {
                    Text("Import Skills")
                }
            }

            errorMessage?.let { msg ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.error.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Close, "Error", tint = OpenRockyPalette.error, modifier = Modifier.size(20.dp))
                        Text(msg, fontSize = 13.sp, color = OpenRockyPalette.error)
                    }
                }
            }

            importedCount?.let { count ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.success.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Check, "Success", tint = OpenRockyPalette.success, modifier = Modifier.size(20.dp))
                        Text(
                            if (count > 0) "Successfully imported $count skill(s)!"
                            else "No new skills found (all already imported or no SKILL.md files)",
                            fontSize = 13.sp, color = OpenRockyPalette.success
                        )
                    }
                }
            }
        }
    }
}
