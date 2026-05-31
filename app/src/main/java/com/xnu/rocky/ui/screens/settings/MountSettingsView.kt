//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-13
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.runtime.tools.MountDefinition
import com.xnu.rocky.runtime.tools.MountStore
import com.xnu.rocky.ui.theme.OpenRockyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MountSettingsView(
    mountStore: MountStore,
    onBack: () -> Unit
) {
    val mounts by mountStore.mounts.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var mountName by remember { mutableStateOf("") }
    var mountReadWrite by remember { mutableStateOf(true) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            pendingUri = uri
            // Suggest name from the last path segment
            mountName = uri.lastPathSegment
                ?.substringAfterLast(":")
                ?.substringAfterLast("/")
                ?.ifBlank { "folder" }
                ?: "folder"
            mountReadWrite = true
            showNameDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("External Folders", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                actions = {
                    if (mounts.size < MountStore.MAX_MOUNTS) {
                        IconButton(onClick = { folderPicker.launch(null) }) {
                            Icon(Icons.Default.Add, "Add", tint = OpenRockyPalette.accent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.FolderOpen, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(32.dp))
                        Column {
                            Text("Mount External Folders", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                            Text(
                                "Select folders from your device storage (e.g. Obsidian vault, project directories) so the AI can read and write files in them.",
                                fontSize = 12.sp, color = OpenRockyPalette.muted
                            )
                        }
                    }
                }
            }

            // Count
            item {
                Text(
                    "Mounted Folders ${mounts.size} / ${MountStore.MAX_MOUNTS}",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.label,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Mount list
            if (mounts.isEmpty()) {
                item {
                    Text(
                        "No folders mounted. Tap + to add one.",
                        fontSize = 13.sp, color = OpenRockyPalette.muted,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            items(mounts, key = { it.id }) { mount ->
                val isAccessible = remember(mount) { mountStore.isAccessible(mount) }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Status dot
                        Box(
                            Modifier.size(8.dp).clip(CircleShape).background(
                                if (isAccessible) OpenRockyPalette.success else OpenRockyPalette.error
                            )
                        )

                        // Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Folder, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(16.dp))
                                Text(mount.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (mount.readWrite) OpenRockyPalette.success.copy(alpha = 0.15f) else OpenRockyPalette.warning.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        if (mount.readWrite) "Read/Write" else "Read Only",
                                        fontSize = 9.sp,
                                        color = if (mount.readWrite) OpenRockyPalette.success else OpenRockyPalette.warning,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                mount.displayPath,
                                fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                color = OpenRockyPalette.muted, maxLines = 1
                            )
                        }

                        // Delete
                        IconButton(onClick = { mountStore.delete(mount.id) }) {
                            Icon(Icons.Default.Delete, "Remove", tint = OpenRockyPalette.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Usage hint
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Once mounted, the AI can access these folders using the tools: external-list, external-read, external-write. Use the mount name as the 'container' parameter.",
                    fontSize = 11.sp, color = OpenRockyPalette.label
                )
            }
        }
    }

    // Name dialog
    if (showNameDialog && pendingUri != null) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false; pendingUri = null },
            title = { Text("Mount Name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Give this folder a short name the AI will use to reference it.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = mountName,
                        onValueChange = { mountName = it },
                        placeholder = { Text("e.g. obsidian, notes, project") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = mountReadWrite, onCheckedChange = { mountReadWrite = it })
                        Text("Allow writing", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingUri ?: return@TextButton
                        val mount = mountStore.createMount(mountName.trim(), uri, mountReadWrite)
                        mountStore.add(mount)
                        showNameDialog = false
                        pendingUri = null
                    },
                    enabled = mountName.isNotBlank()
                ) { Text("Mount") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false; pendingUri = null }) { Text("Cancel") }
            }
        )
    }
}
