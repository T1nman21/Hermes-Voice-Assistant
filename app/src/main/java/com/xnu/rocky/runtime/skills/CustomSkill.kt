//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.skills

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CustomSkill(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val trigger: String = "",
    val prompt: String = "",
    val enabled: Boolean = true,
    val sourceUrl: String? = null
) {
    fun toMarkdown(): String = buildString {
        appendLine("---")
        appendLine("name: $name")
        appendLine("description: $description")
        appendLine("trigger: $trigger")
        appendLine("enabled: $enabled")
        appendLine("---")
        appendLine()
        append(prompt)
    }

    companion object {
        fun fromMarkdown(text: String): CustomSkill? {
            val lines = text.lines()
            if (lines.firstOrNull()?.trim() != "---") return null

            val endIdx = lines.drop(1).indexOfFirst { it.trim() == "---" }
            if (endIdx < 0) return null

            val frontmatter = lines.subList(1, endIdx + 1)
            val content = lines.drop(endIdx + 2).joinToString("\n").trim()

            var name = ""
            var description = ""
            var trigger = ""
            var enabled = true

            for (line in frontmatter) {
                val (key, value) = line.split(":", limit = 2).map { it.trim() }
                when (key) {
                    "name" -> name = value
                    "description" -> description = value
                    "trigger" -> trigger = value
                    "enabled" -> enabled = value.toBoolean()
                }
            }

            return CustomSkill(name = name, description = description, trigger = trigger, prompt = content, enabled = enabled)
        }

        fun sanitizedToolName(name: String): String {
            return "skill-" + name.lowercase().replace(Regex("[^a-z0-9]"), "-").trim('-')
        }
    }
}
