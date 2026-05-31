# Hermes Voice — Android Build Plan

## Goal
Android app that acts as a Siri/Bixby-style voice assistant with wake word ("Hey Hermes"), speech-to-text, text-to-speech, connected to Hermes agent running on desktop via Remote Pi relay.

## Base: OpenRocky Android (Apache 2.0)
OpenRocky is a voice-first AI agent for Android with 30+ native tools, multi-provider AI support, realtime voice, and persistent conversations. It already has:
- OpenAI-compatible chat client with custom host support → compatible with Hermes API
- Foreground voice service
- Widget, Quick Settings tile, digital assistant intent
- Jetpack Compose UI
- Conversation persistence

## Modifications Needed

### 1. Hermes Provider (ProviderKind.kt)
Add `HERMES` to ProviderKind enum with:
- `defaultModel = "hermes"`
- `baseUrl = "http://<desktop-ip>:8642/v1/"`
- `displayName = "Hermes"`
- No API key required (or dummy key)

The existing `OpenAIServiceFactory` with `customHost` already handles the OpenAI-compatible API format — zero changes needed to ChatClient.

### 2. Wake Word Detection (New: WakeWordService.kt)
- Uses Porcupine by Picovoice (free for personal use, on-device, offline)
- Foreground service that continuously listens for "Hey Hermes"
- When detected, fires an intent to open the voice session
- Low battery impact — Porcupine is optimized for always-on

### 3. Text-to-Speech Output (New: TtsManager.kt)
- Android built-in `TextToSpeech` API
- Speaks assistant responses aloud
- Configurable voice/pitch/speed
- Queues responses so they don't overlap

### 4. On-Device Speech Recognition (New: SttManager.kt)
- Android built-in `SpeechRecognizer` API
- For chat-mode voice input (non-realtime path)
- Continuous listening mode during voice sessions
- Falls back to realtime voice API when available

### 5. Hermes-Specific Configuration
- Connection to Hermes via Remote Pi relay
- Desktop IP/port configuration
- Relay URL configuration (wss://remote-pi.jacobmoura.work/relay)

## Files to Create (new)
```
app/src/main/java/com/hermes/voice/
├── providers/
│   └── (no new files — modify existing ProviderKind.kt)
├── services/
│   └── WakeWordService.kt          # Porcupine always-on listening
├── voice/
│   ├── TtsManager.kt               # TextToSpeech output
│   ├── SttManager.kt               # SpeechRecognizer input
│   └── RemotePiClient.kt           # WebSocket client for Hermes relay
└── MainActivity.kt                 # Modified to start wake word service
```

## Files to Modify (existing)
```
app/src/main/java/com/xnu/rocky/providers/ProviderKind.kt    # Add HERMES
app/src/main/AndroidManifest.xml                              # Add service, permissions
app/build.gradle.kts                                          # Add Porcupine dependency
```
