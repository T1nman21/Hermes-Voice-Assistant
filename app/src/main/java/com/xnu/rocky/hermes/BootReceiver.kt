package com.xnu.rocky.hermes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restart the wake word service after device reboot.
 * Only starts if the user has completed onboarding and wake word is enabled.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("hermes_prefs", Context.MODE_PRIVATE)
        val onboarded = prefs.getBoolean("onboarded", false)
        val wakeEnabled = prefs.getBoolean("wake_word_enabled", true)

        if (onboarded && wakeEnabled) {
            WakeWordService.start(context)
        }
    }
}
