//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.xnu.rocky.runtime.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReminderService(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun listReminders(daysAhead: Int = 7): String = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasCalendar(context)) {
            return@withContext "Calendar permission not granted."
        }
        try {
            // On Android, reminders are calendar events with reminders attached
            val now = System.currentTimeMillis()
            val end = now + daysAhead * 86400000L
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.HAS_ALARM
            )
            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.HAS_ALARM} = 1"
            val selectionArgs = arrayOf(now.toString(), end.toString())

            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection, selection, selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            val reminders = mutableListOf<String>()
            val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

            cursor?.use {
                while (it.moveToNext()) {
                    val title = it.getString(1) ?: "Untitled"
                    val start = it.getLong(2)
                    reminders.add("• ${dateFormat.format(Date(start))} - $title")
                }
            }

            if (reminders.isEmpty()) "No reminders in the next $daysAhead days."
            else "Upcoming reminders:\n${reminders.joinToString("\n")}"
        } catch (e: Exception) {
            "Reminder error: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun createReminder(title: String, dateStr: String, notes: String? = null): String = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasCalendarWrite(context)) {
            return@withContext "Calendar write permission not granted."
        }
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val startMillis = dateFormat.parse(dateStr)?.time ?: return@withContext "Invalid date format"

            // Get default calendar
            val calProjection = arrayOf(CalendarContract.Calendars._ID)
            val calCursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI, calProjection, null, null, null
            )
            val calId = calCursor?.use { if (it.moveToFirst()) it.getLong(0) else null }
                ?: return@withContext "No calendar found"

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, notes ?: "")
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, startMillis + 3600000)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 1)
            }

            val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = eventUri?.lastPathSegment?.toLongOrNull()

            if (eventId != null) {
                // Add a reminder alert
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, 10)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }

            "Reminder created: $title"
        } catch (e: Exception) {
            "Failed to create reminder: ${e.message}"
        }
    }
}
