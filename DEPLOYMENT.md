# 🚀 ChronoVault — Deployment Checklist

*Last updated: February 26, 2026*

## ✅ Fixed Issues (Feb 26, 2026)

- **Map markers not appearing**: Root cause was capsules saved with (0,0) coordinates because GPS was never captured on creation. Fixed by adding auto-GPS capture in `CreateCapsuleActivity` using `FusedLocationProviderClient.getCurrentLocation()`.
- **MapViewModel used wrong query**: Changed from `getUserCapsules()` to `getCapsulesForMap()` which excludes (0,0) coordinates.
- **Map not refreshing after capsule creation**: Added `loadMapData()` call in `MapFragment.onResume()`.
- **`setupActionBarWithNavController` crash**: Removed — incompatible with `NoActionBar` theme.
- **Deprecated `<fragment>` tag**: Replaced with `FragmentContainerView` in `activity_main.xml` and `activity_auth.xml`.
- **Hardcoded colors in layouts**: All replaced with `?attr/` theme attributes for proper dark mode.
- **`unlockLatitude/Longitude` never set**: Now set to capsule coordinates when `isLocationBased` is enabled.

---

## 🔴 Must-Do Before Release

### 1. Configure Firebase
- Place `google-services.json` at `app/google-services.json`
- Enable **Email/Password** Authentication
- Create **Firestore** database
- Enable **Cloud Messaging**

### 2. Apply Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
    match /capsules/{capsuleId} {
      allow create: if request.auth != null;
      allow read, update, delete: if request.auth.uid == resource.data.ownerId;
      allow read: if request.auth.token.email in resource.data.sharedWith;
    }
  }
}
```

### 3. Test on Physical Device
GPS location capture requires a real device or an emulator with a configured mock location. Set a location in the emulator's Extended Controls → Location panel.

---

## 🟡 Recommended Before Release

### 4. Add Custom Map Marker Icons
`MapFragment.getMarkerIcon()` is currently a stub returning `null` (default OSMDroid marker). Create colored vector drawables for:
- 🟢 Unlocked
- 🔴 Locked
- 🔵 Nearby
- 🟡 Shared

### 5. Wire Notification List to Real Events
`NotificationsFragment` currently shows mock data. Wire it to a Room table or SharedPreferences store that `TimeBasedUnlockWorker` and `LocationBasedUnlockWorker` write to.

### 6. Implement Sharing Mutual Consent
Current sharing flow is one-way (User A shares → User B sees). Add:
- Share request notification
- Accept/reject UI in `NotificationsFragment`
- Update Firestore `sharedWith` only on acceptance

### 7. Add Glide Image Loading
Replace raw `setImageBitmap()` with Glide for memory-safe loading & disk caching.

### 8. FCM Server Key
To send push notifications from a backend:
- Firebase Console → Project Settings → Cloud Messaging → **Server key**

---

## 🟢 Nice-to-Have

### 9. Capsule Unlock Animation
Add a Lottie animation when a capsule is unlocked in `CapsuleDetailsActivity`.

### 10. Empty State UI
Show placeholder (icon + message) when capsule list / notifications list is empty.

### 11. Shimmer Loading
Replace `ProgressBar` with shimmer animation (use `com.facebook.shimmer:shimmer`).

### 12. Crash Reporting
Add Firebase Crashlytics:
```kotlin
implementation("com.google.firebase:firebase-crashlytics-ktx")
```

---

## 📋 Pre-Release Testing Checklist

- [ ] Create capsule with title + message + image → verify it appears in list
- [ ] Verify capsule appears on map at correct GPS location
- [ ] Create time-locked capsule → wait for WorkManager → verify unlock notification
- [ ] Create location-locked capsule → walk to location → verify proximity unlock
- [ ] Test login → logout → login flow
- [ ] Test dark mode toggle
- [ ] Test on Android 8 (API 26) and Android 14 (API 34)
- [ ] Verify Firestore security rules block unauthorized access
- [ ] Test with airplane mode (offline Room operations)
