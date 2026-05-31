//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.xnu.rocky.ui.theme.OpenRockyPalette
import java.util.UUID

enum class TimelineKind(
    val label: String,
    val symbol: ImageVector,
    val tint: Color
) {
    SPEECH(label = "Speech", symbol = Icons.Default.Mic, tint = OpenRockyPalette.accentBrand),
    SYSTEM(label = "System", symbol = Icons.Default.Settings, tint = OpenRockyPalette.mutedStatic),
    TOOL(label = "Tool", symbol = Icons.Default.Build, tint = OpenRockyPalette.secondaryBrand),
    RESULT(label = "Result", symbol = Icons.Default.CheckCircle, tint = OpenRockyPalette.success);
}

data class TimelineEntry(
    val id: String = UUID.randomUUID().toString(),
    val kind: TimelineKind,
    val time: String,
    val text: String
)
