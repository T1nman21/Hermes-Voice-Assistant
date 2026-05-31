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
import com.xnu.rocky.runtime.LogEntry
import com.xnu.rocky.runtime.LogManager
import com.xnu.rocky.ui.theme.OpenRockyPalette
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsView(
    onViewFile: (String) -> Unit,
    onBack: () -> Unit
) {
    val logFiles = remember { LogManager.listLogFiles() }
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                actions = { TextButton(onClick = { LogManager.clearAll() }) { Text("Clear", color = OpenRockyPalette.error) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        if (logFiles.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No log files", color = OpenRockyPalette.muted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(logFiles) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onViewFile(file.absolutePath) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = OpenRockyPalette.muted, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                                Text("${file.length() / 1024} KB · ${dateFormat.format(Date(file.lastModified()))}", fontSize = 11.sp, color = OpenRockyPalette.muted)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = OpenRockyPalette.label, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFileView(
    filePath: String,
    onBack: () -> Unit
) {
    val entries = remember { LogManager.readLogFile(File(filePath)) }
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    var filter by remember { mutableStateOf<String?>(null) }
    val filtered = if (filter != null) entries.filter { it.level == filter } else entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(File(filePath).name, color = OpenRockyPalette.text, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, "INFO", "WARNING", "ERROR").forEach { level ->
                    FilterChip(selected = filter == level, onClick = { filter = level },
                        label = { Text(level ?: "All", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OpenRockyPalette.accent.copy(alpha = 0.2f), containerColor = OpenRockyPalette.cardElevated))
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filtered) { entry ->
                    val levelColor = when (entry.level) {
                        "ERROR" -> OpenRockyPalette.error; "WARNING" -> OpenRockyPalette.warning; "INFO" -> OpenRockyPalette.accent; else -> OpenRockyPalette.muted
                    }
                    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)) {
                        Column(Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.level, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = levelColor)
                                Spacer(Modifier.width(8.dp))
                                Text(dateFormat.format(Date(entry.timestamp)), fontSize = 10.sp, color = OpenRockyPalette.label)
                                if (entry.source.isNotBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(entry.source, fontSize = 10.sp, color = OpenRockyPalette.muted)
                                }
                            }
                            Text(entry.message, fontSize = 12.sp, color = OpenRockyPalette.text, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
