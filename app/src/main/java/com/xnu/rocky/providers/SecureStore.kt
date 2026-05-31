//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.providers

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureStore {
    private const val FILE_NAME = "openrocky_secure_prefs"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun get(key: String): String? = prefs?.getString(key, null)

    fun set(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    fun delete(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }

    fun keysWithPrefix(prefix: String): Set<String> {
        val all = prefs?.all?.keys ?: return emptySet()
        return all.filter { it.startsWith(prefix) }.toSet()
    }
}
