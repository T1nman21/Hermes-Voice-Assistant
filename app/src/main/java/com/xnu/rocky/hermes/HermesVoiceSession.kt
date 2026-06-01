package com.xnu.rocky.hermes

import android.content.Context
import com.xnu.rocky.OpenRockyApp
import com.xnu.rocky.VoiceForegroundService
import com.xnu.rocky.runtime.LogManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Hermes voice session — manages the full voice interaction loop
 * through the WebSocket relay (NOT direct HTTP to Hermes):
 *
 * 1. Phone connects to relay via WebSocket
 * 2. Desktop client also connects to relay (pairing via room code)
 * 3. Wake word → STT captures speech → text sent via relay
 * 4. Desktop Hermes processes → response back via relay
 * 5. TTS speaks the response → loop back to listening
 *
 * The RelayClient is owned by OpenRockyApp, not this class — it
 * survives Activity lifecycle changes and backgrounding.
 */
class HermesVoiceSession(private val context: Context) {

    companion object {
        private const val TAG = "HermesSession"
        /** Seconds of silence before returning to wake-word-only mode */
        private const val FOLLOW_UP_TIMEOUT_SEC = 8L
    }

    private val tts = TtsManager(context)
    private val stt = SttManager(context)
    private val app = OpenRockyApp.get(context)
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + scopeJob)

    private var isDesktopConnected = false
    private var followUpJob: Job? = null

    enum class State {
        /** Not connected to relay */
        DISCONNECTED,
        /** Connected to relay, waiting for desktop partner */
        WAITING_FOR_DESKTOP,
        /** Fully connected — ready for voice */
        READY,
        /** Listening for user speech */
        LISTENING,
        /** Processing — prompt sent, waiting for response */
        PROCESSING,
        /** Speaking the assistant's response */
        SPEAKING,
    }

    private val _state = MutableStateFlow(State.DISCONNECTED)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

    private val _assistantResponse = MutableStateFlow("")
    val assistantResponse: StateFlow<String> = _assistantResponse.asStateFlow()

    private val _statusText = MutableStateFlow("Connect to desktop first")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private var pendingPromptId: String? = null

    /**
     * Connect to relay and begin observing events.
     * Must be called after configure() on the app-scoped RelayClient.
     */
    fun startObserving() {
        val client = app.relayClient

        // Observe relay events
        scope.launch {
            client.events.collect { event ->
                when (event) {
                    is RelayClient.RelayEvent.Connected -> {
                        _state.value = State.WAITING_FOR_DESKTOP
                        _statusText.value = "Waiting for desktop..."
                    }
                    is RelayClient.RelayEvent.PartnerJoined -> {
                        isDesktopConnected = true
                        _state.value = State.READY
                        _statusText.value = "Ready — tap to talk"
                        VoiceForegroundService.stop(context)
                    }
                    is RelayClient.RelayEvent.PartnerLeft -> {
                        isDesktopConnected = false
                        _state.value = State.WAITING_FOR_DESKTOP
                        _statusText.value = "Desktop disconnected — waiting..."
                    }
                    is RelayClient.RelayEvent.Disconnected -> {
                        _state.value = State.DISCONNECTED
                        _statusText.value = "Connection lost — retrying..."
                    }
                }
            }
        }

        // Observe responses from desktop
        scope.launch {
            client.responses.collect { response ->
                if (response.id == pendingPromptId) {
                    val text = response.text
                    if (response.error != null) {
                        _assistantResponse.value = "Error: ${response.error}"
                        _state.value = State.READY
                        _statusText.value = "Error — tap to retry"
                    } else if (text.isNotBlank()) {
                        _assistantResponse.value = text
                        speakResponse(text)
                    } else {
                        goReadyOrFollowUp()
                    }
                    pendingPromptId = null
                }
            }
        }
    }

    /**
     * Start a voice interaction — begin listening for speech.
     * Called by mic button tap or by wake-word-triggered START_VOICE.
     */
    fun startListening() {
        _state.value = State.LISTENING
        _userTranscript.value = ""
        _assistantResponse.value = ""
        _statusText.value = if (isDesktopConnected) "Listening..." else "Listening (no desktop)..."

        // Show foreground notification for mic access while listening
        VoiceForegroundService.start(context)

        stt.startListening(object : SttManager.SttListener {
            override fun onReady() {
                _statusText.value = if (isDesktopConnected) "Listening..." else "Speak — no desktop connected"
            }

            override fun onSpeechStart() {
                _statusText.value = "Speaking..."
                cancelFollowUpTimer()
            }

            override fun onPartialResult(text: String) {
                _userTranscript.value = text
            }

            override fun onFinalResult(text: String) {
                _userTranscript.value = text
                if (text.isNotBlank()) {
                    if (isDesktopConnected && app.relayClient.isReady()) {
                        sendPrompt(text)
                    } else if (!isDesktopConnected) {
                        _assistantResponse.value = "Desktop not connected. Start the Hermes relay on your PC first."
                        goReadyOrFollowUp()
                        VoiceForegroundService.stop(context)
                    } else {
                        goReadyOrFollowUp()
                        VoiceForegroundService.stop(context)
                    }
                } else {
                    goReadyOrFollowUp()
                    VoiceForegroundService.stop(context)
                }
            }

            override fun onError(error: Int, message: String) {
                LogManager.warning("STT error: $message", TAG)
                goReadyOrFollowUp()
                VoiceForegroundService.stop(context)
            }
        })
    }

    /**
     * Send a text prompt via the relay.
     */
    fun sendPrompt(text: String) {
        _state.value = State.PROCESSING
        _statusText.value = "Hermes is thinking..."
        _userTranscript.value = text

        val client = app.relayClient
        if (!client.isReady()) {
            _assistantResponse.value = "Not connected. Connect to desktop first."
            _state.value = State.DISCONNECTED
            return
        }

        pendingPromptId = client.sendPrompt(text)
    }

    private fun speakResponse(text: String) {
        _state.value = State.SPEAKING
        _statusText.value = "Hermes is speaking..."

        tts.addOnSpeechDoneListener(object : () -> Unit {
            override fun invoke() {
                goReadyOrFollowUp()
                tts.removeOnSpeechDoneListener(this)
            }
        })

        tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH)
    }

    // ── Follow-up mode ─────────────────────────────────────────────

    /**
     * After a response, enter ready state with a follow-up timer.
     * If the user says nothing within FOLLOW_UP_TIMEOUT_SEC, return
     * to wake-word-only mode (stop foreground service, hide UI).
     */
    private fun goReadyOrFollowUp() {
        _state.value = State.READY
        _statusText.value = if (isDesktopConnected) "Ready — you can ask a follow-up" else "Connect desktop first"
        VoiceForegroundService.stop(context)

        // Start follow-up timeout — if user doesn't tap mic or say wake word
        // within the timeout, dismiss the voice session UI
        followUpJob?.cancel()
        followUpJob = scope.launch {
            delay(FOLLOW_UP_TIMEOUT_SEC * 1000)
            // Timeout elapsed — fire intent to dismiss voice mode
            _statusText.value = if (isDesktopConnected) "Listening for 'Hey Hermes'..." else "Connect desktop first"
        }
    }

    private fun cancelFollowUpTimer() {
        followUpJob?.cancel()
        followUpJob = null
    }

    /** True if in a voice interaction (mic active or processing) */
    val isActive: Boolean
        get() = _state.value == State.LISTENING ||
                _state.value == State.PROCESSING ||
                _state.value == State.SPEAKING

    // ── Lifecycle ──────────────────────────────────────────────────

    fun stop() {
        stt.stopListening()
        tts.stop()
        cancelFollowUpTimer()
        if (isDesktopConnected || app.relayClient.isReady()) {
            _state.value = State.READY
        } else {
            _state.value = State.WAITING_FOR_DESKTOP
        }
        _statusText.value = "Ready — tap to talk"
        VoiceForegroundService.stop(context)
    }

    fun destroy() {
        stop()
        tts.destroy()
        stt.destroy()
        scopeJob.cancel()
        _state.value = State.DISCONNECTED
    }

    fun needsTtsData(): Boolean = !tts.hasEnglishData()
}
