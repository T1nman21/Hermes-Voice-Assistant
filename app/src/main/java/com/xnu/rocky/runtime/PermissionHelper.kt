//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun hasLocation(context: Context): Boolean =
        has(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasContacts(context: Context): Boolean =
        has(context, Manifest.permission.READ_CONTACTS)

    fun hasCalendar(context: Context): Boolean =
        has(context, Manifest.permission.READ_CALENDAR)

    fun hasCalendarWrite(context: Context): Boolean =
        has(context, Manifest.permission.WRITE_CALENDAR)

    fun hasMicrophone(context: Context): Boolean =
        has(context, Manifest.permission.RECORD_AUDIO)

    fun hasCamera(context: Context): Boolean =
        has(context, Manifest.permission.CAMERA)

    fun hasNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            has(context, Manifest.permission.POST_NOTIFICATIONS)
        } else true
    }

    fun allRequiredPermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.toTypedArray()
    }

    fun missingPermissions(context: Context): List<String> {
        return allRequiredPermissions().filter { !has(context, it) }
    }

    private fun has(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
