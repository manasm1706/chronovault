# 📖 ChronoVault — API Reference


*Last updated: February 26, 2026*

## ViewModels

### HomeViewModel
| Member | Type | Description |
|--------|------|-------------|
| `userName` | `LiveData<String>` | Current user's display name |
| `dailyQuote` | `LiveData<String>` | Quote fetched from REST API |
| `totalCapsules` | `LiveData<Int>` | All capsule count |
| `lockedCapsules` | `LiveData<Int>` | Locked capsule count |
| `unlockedCapsules` | `LiveData<Int>` | Unlocked capsule count |
| `sharedCapsules` | `LiveData<Int>` | Shared capsule count |
| `loadingState` | `LiveData<LoadingState>` | UI state |
| `getGreeting()` | `String` | Returns "Good Morning/Afternoon/Evening" |

### CapsulesViewModel
| Member | Type | Description |
|--------|------|-------------|
| `capsulesList` | `LiveData<List<CapsuleEntity>>` | Active filter results |
| `loadingState` | `LiveData<LoadingState>` | UI state |
| `createCapsuleState` | `LiveData<CreateCapsuleState>` | Creation progress |
| `capsuleImageBase64` | `LiveData<String?>` | Selected image as Base64 |
| `setFilter(filter)` | `fun` | Switch ALL / LOCKED / UNLOCKED / SHARED |
| `setTitle(title)` | `fun` | Set capsule title |
| `setMessage(msg)` | `fun` | Set capsule message |
| `setLocation(lat, lng)` | `fun` | Set GPS coordinates |
| `setUnlockDate(ts)` | `fun` | Set time-based unlock timestamp |
| `setLocationBased(bool)` | `fun` | Enable location-based unlock |
| `setShareable(bool)` | `fun` | Enable sharing |
| `setImageFromUri(ctx, uri)` | `fun` | Convert image to Base64 |
| `createCapsule()` | `fun` | Validates, inserts to Room, syncs Firestore |
| `deleteCapsule(id)` | `fun` | Removes from Room |

### CapsuleDetailsViewModel
| Member | Type | Description |
|--------|------|-------------|
| `capsule` | `LiveData<CapsuleEntity>` | Selected capsule |
| `loadingState` | `LiveData<LoadingState>` | UI state |
| `unlockCapsule(id)` | `fun` | Marks capsule unlocked |
| `shareCapsule(id, email)` | `fun` | Delegates to `SharingRepository` |
| `deleteCapsule(id)` | `fun` | Owner-only delete |

### MapViewModel
| Member | Type | Description |
|--------|------|-------------|
| `capsuleMarkers` | `LiveData<List<CapsuleEntity>>` | Capsules with valid coordinates |
| `userLocation` | `LiveData<Pair<Double,Double>?>` | Current device lat/lng |
| `nearbyCapsules` | `LiveData<List<CapsuleEntity>>` | Within 100m |
| `selectedCapsule` | `LiveData<CapsuleEntity?>` | Clicked marker |
| `loadMapData()` | `fun` | Loads from `getCapsulesForMap()` (excludes 0,0) |
| `setUserLocation(lat, lng)` | `fun` | Updates user position + checks nearby |
| `getCapsuleStatus(capsule)` | `fun` | Returns "Unlocked"/"Time-Locked"/etc. |

### NotificationsViewModel
| Member | Type | Description |
|--------|------|-------------|
| `notifications` | `LiveData<List<...>>` | All notification entries |
| `markAsRead(id)` | `fun` | Updates read status |
| `deleteNotification(id)` | `fun` | Removes entry |

### ProfileViewModel
| Member | Type | Description |
|--------|------|-------------|
| `userName` | `LiveData<String>` | Name |
| `userEmail` | `LiveData<String>` | Email |
| `userAvatar` | `LiveData<String?>` | Base64 avatar |
| `accountState` | `LiveData<AccountState>` | Logout/delete state |
| `updateName(name)` | `fun` | Updates Firestore + prefs |
| `updateAvatar(ctx, uri)` | `fun` | Converts to Base64, saves |
| `logout()` | `fun` | Signs out + clears state |
| `deleteAccount()` | `fun` | Deletes Firebase Auth + Firestore |

### LoginViewModel / SignupViewModel
| Member | Type | Description |
|--------|------|-------------|
| `loginState` / `signupState` | `LiveData<...State>` | Idle/Loading/Success/Error |
| `login(email, password)` | `fun` | Calls `AuthRepository.loginUser()` |
| `signup()` | `fun` | Calls `AuthRepository.registerUser()` |

---

## Repositories

### CapsuleRepository
```kotlin
fun getUserCapsules(userId: String): Flow<List<CapsuleEntity>>
fun getLockedCapsules(userId: String): Flow<List<CapsuleEntity>>
fun getUnlockedCapsules(userId: String): Flow<List<CapsuleEntity>>
fun getSharedCapsules(userId: String): Flow<List<CapsuleEntity>>
fun getCapsulesForMap(userId: String): Flow<List<CapsuleEntity>>  // Excludes (0,0)
suspend fun insertCapsule(capsule: CapsuleEntity)
suspend fun updateCapsule(capsule: CapsuleEntity)
suspend fun deleteCapsule(capsuleId: String)
suspend fun getCapsuleById(capsuleId: String): CapsuleEntity?
suspend fun unlockCapsule(capsuleId: String)
suspend fun checkTimeBasedUnlocks(currentTime: Long): List<CapsuleEntity>
suspend fun getLocationBasedCapsules(): List<CapsuleEntity>
suspend fun createCapsuleOnFirebase(data: Map<String, Any>): Result<String>
suspend fun fetchCapsuleFromCloud(capsuleId: String): Result<CapsuleEntity>
```

