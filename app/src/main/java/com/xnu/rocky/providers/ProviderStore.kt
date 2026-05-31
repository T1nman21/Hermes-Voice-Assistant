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
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ProviderStore(private val context: Context) {
    private companion object {
        const val TAG = "ProviderStore"
    }
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dir: File get() = File(context.filesDir, "OpenRockyProviders").also { it.mkdirs() }
    private val manifestFile: File get() = File(dir, "manifest.json")

    private val _instances = MutableStateFlow<List<ProviderInstance>>(emptyList())
    val instances: StateFlow<List<ProviderInstance>> = _instances.asStateFlow()

    private val _activeInstanceID = MutableStateFlow<String?>(null)
    val activeInstanceID: StateFlow<String?> = _activeInstanceID.asStateFlow()

    init {
        load()
        cleanupOrphanOpenAIOAuthVault()
    }

    val activeInstance: ProviderInstance?
        get() = _instances.value.find { it.id == _activeInstanceID.value }

    val activeConfiguration: ProviderConfiguration?
        get() {
            val instance = activeInstance ?: return null
            val manualCredential = SecureStore.get(instance.credentialKeychainKey).orEmpty()
            val oauthCredential = openAIOAuthCredential(instance)
            val credential = if (
                instance.kind == ProviderKind.OPENAI &&
                manualCredential.isBlank() &&
                oauthCredential != null
            ) {
                oauthCredential.accessToken
            } else {
                manualCredential
            }
            if (credential.isBlank()) return null
            return instance.toConfiguration(credential)
        }

    fun save(instance: ProviderInstance, credential: String) {
        SecureStore.set(instance.credentialKeychainKey, credential)
        val file = File(dir, "${instance.id}.json")
        file.writeText(json.encodeToString(instance))
        val list = _instances.value.toMutableList()
        val idx = list.indexOfFirst { it.id == instance.id }
        if (idx >= 0) list[idx] = instance else list.add(instance)
        _instances.value = list
        if (_activeInstanceID.value == null) {
            activate(instance.id)
        }
        saveManifest()
    }

    fun activate(id: String) {
        _activeInstanceID.value = id
        saveManifest()
    }

    fun delete(id: String) {
        SecureStore.delete("provider_credential_$id")
        SecureStore.delete(openAIOAuthKeychainKey(id))
        File(dir, "$id.json").delete()
        _instances.value = _instances.value.filter { it.id != id }
        if (_activeInstanceID.value == id) {
            _activeInstanceID.value = _instances.value.firstOrNull()?.id
        }
        cleanupOrphanOpenAIOAuthVault()
        saveManifest()
    }

    fun credentialFor(instance: ProviderInstance): String {
        return SecureStore.get(instance.credentialKeychainKey) ?: ""
    }

    fun openAIOAuthCredential(instance: ProviderInstance): OpenAIOAuthCredential? {
        val raw = SecureStore.get(openAIOAuthKeychainKey(instance.id)) ?: return null
        return runCatching { json.decodeFromString<OpenAIOAuthCredential>(raw) }.getOrNull()
    }

    fun setOpenAIOAuthCredential(credential: OpenAIOAuthCredential?, instanceID: String) {
        val key = openAIOAuthKeychainKey(instanceID)
        if (credential == null) {
            SecureStore.delete(key)
            cleanupOrphanOpenAIOAuthVault()
            return
        }
        val raw = runCatching { json.encodeToString(credential) }.getOrNull() ?: return
        SecureStore.set(key, raw)
        OpenAIOAuthVault.save(credential)
        cleanupOrphanOpenAIOAuthVault()
    }

    private fun load() {
        val list = mutableListOf<ProviderInstance>()
        dir.listFiles()?.filter { it.extension == "json" && it.name != "manifest.json" }?.forEach { file ->
            try {
                list.add(json.decodeFromString<ProviderInstance>(file.readText()))
            } catch (_: Exception) {}
        }
        _instances.value = list

        try {
            val manifest = json.decodeFromString<ManifestData>(manifestFile.readText())
            _activeInstanceID.value = manifest.activeInstanceID
        } catch (_: Exception) {
            _activeInstanceID.value = list.firstOrNull()?.id
        }
    }

    private fun saveManifest() {
        val data = ManifestData(activeInstanceID = _activeInstanceID.value)
        manifestFile.writeText(json.encodeToString(data))
    }

    private fun openAIOAuthKeychainKey(instanceID: String): String {
        return "rocky.provider-instance.$instanceID.openai-oauth"
    }

    private fun cleanupOrphanOpenAIOAuthVault() {
        val referencedAccounts = _instances.value.mapNotNull { instance ->
            openAIOAuthCredential(instance)?.accountID
        }.toSet()
        val prefix = "rocky.openai-oauth.account."
        val vaultKeys = SecureStore.keysWithPrefix(prefix)
        vaultKeys.forEach { key ->
            val accountID = key.removePrefix(prefix)
            if (accountID.isNotBlank() && !referencedAccounts.contains(accountID)) {
                OpenAIOAuthVault.remove(accountID)
                Log.i(TAG, "Removed orphan OpenAI OAuth vault entry for account=$accountID")
            }
        }
    }

    @kotlinx.serialization.Serializable
    private data class ManifestData(val activeInstanceID: String? = null)
}
