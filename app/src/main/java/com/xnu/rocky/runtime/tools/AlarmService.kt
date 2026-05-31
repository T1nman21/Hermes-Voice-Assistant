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
import android.content.Intent
import android.provider.AlarmClock
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object AlarmService {
    /**
     * Set a device alarm. [time] accepts either:
     *   - `HH:mm` — legacy shorthand
     *   - ISO-8601 datetime (`2026-04-23T15:30`, with or without seconds / offset) — matches iOS `apple-alarm`
     *
     * Android's `AlarmClock.ACTION_SET_ALARM` only exposes hour + minute, so the date portion of an
     * ISO-8601 value is acknowledged but not enforced (the clock app picks the next occurrence).
     */
    fun setAlarm(context: Context, time: String, title: String = ""): String {
        return try {
            val (hour, minute) = parseHourMinute(time)
                ?: return "Failed to set alarm: could not parse time '$time' (expected HH:mm or ISO-8601)."

            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                if (title.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, title)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val label = if (title.isNotBlank()) " ($title)" else ""
            "Alarm set for %02d:%02d%s".format(hour, minute, label)
        } catch (e: Exception) {
            "Failed to set alarm: ${e.message}"
        }
    }

    private fun parseHourMinute(raw: String): Pair<Int, Int>? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // Try ISO-8601 forms first (with offset, with local datetime, or bare time).
        runCatching { OffsetDateTime.parse(trimmed) }.getOrNull()?.let { return it.hour to it.minute }
        runCatching { ZonedDateTime.parse(trimmed) }.getOrNull()?.let { return it.hour to it.minute }
        runCatching { LocalDateTime.parse(trimmed) }.getOrNull()?.let { return it.hour to it.minute }
        runCatching { LocalTime.parse(trimmed) }.getOrNull()?.let { return it.hour to it.minute }
        runCatching { LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("H:mm")) }
            .getOrNull()?.let { return it.hour to it.minute }

        // Final fallback: split on ':' for loose HH:mm input ("7:5").
        val parts = trimmed.split(":")
        val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }
}
