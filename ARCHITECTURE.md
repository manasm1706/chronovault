# 🏗️ ChronoVault — Architecture

*Last updated: February 26, 2026*

## Pattern: MVVM + Clean Architecture

```
┌─────────────────────────────────────┐
│  UI Layer                           │
│  Fragments / Activities / Adapters  │
│  Observes LiveData from ViewModels  │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│  ViewModel Layer  (8 ViewModels)    │
│  State management, business logic   │
│  LiveData + sealed LoadingState     │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│  Repository Layer  (4 Repositories) │
│  Data abstraction & sync logic      │
└──────┬──────────────────┬───────────┘
       │                  │
┌──────▼──────┐   ┌───────▼────────┐
│ Local Data  │   │  Remote Data   │
│ Room + Prefs│   │  Firebase      │
│             │   │  ├─ Auth       │
│             │   │  ├─ Firestore  │
│             │   │  └─ FCM        │
└─────────────┘   └────────────────┘
```

---

## 📁 Package Structure

```
com.example.chronovault/
├── ChronoVaultApplication.kt      — App init, WorkManager scheduling
├── MainActivity.kt                — Bottom navigation host (FragmentContainerView)
│
├── data/
│   ├── local/
│   │   ├── ChronoVaultDatabase.kt — Room database (v1)
│   │   ├── CapsuleDao.kt          — DAO queries (CRUD, filters, map, counts)
│   │   ├── Converters.kt          — TypeConverter: List<String> ↔ String
│   │   └── entity/
│   │       └── CapsuleEntity.kt   — Room entity (34 fields)
│   ├── remote/
│   │   ├── api/
│   │   │   └── QuoteApi.kt        — Daily quotes REST API
│   │   ├── firebase/
│   │   │   ├── FirebaseServices.kt     — Auth + Firestore + Sharing services
│   │   │   └── FirebaseUserService.kt  — User profile CRUD
│   │   └── RetrofitClient.kt      — Retrofit singleton
│   ├── repository/
│   │   ├── AuthRepository.kt      — Login, signup, session
│   │   ├── CapsuleRepository.kt   — CRUD + Firestore sync + map queries
│   │   ├── SharingRepository.kt   — Share/unshare capsules
│   │   └── UserRepository.kt      — Profile management
│   └── ServiceLocator.kt          — Manual DI (lazy singletons)
│
├── ui/
│   ├── auth/         — AuthActivity, LoginFragment, SignupFragment + ViewModels
│   ├── splash/       — SplashActivity (2s delay → onboarding/auth/main)
│   ├── onboarding/   — OnboardingActivity (ViewPager2, 3 pages)
│   ├── home/         — HomeFragment + HomeViewModel (stats, quote)
│   ├── capsules/     — CapsulesFragment, CreateCapsuleActivity,
│   │                   CapsuleDetailsActivity + ViewModels + Adapter
│   ├── map/          — MapFragment + MapViewModel + CapsulePreviewBottomSheet
│   ├── notifications/ — NotificationsFragment + ViewModel + Adapter
│   ├── profile/      — ProfileFragment + ProfileViewModel
│   └── common/       — UiState.kt (sealed LoadingState)
│
├── services/
│   ├── ChronoVaultMessagingService.kt  — FCM push handling
│   └── ForegroundLocationService.kt    — Continuous GPS tracking
│
├── workers/
│   ├── TimeBasedUnlockWorker.kt     — Every 15 min
│   ├── LocationBasedUnlockWorker.kt — Every 30 min
│   └── WorkScheduler.kt            — Schedules both workers
│
└── utils/
    ├── ImageConverter.kt     — Uri ↔ Base64, auto-compress
    ├── LocationHelper.kt     — Haversine distance, proximity check
    ├── NotificationHelper.kt — Channels, local notifications
    ├── PreferencesManager.kt — SharedPreferences wrapper
    └── Extensions.kt         — Kotlin extensions (formatDate, isValidEmail, etc.)
```

---

## 🗄️ CapsuleEntity Schema

```kotlin
@Entity(tableName = "capsules")
data class CapsuleEntity(
    @PrimaryKey val id: String,          // UUID
    val title: String,
    val message: String,
    val imageBase64: String?,            // Base64-encoded image (≤2MB)
    val latitude: Double,               // Capsule creation GPS lat
    val longitude: Double,              // Capsule creation GPS lng
    val createdAt: Long,                // System.currentTimeMillis()
    val unlockTime: Long?,              // null = no time lock
    val unlockLatitude: Double?,        // GPS lat for proximity unlock
    val unlockLongitude: Double?,       // GPS lng for proximity unlock
    val isUnlocked: Boolean,
    val isLocationBased: Boolean,
    val isTimeBased: Boolean,
    val ownerId: String,                // Firebase Auth UID
    val sharedWith: List<String>,       // List of user emails
    val canBeShared: Boolean,
    val isSharedWithMe: Boolean,
    val sharedByName: String?,
    val sharedAt: Long?,
    val imageMimeType: String           // "image/jpeg" etc.
)
```

### CommentEntity Schema

