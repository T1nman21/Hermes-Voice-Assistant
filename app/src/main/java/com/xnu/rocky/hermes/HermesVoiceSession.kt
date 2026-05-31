package com.xnu.rocky.hermes

import android.content.Context
import com.xnu.rocky.providers.ChatClient
import com.xnu.rocky.providers.ChatMessage
import com.xnu.rocky.providers.HermesProviderConfig
import com.xnu.rocky.providers.ProviderConfiguration
import com.xnu.rocky.runtime.LogManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hermes voice session — manages the full voice interaction loop:
 *
 * 1. Wake word detected → start listening
 * 2. STT captures speech → convert to text
 * 3. Text sent to Hermes API → stream response
 * 4. TTS speaks the response → loop back to listening
 */
class HermesVoiceSession(private val context: Context) {

    companion object {
        private const val TAG = "HermesSession"
    }

    private val tts = TtsManager(context)
    private val stt = SttManager(context)
    private var chatClient: ChatClient? = null
    private var conversationHistory = mutableListOf<ChatMessage>()

    enum class State {
        /** Idle, waiting for wake word or manual activation */
        IDLE,
        /** Listening for user speech */
        LISTENING,
        /** Processing — sending to Hermes and waiting for response */
        PROCESSING,
        /** Speaking the assistant's response */
        SPEAKING
    }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

    private val _assistantResponse = MutableStateFlow("")
    val assistantResponse: StateFlow<String> = _assistantResponse.asStateFlow()

    private val _statusText = MutableStateFlow("Tap to talk")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private var currentHost: String = HermesProviderConfig.DEFAULT_HOST
    private var currentModel: String = HermesProviderConfig.DEFAULT_MODEL
    private var currentApiKey: String = "hermes-local"

    /**
     * Configure Hermes connection.
     *
     * @param host Hermes API base URL (e.g., "http://192.168.1.100:8642/v1")
     * @param model Model name (default "hermes")
     * @param apiKey API key (use "hermes-local" for localhost, or real key for remote)
     */
    fun configure(
        host: String = HermesProviderConfig.DEFAULT_HOST,
        model: String = HermesProviderConfig.DEFAULT_MODEL,
        apiKey: String = "hermes-local"
    ) {
        currentHost = host
        currentModel = model
        currentApiKey = apiKey
        chatClient = ChatClient(HermesProviderConfig.configuration(host, model, apiKey))
    }

    /**
     * Start a voice interaction — begin listening for speech.
     */
    fun startListening() {
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
                    processUserInput(text)
                } else {
                    _state.value = State.IDLE
                    _statusText.value = "Tap to talk"
                }
            }

            override fun onError(error: Int, message: String) {
                LogManager.warning("STT error: $message", TAG)
                _state.value = State.IDLE
                _statusText.value = "Tap to talk"
            }
        })
    }

    /**
     * Send a text prompt directly (from chat/typing).
     */
    fun sendText(text: String) {
        _userTranscript.value = text
        processUserInput(text)
    }

    private fun processUserInput(text: String) {
        _state.value = State.PROCESSING
        _statusText.value = "Hermes is thinking..."

        conversationHistory.add(ChatMessage(role = "user", content = text))

        // Ensure client is configured
        if (chatClient == null) {
            configure(currentHost, currentModel, currentApiKey)
        }

        // Stream response from Hermes
        val responseBuilder = StringBuilder()
        val client = chatClient ?: run {
            _assistantResponse.value = "Error: Hermes not configured"
            _state.value = State.IDLE
            return
        }

        try {
            // Build messages from conversation history
            val messages = listOf(
                ChatMessage(
                    role = "system",
                    content = "You are Hermes, a helpful voice assistant. Keep responses concise and conversational for voice interaction."
                )
            ) + conversationHistory.toList()

            // Note: Flow collection needs coroutine scope — this runs on the calling thread.
            // In production, use a coroutine scope (ViewModel scope or similar).
            kotlinx.coroutines.runBlocking {
                client.streamChat(messages = messages).collect { delta ->
                    delta.content?.let { content ->
                        responseBuilder.append(content)
                        _assistantResponse.value = responseBuilder.toString()
                    }
                    delta.toolCalls?.let { toolCalls ->
                        responseBuilder.append("[tool: ${toolCalls.joinToString { it.function?.name ?: "?" }}]")
                        _assistantResponse.value = responseBuilder.toString()
                    }
                }
            }

            val fullResponse = responseBuilder.toString()
            if (fullResponse.isNotBlank()) {
                conversationHistory.add(ChatMessage(role = "assistant", content = fullResponse))
                speakResponse(fullResponse)
            } else {
                _state.value = State.IDLE
                _statusText.value = "Tap to talk"
            }
        } catch (e: Exception) {
            LogManager.error("Hermes API error: ${e.message}", TAG)
            _assistantResponse.value = "Error: ${e.message}"
            _state.value = State.IDLE
            _statusText.value = "Connection error — tap to retry"
        }
    }

    private fun speakResponse(text: String) {
        _state.value = State.SPEAKING
        _statusText.value = "Hermes is speaking..."

        tts.addOnSpeechDoneListener(object : () -> Unit {
            override fun invoke() {
                _state.value = State.IDLE
                _statusText.value = "Tap to talk"
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
        _state.value = State.IDLE
        _statusText.value = "Tap to talk"
    }

    /**
     * Release all resources.
     */
    fun destroy() {
        stop()
        tts.destroy()
        stt.destroy()
        chatClient = null
        conversationHistory.clear()
    }

    /**
     * Check if Hermes TTS has English voice data installed.
     */
    fun needsTtsData(): Boolean = !tts.hasEnglishData()
}
