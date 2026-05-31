package com.xnu.rocky.hermes

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.xnu.rocky.runtime.LogManager
import java.util.Locale
import java.util.UUID

/**
 * Text-to-Speech output manager for Hermes Voice.
 *
 * Speaks assistant responses aloud using Android's built-in TTS engine.
 * Responses are queued to avoid overlapping speech. Supports configurable
 * speech rate, pitch, and voice selection.
 */
class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TtsManager"
        private const val DEFAULT_PITCH = 1.0f
        private const val DEFAULT_SPEECH_RATE = 1.0f
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    /** True while TTS is currently speaking */
    val isSpeaking: Boolean
        get() = tts?.isSpeaking == true

    /** Listeners notified when speech starts/stops */
    private val onSpeechStartListeners = mutableListOf<() -> Unit>()
    private val onSpeechDoneListeners = mutableListOf<() -> Unit>()

    init {
        tts = TextToSpeech(context, this).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setSpeechRate(DEFAULT_SPEECH_RATE)
            setPitch(DEFAULT_PITCH)
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeechStartListeners.forEach { it.invoke() }
                }
                override fun onDone(utteranceId: String?) {
                    onSpeechDoneListeners.forEach { it.invoke() }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    LogManager.error("TTS error for utterance $utteranceId", TAG)
                }
            })
        }
    }

    override fun onInit(status: Int) {
        isInitialized = status == TextToSpeech.SUCCESS
        if (isInitialized) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                LogManager.warning("TTS: US English not supported, language may be unavailable", TAG)
            }
            LogManager.info("TTS initialized successfully", TAG)
        } else {
            LogManager.error("TTS initialization failed with status $status", TAG)
        }
    }

    /**
     * Speak text aloud. If already speaking, this queues after the current utterance.
     *
     * @param text The text to speak
     * @param queueMode TTS queue mode (default QUEUE_ADD to wait for current speech)
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_ADD) {
        if (!isInitialized) {
            LogManager.warning("TTS not initialized. Can't speak: ${text.take(60)}", TAG)
            return
        }
        val utteranceId = UUID.randomUUID().toString()
        // Use deprecated speak() for compatibility, AudioAttributes handle the rest
        @Suppress("DEPRECATION")
        tts?.speak(text, queueMode, null, utteranceId)
    }

    /**
     * Stop any current speech immediately.
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * Check if TTS engine has data for US English.
     * Returns false if user needs to install TTS data from Play Store.
     */
    fun hasEnglishData(): Boolean {
        val result = tts?.isLanguageAvailable(Locale.US) ?: return false
        return result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                result == TextToSpeech.LANG_AVAILABLE
    }

    fun addOnSpeechStartListener(listener: () -> Unit) {
        onSpeechStartListeners.add(listener)
    }

    fun addOnSpeechDoneListener(listener: () -> Unit) {
        onSpeechDoneListeners.add(listener)
    }

    fun removeOnSpeechStartListener(listener: () -> Unit) {
        onSpeechStartListeners.remove(listener)
    }

    fun removeOnSpeechDoneListener(listener: () -> Unit) {
        onSpeechDoneListeners.remove(listener)
    }

    /**
     * Release TTS engine resources. Call when done.
     */
    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        onSpeechStartListeners.clear()
        onSpeechDoneListeners.clear()
    }
}
