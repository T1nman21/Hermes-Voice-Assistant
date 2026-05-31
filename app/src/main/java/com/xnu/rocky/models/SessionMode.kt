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

enum class SessionMode(
    val title: String,
    val subtitle: String,
    val buttonTitle: String,
    val symbol: ImageVector,
    val tint: Color
) {
    LISTENING(
        title = "Listening",
        subtitle = "Waiting for your intent…",
        buttonTitle = "Start Planning",
        symbol = Icons.Default.GraphicEq,
        tint = OpenRockyPalette.accentBrand
    ),
    PLANNING(
        title = "Planning",
        subtitle = "Converting speech to task graph…",
        buttonTitle = "Execute",
        symbol = Icons.Default.AccountTree,
        tint = OpenRockyPalette.accentBrand
    ),
    EXECUTING(
        title = "Executing",
        subtitle = "Tools running — timeline updating…",
        buttonTitle = "Pause",
        symbol = Icons.Default.Bolt,
        tint = OpenRockyPalette.secondaryBrand
    ),
    READY(
        title = "Ready",
        subtitle = "Session quiet — context attached.",
        buttonTitle = "Start Listening",
        symbol = Icons.Default.CheckCircle,
        tint = OpenRockyPalette.success
    );

    fun next(): SessionMode = when (this) {
        LISTENING -> PLANNING
        PLANNING -> EXECUTING
        EXECUTING -> READY
        READY -> LISTENING
    }
}
