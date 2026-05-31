//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-14
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lightweight wrapper around SharedPreferences for app-wide preferences. */
class Preferences(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "openrocky_prefs"
        private const val KEY_AUTO_COMPRESS_HISTORY = "auto_compress_history"
        private const val KEY_VOICE_AUTO_START_ON_LAUNCH = "voice_auto_start_on_launch"
    }

    private val prefs get() = context.getSharedPreferences(PREFS_NAME, 0)

    private val _autoCompressHistory = MutableStateFlow(prefs.getBoolean(KEY_AUTO_COMPRESS_HISTORY, true))
    val autoCompressHistory: StateFlow<Boolean> = _autoCompressHistory.asStateFlow()

    fun setAutoCompressHistory(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COMPRESS_HISTORY, enabled).apply()
        _autoCompressHistory.value = enabled
    }

    /** Whether to enter voice mode automatically on a normal cold launch.
     *  External voice launches (assist intent / shortcut) always auto-enter
     *  regardless of this setting. Mirrors iOS `voiceAutoStartOnLaunch`. */
    private val _voiceAutoStartOnLaunch = MutableStateFlow(prefs.getBoolean(KEY_VOICE_AUTO_START_ON_LAUNCH, true))
    val voiceAutoStartOnLaunch: StateFlow<Boolean> = _voiceAutoStartOnLaunch.asStateFlow()

    fun setVoiceAutoStartOnLaunch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_AUTO_START_ON_LAUNCH, enabled).apply()
        _voiceAutoStartOnLaunch.value = enabled
    }
}
