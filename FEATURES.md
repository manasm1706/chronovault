# ✅ ChronoVault — Feature Implementation Status

*Last updated: February 26, 2026*

## Authentication

| Feature | Status | Notes |
|---------|--------|-------|
| Email/password sign-up | ✅ Done | `SignupFragment` + `SignupViewModel` + `FirebaseAuthService` |
| Email/password login | ✅ Done | `LoginFragment` + `LoginViewModel` |
| Session persistence | ✅ Done | Firebase Auth + `PreferencesManager` |
| Logout | ✅ Done | Clears prefs + redirects to `AuthActivity` |
| Account deletion | ✅ Done | `ProfileViewModel.deleteAccount()` |

---

## Capsule Management

| Feature | Status | Notes |
|---------|--------|-------|
| Create capsule (title + message) | ✅ Done | `CreateCapsuleActivity` |
| Attach image (Base64) | ✅ Done | `ImageConverter` auto-compresses to ≤2 MB |
| **Auto-capture GPS location** | ✅ Done | `FusedLocationProviderClient.getCurrentLocation()` on create |
| Set time-based unlock date | ✅ Done | `MaterialDatePicker` → stored as `unlockTime` |
| Enable location-based unlock | ✅ Done | Toggle sets `isLocationBased` + `unlockLatitude/Longitude` |
| Save to Room (offline) | ✅ Done | `CapsuleDao.insertCapsule()` |
| Sync to Firestore | ✅ Done | `CapsuleRepository.createCapsuleOnFirebase()` |
| List capsules | ✅ Done | `CapsulesFragment` + `CapsulesAdapter` |
| Filter All / Locked / Unlocked / Shared | ✅ Done | `CapsulesViewModel.setFilter()` |
| View capsule details | ✅ Done | `CapsuleDetailsActivity` |
| Delete capsule | ✅ Done | Owner-only, removes from Room |
| Manual unlock | ✅ Done | `CapsuleDetailsViewModel.unlockCapsule()` |
| Automatic time unlock | ✅ Done | `TimeBasedUnlockWorker` every 15 min |
| Automatic location unlock | ✅ Done | `LocationBasedUnlockWorker` every 30 min, 100m radius |

---

## Map (OSMDroid / OpenStreetMap)

| Feature | Status | Notes |
|---------|--------|-------|
| OSMDroid map display | ✅ Done | `MapFragment` with MAPNIK tiles |
| Capsule markers at GPS coords | ✅ Done | `GeoPoint(capsule.latitude, capsule.longitude)` |
| Skip invalid (0,0) coordinates | ✅ Done | DAO query + runtime filter in `updateMarkers()` |
| User current location | ✅ Done | `MyLocationNewOverlay` + `FusedLocationProviderClient` |
| Marker click → bottom sheet | ✅ Done | `CapsulePreviewBottomSheet` |
| Auto-refresh on resume | ✅ Done | `MapFragment.onResume()` calls `loadMapData()` |
| Camera center on user | ✅ Done | `mapView.controller.animateTo()` |
| Marker color differentiation | ⚠️ Stub | `getMarkerIcon()` scaffolded, returns default marker |
| Nearby capsule detection | ✅ Done | Haversine formula, 100m threshold |

---

## Sharing & Collaboration

| Feature | Status | Notes |
|---------|--------|-------|
| Share capsule by email | ✅ Done | `CapsuleDetailsViewModel.shareCapsule()` + AlertDialog with EditText |
| View shared-with list | ✅ Done | `SharedWithAdapter` RecyclerView on details screen |
| Remove individual share | ✅ Done | Per-email "Remove" button via `unshareCapsule()` |
| Make capsule private | ✅ Done | Revokes all shares + disables sharing flag via `makeCapsulePrivate()` |
| Comments on shared capsules | ✅ Done | `CommentEntity` + `CommentDao` + `CommentsAdapter` |
| Delete own comments | ✅ Done | Delete button on comments authored by current user |
| Comment input bar | ✅ Done | Appears only when capsule is shared |
| Mutual consent flow | 🔴 Not started | Accept/reject not implemented |

---

## Notifications

| Feature | Status | Notes |
|---------|--------|-------|
| Local unlock notifications | ✅ Done | `NotificationHelper.sendCapsuleUnlockedNotification()` |
| Location-based notifications | ✅ Done | `NotificationHelper.sendLocationBasedUnlockNotification()` |
| Notification channels (Android 8+) | ✅ Done | Created in `ChronoVaultApplication.onCreate()` |
| In-app notification list | ✅ Done | `NotificationsFragment` + `NotificationsAdapter` |
| Mark as read / delete / clear | ✅ Done | `NotificationsViewModel` |
| FCM push notifications | ✅ Done | `ChronoVaultMessagingService` (requires server-side setup) |

---

## User Profile

| Feature | Status | Notes |
|---------|--------|-------|
| Display name & email | ✅ Done | `ProfileFragment` |
| Update display name | ✅ Done | `ProfileViewModel.updateName()` |
| Upload avatar (Base64) | ✅ Done | `ProfileViewModel.updateAvatar()` |
| View capsule stats | ✅ Done | `HomeFragment` stats cards |
| Logout | ✅ Done | `MainActivity.logout()` |
| Delete account | ✅ Done | `ProfileViewModel.deleteAccount()` |

---

## Background Services

| Feature | Status | Notes |
|---------|--------|-------|
| `TimeBasedUnlockWorker` | ✅ Done | Every 15 min, checks `unlockTime <= now` |
| `LocationBasedUnlockWorker` | ✅ Done | Every 30 min, Haversine ≤100m |
| `ForegroundLocationService` | ✅ Done | Optional continuous tracking |
| `WorkScheduler` | ✅ Done | Started in `ChronoVaultApplication.onCreate()` |

---

## Design System

| Feature | Status | Notes |
|---------|--------|-------|
| Material 3 theme | ✅ Done | `Theme.Material3.DayNight.NoActionBar` |
| Light mode | ✅ Done | `values/themes.xml` + `values/colors.xml` |
| Dark mode | ✅ Done | `values-night/themes.xml` + `values-night/colors.xml` |
| No hardcoded colors in layouts | ✅ Done | All use `?attr/` theme attributes |
| Reusable component styles | ✅ Done | Cards, buttons, inputs, badges, bottom nav |
| Spacing system | ✅ Done | `dimens.xml` with xs/sm/md/lg/xl/xxl |

---

## Known Issues / Next Steps

| Item | Priority | Notes |
|------|----------|-------|
| Marker color differentiation | 🟡 Medium | `getMarkerIcon()` stub exists; needs custom colored drawables |
| Sharing mutual consent flow | 🟡 Medium | Accept/reject UI not built; currently one-way share |
| Capsule unlock animation | 🟢 Low | Placeholder text; no Lottie animation yet |
| Notifications from real events | 🟡 Medium | List shows mock data; wire to Room/WorkManager events |
| Empty state screens | 🟢 Low | Show placeholder when lists empty |
| Glide for image loading | 🟢 Low | Currently using raw `setImageBitmap()` |
| Firestore security rules | 🔴 High | Still in test mode |
| Comment sync to Firestore | 🟡 Medium | Comments stored locally only; not synced to cloud |

*Last updated: February 27, 2026*
