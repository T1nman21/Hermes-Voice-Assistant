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

class MediaService(private val context: Context) {

    fun captureCamera(): String {
        // Camera capture requires Activity context and result callbacks
        // Return instruction for the UI layer to handle
        return "Camera capture requested. Please use the camera button in the app to take a photo."
    }

    fun pickPhoto(): String {
        return "Photo picker requested. Please use the photo button in the app to select an image."
    }

    fun pickFile(): String {
        return "File picker requested. Please use the file button in the app to select a file."
    }

    fun listWorkspaceMedia(): String {
        val dir = File(context.filesDir, "OpenRockyWorkspace")
        val mediaFiles = dir.listFiles()?.filter {
            it.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "webp", "mp4", "mp3", "pdf")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        if (mediaFiles.isEmpty()) return "No media files in workspace."

        return "Media files:\n" + mediaFiles.joinToString("\n") { file ->
            "• ${file.name} (${file.length() / 1024} KB)"
        }
    }
}
