//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-05-04
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.voice

import com.xnu.rocky.runtime.ConversationMessage

/**
 * One pre-existing turn replayed into a fresh realtime session so the model
 * can continue an in-progress conversation. Text-only — tool traces and audio
 * don't help the model and would just inflate the input tokens. Mirrors iOS
 * `OpenRockyVoicePrimingItem`.
 */
data class VoicePrimingItem(val role: Role, val text: String) {
    enum class Role(val wireValue: String) {
        USER("user"),
        ASSISTANT("assistant")
    }
}

/**
 * Distills a stored conversation into a compact list of items that can be
 * replayed into a fresh realtime session via `conversation.item.create` so the
 * model can continue where the user left off (instead of starting blind).
 *
 * Tool-call / tool-result rows are deliberately dropped: the realtime model
 * can't act on a replayed tool result, and the costs of including them
 * (extra input tokens, confused turn boundaries) outweigh the value. Mirrors
 * iOS `OpenRockyVoicePriming`.
 */
object VoicePriming {
    /**
     * Caps tuned to keep priming under ~5–10s of latency on the
     * session.update round-trip and well under the realtime input budget.
     * These are local heuristics, not server limits.
     */
    const val MAX_ITEM_COUNT = 12
    const val MAX_TOTAL_CHARACTERS = 4000
    const val MAX_PER_ITEM_CHARACTERS = 1500

    /**
     * Build priming items from the most recent N messages of a conversation.
     * Order is preserved (oldest → newest within the kept window) so the
     * model sees the conversation in chronological order.
     */
    fun items(messages: List<ConversationMessage>): List<VoicePrimingItem> {
        // Project each message to a priming item; tool_call / tool_result
        // rows and empty text drop out here.
        val candidates: List<VoicePrimingItem> = messages.mapNotNull { message ->
            val role = primingRole(message.role) ?: return@mapNotNull null
            val text = message.content.trim()
            if (text.isEmpty()) null else VoicePrimingItem(role, text)
        }
        return capped(candidates)
    }

    /**
     * Apply the same count/character budgets to a pre-built item list. Used on
     * reconnect to compress (initial priming + in-session turns) back into the
     * budget — without it a long live conversation would balloon the replay.
     */
    fun capped(items: List<VoicePrimingItem>): List<VoicePrimingItem> {
        if (items.isEmpty()) return emptyList()

        // Keep the tail (most-recent), preserving chronological order.
        val tail = items.takeLast(MAX_ITEM_COUNT)

        // Walk newest → oldest accumulating into the budget; reverse at the
        // end so the final list stays oldest → newest.
        val kept = mutableListOf<VoicePrimingItem>()
        var remainingBudget = MAX_TOTAL_CHARACTERS
        for (item in tail.asReversed()) {
            if (remainingBudget <= 0) break
            val truncated = truncate(item.text, kotlin.math.min(MAX_PER_ITEM_CHARACTERS, remainingBudget))
            if (truncated.isEmpty()) break
            kept.add(VoicePrimingItem(item.role, truncated))
            remainingBudget -= truncated.length
        }
        return kept.asReversed()
    }

    private fun primingRole(role: String): VoicePrimingItem.Role? = when (role) {
        "user" -> VoicePrimingItem.Role.USER
        "assistant" -> VoicePrimingItem.Role.ASSISTANT
        else -> null
    }

    private fun truncate(text: String, max: Int): String {
        if (text.length <= max) return text
        if (max <= 1) return ""
        // Use the leading prefix and add an ellipsis to mark the cut so the
        // model knows the turn was clipped, not that the user trailed off.
        return text.take(max - 1) + "…"
    }
}
