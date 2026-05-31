//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.content.Context

data class ToolInfo(
    val name: String,
    val description: String,
    val group: String
)

class BuiltInToolStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("openrocky_tools", Context.MODE_PRIVATE)

    val allTools = listOf(
        // Location & Weather
        ToolInfo("android-location", "Get current GPS location", "Location"),
        ToolInfo("android-geocode", "Convert address to coordinates", "Location"),
        ToolInfo("weather", "Current weather and forecast", "Location"),
        ToolInfo("nearby-search", "Search nearby places and POIs", "Location"),
        // Memory
        ToolInfo("memory_get", "Read persistent memory", "Memory"),
        ToolInfo("memory_write", "Write persistent memory", "Memory"),
        // Files & Shell
        ToolInfo("file-read", "Read workspace files", "Files"),
        ToolInfo("file-write", "Write workspace files", "Files"),
        ToolInfo("shell-execute", "Execute shell commands", "Files"),
        ToolInfo("python-execute", "Execute Python code", "Files"),
        // Media
        ToolInfo("camera-capture", "Take photo with camera", "Media"),
        ToolInfo("photo-pick", "Pick photo from gallery", "Media"),
        ToolInfo("file-pick", "Pick file from storage", "Media"),
        // Productivity
        ToolInfo("todo", "Manage todo list", "Productivity"),
        ToolInfo("android-alarm", "Set device alarm", "Productivity"),
        ToolInfo("notification-schedule", "Schedule notifications", "Productivity"),
        // Calendar & Reminders
        ToolInfo("android-calendar-list", "List calendar events", "Calendar"),
        ToolInfo("android-calendar-create", "Create calendar events", "Calendar"),
        ToolInfo("android-reminder-list", "List upcoming reminders", "Calendar"),
        ToolInfo("android-reminder-create", "Create reminders with alerts", "Calendar"),
        // Contacts
        ToolInfo("android-contacts-search", "Search contacts", "Contacts"),
        // Browser & Web
        ToolInfo("browser-open", "Open URL in browser", "Browser"),
        ToolInfo("browser-read", "Read web page content", "Browser"),
        ToolInfo("web-search", "Search the web", "Web"),
        ToolInfo("open-url", "Open URLs and deep links", "Web"),
        // System
        ToolInfo("crypto", "Cryptographic operations", "System"),
        ToolInfo("oauth-authenticate", "OAuth authentication flow", "System"),
        ToolInfo("delegate-task", "Delegate complex tasks to background subagents", "System"),
        ToolInfo("email-send", "Compose and send email", "System"),
        ToolInfo("app-exit", "Exit the application", "System"),
    )

    val groups: List<String> get() = allTools.map { it.group }.distinct()

    fun toolsInGroup(group: String): List<ToolInfo> = allTools.filter { it.group == group }

    fun isEnabled(name: String): Boolean {
        val disabledSet = prefs.getStringSet("disabled_tools", emptySet()) ?: emptySet()
        return name !in disabledSet
    }

    fun setEnabled(name: String, enabled: Boolean) {
        val disabledSet = (prefs.getStringSet("disabled_tools", emptySet()) ?: emptySet()).toMutableSet()
        if (enabled) disabledSet.remove(name) else disabledSet.add(name)
        prefs.edit().putStringSet("disabled_tools", disabledSet).apply()
    }
}
