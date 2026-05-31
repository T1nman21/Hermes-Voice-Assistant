//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.ui.theme.OpenRockyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSettingsView(
    onBack: () -> Unit,
    onChatProviders: () -> Unit,
    onVoiceProviders: () -> Unit,
    onCharacters: () -> Unit,
    onSoul: () -> Unit,
    onSkills: () -> Unit,
    onCustomSkills: () -> Unit,
    onMCPServers: () -> Unit,
    onMemory: () -> Unit,
    onEmail: () -> Unit,
    onFeatures: () -> Unit,
    onUsage: () -> Unit,
    onMounts: () -> Unit,
    onWorkspace: () -> Unit,
    onLogs: () -> Unit,
    onDebug: () -> Unit,
    onSystemIntegrations: () -> Unit,
    onAbout: () -> Unit,
    voiceAutoStartOnLaunch: Boolean = true,
    onVoiceAutoStartChange: (Boolean) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = OpenRockyPalette.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                SectionHeader("Providers")
            }
            item {
                SettingsRow(Icons.AutoMirrored.Filled.Chat, "Chat Providers", "Configure AI models", OpenRockyPalette.accent, onChatProviders)
            }
            item {
                SettingsRow(Icons.Default.GraphicEq, "Voice Provider", "OpenAI Realtime configuration", OpenRockyPalette.secondary, onVoiceProviders)
            }
            item {
                SettingsToggleRow(
                    icon = Icons.Default.Mic,
                    title = "Auto-Start on Launch",
                    subtitle = "Enter voice mode automatically when the app opens",
                    tint = OpenRockyPalette.secondary,
                    checked = voiceAutoStartOnLaunch,
                    onCheckedChange = onVoiceAutoStartChange
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                SectionHeader("Personalization")
            }
            item {
                SettingsRow(Icons.Default.Person, "Characters", "Manage AI personalities", OpenRockyPalette.accent, onCharacters)
            }
            item {
                SettingsRow(Icons.Default.Psychology, "Soul", "Core AI persona & behavior style", OpenRockyPalette.secondary, onSoul)
            }
            item {
                SettingsRow(Icons.Default.Build, "Tools", "Built-in tools", OpenRockyPalette.warning, onSkills)
            }
            item {
                SettingsRow(Icons.Default.AutoAwesome, "Skills", "Custom skills", OpenRockyPalette.secondary, onCustomSkills)
            }
            item {
                SettingsRow(Icons.Default.Lan, "MCP Servers", "Model Context Protocol", OpenRockyPalette.secondary, onMCPServers)
            }
            item {
                SettingsRow(Icons.Default.Memory, "Memory", "Persistent key-value store", OpenRockyPalette.success, onMemory)
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                SectionHeader("System")
            }
            item {
                SettingsRow(Icons.Default.Email, "Email", "SMTP email configuration", OpenRockyPalette.accent, onEmail)
            }
            item {
                SettingsRow(Icons.Default.ToggleOn, "Features", "Optional integrations", OpenRockyPalette.warning, onFeatures)
            }
            item {
                SettingsRow(Icons.Default.BarChart, "Usage", "Token usage analytics", OpenRockyPalette.accent, onUsage)
            }
            item {
                SettingsRow(Icons.Default.FolderOpen, "External Folders", "Mount device folders for AI access", OpenRockyPalette.accent, onMounts)
            }
            item {
                SettingsRow(Icons.Default.Folder, "Workspace", "Browse workspace files", OpenRockyPalette.muted, onWorkspace)
            }
            item {
                SettingsRow(Icons.Default.BugReport, "Logs", "Debug logs viewer", OpenRockyPalette.muted, onLogs)
            }
            item {
                SettingsRow(Icons.Default.Code, "Debug", "Session & runtime inspector", OpenRockyPalette.label, onDebug)
            }
            item {
                SettingsRow(Icons.Default.SettingsRemote, "System Integrations", "Assist, notifications, shortcuts, automation", OpenRockyPalette.accent, onSystemIntegrations)
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                SettingsRow(Icons.Default.Info, "About", "App info & links", OpenRockyPalette.label, onAbout)
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = OpenRockyPalette.label,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                Text(subtitle, fontSize = 12.sp, color = OpenRockyPalette.muted)
            }
            Icon(Icons.Default.ChevronRight, null, tint = OpenRockyPalette.label, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                Text(subtitle, fontSize = 12.sp, color = OpenRockyPalette.muted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = OpenRockyPalette.accent)
            )
        }
    }
}
