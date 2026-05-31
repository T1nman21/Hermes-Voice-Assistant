//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-21
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.content.Context
import android.media.MediaPlayer
import com.xnu.rocky.runtime.LogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class MediaPlaybackMode { SEQUENTIAL, LOOP, RANDOM, REPEAT_ONE }

data class MediaItem(
    val id: String,
    val path: String,
    val filename: String,
    val kind: Kind
) {
    enum class Kind { AUDIO, VIDEO }

    companion object {
        val audioExt = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus")
        val videoExt = setOf("mp4", "mov", "m4v", "mkv", "webm")

        fun from(file: File): MediaItem? {
            val ext = file.extension.lowercase()
            val kind = when {
                audioExt.contains(ext) -> Kind.AUDIO
                videoExt.contains(ext) -> Kind.VIDEO
                else -> return null
            }
            return MediaItem(
                id = file.absolutePath.hashCode().toString(),
                path = file.absolutePath,
                filename = file.name,
                kind = kind
            )
        }
    }
}

/**
 * Lightweight media player: scans a directory for audio/video and plays audio via MediaPlayer.
 * Video playback is reported as "not directly supported in tool" — the caller should hand off
 * the file path to the user for opening. Parallel of OpenRockyMediaPlayerService on iOS.
 */
class MediaPlayerService(private val context: Context) {
    companion object { private const val TAG = "MediaPlayer" }

    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlist: StateFlow<List<MediaItem>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow<Int?>(null)
    val currentIndex: StateFlow<Int?> = _currentIndex.asStateFlow()

    private val _mode = MutableStateFlow(MediaPlaybackMode.SEQUENTIAL)
    val mode: StateFlow<MediaPlaybackMode> = _mode.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var player: MediaPlayer? = null

    fun load(path: String, recursive: Boolean = true): Int {
        val dir = resolve(path)
        val items = scan(dir, recursive)
        _playlist.value = items
        _currentIndex.value = if (items.isEmpty()) null else 0
        LogManager.info("MediaPlayer loaded ${items.size} items from ${dir.absolutePath}", TAG)
        return items.size
    }

    fun setMode(mode: MediaPlaybackMode) { _mode.value = mode }

    fun play(index: Int? = null): MediaItem? {
        val items = _playlist.value
        if (items.isEmpty()) return null
        val idx = index ?: _currentIndex.value ?: 0
        if (idx !in items.indices) return null
        _currentIndex.value = idx
        val item = items[idx]
        if (item.kind == MediaItem.Kind.VIDEO) {
            LogManager.info("Video selected (${item.filename}); open externally", TAG)
            return item
        }
        stop()
        val mp = MediaPlayer().apply {
            setDataSource(item.path)
            setOnCompletionListener { advance() }
            prepare()
            start()
        }
        player = mp
        _isPlaying.value = true
        return item
    }

    fun pause() {
        player?.pause()
        _isPlaying.value = false
    }

    fun resume() {
        player?.start()
        _isPlaying.value = true
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        _isPlaying.value = false
    }

    fun next(): MediaItem? {
        val items = _playlist.value
        if (items.isEmpty()) return null
        val next = nextIndex()
        return play(next)
    }

    fun previous(): MediaItem? {
        val items = _playlist.value
        if (items.isEmpty()) return null
        val current = _currentIndex.value ?: 0
        val prev = if (current <= 0) items.size - 1 else current - 1
        return play(prev)
    }

    private fun advance() {
        val items = _playlist.value
        if (items.isEmpty()) { stop(); return }
        when (_mode.value) {
            MediaPlaybackMode.REPEAT_ONE -> play(_currentIndex.value ?: 0)
            MediaPlaybackMode.SEQUENTIAL -> {
                val next = (_currentIndex.value ?: 0) + 1
                if (next < items.size) play(next) else stop()
            }
            MediaPlaybackMode.LOOP -> play(nextIndex())
            MediaPlaybackMode.RANDOM -> play(items.indices.random())
        }
    }

    private fun nextIndex(): Int {
        val items = _playlist.value
        if (items.isEmpty()) return 0
        val current = _currentIndex.value ?: -1
        return when (_mode.value) {
            MediaPlaybackMode.RANDOM -> items.indices.random()
            MediaPlaybackMode.LOOP -> (current + 1) % items.size
            else -> (current + 1).coerceAtMost(items.size - 1)
        }
    }

    private fun resolve(path: String): File {
        val file = File(path)
        if (!file.isAbsolute) return File(context.filesDir, path)
        return file
    }

    private fun scan(root: File, recursive: Boolean): List<MediaItem> {
        if (!root.exists() || !root.isDirectory) return emptyList()
        val out = mutableListOf<MediaItem>()
        if (recursive) {
            root.walkTopDown()
                .filter { it.isFile }
                .mapNotNull(MediaItem::from)
                .forEach { out.add(it) }
        } else {
            root.listFiles()?.filter { it.isFile }?.mapNotNull(MediaItem::from)?.forEach { out.add(it) }
        }
        return out.sortedBy { it.filename.lowercase() }
    }

    fun currentStatus(): String {
        val items = _playlist.value
        val idx = _currentIndex.value
        if (items.isEmpty() || idx == null) return "No media loaded."
        val item = items[idx]
        return buildString {
            appendLine("Playing: ${item.filename} (${item.kind.name.lowercase()})")
            appendLine("Mode: ${_mode.value.name.lowercase()}")
            appendLine("Playlist: ${idx + 1}/${items.size}")
        }.trim()
    }
}
