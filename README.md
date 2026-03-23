# 🕰️ ChronoVault

> **Preserve Moments. Rediscover Yourself.**

ChronoVault is a production-grade Android app that lets users create digital time capsules — sealed with memories, photos, and messages — that unlock based on a scheduled date or physical location proximity. Shared capsules support collaborative commenting.

---

## 📊 Project Status

| Area | Status | Notes |
|------|--------|-------|
| Authentication (Firebase) | ✅ Working | Email/password login & signup |
| Capsule CRUD | ✅ Working | Create, view, edit, delete with Room + Firestore sync |
| GPS Location Capture | ✅ Working | Auto-captured on capsule creation |
| Map Display (OSMDroid) | ✅ Working | Capsule markers at real GPS coordinates |
| Time-Based Unlock | ✅ Working | WorkManager checks every 15 min |
| Location-Based Unlock | ✅ Working | WorkManager checks every 30 min (100m radius) |
| Notifications | ✅ Working | Local notifications on unlock events |
| Sharing System | ✅ Working | Share by email, view shared-with list, remove individual shares |
| Make Private | ✅ Working | Revoke all shares and disable sharing with one tap |
| Comments on Shared Capsules | ✅ Working | Comment thread on any shared capsule; delete own comments |
| Capsule Details | ✅ Working | Full details view with location, unlock status, sharing, comments |
| **Home Hub: Dynamic Greeting + Context Subtitle** | 🚧 In Progress | Time-of-day greeting with user context is being refined |
| **Home Hub: Interactive Stats Grid** | 🚧 In Progress | 4-card clickable grid (Total/Locked/Unlocked/Shared) wired to filtered navigation |
| **Home Hub: Recent Memories Section** | 🚧 In Progress | RecyclerView of latest 2-3 capsules with quick-open details |
| **Home Hub: Nearby Memory Card** | 🚧 In Progress | Conditional card shown when user is within 100m of a capsule |
| **Home Hub: Empty State Handling** | 🚧 In Progress | Friendly first-use state with stats/recent sections hidden when empty |
| Dark Mode | ✅ Working | Full Material 3 light + dark theme |
| Design System | ✅ Working | Centralized theme attrs, no hardcoded colors |

---

## 📱 Screens

```
SplashActivity → OnboardingActivity → AuthActivity
                                        ├── LoginFragment
                                        └── SignupFragment

MainActivity (Bottom Navigation — 5 tabs)
├── HomeFragment        — Dynamic memory hub (personalized greeting, daily quote, interactive stats, recent memories, contextual nearby-memory prompt, create CTA)
├── CapsulesFragment    — Capsule list with chip filters (All / Locked / Unlocked / Shared)
│   ├── CreateCapsuleActivity (auto GPS capture)
│   └── CapsuleDetailsActivity (details, sharing, comments, make-private)
├── MapFragment         — OSMDroid map with capsule markers
├── NotificationsFragment — Unlock & sharing events
└── ProfileFragment     — Account management & logout
```

---

## ✨ Latest Enhancements

- Home dashboard upgrade is underway to make the experience more contextual and memory-focused.
- Interactive stat cards are being aligned with filtered navigation into `CapsulesFragment`.
- "Recent Memories" list and conditional "You're near a memory" card are being integrated with ViewModel-driven data.
- Empty-state UX is being polished for first-time users with no capsules.

---

## 🏆 Key Features

### 🔐 Authentication
- Email/password sign-up and login via Firebase Auth
- Persistent sessions; logout clears local state

### 📦 Capsule Management
- Create capsules with title, message, and image (Base64, auto-compressed)
- **GPS location automatically captured** on capsule creation
- Set a future unlock date (time-based)
- Enable location-based unlock (within 100m of capsule GPS coordinate)
- Filter: All / Locked / Unlocked / Shared
- View detailed capsule info including coordinates, unlock conditions

### 🤝 Sharing & Collaboration
- **Share capsules** by entering a user's email address
- **View shared-with list** — see who has access to your capsule
- **Remove individual shares** — revoke access per user
- **Make Private** — one-tap button to revoke all shares and disable sharing
- **Comments** — leave comments on shared capsules; delete your own comments
- Comments visible to capsule owner and all shared users
- Comment input bar appears only when capsule is shared

### 🗺️ Map (OSMDroid / OpenStreetMap)
- OpenStreetMap tiles via OSMDroid (no Google Maps dependency)
- Capsule markers placed at real GPS coordinates
- User's current location overlay
- Marker tap → bottom sheet capsule preview
- Capsules at (0,0) are filtered out (invalid coordinates)
- Map reloads data on resume (picks up newly created capsules)

### 🔔 Notifications
- Local notifications when a capsule unlocks (time or location)
- Notification channels with customizable sound & vibration
- In-app notification list with read/delete/clear

### 🌍 Background Services
- `TimeBasedUnlockWorker` — runs every 15 min via WorkManager
- `LocationBasedUnlockWorker` — runs every 30 min via WorkManager
- `ForegroundLocationService` — optional continuous GPS tracking
- `WorkScheduler` — initialized in `ChronoVaultApplication.onCreate()`

---

## 🗄️ Database Schema

### CapsuleEntity (Room)
| Field | Type | Description |
|-------|------|-------------|
| `id` | String (PK) | UUID |
| `title` | String | Capsule title |
| `message` | String | Capsule message body |
| `imageBase64` | String? | Compressed Base64 image |
| `latitude` / `longitude` | Double | GPS coordinates |
| `unlockTime` | Long? | Time-based unlock timestamp |
| `unlockLatitude` / `unlockLongitude` | Double? | Location-based unlock coords |
| `isUnlocked` | Boolean | Whether capsule is open |
| `isTimeBased` / `isLocationBased` | Boolean | Unlock method flags |
| `ownerId` | String | Firebase Auth UID |
| `sharedWith` | List\<String\> | Shared user emails |
| `canBeShared` | Boolean | Sharing enabled flag |

### CommentEntity (Room)
| Field | Type | Description |
|-------|------|-------------|
| `id` | String (PK) | UUID |
| `capsuleId` | String | Parent capsule ID |
| `authorId` | String | Firebase Auth UID |
| `authorName` | String | Display name of commenter |
| `text` | String | Comment body |
| `createdAt` | Long | Timestamp |

---

## 🎨 Design System

| Token | Light | Dark |
|-------|-------|------|
| Primary | `#2f6f5e` Metallic Green | `#4c8c7a` |
| Secondary | `#d6a75f` Soft Ochre | `#e8c896` |
| Background | `#f4f7f5` | `#0f172a` |
| Surface | `#ffffff` | `#111827` |
| Error | `#ef4444` | `#f87171` |
| Font | Roboto | Roboto |
| Card radius | 12dp | 12dp |
| Button radius | 24dp | 24dp |

All layouts use `?attr/` theme attributes — zero hardcoded colors.

---

## 📂 Documentation

| File | Contents |
|------|----------|
| `README.md` | This file — overview, features, status |
| `SETUP.md` | Firebase & build instructions |
| `ARCHITECTURE.md` | Code structure, MVVM patterns, data flow |
| `FEATURES.md` | Detailed feature implementation status |
| `API_REFERENCE.md` | Key classes, ViewModels, repositories |
| `DEPLOYMENT.md` | Pre-release checklist & known issues |

---

*Last updated: March 17, 2026*
