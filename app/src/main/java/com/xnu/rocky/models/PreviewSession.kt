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
import com.xnu.rocky.ui.theme.OpenRockyPalette

data class OpenRockySession(
    val mode: SessionMode = SessionMode.READY,
    val liveTranscript: String = "",
    val assistantReply: String = "",
    val eta: String = "",
    val provider: ProviderStatus = ProviderStatus(),
    val plan: List<PlanStep> = emptyList(),
    val timeline: List<TimelineEntry> = emptyList(),
    val quickTasks: List<QuickTask> = emptyList(),
    val capabilityGroups: List<CapabilityGroup> = emptyList(),
    val artifactCount: Int = 0,
    val sessionTag: String = "New"
)

object PreviewSession {
    fun sample(): OpenRockySession = OpenRockySession(
        mode = SessionMode.EXECUTING,
        liveTranscript = "Set an alarm for 6 AM and check the weather",
        assistantReply = "I've set your alarm for 6:00 AM. Currently checking weather conditions for your area…",
        eta = "~4 s",
        provider = ProviderStatus(name = "OpenAI", model = "gpt-4o", isConnected = true),
        plan = listOf(
            PlanStep(title = "Parse Intent", detail = "Extracted: alarm + weather", state = PlanStepState.DONE),
            PlanStep(title = "Create Alarm", detail = "06:00 via AlarmManager", state = PlanStepState.DONE),
            PlanStep(title = "Fetch Weather", detail = "Open-Meteo API call…", state = PlanStepState.ACTIVE),
            PlanStep(title = "Compose Reply", detail = "Summarise results", state = PlanStepState.QUEUED),
        ),
        timeline = listOf(
            TimelineEntry(kind = TimelineKind.SPEECH, time = "09:41", text = "Set an alarm for 6 AM and check the weather"),
            TimelineEntry(kind = TimelineKind.TOOL, time = "09:41", text = "alarm-create → 06:00"),
            TimelineEntry(kind = TimelineKind.TOOL, time = "09:42", text = "weather → fetching…"),
            TimelineEntry(kind = TimelineKind.SYSTEM, time = "09:42", text = "Plan step 3/4 active"),
        ),
        quickTasks = listOf(
            QuickTask(title = "Weather", prompt = "What's the weather like?", symbol = Icons.Default.WbSunny, tint = OpenRockyPalette.warning),
            QuickTask(title = "Reminders", prompt = "Show my reminders", symbol = Icons.Default.Checklist, tint = OpenRockyPalette.success),
            QuickTask(title = "Translate", prompt = "Translate to Spanish", symbol = Icons.Default.Translate, tint = OpenRockyPalette.accentBrand),
            QuickTask(title = "Summarize", prompt = "Summarize this page", symbol = Icons.Default.Summarize, tint = OpenRockyPalette.secondaryBrand),
        ),
        capabilityGroups = listOf(
            CapabilityGroup(
                title = "Android Native Bridge",
                status = "Active",
                summary = "Location, Calendar, Contacts, Notifications",
                items = listOf("android-location", "android-calendar", "android-contacts", "android-notifications", "android-alarm"),
                tint = OpenRockyPalette.accentBrand
            ),
            CapabilityGroup(
                title = "AI Tool Layer",
                status = "Active",
                summary = "Memory, Todo, Weather, Web Search",
                items = listOf("memory_get", "memory_write", "todo", "weather", "web-search"),
                tint = OpenRockyPalette.secondaryBrand
            ),
            CapabilityGroup(
                title = "Platform Integrations",
                status = "Ready",
                summary = "Browser, File I/O, Crypto",
                items = listOf("browser-read", "file-read", "file-write", "crypto"),
                tint = OpenRockyPalette.success
            ),
        ),
        artifactCount = 2,
        sessionTag = "voice-0941"
    )

    fun liveSeed(): OpenRockySession = OpenRockySession(
        mode = SessionMode.READY,
        quickTasks = listOf(
            // Row 1
            QuickTask(title = "What Can You Do?", prompt = "What can you do? Show me your capabilities.", symbol = Icons.Default.AutoAwesome, tint = OpenRockyPalette.accentBrand),
            QuickTask(title = "Where Am I", prompt = "Where am I right now?", symbol = Icons.Default.MyLocation, tint = OpenRockyPalette.success),
            // Row 2
            QuickTask(title = "Weather", prompt = "What's the weather like right now?", symbol = Icons.Default.WbSunny, tint = OpenRockyPalette.warning),
            QuickTask(title = "Today's Events", prompt = "What's on my calendar today?", symbol = Icons.Default.CalendarToday, tint = OpenRockyPalette.secondaryBrand),
        ),
    )
}