### AuthRepository
```kotlin
suspend fun loginUser(email: String, password: String): Flow<Result<String>>
suspend fun registerUser(email: String, password: String, name: String): Flow<Result<String>>
fun isUserLoggedIn(): Boolean
fun logoutUser()
```

### SharingRepository
```kotlin
suspend fun shareCapsuleWithUser(capsuleId: String, userEmail: String): Result<Unit>
suspend fun unshareCapsuleWithUser(capsuleId: String, userEmail: String): Result<Unit>
suspend fun getSharedWithMeCapsules(): Result<List<Map<String, Any>>>
```

### UserRepository
```kotlin
suspend fun getUserProfile(userId: String): Result<Map<String, Any>>
suspend fun updateUserName(userId: String, name: String): Result<Unit>
suspend fun updateUserAvatar(userId: String, base64: String): Result<Unit>
```

---

## CapsuleDao Key Queries

```kotlin
@Query("SELECT * FROM capsules WHERE ownerId = :ownerId ORDER BY createdAt DESC")
fun getUserCapsules(ownerId: String): Flow<List<CapsuleEntity>>

@Query("SELECT * FROM capsules WHERE (ownerId = :userId OR isSharedWithMe = 1) 
        AND NOT (latitude = 0.0 AND longitude = 0.0) ORDER BY createdAt DESC")
fun getCapsulesForMap(userId: String): Flow<List<CapsuleEntity>>

@Query("SELECT * FROM capsules WHERE isUnlocked = 0 AND isTimeBased = 1 AND unlockTime <= :currentTime")
suspend fun getUnlockedByTime(currentTime: Long): List<CapsuleEntity>

@Query("SELECT * FROM capsules WHERE isUnlocked = 0 AND isLocationBased = 1")
suspend fun getLocationBasedCapsules(): List<CapsuleEntity>
```

---

## Utilities

### ImageConverter
```kotlin
fun uriToBase64(context: Context, uri: Uri, quality: Int = 85): String?
fun compressBase64IfNeeded(base64: String, maxSizeMB: Double = 2.0): String
fun base64ToBitmap(base64: String): Bitmap?
fun getSizeInMB(base64: String): Double
```

### LocationHelper
```kotlin
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float
fun isWithinRadius(lat1: Double, lon1: Double, lat2: Double, lon2: Double,
                   radiusMeters: Float = 100f): Boolean
```

### NotificationHelper
```kotlin
fun createNotificationChannel(context: Context)
fun sendCapsuleUnlockedNotification(context: Context, title: String, message: String)
fun sendLocationBasedUnlockNotification(context: Context, title: String, message: String)
fun sendSharedNotification(context: Context, title: String, message: String)
fun showNotification(context: Context, notificationId: Int, builder: NotificationCompat.Builder)
```

### PreferencesManager
```kotlin
fun saveUserId(id: String)
fun getUserId(): String?
fun saveUserEmail(email: String)
fun getUserEmail(): String?
fun saveUserName(name: String)
fun getUserName(): String?
fun setFirstLaunch(isFirst: Boolean)
fun isFirstLaunch(): Boolean
fun isLocationTrackingEnabled(): Boolean
fun clearAll()
```

---

## State Classes

```kotlin
sealed class LoadingState {
    object Idle : LoadingState()
    object Loading : LoadingState()
    object Success : LoadingState()
    data class Error(val message: String) : LoadingState()
}

sealed class CreateCapsuleState {
    object Idle : CreateCapsuleState()
    object Loading : CreateCapsuleState()
    data class Success(val capsuleId: String) : CreateCapsuleState()
    data class Error(val message: String) : CreateCapsuleState()
}
```

---

## Workers

### TimeBasedUnlockWorker
- Runs every **15 minutes** via `WorkManager`
- Queries Room: `getUnlockedByTime(System.currentTimeMillis())`
- Marks matching capsules unlocked via `unlockCapsule(id)`
- Fires local notification via `NotificationHelper`

### LocationBasedUnlockWorker
- Runs every **30 minutes** via `WorkManager`
- Gets current location via `FusedLocationProviderClient.lastLocation`
- For each `isLocationBased` capsule: calculates Haversine distance
- Unlocks capsules within **100 m**, fires notification

---

## Image Flow (end-to-end)

```
1. User picks image → Uri (ActivityResultContracts.GetContent)
2. ImageConverter.uriToBase64(context, uri)         → String (Base64)
3. ImageConverter.compressBase64IfNeeded(base64)    → String (≤ 2 MB)
4. Stored in CapsuleEntity.imageBase64              (Room)
5. Synced to Firestore as "imageBase64" field
6. On display: ImageConverter.base64ToBitmap(base64) → Bitmap
7. imageView.setImageBitmap(bitmap)
```

---

## GPS Location Flow (capsule creation)

```
1. CreateCapsuleActivity.onCreate() → requestLocationAndCapture()
2. Check/request ACCESS_FINE_LOCATION permission
3. FusedLocationProviderClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY)
4. On success: CapsulesViewModel.setLocation(lat, lng)
5. On create: CapsuleEntity(latitude=lat, longitude=lng)
6. If isLocationBased: also sets unlockLatitude/unlockLongitude
7. Saved to Room → observed by MapViewModel via Flow
8. MapFragment.updateMarkers() → GeoPoint(capsule.latitude, capsule.longitude)
```
