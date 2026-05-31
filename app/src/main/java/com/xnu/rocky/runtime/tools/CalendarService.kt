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

class CalendarService(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun listEvents(daysAhead: Int = 7): String = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasCalendar(context)) {
            return@withContext "Calendar permission not granted. Please enable calendar access in Settings."
        }
        try {
            val now = System.currentTimeMillis()
            val end = now + daysAhead * 86400000L
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_LOCATION
            )
            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
            val selectionArgs = arrayOf(now.toString(), end.toString())

            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection, selection, selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            val events = mutableListOf<String>()
            val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

            cursor?.use {
                while (it.moveToNext()) {
                    val title = it.getString(1) ?: "Untitled"
                    val start = it.getLong(2)
                    val location = it.getString(4) ?: ""
                    val dateStr = dateFormat.format(Date(start))
                    val locationStr = if (location.isNotBlank()) " @ $location" else ""
                    events.add("• $dateStr - $title$locationStr")
                }
            }

            if (events.isEmpty()) "No events in the next $daysAhead days."
            else "Upcoming events:\n${events.joinToString("\n")}"
        } catch (e: Exception) {
            "Calendar error: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun createEvent(title: String, startDate: String, endDate: String?, location: String?): String = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasCalendarWrite(context)) {
            return@withContext "Calendar write permission not granted."
        }
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val startMillis = dateFormat.parse(startDate)?.time ?: return@withContext "Invalid start date"
            val endMillis = if (endDate != null) dateFormat.parse(endDate)?.time ?: (startMillis + 3600000) else startMillis + 3600000

            val calId = getDefaultCalendarId() ?: return@withContext "No calendar found"

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                if (location != null) put(CalendarContract.Events.EVENT_LOCATION, location)
            }

            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            "Event created: $title"
        } catch (e: Exception) {
            "Failed to create event: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDefaultCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection, null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        return null
    }
}
