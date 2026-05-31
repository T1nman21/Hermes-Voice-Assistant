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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Single-instance Realtime provider store. The UI no longer surfaces add/list/delete — it edits
 * the active instance in place. We still keep the `instances`/`activeInstanceID` flows so existing
 * call sites (DebugPanelView, MainActivity collectors) compile, but [save] always writes back to
 * the same active slot.
 */
class RealtimeProviderStore(private val context: Context) {
    // coerceInputValues lets stored instances whose `kind` enum value has been removed
    // (e.g. legacy "GLM") fall back to the field default instead of crashing on load.
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; coerceInputValues = true }
    private val dir: File get() = File(context.filesDir, "OpenRockyRealtimeProviders").also { it.mkdirs() }
    private val manifestFile: File get() = File(dir, "manifest.json")

    private val _instances = MutableStateFlow<List<RealtimeProviderInstance>>(emptyList())
    val instances: StateFlow<List<RealtimeProviderInstance>> = _instances.asStateFlow()

    private val _activeInstanceID = MutableStateFlow<String?>(null)
    val activeInstanceID: StateFlow<String?> = _activeInstanceID.asStateFlow()

    init {
        load()
        ensureSingleInstance()
    }

    val activeInstance: RealtimeProviderInstance?
        get() = _instances.value.find { it.id == _activeInstanceID.value }

    val activeConfiguration: RealtimeProviderConfiguration?
        get() {
            val instance = activeInstance ?: return null
            val credential = SecureStore.get(instance.credentialKeychainKey) ?: return null
            if (credential.isBlank()) return null
            return instance.toConfiguration(credential)
        }

    /** Persist the active instance and credential. If no active instance exists yet, this one
     *  becomes the active one (and any leftover instances from older multi-instance state get pruned). */
    fun save(instance: RealtimeProviderInstance, credential: String) {
        SecureStore.set(instance.credentialKeychainKey, credential)
        File(dir, "${instance.id}.json").writeText(json.encodeToString(instance))
        val others = _instances.value.filter { it.id != instance.id }
        // Drop any stale extras left over from the multi-instance era.
        for (extra in others) {
            SecureStore.delete(extra.credentialKeychainKey)
            File(dir, "${extra.id}.json").delete()
        }
        _instances.value = listOf(instance)
        _activeInstanceID.value = instance.id
        saveManifest()
    }

    fun credentialFor(instance: RealtimeProviderInstance): String {
        return SecureStore.get(instance.credentialKeychainKey) ?: ""
    }

    private fun load() {
        val list = mutableListOf<RealtimeProviderInstance>()
        dir.listFiles()?.filter { it.extension == "json" && it.name != "manifest.json" }?.forEach { file ->
            try {
                list.add(json.decodeFromString<RealtimeProviderInstance>(file.readText()))
            } catch (_: Exception) {}
        }
        _instances.value = list

        try {
            val manifest = json.decodeFromString<ManifestData>(manifestFile.readText())
            _activeInstanceID.value = manifest.activeInstanceID ?: list.firstOrNull()?.id
        } catch (_: Exception) {
            _activeInstanceID.value = list.firstOrNull()?.id
        }
    }

    /** Collapse to a single instance: drop everything except the active (or first) one. */
    private fun ensureSingleInstance() {
        val list = _instances.value
        if (list.size <= 1) return
        val keepId = _activeInstanceID.value ?: list.first().id
        val keep = list.find { it.id == keepId } ?: list.first()
        for (extra in list) {
            if (extra.id == keep.id) continue
            SecureStore.delete(extra.credentialKeychainKey)
            File(dir, "${extra.id}.json").delete()
        }
        _instances.value = listOf(keep)
        _activeInstanceID.value = keep.id
        saveManifest()
    }

    private fun saveManifest() {
        val data = ManifestData(activeInstanceID = _activeInstanceID.value)
        manifestFile.writeText(json.encodeToString(data))
    }

    @kotlinx.serialization.Serializable
    private data class ManifestData(val activeInstanceID: String? = null)
}
