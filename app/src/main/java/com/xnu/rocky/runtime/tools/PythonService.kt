//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import com.chaquo.python.Python
import com.xnu.rocky.runtime.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class PythonResult(
    val success: Boolean,
    val output: String,
    val error: String?
)

class PythonService {
    private val json = Json { ignoreUnknownKeys = true }

    fun isAvailable(): Boolean = Python.isStarted()

    fun version(): String? {
        return try {
            val py = Python.getInstance()
            val module = py.getModule("openrocky_exec")
            module.callAttr("version").toString()
        } catch (e: Exception) {
            LogManager.error("Python version check failed: ${e.message}", "Python")
            null
        }
    }

    suspend fun execute(code: String): PythonResult = withContext(Dispatchers.IO) {
        LogManager.info("Python exec: ${code.take(120)}", "Python")
        try {
            val py = Python.getInstance()
            val module = py.getModule("openrocky_exec")
            val resultJson = module.callAttr("execute", code).toString()

            val parsed = json.decodeFromString<PythonResultDto>(resultJson)
            PythonResult(
                success = parsed.success,
                output = parsed.output.ifBlank { "(no output)" },
                error = parsed.error
            )
        } catch (e: Exception) {
            LogManager.error("Python execution failed: ${e.message}", "Python")
            PythonResult(
                success = false,
                output = "",
                error = "Python error: ${e.message}"
            )
        }
    }

    @Serializable
    private data class PythonResultDto(
        val success: Boolean = false,
        val output: String = "",
        val error: String? = null
    )
}
