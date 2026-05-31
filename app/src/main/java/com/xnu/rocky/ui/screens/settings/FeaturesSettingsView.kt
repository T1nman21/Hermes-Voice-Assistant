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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.runtime.tools.EmailConfig
import com.xnu.rocky.ui.theme.OpenRockyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesSettingsView(onBack: () -> Unit) {
    val context = LocalContext.current
    val emailConfig = EmailConfig.load(context)
    val emailConfigured = emailConfig?.isConfigured == true && emailConfig.hasCredential
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Features", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
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
            item {
                Text("Configurable integrations and optional features.", fontSize = 14.sp, color = OpenRockyPalette.muted)
            }

            // Gmail
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, null, tint = OpenRockyPalette.secondary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Send Email", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                            Text(
                                if (emailConfigured) "Ready via ${emailConfig!!.provider.displayName} — ${emailConfig.displayIdentity}"
                                else "Setup required — SMTP (Gmail, Outlook, QQ) or Resend",
                                fontSize = 12.sp, color = OpenRockyPalette.muted
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (emailConfigured) OpenRockyPalette.success.copy(alpha = 0.15f) else OpenRockyPalette.label.copy(alpha = 0.15f)
                        ) {
                            Text(
                                if (emailConfigured) "ON" else "OFF",
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = if (emailConfigured) OpenRockyPalette.success else OpenRockyPalette.label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Notifications
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Notifications", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                            Text("Allow Rocky to send notifications", fontSize = 12.sp, color = OpenRockyPalette.muted)
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = OpenRockyPalette.success.copy(alpha = 0.15f)) {
                            Text("ON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.success, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
