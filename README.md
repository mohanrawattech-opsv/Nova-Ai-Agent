# Nova JARVIS — Android / Android TV foundation

Nova is an open-source Kotlin assistant starter. It is being developed in verifiable steps; it is not yet a complete autonomous app store or production-ready TV launcher.

## Current capabilities
- Jetpack Compose dark dashboard
- Speech-to-text using the device speech recognizer
- Launch a small allowlist of installed apps
- Open Android alarm UI
- Optional, user-enabled Accessibility Service for visible text clicks, focused-field typing and screenshots
- Optional Node.js chat-proxy service (see `server/`)

## Build APK without a PC
GitHub Actions builds a debug APK on pushes to the repository and on manual dispatch. Open **Actions → Android CI → latest run → Artifacts** and download `nova-jarvis-debug-apk`. Debug APKs are for testing, not Play Store distribution.

## Android TV compatibility warning
The app currently uses a phone-style Compose screen and has not been verified on Android TV. TV vendors differ in speech services, accessibility behavior, installer permissions, CPU ABI and storage. Test on the target TV before relying on it. Phone-only apps and games may not be TV-compatible.

## App installation safety
Android intentionally requires the user/system installer to approve sideloaded APK installation. Nova must not bypass this. Only install APKs from a developer's official release page or a trusted store, check package identity/signature, and show the Android confirmation screen. Never download APKs from arbitrary mirrors.

## AI configuration
The server is an optional OpenAI-compatible chat proxy. Configure `OPENAI_API_KEY`, `OPENAI_MODEL`, and a strong `NOVA_ACCESS_TOKEN` in the deployment environment. Deploy behind HTTPS. A server-side provider key is paid for by the operator; there is no unlimited free hosted AI. Users may instead supply their own API key. Never commit secrets.

## Current limitations / not implemented yet
- No in-app LLM chat integration in the Android client
- No autonomous tool planner or safe action-confirmation queue
- No integrated Aurora/F-Droid catalogue search or verified APK metadata/signature pipeline
- No APK download/install UI in the app
- No TV launcher/home replacement, D-pad-first layout, gamepad hub, wake-word service, or TV hardware certification
- No OAuth integrations (Gmail, etc.)
- No release signing key; CI produces a debug APK only

These are explicit gaps, not hidden claims. Implement each behind tests and visible user confirmation before calling Nova complete.

## Device setup
1. Install the CI debug APK.
2. Grant microphone permission if requested.
3. Enable the accessibility service only if you need visible UI automation.
4. Review each action before allowing it to operate another app.
