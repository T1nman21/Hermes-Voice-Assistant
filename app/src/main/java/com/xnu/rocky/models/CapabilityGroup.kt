//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.models

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class CapabilityGroup(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val status: String,
    val summary: String,
    val items: List<String>,
    val tint: Color
)
