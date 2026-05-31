//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//

package com.xnu.rocky.runtime.tools

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory ring buffer of the most recent notifications Rocky has observed via
 * [com.xnu.rocky.RockyNotificationListenerService]. Backs the `notifications-read` tool, so
 * the assistant can answer "what did I miss?" or summarize unread messages per-app.
 *
 * Deliberately process-local: we do NOT persist notification content to disk.
 */
object NotificationInboxStore {
    private const val MAX_ENTRIES = 64

    data class Entry(
        val key: String,
        val packageName: String,
        val appLabel: String?,
        val title: String,
        val text: String,
        val subText: String?,
        val postedAtMs: Long
    )

    private val buffer = CopyOnWriteArrayList<Entry>()

    private val _granted = MutableStateFlow(false)
    /** True when the NotificationListenerService is bound by the system (user granted access). */
    val granted: StateFlow<Boolean> = _granted.asStateFlow()

    fun setGranted(value: Boolean) { _granted.value = value }

    fun record(entry: Entry) {
        // Dedup by key: if an app updates its notification, replace in-place.
        val existing = buffer.indexOfFirst { it.key == entry.key }
        if (existing >= 0) buffer[existing] = entry else buffer.add(entry)
        // Trim oldest.
        while (buffer.size > MAX_ENTRIES) buffer.removeAt(0)
    }

    fun remove(key: String) {
        buffer.removeAll { it.key == key }
    }

    /** Most-recent-first snapshot. */
    fun snapshot(limit: Int = MAX_ENTRIES, packageFilter: String? = null): List<Entry> {
        val filtered = if (packageFilter != null) buffer.filter { it.packageName == packageFilter } else buffer.toList()
        return filtered.sortedByDescending { it.postedAtMs }.take(limit)
    }

    fun clear() {
        buffer.clear()
    }
}
