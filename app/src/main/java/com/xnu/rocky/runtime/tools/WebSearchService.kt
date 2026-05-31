//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object WebSearchService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encoded"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext "No results"

            val results = mutableListOf<String>()
            val regex = Regex("""<a[^>]*class="result__a"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
            val snippetRegex = Regex("""<a[^>]*class="result__snippet"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)

            val titles = regex.findAll(body).toList()
            val snippets = snippetRegex.findAll(body).toList()

            for (i in 0 until minOf(5, titles.size)) {
                val title = titles[i].groupValues[1].replace(Regex("<[^>]+>"), "").trim()
                val snippet = snippets.getOrNull(i)?.groupValues?.get(1)?.replace(Regex("<[^>]+>"), "")?.trim() ?: ""
                results.add("${i + 1}. $title\n   $snippet")
            }

            if (results.isEmpty()) "No results found for: $query"
            else "Search results for \"$query\":\n\n${results.joinToString("\n\n")}"
        } catch (e: Exception) {
            "Search error: ${e.message}"
        }
    }
}
