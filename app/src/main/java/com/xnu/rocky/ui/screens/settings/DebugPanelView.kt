//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-13
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.models.OpenRockySession
import com.xnu.rocky.models.ProviderStatus
import com.xnu.rocky.ui.theme.OpenRockyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPanelView(
    session: OpenRockySession,
    chatProviderStatus: ProviderStatus,
    voiceProviderStatus: ProviderStatus,
    toolCount: Int,
    skillCount: Int,
    memoryCount: Int,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Session Card
            item {
                DebugCard(title = "Session", icon = Icons.Default.Memory, tint = OpenRockyPalette.accent) {
                    DebugRow("Mode", session.mode.title, session.mode.tint)
                    DebugRow("Session Tag", session.sessionTag)
                    DebugRow("Plan Steps", "${session.plan.size}")
                    DebugRow("Timeline Entries", "${session.timeline.size}")
                    if (session.liveTranscript.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("TRANSCRIPT", fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = OpenRockyPalette.label)
                        Text(session.liveTranscript, fontSize = 13.sp, color = OpenRockyPalette.text)
                    }
                }
            }

            // Providers Card
            item {
                DebugCard(title = "Providers", icon = Icons.Default.Cloud, tint = OpenRockyPalette.success) {
                    DebugRow(
                        "Chat Provider",
                        "${chatProviderStatus.name} / ${chatProviderStatus.model}",
                        if (chatProviderStatus.isConnected) OpenRockyPalette.success else OpenRockyPalette.warning
                    )
                    DebugRow(
                        "Voice Provider",
                        "${voiceProviderStatus.name} / ${voiceProviderStatus.model}",
                        if (voiceProviderStatus.isConnected) OpenRockyPalette.success else OpenRockyPalette.warning
                    )
                }
            }

            // Runtime Card
            item {
                DebugCard(title = "Runtime", icon = Icons.Default.Settings, tint = OpenRockyPalette.warning) {
                    DebugRow("Tools Enabled", "$toolCount")
                    DebugRow("Skills Active", "$skillCount")
                    DebugRow("Memory Entries", "$memoryCount")
                    DebugRow("Platform", "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                    DebugRow("Device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                }
            }

            // Quick Tasks Card
            item {
                DebugCard(title = "Quick Tasks", icon = Icons.Default.FlashOn, tint = OpenRockyPalette.secondary) {
                    session.quickTasks.forEach { task ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier.size(6.dp).clip(CircleShape).background(OpenRockyPalette.accent)
                            )
                            Column {
                                Text(task.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                                Text(task.prompt, fontSize = 11.sp, color = OpenRockyPalette.muted, maxLines = 1)
                            }
                        }
                    }
                    if (session.quickTasks.isEmpty()) {
                        Text("No quick tasks", fontSize = 12.sp, color = OpenRockyPalette.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugCard(title: String, icon: ImageVector, tint: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, title, tint = tint, modifier = Modifier.size(16.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = OpenRockyPalette.text)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String, tint: Color? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = OpenRockyPalette.label)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (tint != null) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
            }
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = OpenRockyPalette.text)
        }
    }
}