```kotlin
@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,          // UUID
    val capsuleId: String,               // FK to CapsuleEntity.id
    val authorId: String,                // Firebase Auth UID
    val authorName: String,              // Display name at time of posting
    val text: String,                    // Comment body
    val createdAt: Long                  // System.currentTimeMillis()
)
```

**Database version**: 2 (with `fallbackToDestructiveMigration`)

---

## 🤝 Sharing & Comments Flow

```
Owner taps "Share" → AlertDialog with email input
  → CapsuleDetailsViewModel.shareCapsule(email)
    → SharingRepository.shareCapsuleWithUser() → Firestore
    → CapsuleRepository.updateSharingEnabled() → Room
    → SharedWithAdapter updates list in real time

Owner taps "Make Private"
  → CapsuleDetailsViewModel.makeCapsulePrivate()
    → CapsuleRepository.makeCapsulePrivate()
      → CapsuleDao.updateSharingEnabled(id, false)
      → CapsuleDao.clearSharedWith(id)
    → UI hides all sharing controls

Comment flow:
  → User types in comment input → taps "Send"
    → CapsuleDetailsViewModel.addComment(text)
      → CommentDao.insertComment(CommentEntity)
      → Room Flow auto-pushes to CommentsAdapter
  → Delete: CommentDao.deleteComment(id, userId)
    → Only own comments deletable
```

---

## 🔓 Unlock Logic Flow

### Time-Based Unlock
```
WorkScheduler → TimeBasedUnlockWorker (every 15 min)
  1. Query: SELECT * FROM capsules WHERE isTimeBased=1 AND unlockTime <= now AND isUnlocked=0
  2. For each result: UPDATE capsules SET isUnlocked=1
  3. Send local notification via NotificationHelper
  4. Room Flow auto-notifies UI observers
```

### Location-Based Unlock
```
WorkScheduler → LocationBasedUnlockWorker (every 30 min)
  1. Check if location tracking is enabled (PreferencesManager)
  2. Get current location via FusedLocationProviderClient.lastLocation
  3. Query: SELECT * FROM capsules WHERE isLocationBased=1 AND isUnlocked=0
  4. For each capsule: calculate Haversine distance to (capsule.latitude, capsule.longitude)
  5. If distance ≤ 100m: UPDATE capsules SET isUnlocked=1 + send notification
```

### Haversine Formula (LocationHelper.kt)
```
distance(lat1, lon1, lat2, lon2):
  R = 6,371,000 m (Earth radius)
  dLat = toRadians(lat2 - lat1)
  dLon = toRadians(lon2 - lon1)
  a = sin²(dLat/2) + cos(lat1) * cos(lat2) * sin²(dLon/2)
  c = 2 * atan2(√a, √(1-a))
  return R * c  (meters)
```

---

## 🗺️ Map System

**Provider**: OSMDroid (OpenStreetMap) — NOT Google Maps

**Data Flow**:
```
Room DB (capsules table)
  → CapsuleDao.getCapsulesForMap(userId)     // Excludes (0,0) coordinates
    → CapsuleRepository.getCapsulesForMap()  // Returns Flow<List<CapsuleEntity>>
      → MapViewModel.capsuleMarkers          // LiveData observed by Fragment
        → MapFragment.updateMarkers()        // Creates OSMDroid Marker objects
          → GeoPoint(capsule.latitude, capsule.longitude)
```

**Key behaviors**:
- Map reloads data on `onResume()` to pick up newly created capsules
- Capsules with lat=0, lng=0 are excluded (never had GPS captured)
- Marker click opens `CapsulePreviewBottomSheet`
- User location via `MyLocationNewOverlay` + `FusedLocationProviderClient`

---

## 🔔 Notification Triggers

| Trigger | Source | Method |
|---------|--------|--------|
| Time unlock | `TimeBasedUnlockWorker` | `NotificationHelper.sendCapsuleUnlockedNotification()` |
| Location proximity | `LocationBasedUnlockWorker` | `NotificationHelper.sendLocationBasedUnlockNotification()` |
| Shared capsule | `FirebaseSharingService` | `NotificationHelper.sendSharedNotification()` |
| FCM push | `ChronoVaultMessagingService` | Routes by `type` field in data payload |

---

## 🎨 Design System

All layouts reference `?attr/` theme attributes. Zero hardcoded colors.

- **Theme base**: `Theme.Material3.DayNight.NoActionBar`
- **Light + Dark**: `values/colors.xml` + `values-night/colors.xml`
- **Reusable styles**: `Widget.ChronoVault.Button.Primary`, `.Secondary`, `.Outlined`, `.Card`, `.TextInput`, `.BottomNav`
- **Typography**: `TextAppearance.ChronoVault.*` (Display, Headline, Title, Body, Label, Caption)
- **Spacing**: `dimens.xml` with `spacing_xs` (4dp) through `spacing_xxxl` (48dp)

---

## 🔗 Navigation

- **Auth flow**: `auth_navigation.xml` → LoginFragment ↔ SignupFragment
- **Main flow**: `mobile_navigation.xml` → 5-tab bottom nav (Home, Capsules, Map, Notifications, Profile)
- `CreateCapsuleActivity` and `CapsuleDetailsActivity` launched via explicit `Intent`
- `FragmentContainerView` used (not deprecated `<fragment>` tag)
