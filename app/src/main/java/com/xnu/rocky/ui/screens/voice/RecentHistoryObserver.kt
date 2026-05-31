//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.voice

import androidx.compose.runtime.*
import com.xnu.rocky.runtime.ConversationMessage
import com.xnu.rocky.runtime.PersistentStorageProvider
import kotlinx.coroutines.delay

/**
 * Polls persistent storage at 1 Hz for the most recent messages on the current conversation,
 * matching iOS `RecentHistoryObserver`. The 1Hz cadence is intentional — we don't want to thrash
 * the voice home recomposition on every assistant delta during a turn, and the strip is just
 * supporting context (the live turn renders separately from the session runtime).
 */
@Composable
fun rememberRecentMessages(
    storage: PersistentStorageProvider,
    conversationId: String?
): State<List<ConversationMessage>> {
    val state = remember { mutableStateOf<List<ConversationMessage>>(emptyList()) }
    LaunchedEffect(conversationId) {
        if (conversationId == null) {
            state.value = emptyList()
            return@LaunchedEffect
        }
        // Initial load is immediate; subsequent refreshes tick on the second so a long voice turn
        // doesn't cause the recent-history strip to redraw on every assistant delta.
        state.value = storage.loadMessages(conversationId)
        while (true) {
            delay(1_000)
            val fresh = storage.loadMessages(conversationId)
            if (fresh.size != state.value.size ||
                fresh.lastOrNull()?.id != state.value.lastOrNull()?.id
            ) {
                state.value = fresh
            }
        }
    }
    return state
}
