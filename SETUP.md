# Hermes Voice — Setup Guide

## Prerequisites

1. **Android Studio** (Ladybug or newer) with Android SDK 35
2. **Hermes Agent** running on your desktop with API server enabled
3. (Optional) **Picovoice account** for "Hey Hermes" wake word

## Quick Start (Without Wake Word)

The app works in "tap to talk" mode without any wake word setup:

```bash
cd hermes-voice
./gradlew assembleStandardDebug
```

Install the APK on your phone. Open the app, configure Hermes:

1. Go to **Settings → Chat Providers → Add Provider**
2. Select **Hermes** as the provider kind
3. Enter your desktop's IP address as the custom host:
   ```
   http://192.168.1.100:8642/v1
   ```
4. Use `hermes-local` as the API key

Then tap the mic button to start talking.

## Wake Word Setup ("Hey Hermes")

For always-on wake word detection, you need Porcupine by Picovoice:

### 1. Get a Picovoice Access Key

1. Go to https://picovoice.ai/
2. Click "Get Started" → sign up for free
3. Get your **AccessKey** from the Picovoice Console

### 2. Train "Hey Hermes" Wake Word

1. Go to https://console.picovoice.ai/
2. Create a new **Wake Word**
3. Type "Hey Hermes" (or your preferred phrase)
4. Download the model file:
   - Format: **Porcupine**
   - Platform: **Android**
   - Language: **English**
5. You'll get a `.ppn` file like `hey-hermes_en_android_v3_0_0.ppn`

### 3. Add to the app

1. Place the `.ppn` file in:
   ```
   app/src/main/assets/hey-hermes_en_android_v3_0_0.ppn
   ```
2. Create the `assets` directory if it doesn't exist
3. Add Porcupine to `app/build.gradle.kts`:
   ```kotlin
   implementation("ai.picovoice:porcupine-android:3.0.3")
   ```
4. Update `WakeWordService.kt` — set your `ACCESS_KEY`
5. Rebuild and install

### 4. Enable Wake Word

1. Open the app
2. Grant microphone permission
3. The notification bar shows: "Listening for 'Hey Hermes'..."
4. Say "Hey Hermes" to activate the assistant

## Hermes Desktop Setup

Make sure Hermes agent is running with the API server:

```bash
# Start Hermes with API server (check Hermes docs for exact flags)
hermes-agent --api-server --port 8642
```

For access from your phone on the same network:
- Find your desktop IP: `ipconfig` (Windows) or `ifconfig` (Mac/Linux)
- Allow port 8642 through your firewall
- Or use a local tunnel like `ngrok http 8642`

## Connection Methods

| Method | Pros | Cons |
|--------|------|------|
| **Local Wi-Fi** | Fast, no extra services | Must be on same network |
| **Remote Pi relay** | Works from anywhere | Requires Pi running on desktop |
| **Tailscale VPN** | Secure, works anywhere | Extra setup |
| **ngrok tunnel** | Quick setup | Free tier has limits |

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "Hermes not configured" | Add a Hermes provider in Settings |
| Can't connect to desktop | Check firewall, verify IP, try `ping <desktop-ip>` from phone |
| TTS not speaking | Install Google TTS from Play Store; check language data |
| STT not working | Check microphone permission; install offline speech recognition |
| Wake word not detecting | Verify Picovoice key, check .ppn file in assets |
