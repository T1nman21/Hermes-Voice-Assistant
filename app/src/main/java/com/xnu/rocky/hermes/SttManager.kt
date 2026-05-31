package com.xnu.rocky.hermes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.xnu.rocky.runtime.LogManager

/**
 * On-device Speech-to-Text for Hermes Voice.
 *
 * Uses Android's built-in SpeechRecognizer API for converting speech to text.
 * Works offline (depending on device language packs) and provides real-time
 * partial results for a natural voice interaction flow.
 */
class SttManager(private val context: Context) {

    companion object {
        private const val TAG = "SttManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: SttListener? = null
    private var isListening = false

    /** Current best-guess partial transcript (updated as user speaks) */
    var partialTranscript: String = ""
        private set

    /** The final transcript after the user stops speaking */
    var finalTranscript: String = ""
        private set

    /** Callbacks for speech recognition events */
    interface SttListener {
        /** Called when speech is detected (user started talking) */
        fun onSpeechStart()

        /** Called during speech with partial results */
        fun onPartialResult(text: String)

        /** Called when speech ends with the final transcription */
        fun onFinalResult(text: String)

        /** Called on recognition error */
        fun onError(error: Int, message: String)

        /** Called when the recognizer is ready to listen */
        fun onReady()
    }

    /**
     * Start listening for speech.
     */
    fun startListening(listener: SttListener) {
        this.listener = listener
        stopListening() // Cancel any existing session

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    partialTranscript = ""
                    finalTranscript = ""
                    listener?.onReady()
                }

                override fun onBeginningOfSpeech() {
                    listener?.onSpeechStart()
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Raw audio level — could show a waveform/VU meter
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    // Automatic end detected — final result will come soon
                }

                override fun onError(error: Int) {
                    isListening = false
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error ($error)"
                    }
                    LogManager.warning("STT error $error: $message", TAG)
                    listener?.onError(error, message)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    finalTranscript = matches?.firstOrNull() ?: ""
                    if (finalTranscript.isNotBlank()) {
                        listener?.onFinalResult(finalTranscript)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    partialTranscript = matches?.firstOrNull() ?: ""
                    if (partialTranscript.isNotBlank()) {
                        listener?.onPartialResult(partialTranscript)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
        }
        speechRecognizer?.startListening(intent)
    }

    /**
     * Stop listening and cancel recognition.
     */
    fun stopListening() {
        isListening = false
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        listener = null
    }

    /**
     * Check if speech recognition is available on this device.
     */
    fun isAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    fun destroy() {
        stopListening()
    }
}
