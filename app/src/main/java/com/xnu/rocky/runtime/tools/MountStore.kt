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
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class MountDefinition(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val uriString: String = "",
    val readWrite: Boolean = true,
    val displayPath: String = ""
) {
    fun toUri(): Uri = Uri.parse(uriString)
}

class MountStore(private val context: Context) {
    companion object {
        const val MAX_MOUNTS = 10
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val mountsFile: File
        get() = File(context.filesDir, "OpenRockyMounts/mounts.json").also { it.parentFile?.mkdirs() }

    private val _mounts = MutableStateFlow<List<MountDefinition>>(emptyList())
    val mounts: StateFlow<List<MountDefinition>> = _mounts.asStateFlow()

    init {
        load()
    }

    fun add(mount: MountDefinition) {
        if (_mounts.value.size >= MAX_MOUNTS) return
        val list = _mounts.value.toMutableList()
        list.add(mount)
        _mounts.value = list
        save()
    }

    fun delete(id: String) {
        val mount = _mounts.value.find { it.id == id } ?: return
        // Release persistable URI permission
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(mount.toUri(), flags)
        } catch (_: Exception) {}
        _mounts.value = _mounts.value.filter { it.id != id }
        save()
    }

    fun mount(name: String): MountDefinition? {
        return _mounts.value.find { it.name.equals(name, ignoreCase = true) }
    }

    fun createMount(name: String, uri: Uri, readWrite: Boolean): MountDefinition {
        // Take persistable permission so it survives app restart
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                (if (readWrite) android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
        context.contentResolver.takePersistableUriPermission(uri, flags)

        val docFile = DocumentFile.fromTreeUri(context, uri)
        val displayPath = docFile?.name ?: uri.lastPathSegment ?: uri.toString()

        return MountDefinition(
            name = name,
            uriString = uri.toString(),
            readWrite = readWrite,
            displayPath = displayPath
        )
    }

    fun isAccessible(mount: MountDefinition): Boolean {
        return try {
            val docFile = DocumentFile.fromTreeUri(context, mount.toUri())
            docFile?.exists() == true
        } catch (_: Exception) {
            false
        }
    }

    // -- File operations on mounts --

    fun listFiles(mount: MountDefinition, relativePath: String): String {
        val rootDoc = DocumentFile.fromTreeUri(context, mount.toUri())
            ?: return "Mount not accessible"

        val targetDoc = if (relativePath.isBlank() || relativePath == "/") {
            rootDoc
        } else {
            navigateToPath(rootDoc, relativePath) ?: return "Path not found: $relativePath"
        }

        if (!targetDoc.isDirectory) return "Not a directory: $relativePath"

        val entries = targetDoc.listFiles().take(200).map { file ->
            buildString {
                append(if (file.isDirectory) "[dir]  " else "[file] ")
                append(file.name ?: "?")
                if (file.isFile) append("  (${formatSize(file.length())})")
            }
        }

        return if (entries.isEmpty()) "Empty directory"
        else "Contents of ${mount.name}:/${relativePath.trimStart('/')} (${entries.size} items):\n${entries.joinToString("\n")}"
    }

    fun readFile(mount: MountDefinition, relativePath: String): String {
        val rootDoc = DocumentFile.fromTreeUri(context, mount.toUri())
            ?: return "Mount not accessible"

        val fileDoc = navigateToPath(rootDoc, relativePath)
            ?: return "File not found: $relativePath"

        if (!fileDoc.isFile) return "Not a file: $relativePath"

        return try {
            val uri = fileDoc.uri
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.size > 16000) {
                    String(bytes, 0, 16000, Charsets.UTF_8) + "\n\n[Truncated at 16KB]"
                } else {
                    String(bytes, Charsets.UTF_8)
                }
            } ?: "Failed to open file"
        } catch (e: Exception) {
            "Failed to read file: ${e.message}"
        }
    }

    fun writeFile(mount: MountDefinition, relativePath: String, content: String): String {
        if (!mount.readWrite) return "Mount '${mount.name}' is read-only"

        val rootDoc = DocumentFile.fromTreeUri(context, mount.toUri())
            ?: return "Mount not accessible"

        // Split path into directory and filename
        val parts = relativePath.trimStart('/').split("/")
        val fileName = parts.last()
        val dirParts = parts.dropLast(1)

        // Navigate/create directories
        var currentDoc = rootDoc
        for (dirName in dirParts) {
            val existing = currentDoc.findFile(dirName)
            currentDoc = if (existing != null && existing.isDirectory) {
                existing
            } else {
                currentDoc.createDirectory(dirName) ?: return "Failed to create directory: $dirName"
            }
        }

        // Find or create file
        val existingFile = currentDoc.findFile(fileName)
        val fileDoc = existingFile ?: currentDoc.createFile("application/octet-stream", fileName)
            ?: return "Failed to create file: $fileName"

        return try {
            context.contentResolver.openOutputStream(fileDoc.uri, "wt")?.use { stream ->
                val bytes = content.toByteArray(Charsets.UTF_8)
                stream.write(bytes)
                "Written ${bytes.size} bytes to ${mount.name}:/$relativePath"
            } ?: "Failed to open file for writing"
        } catch (e: Exception) {
            "Failed to write file: ${e.message}"
        }
    }

    // -- Internal helpers --

    private fun navigateToPath(root: DocumentFile, path: String): DocumentFile? {
        val parts = path.trimStart('/').split("/").filter { it.isNotBlank() }
        var current = root
        for (part in parts) {
            current = current.findFile(part) ?: return null
        }
        return current
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> "%.1fMB".format(bytes / (1024.0 * 1024.0))
    }

    private fun load() {
        try {
            if (mountsFile.exists()) {
                val data = mountsFile.readText()
                _mounts.value = json.decodeFromString<List<MountDefinition>>(data)
            }
        } catch (_: Exception) {}
    }

    private fun save() {
        try {
            mountsFile.parentFile?.mkdirs()
            mountsFile.writeText(json.encodeToString(_mounts.value))
        } catch (_: Exception) {}
    }
}
