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
import android.content.Context
import android.provider.ContactsContract
import com.xnu.rocky.runtime.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsService(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasContacts(context)) {
            return@withContext "Contacts permission not granted. Please enable contacts access in Settings."
        }
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            val contacts = mutableListOf<String>()
            cursor?.use {
                while (it.moveToNext() && contacts.size < 10) {
                    val name = it.getString(0) ?: "Unknown"
                    val phone = it.getString(1) ?: ""
                    contacts.add("$name: $phone")
                }
            }

            if (contacts.isEmpty()) "No contacts found for: $query"
            else "Found ${contacts.size} contacts:\n${contacts.joinToString("\n")}"
        } catch (e: Exception) {
            "Contacts error: ${e.message}"
        }
    }
}
