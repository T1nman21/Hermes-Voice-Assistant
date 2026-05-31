package com.xnu.rocky.hermes

import android.content.Context
import com.xnu.rocky.runtime.LogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Hermes voice session — manages the full voice interaction loop
 * through the WebSocket relay (NOT direct HTTP to Hermes):
 *
 * 1. Phone connects to relay via WebSocket
 * 2. Desktop client also connects to relay (pairing via room code)
 * 3. Wake word → STT captures speech → text sent via relay
 * 4. Desktop Hermes processes → response back via relay
 * 5. TTS speaks the response → loop back to listening
 */
class HermesVoiceSession(private val context: Context) {

    companion object {
        private const val TAG = "HermesSession"
        // Default: Cloudflare Tunnel (works from anywhere on the internet)
        private const val DEFAULT_RELAY_URL = "wss://hospitality-musicians-hunting-wedding.trycloudflare.com"
    }

    private val tts = TtsManager(context)
    private val stt = SttManager(context)
    private var relayClient: RelayClient? = null
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + scopeJob)

    private var relayUrl: String = DEFAULT_RELAY_URL
    private var roomCode: String = ""
    private var isDesktopConnected = false

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
        SPEAKING
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
     * Configure relay connection.
     *
     * @param relayUrl WebSocket URL of the relay server (default ws://<desktop-ip>:8643)
     * @param room 4-6 char room code to pair with desktop
     */
    fun configure(relayUrl: String = DEFAULT_RELAY_URL, room: String) {
        this.relayUrl = relayUrl
        this.roomCode = room
    }

    /**
     * Connect to the relay server and wait for desktop pairing.
     */
    fun connect() {
        if (roomCode.isBlank()) {
            LogManager.warning("Cannot connect: no room code set", TAG)
            return
        }

        relayClient?.disconnect()
        relayClient = RelayClient(relayUrl)
        _state.value = State.DISCONNECTED
        _statusText.value = "Connecting to relay..."

        // Observe relay events
        scope.launch {
            relayClient?.events?.collect { event ->
                when (event) {
                    is RelayClient.RelayEvent.Connected -> {
                        _state.value = State.WAITING_FOR_DESKTOP
                        _statusText.value = "Waiting for desktop..."
                    }
                    is RelayClient.RelayEvent.PartnerJoined -> {
                        isDesktopConnected = true
                        _state.value = State.READY
                        _statusText.value = "Ready — tap to talk"
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
            relayClient?.responses?.collect { response ->
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
                        _state.value = State.READY
                        _statusText.value = "Ready — tap to talk"
                    }
                    pendingPromptId = null
                }
            }
        }

        relayClient?.connect(roomCode)
    }

    /**
     * Start a voice interaction — begin listening for speech.
     */
    fun startListening() {
        if (!isDesktopConnected) {
            _statusText.value = "Desktop not connected. Try reconnecting."
            return
        }

        _state.value = State.LISTENING
        _userTranscript.value = ""
        _assistantResponse.value = ""
        _statusText.value = "Listening..."

        stt.startListening(object : SttManager.SttListener {
            override fun onReady() {
                _statusText.value = "Listening..."
            }

            override fun onSpeechStart() {
                _statusText.value = "Speaking..."
            }

            override fun onPartialResult(text: String) {
                _userTranscript.value = text
            }

            override fun onFinalResult(text: String) {
                _userTranscript.value = text
                if (text.isNotBlank()) {
                    sendPrompt(text)
                } else {
                    _state.value = State.READY
                    _statusText.value = "Ready — tap to talk"
                }
            }

            override fun onError(error: Int, message: String) {
                LogManager.warning("STT error: $message", TAG)
                _state.value = State.READY
                _statusText.value = "Tap to try again"
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

        val client = relayClient
        if (client == null || !client.isReady()) {
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
                _state.value = State.READY
                _statusText.value = "Ready — tap to talk"
                tts.removeOnSpeechDoneListener(this)
            }
        })

        tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH)
    }

    /**
     * Stop current voice interaction.
     */
    fun stop() {
        stt.stopListening()
        tts.stop()
        _state.value = if (isDesktopConnected) State.READY else State.WAITING_FOR_DESKTOP
        _statusText.value = "Ready — tap to talk"
    }

    /**
     * Release all resources.
     */
    fun destroy() {
        stop()
        relayClient?.disconnect()
        relayClient = null
        tts.destroy()
        stt.destroy()
        scopeJob.cancel()
        _state.value = State.DISCONNECTED
    }

    fun needsTtsData(): Boolean = !tts.hasEnglishData()
}
