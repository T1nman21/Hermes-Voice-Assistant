# Hermes Voice

**Wake-word-activated voice assistant for Android, powered by Hermes Agent on your desktop.**

Say "Hey Hermes" — your phone listens, sends the request to Hermes running on your PC, and speaks the answer back to you. Like Siri, but powered by your own AI agent.

## Features

- 🎤 **"Hey Hermes" wake word** — always-on listening (Porcupine)
- 🗣️ **Speech-to-text** — on-device Android speech recognition
- 🔊 **Text-to-speech** — assistant responses spoken aloud
- 🖥️ **Hermes Agent integration** — connects to Hermes on your desktop
- 📱 **Foreground service** — works with screen off
- 🏠 **Widget + Quick Settings tile + Assist button** — multiple activation methods

## Architecture

```
[You say "Hey Hermes"] → WakeWordService detects it
  → SttManager converts speech to text
  → HermesVoiceSession sends to Hermes API (desktop)
  → Hermes Agent processes and responds
  → TtsManager speaks the response aloud
```

Based on [OpenRocky](https://github.com/openrocky/openrocky_android) (Apache 2.0).

## Getting Started

See [SETUP.md](SETUP.md) for full setup instructions.

## Build

```bash
./gradlew assembleStandardDebug
```

Requires Android SDK 35 and JDK 11+.
