# Nova JARVIS

An Android 11+ voice-command assistant starter built with Kotlin and Jetpack Compose.

## Current starter features
- Dark JARVIS-inspired dashboard UI.
- Speech-to-text through Android's speech recognizer.
- Launch common apps (YouTube, WhatsApp, Chrome, Settings, Camera, Maps, Gmail).
- Open the Android alarm screen with a requested time.
- Optional Accessibility Service for user-requested visible text clicks, focused-field typing, and screenshots.

## Build
Open this repository in Android Studio (Hedgehog or newer), allow Gradle sync, then build:
**Build > Build Bundle(s) / APK(s) > Build APK(s)**

## Device setup
1. Install the debug APK.
2. Grant microphone permission when asked.
3. Tap **ENABLE DEVICE CONTROL** and explicitly enable Nova JARVIS Control in Android Accessibility settings for click/type/screenshot features.

## Important limitations
- Android controls app permissions and may show confirmation UI for sensitive actions.
- Speech recognition availability and language support depend on device speech services.
- This is a command-based starter, not yet a full conversational autonomous agent. It does not include an LLM/API integration, background wake-word listener, Gmail OAuth, or robust task planning.
- Accessibility automation only acts on visible, accessible UI elements; it cannot bypass app security, lock screens, or permission prompts.
