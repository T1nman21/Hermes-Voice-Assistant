//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.runtime.UsageService
import com.xnu.rocky.ui.theme.OpenRockyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageSettingsView(
    usageService: UsageService,
    onBack: () -> Unit
) {
    var daysBack by remember { mutableIntStateOf(30) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage", color = OpenRockyPalette.text) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text) } },
                actions = {
                    TextButton(onClick = { usageService.clearAll() }) { Text("Clear", color = OpenRockyPalette.error) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Date range selector
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 14, 30).forEach { days ->
                        FilterChip(
                            selected = daysBack == days, onClick = { daysBack = days },
                            label = { Text("${days}d") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OpenRockyPalette.accent.copy(alpha = 0.2f), containerColor = OpenRockyPalette.cardElevated)
                        )
                    }
                }
            }

            // Summary cards
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Total Tokens", formatNumber(usageService.totalTokens(daysBack)), OpenRockyPalette.accent, Modifier.weight(1f))
                    SummaryCard("Requests", usageService.totalRequests(daysBack).toString(), OpenRockyPalette.secondary, Modifier.weight(1f))
                    SummaryCard("Daily Avg", formatNumber(usageService.dailyAverage(daysBack)), OpenRockyPalette.success, Modifier.weight(1f))
                }
            }

            // Model breakdown
            val modelSummaries = usageService.modelSummaries(daysBack)
            if (modelSummaries.isNotEmpty()) {
                item { Text("MODEL BREAKDOWN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.label) }
                items(modelSummaries) { summary ->
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(summary.model, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                                Text("${summary.requestCount} requests", fontSize = 11.sp, color = OpenRockyPalette.muted)
                            }
                            Text(formatNumber(summary.totalTokens), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = OpenRockyPalette.label, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

private fun formatNumber(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fK".format(n / 1_000.0)
    else -> n.toString()
}
