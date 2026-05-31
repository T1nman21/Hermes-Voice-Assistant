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
import android.webkit.CookieManager
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class BrowserService(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun openUrl(url: String): String {
        return try {
            val intent = CustomTabsIntent.Builder().build()
            intent.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.launchUrl(context, Uri.parse(url))
            "Opened in browser: $url"
        } catch (e: Exception) {
            // Fallback to regular intent
            try {
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                fallback.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(fallback)
                "Opened: $url"
            } catch (e2: Exception) {
                "Failed to open URL: ${e2.message}"
            }
        }
    }

    fun getCookies(domain: String): String {
        return try {
            val cookieManager = CookieManager.getInstance()
            val url = if (domain.startsWith("http")) domain else "https://$domain"
            val cookies = cookieManager.getCookie(url)
            if (cookies.isNullOrBlank()) {
                "No cookies found for $domain"
            } else {
                val parsed = cookies.split(";").map { it.trim() }.filter { it.isNotBlank() }
                buildString {
                    appendLine("Cookies for $domain (${parsed.size} cookies):")
                    parsed.forEach { cookie ->
                        val parts = cookie.split("=", limit = 2)
                        if (parts.size == 2) {
                            appendLine("  ${parts[0].trim()} = ${parts[1].trim()}")
                        } else {
                            appendLine("  $cookie")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            "Failed to get cookies: ${e.message}"
        }
    }

    suspend fun readContent(url: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext "Empty response"

            // Strip HTML tags for a text summary
            val text = body
                .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(4000)

            if (text.isBlank()) "Page loaded but no readable text content"
            else "Content from $url:\n\n$text"
        } catch (e: Exception) {
            "Failed to read URL: ${e.message}"
        }
    }
}
