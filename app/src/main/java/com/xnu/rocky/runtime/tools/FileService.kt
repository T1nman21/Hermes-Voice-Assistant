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
import java.io.File

object FileService {
    private fun workspaceDir(context: Context): File {
        return File(context.filesDir, "OpenRockyWorkspace").also { it.mkdirs() }
    }

    fun readFile(context: Context, path: String): String {
        return try {
            val file = File(workspaceDir(context), path)
            if (!file.exists()) return "File not found: $path"
            file.readText()
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    fun writeFile(context: Context, path: String, content: String): String {
        return try {
            val file = File(workspaceDir(context), path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            "Written ${content.length} bytes to $path"
        } catch (e: Exception) {
            "Error writing file: ${e.message}"
        }
    }

    fun listFiles(context: Context, path: String = ""): List<File> {
        val dir = if (path.isBlank()) workspaceDir(context) else File(workspaceDir(context), path)
        return dir.listFiles()?.sortedBy { it.name } ?: emptyList()
    }

    fun deleteFile(context: Context, path: String): String {
        return try {
            val file = File(workspaceDir(context), path)
            if (file.deleteRecursively()) "Deleted: $path" else "Failed to delete: $path"
        } catch (e: Exception) {
            "Error deleting file: ${e.message}"
        }
    }
}
