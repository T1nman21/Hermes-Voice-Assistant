# Development Guide

> English | [中文](DEVELOP_zh.md)

## Project Structure

```
app/                        # Android app module
  src/main/java/.../        # Kotlin source
    ui/screens/             # Jetpack Compose UI screens
    providers/              # AI provider configuration & clients
    runtime/                # ROS runtime core
      tools/                # 30+ native bridge services
      skills/               # Skill system
      voice/                # Realtime voice clients
    models/                 # Data models
  src/main/python/          # Python runtime scripts
  src/androidTest/          # Instrumented tests
  src/test/                 # Unit tests
scripts/                    # Build & deploy scripts
keystore/                   # Signing configuration
store-assets/               # Play Store assets
```

## Requirements

- Android Studio Hedgehog+
- Android SDK 35 (target), SDK 28+ (minimum)
- Kotlin with Jetpack Compose
- JDK 11+

## Build

```bash
# Debug build
./gradlew assembleStandardDebug

# Release build (requires keystore setup)
./gradlew bundleStandardRelease

# Run tests
./gradlew testStandardDebugUnitTest
./gradlew connectedStandardDebugAndroidTest
```

## Deploy to Google Play

```bash
# First-time setup (keystore + service account)
./scripts/deploy-playstore.sh --setup

# Deploy to internal testing track
./scripts/deploy-playstore.sh

# Deploy to beta or production
./scripts/deploy-playstore.sh --track beta
./scripts/deploy-playstore.sh --track production
```

## Code Style

- 4-space indentation
- `PascalCase` for types, `camelCase` for properties/methods
- Jetpack Compose + Material 3 + MVVM architecture
- Kotlin coroutines with async/await patterns

## Testing

Uses JUnit and Espresso for instrumented tests. Tests cover provider inventory, tool registration, skill store, character system, and UI components.
