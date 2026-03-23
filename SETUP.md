# ⚙️ ChronoVault — Setup Guide

*Last updated: February 26, 2026*

## Prerequisites

- Android Studio (Hedgehog or later)
- Android SDK 26+
- A Firebase account

---

## 1. Firebase Setup

### 1.1 Create Firebase Project
1. Go to [https://console.firebase.google.com/](https://console.firebase.google.com/)
2. **Create a project** → name it `ChronoVault`
3. Register an Android app with package `com.example.chronovault`
4. Download `google-services.json` and place it at `app/google-services.json`

### 1.2 Enable Authentication
1. Firebase Console → **Authentication** → **Get Started**
2. Sign-in method → **Email/Password** → Enable → Save

### 1.3 Create Firestore Database
1. Firebase Console → **Firestore Database** → **Create database**
2. Start in **Test mode** for development
3. Apply production rules before release (see `DEPLOYMENT.md`)

### 1.4 Enable Cloud Messaging (FCM)
1. Firebase Console → **Cloud Messaging** — enabled by default for new projects

---

## 2. Map Setup (OSMDroid)

ChronoVault uses **OSMDroid** (OpenStreetMap) — **no Google Maps API key required**.

OSMDroid is already configured in `build.gradle.kts`:
```
implementation("org.osmdroid:osmdroid-android:6.1.18")
```

The map uses MAPNIK tile source (free, no API key).
User agent is set to the app's package name in `MapFragment.onViewCreated()`.

---

## 3. Build & Run

```
1. Open project in Android Studio
2. File → Sync Project with Gradle Files
3. Run → Run 'app'  (select emulator or device)
```

**Important**: If using an emulator, set a GPS location in the emulator's Extended Controls → Location panel, otherwise capsule creation will save (0,0) coordinates.

---

## 4. Firestore Data Structure

```
users/{userId}
├── email        : String
├── name         : String
├── avatarBase64 : String  (optional)
├── createdAt    : Long
└── updatedAt    : Long

capsules/{capsuleId}
├── title            : String
├── message          : String
├── imageBase64      : String  (optional, ≤2 MB after compression)
├── imageMimeType    : String
├── latitude         : Double
├── longitude        : Double
├── unlockTime       : Long?   (null = no time lock)
├── unlockLatitude   : Double?
├── unlockLongitude  : Double?
├── isLocationBased  : Boolean
├── isTimeBased      : Boolean
├── isUnlocked       : Boolean
├── ownerId          : String  (Firebase Auth UID)
├── sharedWith       : List<String>  (user emails)
├── canBeShared      : Boolean
└── createdAt        : Long
```

---

## 5. Permissions Overview

All permissions are declared in `AndroidManifest.xml`:

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Firebase, API calls, OSMDroid tiles |
| `ACCESS_FINE_LOCATION` | GPS for map & capsule creation |
| `ACCESS_BACKGROUND_LOCATION` | Background unlock worker |
| `POST_NOTIFICATIONS` | Capsule unlock alerts (Android 13+) |
| `FOREGROUND_SERVICE` | Location service |
| `FOREGROUND_SERVICE_LOCATION` | Location service type |
| `READ_MEDIA_IMAGES` | Image picker (Android 13+) |
| `ACCESS_NETWORK_STATE` | Network availability checks |

---

## 6. Room Database

- **Database name**: `chronovault_db`
- **Version**: 1
- **Entity**: `CapsuleEntity` (see `ARCHITECTURE.md` for full schema)
- **TypeConverters**: `List<String> ↔ comma-separated String`
- **Note**: If you change the schema, increment the version and add a migration or use `fallbackToDestructiveMigration()`
