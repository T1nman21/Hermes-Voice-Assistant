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
import com.xnu.rocky.runtime.tools.FileService
import com.xnu.rocky.ui.theme.OpenRockyPalette
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceFilesView(
    context: android.content.Context,
    currentPath: String = "",
    onNavigate: (String) -> Unit,
    onPreview: (String) -> Unit,
    onBack: () -> Unit
) {
    val files = remember(currentPath) { FileService.listFiles(context, currentPath) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentPath.isBlank()) "Workspace" else currentPath, color = OpenRockyPalette.text, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Empty directory", color = OpenRockyPalette.muted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(files) { file ->
                    val icon = when {
                        file.isDirectory -> Icons.Default.Folder
                        file.extension in listOf("md", "txt") -> Icons.Default.Description
                        file.extension in listOf("kt", "java", "py", "js") -> Icons.Default.Code
                        file.extension in listOf("png", "jpg", "jpeg", "webp") -> Icons.Default.Image
                        else -> Icons.Default.InsertDriveFile
                    }
                    val tint = when {
                        file.isDirectory -> OpenRockyPalette.accent
                        else -> OpenRockyPalette.muted
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (file.isDirectory) {
                                val newPath = if (currentPath.isBlank()) file.name else "$currentPath/${file.name}"
                                onNavigate(newPath)
                            } else {
                                onPreview(file.absolutePath)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name, fontSize = 14.sp, color = OpenRockyPalette.text)
                                if (!file.isDirectory) { Text("${file.length() / 1024} KB", fontSize = 11.sp, color = OpenRockyPalette.label) }
                            }
                            if (file.isDirectory) { Icon(Icons.Default.ChevronRight, null, tint = OpenRockyPalette.label, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }
    }
}
