# Hermes Assistant App — Hermes-Tailored Roadmap

## What Hermes Already Has

After reading through the full hermes-agent codebase:

| Capability | Hermes Built-in | App needs? |
|-----------|----------------|------------|
| OpenAI-compatible API (`:8642/v1`) | ✅ `gateway/platforms/api_server.py` | Use via relay |
| WebSocket TUI gateway (`/api/ws`) | ✅ `tui_gateway/ws.py` (JSON-RPC) | Use directly |
| Multi-platform gateway (Telegram, Discord, etc.) | ✅ 20+ platforms | Not needed |
| Voice/STT/TTS | ✅ `voice_mode.py` + `transcription_tools.py` + `tts_tool.py` | Not needed (do on-device) |
| Sessions & memory | ✅ SessionDB + MEMORY.md | Leverage via API |
| Skills system | ✅ 100+ skills | N/A |
| Cron/scheduling | ✅ `cronjob_tools.py` | N/A |
| Profiles | ✅ Multiple isolated instances | N/A |

## The Current App (post-fork from OpenRocky)

The app inherited OpenRocky's architecture:
- Multi-provider chat client (OpenAI, Anthropic, Gemini, etc.)
- Realtime voice bridge (OpenAI Realtime API)
- Provider settings UI
- Onboarding for cloud API keys
- 30+ Android system tools (contacts, calendar, etc.)

All of this is unnecessary for a Hermes client. Hermes handles the "thinking" — the app should just be a voice terminal.

## Changes Needed

### Phase 1: Strip Down to Hermes-Only

| File | Change |
|------|--------|
| `ProviderKind.kt` | Remove all providers except `HERMES` |
| `ProviderInstanceEditorView.kt` | Remove provider dropdown — just show relay connection |
| `ProviderInstanceListView.kt` | Remove multi-provider list — single Hermes connection |
| `ProviderSettingsView.kt` | Simplify — no "Chat Providers" vs "Voice Providers" distinction |
| `RealtimeProviderConfiguration.kt` | Remove — Hermes doesn't use OpenAI Realtime |
| `RealtimeVoiceBridge.kt` | Remove — we use our own relay |
| `ChatInferenceRuntime.kt` | Remove — Hermes handles inference |
| `OnboardingView.kt` | Already updated for relay, but simplify further |
| `SettingsView` | Strip to bare essentials: relay connection, room code, token, about |
| `AndroidManifest.xml` | Remove accessibility/notification services (not needed) |

### Phase 2: Improve Voice UX

| Change | Why |
|--------|-----|
| Full-screen voice UI | Not a chat app — voice-first like Google Assistant |
| Waveform animation | Visual feedback while listening |
| Quick response cards | Hermes responses in a glanceable format |
| Hands-free mode | Auto-listen after TTS finishes (conversation loop) |
| Persistent notification | "Listening for Hey Hermes" in notification shade |
| System prompt | Tell Hermes: "You are a voice assistant. Keep responses concise and conversational." |

### Phase 3: Direct Hermes Integration

| Change | Why |
|--------|-----|
| Connect directly to Hermes TUI gateway WebSocket | Bypass the relay for local connections |
| Pass Hermes session ID | Responses persist in Hermes's session history |
| Show session history | Browse past conversations from the app |
| Trigger skills | "Hey Hermes, check my calendar" → `calendar` skill |
| Memory integration | Hermes remembers preferences across sessions |

### Phase 4: Make It a Real Hermes Platform

Long-term: Create a `gateway/platforms/hermes_voice.py` in the hermes-agent codebase.
The Android app becomes just the mobile UI — all logic stays in Hermes.

## What to Cut Immediately

```
DELETE:
- provider/RealtimeProviderConfiguration.kt
- provider/RealtimeProviderInstance.kt  
- provider/RealtimeProviderKind.kt
- provider/RealtimeProviderStore.kt
- provider/RealtimeAdvancedSettings.kt
- runtime/voice/OpenAIRealtimeVoiceClient.kt
- runtime/voice/RealtimeVoiceBridge.kt
- runtime/voice/RealtimeVoiceClient.kt
- runtime/voice/RealtimeEvent.kt
- runtime/voice/VoiceErrorTriage.kt
- runtime/voice/VoicePriming.kt
- runtime/voice/VoiceFeatures.kt
- runtime/ChatInferenceRuntime.kt
- runtime/tools/* (all 30 tools — Hermes handles these)
- runtime/skills/* (all skill files)
- runtime/CharacterStore.kt
- runtime/SoulStore.kt
- runtime/MemoryService.kt
- runtime/SubagentRuntime.kt
- runtime/UsageService.kt
- providers/ChatClient.kt
- providers/OpenAIOAuth*.kt
- providers/SecureStore.kt
- ui/screens/providers/ProviderInstanceEditorView.kt
- ui/screens/providers/ProviderInstanceListView.kt
- ui/screens/providers/RealtimeProviderInstanceEditorView.kt
- ui/screens/providers/OnboardingView.kt (replace with simpler relay-only onboarding)
- ui/screens/settings/* (most settings screens)
- RockyAccessibilityService.kt
- RockyNotificationListenerService.kt
```

## What Stays

```
KEEP:
- RelayClient.kt (WebSocket relay connection)
- HermesVoiceSession.kt (voice loop: STT → relay → TTS)
- SttManager.kt (on-device speech recognition)
- TtsManager.kt (on-device text-to-speech)
- WakeWordService.kt (Porcupine/tap-to-talk)
- MainActivity.kt (simplified)
- OpenRockyViewModel.kt (simplified)
- OpenRockyApp.kt (simplified)
- VoiceForegroundService.kt (keep mic alive)
- VoiceHomeScreen.kt (simplified voice UI)
- Theme/* (brand colors — already Hermes purple)
```

## Build Size Impact

Current APK: 134 MB (includes Chaquopy Python runtime + 30 tools)
After stripping: ~15-20 MB (just voice pipeline + relay + UI)
