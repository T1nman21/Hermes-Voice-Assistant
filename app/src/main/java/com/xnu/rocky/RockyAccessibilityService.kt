//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//

package com.xnu.rocky

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.lang.ref.WeakReference

/**
 * Opt-in accessibility service: gives Rocky the ability to read the text visible on the
 * currently-active window *on demand*. Nothing is logged or stored without the user asking
 * — the service exists purely so the `screen-read` tool can answer "what am I looking at?".
 *
 * iOS has no comparable user-grantable capability for third-party apps.
 */
class RockyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
    }

    override fun onDestroy() {
        if (instanceRef?.get() === this) instanceRef = null
        super.onDestroy()
    }

    // We intentionally do NOT cache per-event content here (privacy). When the tool is called we
    // pull the current tree on demand via [captureActiveWindow] below.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        private var instanceRef: WeakReference<RockyAccessibilityService>? = null

        /** True only when the user has enabled the service in system settings and it is bound. */
        fun isActive(): Boolean = instanceRef?.get() != null

        /**
         * Walk the active window's node tree and return the visible text. Returns `null` if the
         * service is not bound (user hasn't granted access yet).
         */
        fun captureActiveWindow(maxChars: Int = 4000): String? {
            val svc = instanceRef?.get() ?: return null
            val root = runCatching { svc.rootInActiveWindow }.getOrNull() ?: return ""
            val sb = StringBuilder()
            collect(root, sb, maxChars)
            return sb.toString()
        }

        private fun collect(node: AccessibilityNodeInfo?, sb: StringBuilder, maxChars: Int) {
            if (node == null || sb.length >= maxChars) return
            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()
            val combined = listOf(text, desc).filter { it.isNotEmpty() }.distinct().joinToString(" / ")
            if (combined.isNotEmpty()) {
                if (sb.isNotEmpty()) sb.append('\n')
                sb.append(combined)
            }
            for (i in 0 until node.childCount) {
                collect(node.getChild(i), sb, maxChars)
                if (sb.length >= maxChars) {
                    sb.append("\n…(truncated)")
                    return
                }
            }
        }
    }
}
