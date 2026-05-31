//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-13
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FFmpegService(private val context: Context) {

    private val workDir: File
        get() = File(context.filesDir, "OpenRockyWorkspace").also { it.mkdirs() }

    suspend fun execute(command: String): String = withContext(Dispatchers.IO) {
        try {
            // Resolve relative paths in the command against the workspace directory
            val resolvedCommand = resolveWorkspacePaths(command)

            // Try to run ffmpeg via the shell
            val process = ProcessBuilder("sh", "-c", "ffmpeg -y $resolvedCommand")
                .directory(workDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            when (exitCode) {
                0 -> {
                    if (output.isBlank()) "FFmpeg command completed successfully."
                    else "FFmpeg completed:\n${output.takeLast(2000)}"
                }
                127 -> "FFmpeg is not installed on this device. To use FFmpeg, install Termux and its ffmpeg package, or use python-execute with pydub for basic audio operations."
                else -> "FFmpeg failed (exit code $exitCode):\n${output.takeLast(2000)}"
            }
        } catch (e: Exception) {
            "FFmpeg error: ${e.message}. FFmpeg may not be available on this device."
        }
    }

    private fun resolveWorkspacePaths(command: String): String {
        // Resolve relative file paths (with extensions, not starting with -)
        return command.split(" ").joinToString(" ") { arg ->
            if (!arg.startsWith("-") && arg.contains(".") && !arg.startsWith("/")) {
                File(workDir, arg).absolutePath
            } else {
                arg
            }
        }
    }
}
