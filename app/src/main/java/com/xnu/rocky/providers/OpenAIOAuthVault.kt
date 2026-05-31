//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-11
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.providers

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object OpenAIOAuthVault {
    private const val TAG = "OpenAIOAuthVault"
    private const val KEY_PREFIX = "rocky.openai-oauth.account"
    private val json = Json { ignoreUnknownKeys = true }
    private val accountLocks = ConcurrentHashMap<String, Mutex>()

    fun credential(accountID: String): OpenAIOAuthCredential? {
        val raw = SecureStore.get(accountKey(accountID)) ?: return null
        return runCatching { json.decodeFromString<OpenAIOAuthCredential>(raw) }.getOrNull()
    }

    fun save(credential: OpenAIOAuthCredential) {
        val payload = runCatching { json.encodeToString(credential) }.getOrNull() ?: return
        SecureStore.set(accountKey(credential.accountID), payload)
    }

    fun remove(accountID: String) {
        SecureStore.delete(accountKey(accountID))
    }

    suspend fun resolvedAccessToken(rawCredential: String): String {
        val accountID = OpenAIOAuthService.accountIDFromAccessToken(rawCredential) ?: return rawCredential
        val lock = accountLocks.getOrPut(accountID) { Mutex() }
        return lock.withLock {
            val stored = credential(accountID) ?: return@withLock rawCredential
            val updated = OpenAIOAuthService.refreshIfNeeded(stored)
            if (updated != stored) {
                save(updated)
                Log.i(TAG, "Refreshed OpenAI OAuth access token for account=$accountID")
            }
            updated.accessToken
        }
    }

    private fun accountKey(accountID: String): String = "$KEY_PREFIX.$accountID"
}
