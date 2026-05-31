//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-05-04
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.runtime.DelegateProgress
import com.xnu.rocky.ui.theme.OpenRockyPalette
import java.util.Locale

/**
 * Voice overlay live-progress panel — renders the in-flight delegate-task,
 * its subtasks and the last few tool/skill events per subtask. Mirrors iOS
 * `OpenRockyDelegateProgressPanel`. Animates in/out so the orb-only resting
 * state isn't disrupted.
 */
@Composable
fun DelegateProgressPanel(
    progress: DelegateProgress?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = progress != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = modifier
    ) {
        progress?.let { Body(it) }
    }
}

@Composable
private fun Body(progress: DelegateProgress) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.cardElevated)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderRow(progress)
            if (progress.subtasks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    progress.subtasks.forEach { subtask -> SubtaskRow(subtask) }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(progress: DelegateProgress) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!progress.isFinished) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = OpenRockyPalette.accent,
                strokeWidth = 2.dp
            )
        } else {
            Text("✓", color = OpenRockyPalette.success, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (progress.isFinished) "Delegate-task done" else "Delegate-task running",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OpenRockyPalette.text
            )
            val subtitle = progress.taskDescription.take(120)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 11.sp, color = OpenRockyPalette.muted)
            }
        }
        if (progress.isFinished && progress.planSucceededCount != null && progress.planTotalCount != null) {
            Text(
                "${progress.planSucceededCount}/${progress.planTotalCount}",
                fontSize = 12.sp,
                color = OpenRockyPalette.muted
            )
        }
    }
}

@Composable
private fun SubtaskRow(subtask: DelegateProgress.Subtask) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            statusIndicator(subtask.status, subtask.succeeded)
            Spacer(Modifier.width(6.dp))
            Text(
                "${subtask.index + 1}/${subtask.totalCount} ${subtask.description.take(80)}",
                fontSize = 12.sp,
                color = OpenRockyPalette.text,
                modifier = Modifier.weight(1f)
            )
            subtask.elapsedSeconds?.let {
                Text(
                    String.format(Locale.US, "%.1fs", it),
                    fontSize = 11.sp,
                    color = OpenRockyPalette.muted
                )
            }
        }
        for (event in subtask.toolEvents) ToolEventRow(event)
    }
}

@Composable
private fun statusIndicator(status: DelegateProgress.Subtask.Status, succeeded: Boolean?) {
    when (status) {
        is DelegateProgress.Subtask.Status.Running,
        is DelegateProgress.Subtask.Status.Thinking -> CircularProgressIndicator(
            modifier = Modifier.size(10.dp),
            color = OpenRockyPalette.accent,
            strokeWidth = 1.5.dp
        )
        is DelegateProgress.Subtask.Status.Completed -> {
            val mark = if (succeeded == false) "✗" else "✓"
            val tint = if (succeeded == false) OpenRockyPalette.warning else OpenRockyPalette.success
            Text(mark, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ToolEventRow(event: DelegateProgress.ToolEvent) {
    Row(
        modifier = Modifier.padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (val s = event.status) {
            is DelegateProgress.ToolEvent.Status.Running -> CircularProgressIndicator(
                modifier = Modifier.size(8.dp),
                color = OpenRockyPalette.muted,
                strokeWidth = 1.dp
            )
            is DelegateProgress.ToolEvent.Status.Completed -> Text(
                if (s.succeeded) "✓" else "✗",
                color = if (s.succeeded) OpenRockyPalette.success else OpenRockyPalette.warning,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(6.dp))
        // Tag the kind so the user can tell tool / skill / mcp apart at a
        // glance — mirrors iOS panel's [tool] / [skill] / [mcp] prefix.
        val tag = when {
            event.name.startsWith("mcp-") -> "mcp"
            event.isSkill -> "skill"
            else -> "tool"
        }
        Text(
            "[$tag] ${event.name}",
            fontSize = 11.sp,
            color = OpenRockyPalette.muted,
            modifier = Modifier.weight(1f)
        )
        event.elapsedSeconds?.let {
            Text(
                String.format(Locale.US, "%.1fs", it),
                fontSize = 10.sp,
                color = OpenRockyPalette.muted
            )
        }
    }
}
