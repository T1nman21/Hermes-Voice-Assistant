//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.skills

import android.content.Context
import com.xnu.rocky.providers.ToolDefinition
import com.xnu.rocky.providers.ToolFunctionDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File

class CustomSkillStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dir: File get() = File(context.filesDir, "OpenRockyCustomSkills").also { it.mkdirs() }

    private val _skills = MutableStateFlow<List<CustomSkill>>(emptyList())
    val skills: StateFlow<List<CustomSkill>> = _skills.asStateFlow()

    init {
        load()
        seedBuiltInsIfNeeded()
    }

    fun save(skill: CustomSkill) {
        val file = File(dir, "${skill.id}.json")
        file.writeText(json.encodeToString(skill))
        val list = _skills.value.toMutableList()
        val idx = list.indexOfFirst { it.id == skill.id }
        if (idx >= 0) list[idx] = skill else list.add(skill)
        _skills.value = list
    }

    fun delete(id: String) {
        File(dir, "$id.json").delete()
        _skills.value = _skills.value.filter { it.id != id }
    }

    fun toggleEnabled(id: String) {
        val skill = _skills.value.find { it.id == id } ?: return
        save(skill.copy(enabled = !skill.enabled))
    }

    fun toolDefinitions(): List<ToolDefinition> {
        return _skills.value.filter { it.enabled }.map { skill ->
            ToolDefinition(function = ToolFunctionDef(
                name = CustomSkill.sanitizedToolName(skill.name),
                description = skill.description,
                parameters = buildJsonObject {
                    put("type", JsonPrimitive("object"))
                    put("properties", buildJsonObject {
                        put("input", buildJsonObject {
                            put("type", JsonPrimitive("string"))
                            put("description", JsonPrimitive("User input for this skill"))
                        })
                    })
                }
            ))
        }
    }

    fun skillPromptForTool(toolName: String): String? {
        return _skills.value.find { CustomSkill.sanitizedToolName(it.name) == toolName }?.prompt
    }

    suspend fun importFromUrl(url: String): Result<CustomSkill> = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val text = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val skill = CustomSkill.fromMarkdown(text) ?: return@withContext Result.failure(Exception("Invalid skill format"))
            save(skill)
            Result.success(skill)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromGitHubRepo(urlString: String): Int = withContext(Dispatchers.IO) {
        val (owner, repo) = parseGitHubURL(urlString)
        val client = okhttp3.OkHttpClient()

        // Fetch repo contents
        val apiUrl = "https://api.github.com/repos/$owner/$repo/contents"
        val request = okhttp3.Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/vnd.github.v3+json")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("GitHub API error: ${response.code}")

        val body = response.body?.string() ?: throw Exception("Empty response")
        val items = Json.parseToJsonElement(body).jsonArray

        val dirs = items.filter {
            val obj = it.jsonObject
            obj["type"]?.jsonPrimitive?.contentOrNull == "dir" &&
            obj["name"]?.jsonPrimitive?.contentOrNull != ".github"
        }

        var importedCount = 0
        for (dir in dirs) {
            val dirName = dir.jsonObject["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val skillUrl = "https://raw.githubusercontent.com/$owner/$repo/main/$dirName/SKILL.md"

            try {
                val skillRequest = okhttp3.Request.Builder().url(skillUrl).build()
                val skillResponse = client.newCall(skillRequest).execute()
                val content = skillResponse.body?.string() ?: continue

                val parsed = CustomSkill.fromMarkdown(content) ?: continue
                // Skip if already exists by name
                if (_skills.value.any { it.name == parsed.name }) continue

                val skill = parsed.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    sourceUrl = "https://github.com/$owner/$repo/tree/main/$dirName"
                )
                save(skill)
                importedCount++
            } catch (_: Exception) {
                continue
            }
        }

        importedCount
    }

    private fun parseGitHubURL(urlString: String): Pair<String, String> {
        // Parse https://github.com/owner/repo or github.com/owner/repo
        val cleaned = urlString.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("github.com/")
            .removeSuffix("/")
            .removeSuffix(".git")
        val parts = cleaned.split("/")
        if (parts.size < 2) throw Exception("Invalid GitHub URL: expected github.com/owner/repo")
        return parts[0] to parts[1]
    }

    fun exportSkill(id: String): String? {
        return _skills.value.find { it.id == id }?.toMarkdown()
    }

    /** Seeds built-in skills the first time, and again whenever [BuiltInSkills.version]
     *  is bumped — adding any new built-ins without touching user-edited skills.
     *  Mirrors iOS `seedBuiltInSkillsIfNeeded`. Skills are matched by name, so a
     *  user who renamed/edited a built-in keeps their version. */
    private fun seedBuiltInsIfNeeded() {
        val prefs = context.getSharedPreferences("openrocky_prefs", Context.MODE_PRIVATE)
        val current = prefs.getInt(BUILT_IN_VERSION_KEY, 0)
        if (current >= BuiltInSkills.version) return

        for (skill in BuiltInSkills.all) {
            if (_skills.value.any { it.name == skill.name }) continue
            save(skill)
        }
        prefs.edit().putInt(BUILT_IN_VERSION_KEY, BuiltInSkills.version).apply()
    }

    companion object {
        private const val BUILT_IN_VERSION_KEY = "built_in_skills_version"
    }

    private fun load() {
        val list = mutableListOf<CustomSkill>()
        dir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            try {
                list.add(json.decodeFromString<CustomSkill>(file.readText()))
            } catch (_: Exception) {}
        }
        _skills.value = list
    }
}
