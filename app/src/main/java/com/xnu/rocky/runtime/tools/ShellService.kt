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
import com.xnu.rocky.runtime.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

data class ShellResult(
    val command: String,
    val exitCode: Int,
    val output: String
)

class ShellService(private val context: Context) {
    private val workspaceDir: File by lazy {
        File(context.filesDir, "OpenRockyWorkspace").also { it.mkdirs() }
    }

    // Allowed commands — safe subset for sandbox execution
    private val allowedCommands = setOf(
        "ls", "cat", "echo", "pwd", "cp", "mv", "mkdir", "rm",
        "grep", "wc", "sort", "head", "tail", "find", "du", "df",
        "date", "whoami", "uname", "env", "printenv",
        "touch", "chmod", "basename", "dirname", "realpath",
        "tr", "cut", "paste", "uniq", "tee", "xargs",
        "md5sum", "sha256sum", "base64",
        "curl", "wget", "ping", "nslookup", "host",
        "tar", "gzip", "gunzip", "zip", "unzip",
        "sed", "awk", "diff", "comm",
        "python3", "python",
    )

    suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        LogManager.info("Shell exec: ${command.take(120)}", "Shell")

        // Parse first token to check if command is allowed
        val firstToken = command.trim().split("\\s+".toRegex()).firstOrNull() ?: ""
        val baseName = firstToken.substringAfterLast('/')

        if (baseName !in allowedCommands) {
            return@withContext ShellResult(
                command = command,
                exitCode = 1,
                output = "Command not allowed: $baseName. Allowed commands: ${allowedCommands.sorted().joinToString(", ")}"
            )
        }

        try {
            val process = ProcessBuilder("sh", "-c", command)
                .directory(workspaceDir)
                .redirectErrorStream(true)
                .start()

            val completed = process.waitFor(30, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return@withContext ShellResult(command = command, exitCode = 137, output = "Command timed out after 30s")
            }

            val output = process.inputStream.bufferedReader().readText()
                .trim()
                .take(8000)

            val exitCode = process.exitValue()

            if (exitCode != 0) {
                LogManager.warning("Shell exit $exitCode: ${command.take(80)}", "Shell")
            }

            ShellResult(
                command = command,
                exitCode = exitCode,
                output = output.ifEmpty { "(no output)" }
            )
        } catch (e: Exception) {
            LogManager.error("Shell error: ${e.message}", "Shell")
            ShellResult(command = command, exitCode = 1, output = "Error: ${e.message}")
        }
    }

    fun getWorkspacePath(): String = workspaceDir.absolutePath
}
