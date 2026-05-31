//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//

package com.xnu.rocky

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.xnu.rocky.runtime.ConversationMetadata

/**
 * Publishes the three most recent conversations as *dynamic* launcher shortcuts, rendered
 * under the three static shortcuts declared in [R.xml.shortcuts] when the user long-presses
 * Rocky's launcher icon. Tapping one re-opens that conversation directly.
 *
 * Dynamic shortcuts are an Android launcher feature with no iOS equivalent (3D Touch
 * quick-actions were also only static).
 */
object RecentConversationsShortcuts {

    private const val MAX = 3
    private const val PREFIX = "convo_"

    fun refresh(context: Context, conversations: List<ConversationMetadata>) {
        val top = conversations
            .sortedByDescending { it.updatedAt }
            .take(MAX)

        val target = ComponentName(context, MainActivity::class.java)
        val shortcuts = top.mapIndexed { index, c ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                component = target
                action = ACTION_OPEN_CONVERSATION
                putExtra(EXTRA_CONVERSATION_ID, c.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val label = c.title.ifBlank { "Conversation" }.take(40)
            ShortcutInfoCompat.Builder(context, "$PREFIX${c.id}")
                .setShortLabel(label)
                .setLongLabel(label)
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_tile_voice))
                .setIntent(intent)
                .setRank(index)
                .build()
        }

        // Replace prior dynamic shortcuts atomically so stale entries can't leak.
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }
}
