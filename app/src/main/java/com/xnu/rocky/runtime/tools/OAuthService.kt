//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object OAuthService {
    fun authenticate(context: Context, authUrl: String): String {
        return try {
            val intent = CustomTabsIntent.Builder().build()
            intent.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.launchUrl(context, Uri.parse(authUrl))
            "OAuth flow started. Complete authentication in the browser."
        } catch (e: Exception) {
            "OAuth error: ${e.message}"
        }
    }
}
