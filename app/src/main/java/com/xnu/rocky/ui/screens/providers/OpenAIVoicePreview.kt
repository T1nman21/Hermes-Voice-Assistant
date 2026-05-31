//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.providers

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Plays a short MP3 preview of an OpenAI TTS voice. Used by the Realtime editor's voice picker
 * to mirror iOS `OpenRockyOpenAIVoicePreview`. One-shot fetch from `/v1/audio/speech`, cached
 * to a temp file, played via [MediaPlayer]. Tapping a different voice cancels the in-flight
 * play and starts a new one.
 */
class OpenAIVoicePreviewController(private val context: Context) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null
    private var player: MediaPlayer? = null

    var playingVoice by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun toggle(voice: String, credential: String, customHost: String) {
        if (playingVoice == voice) {
            stop()
            return
        }
        stop()
        if (credential.isBlank()) {
            error = "Add an API key first."
            return
        }
        playingVoice = voice
        loading = true
        error = null
        currentJob = scope.launch {
            val result = fetchPreview(voice, credential, customHost)
            result.onSuccess { file ->
                loading = false
                playFile(file)
            }.onFailure {
                loading = false
                playingVoice = null
                error = it.message ?: "preview failed"
            }
        }
    }

    fun stop() {
        currentJob?.cancel()
        currentJob = null
        player?.runCatching { stop(); release() }
        player = null
        playingVoice = null
        loading = false
    }

    fun dispose() {
        stop()
        scope.cancel()
    }

    private suspend fun fetchPreview(
        voice: String,
        credential: String,
        customHost: String
    ): Result<File> = withContext(Dispatchers.IO) {
        val baseRaw = customHost.trim().ifBlank { "https://api.openai.com" }
            .replace("wss://", "https://").replace("ws://", "http://").trimEnd('/')
        val url = "$baseRaw/v1/audio/speech"
        val body = """{"model":"tts-1","voice":"$voice","input":"Hi, this is a preview of my voice.","response_format":"mp3"}"""
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $credential")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()?.take(160).orEmpty()
                    error("HTTP ${response.code} $errBody")
                }
                val bytes = response.body?.bytes() ?: error("empty response")
                val outFile = File(context.cacheDir, "openai_voice_preview_$voice.mp3")
                outFile.writeBytes(bytes)
                outFile
            }
        }
    }

    private fun playFile(file: File) {
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                this@OpenAIVoicePreviewController.stop()
            }
            setOnErrorListener { _, _, _ ->
                this@OpenAIVoicePreviewController.stop()
                true
            }
            prepare()
            start()
        }
    }
}

@Composable
fun rememberOpenAIVoicePreview(): OpenAIVoicePreviewController {
    val context = LocalContext.current
    val controller = remember { OpenAIVoicePreviewController(context) }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    return controller
}
