First alpha release of Hermes Voice Assistant.

### What's included
- 🎤 Wake word service scaffold (Porcupine — needs key)
- 🗣️ Speech-to-text — on-device Android recognition
- 🔊 Text-to-speech — responses spoken aloud
- 🖥️ Hermes Agent integration — connects to Hermes on desktop
- 📱 Foreground service — works with screen off
- 🏠 Widget + Quick Settings tile + Assist button

### Setup
1. Install the APK on your phone
2. Settings → Chat Providers → Add Provider → Hermes
3. Enter desktop IP as custom host (e.g. http://192.168.1.100:8642/v1)
4. Use `hermes-local` as the API key

### Known issues
- Wake word requires Porcupine setup (Picovoice key + model file)
- App package is still `com.xnu.rocky` (rename in progress)
