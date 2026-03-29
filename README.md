# ChronoVault

ChronoVault is an Android app for creating digital time capsules that unlock in the future by time, by place, or both.

You write a message, optionally attach an image, set unlock rules, and let the app reveal memories when their conditions are met. Capsules can also be shared with other users, viewed on a live map, and discussed in chat.

## Why this project is interesting

Most journaling apps are immediate. ChronoVault is delayed on purpose.

- Time-locked memories: reveal after a chosen timestamp.
- Location-locked memories: reveal only when the user is physically near the unlock point.
- Hybrid memory model: local-first Room storage with Firebase sync.
- Ambient rediscovery: background workers continuously check unlock conditions.
- Social layer: sharing, notifications, and chat around memories.

## Current app flow

Splash -> Onboarding -> Auth (Login/Signup) -> Main shell

Main shell tabs/features include:
- Home dashboard (greeting, quote, capsule stats, nearby memory)
- Capsules list with filters and countdowns
- Map with capsule markers and clue-based location guidance
- Chat list and chat thread
- Profile and account actions

Notifications are accessed via the Home header action.

## Core features

### Capsule system
- Create capsule with title, message, optional image
- Time-based lock and countdown UX
- Location-based lock with proximity checks (100m unlock radius)
- Owner and shared capsule visibility
- Room-backed filtering: all, locked, unlocked, shared

### Unlock engine
- WorkManager time worker runs every 15 minutes
- WorkManager location worker runs every 30 minutes
- Automatic unlock + local notification when conditions pass

### Mapping
- OSMDroid (OpenStreetMap), not Google Maps rendering
- Marker data from Room flows
- User location overlay and nearby capsule checks
- Clue circle behavior for locked location capsules

### Cloud + identity
- Firebase Authentication
- Firestore for remote capsule/sharing/chat data
- Firebase Cloud Messaging integration

### UX and architecture
- MVVM + clean layering
- LiveData and Kotlin Flow for reactive updates
- Manual DI via ServiceLocator
- Material 3 theme with day/night support and style tokens

## Architecture at a glance

```text
UI (Activities/Fragments/Adapters)
  -> ViewModels (state + presentation logic)
    -> Repositories (domain/data orchestration)
      -> Local: Room + Preferences
      -> Remote: Firebase + Retrofit API
```

Important modules in codebase:
- `data/local`: Room database, DAOs, entities, converters
- `data/repository`: auth, capsules, sharing, chat, notifications, user
- `data/remote`: Firebase services and REST clients
- `ui/*`: feature packages by screen
- `workers/*`: periodic unlock jobs
- `services/*`: FCM + foreground location service
- `utils/*`: image conversion, distance helpers, notification utilities

## Tech stack

- Kotlin 2.0.x
- Android Gradle Plugin 8.12.x
- Android SDK: min 26, target 36, compile 36
- Room 2.6.x
- WorkManager 2.9.x
- Firebase (Auth, Firestore, Messaging)
- Retrofit + OkHttp + Gson
- OSMDroid for map rendering
- Glide for image loading

## Requirements

- Android Studio (latest stable)
- JDK 11
- Android SDK 36 installed
- A Firebase project configured for Android

## Setup

1. Clone the repo.
2. Open the project in Android Studio.
3. Ensure `app/google-services.json` is present and matches your Firebase app.
4. Sync Gradle.
5. Build and run on an emulator/device (Android 8.0+).

## Run from terminal

```bash
# Windows
.\gradlew.bat assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

Install debug APK from:
- `app/build/outputs/apk/debug/`

## Permissions used

ChronoVault requests permissions for:
- Network access
- Fine/coarse/background location
- Notifications
- Foreground location service
- Camera and image/media access

These are required for map-based unlocks, reminders, and media-backed capsules.

## Data model notes

The primary entity is `CapsuleEntity`, including:
- Ownership and sharing fields
- Lock mode flags (time/location)
- Coordinates and unlock coordinates
- Unlock state and timestamps
- Optional Base64 image payload + mime type

Comments are represented with `CommentEntity` tied to capsule IDs.

## Project docs

- `ARCHITECTURE.md`: package map, flows, unlock logic, design system
- `API_REFERENCE.md`: ViewModels, repositories, utility contracts
- `logs.md`: implementation status and page-by-page behavior tracking

## Development notes

- Database is currently configured with destructive migration fallback.
- WorkManager scheduling is initialized by application startup wiring.
- Home and map screens actively consume reactive data updates.
- Deep-link style chat navigation is handled in `MainActivity` from notification extras.

## Roadmap ideas

- End-to-end instrumentation coverage for lock/unlock flows
- Better offline conflict handling for cloud sync
- Capsule media storage optimization beyond Base64 payloads
- Stronger role/permission model around shared capsules

## License

No license file is currently included in this repository.
If you plan to distribute this project, add a `LICENSE` file at repo root.
