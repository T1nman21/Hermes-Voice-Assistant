package com.xnu.rocky.hermes

import com.xnu.rocky.runtime.LogManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android WebSocket client for the Hermes Voice relay.
 *
 * Connects to the relay server, joins a room, and exchanges
 * prompt/response messages with the desktop Hermes agent.
 *
 * Protocol:
 *   → {"type":"hello","room":"ABCD","role":"phone"}
 *   ← {"type":"ready","room":"ABCD"}
 *   → {"type":"prompt","text":"...","id":"msg-1"}
 *   ← {"type":"response","text":"...","id":"msg-1","error":null}
 *   ← {"type":"partner_joined"|"partner_left"}
 */
class RelayClient {

    companion object {
        private const val TAG = "RelayClient"
        private const val PING_INTERVAL_MS = 30_000L
        private const val RECONNECT_DELAY_MS = 3_000L
    }

    enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        JOINED,
        READY,
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(PING_INTERVAL_MS, TimeUnit.MILLISECONDS)
        .build()

    private var ws: WebSocket? = null
    private var relayUrl: String = ""
    private var roomId: String? = null
    private var token: String = ""
    private val msgCounter = AtomicInteger(0)
    private var scope: CoroutineScope? = null

    private val _state = MutableStateFlow(State.DISCONNECTED)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _responses = MutableSharedFlow<RelayResponse>(extraBufferCapacity = 10)
    val responses: SharedFlow<RelayResponse> = _responses.asSharedFlow()

    private val _events = MutableSharedFlow<RelayEvent>(extraBufferCapacity = 5)
    val events: SharedFlow<RelayEvent> = _events.asSharedFlow()

    data class RelayResponse(
        val text: String,
        val id: String,
        val error: String?
    )

    sealed class RelayEvent {
        /** Desktop partner connected */
        data object PartnerJoined : RelayEvent()
        /** Desktop partner disconnected */
        data object PartnerLeft : RelayEvent()
        /** Connection to relay established, room joined */
        data class Connected(val room: String) : RelayEvent()
        /** Connection lost */
        data class Disconnected(val reason: String?) : RelayEvent()
    }

    /**
     * Configure relay connection parameters. Call before connect().
     */
    fun configure(relayUrl: String, room: String, token: String = "") {
        this.relayUrl = relayUrl
        this.roomId = room
        this.token = token
    }

    /**
     * Connect to the relay server and join a room.
     * Must call configure() first (or pass parameters).
     */
    fun connect() {
        val room = roomId ?: return
        val url = relayUrl
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        _state.value = State.CONNECTING

        LogManager.info("Connecting to relay: $url room=$room", TAG)

        val request = Request.Builder()
            .url(url)
            .build()

        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                LogManager.info("Relay WebSocket opened", TAG)
                _state.value = State.CONNECTED
                // Send hello to join the room
                val hello = """{"type":"hello","room":"$room","role":"phone","token":"$token"}"""
                webSocket.send(hello)
                _state.value = State.JOINED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                LogManager.info("Relay closing: $code $reason", TAG)
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                LogManager.info("Relay closed: $code $reason", TAG)
                _state.value = State.DISCONNECTED
                _events.tryEmit(RelayEvent.Disconnected(reason))
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                LogManager.error("Relay failure: ${t.message}", TAG)
                _state.value = State.DISCONNECTED
                _events.tryEmit(RelayEvent.Disconnected(t.message))
                scheduleReconnect()
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val msg = org.json.JSONObject(text)
            when (msg.optString("type")) {
                "ready" -> {
                    _state.value = State.READY
                    _events.tryEmit(RelayEvent.Connected(msg.optString("room", "")))
                    LogManager.info("Relay: joined room, ready", TAG)
                }
                "response" -> {
                    _responses.tryEmit(RelayResponse(
                        text = msg.optString("text", ""),
                        id = msg.optString("id", ""),
                        error = if (msg.has("error") && !msg.isNull("error")) msg.getString("error") else null
                    ))
                }
                "partner_joined" -> {
                    _events.tryEmit(RelayEvent.PartnerJoined)
                    LogManager.info("Relay: partner (desktop) joined", TAG)
                }
                "partner_left" -> {
                    _events.tryEmit(RelayEvent.PartnerLeft)
                    LogManager.info("Relay: partner (desktop) left", TAG)
                }
                "error" -> {
                    LogManager.warning("Relay error: ${msg.optString("message")}", TAG)
                }
            }
        } catch (e: Exception) {
            LogManager.warning("Failed to parse relay message: ${e.message}", TAG)
        }
    }

    /**
     * Send a voice prompt to the desktop Hermes agent.
     */
    fun sendPrompt(text: String): String {
        val msgId = "msg-${msgCounter.incrementAndGet()}"
        val payload = """{"type":"prompt","text":${org.json.JSONObject.quote(text)},"id":"$msgId"}"""
        ws?.send(payload)
        LogManager.info("Prompt sent: ${text.take(60)}", TAG)
        return msgId
    }

    private fun scheduleReconnect() {
        scope?.launch {
            delay(RECONNECT_DELAY_MS)
            connect()
        }
    }

    /**
     * Check if we're ready to send prompts.
     */
    fun isReady(): Boolean = _state.value == State.READY

    /**
     * Disconnect from the relay.
     */
    fun disconnect() {
        scope?.cancel()
        ws?.close(1000, "Client disconnect")
        ws = null
        _state.value = State.DISCONNECTED
    }
}
