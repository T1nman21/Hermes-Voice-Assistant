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

enum class PlanStepState(
    val label: String,
    val symbol: ImageVector,
    val tint: Color
) {
    DONE(label = "Done", symbol = Icons.Default.CheckCircle, tint = OpenRockyPalette.success),
    ACTIVE(label = "Active", symbol = Icons.Default.Bolt, tint = OpenRockyPalette.secondaryBrand),
    QUEUED(label = "Queued", symbol = Icons.Default.RadioButtonUnchecked, tint = OpenRockyPalette.mutedStatic);
}

data class PlanStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val detail: String,
    val state: PlanStepState
)
