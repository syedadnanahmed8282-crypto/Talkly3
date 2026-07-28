# Project Guidelines & Rules

## 1. Step-by-Step Code Generation
- Make precise, modular, and complete changes.
- Do not write incomplete code, placeholder methods, or experimental APIs (e.g., experimental Kotlin foundation APIs) without proper annotations or clean stable implementations.
- Ensure all brackets, syntax, missing imports, and variables are fully resolved before saving.

## 2. Production-Ready Logic (No Fake/Mock Data)
- Never inject mock users, automated fake responses, or auto-answering call logic.
- Always query and validate actual Firebase Firestore collections/documents before treating a user or phone number as active or registered.

## 3. Error Prevention & Type Safety
- Ensure native Android configurations (`build.gradle.kts`, `AndroidManifest.xml`) use standard production/debug signing without missing local keystores or missing `google-services.json` paths.
- Always initialize native services (Firebase, ZEGOCloud) inside application startup safely with error handling to avoid startup crashes.
- Verify camera, microphone, and internet permissions at runtime before executing call/media functions.

## 4. Self-Verification Before Push
- Double-check that all updated Kotlin files compile cleanly without any syntax or build errors (e.g., verify `ChatListScreen` and main build tasks).
- Keep code backward-compatible and stable.
