# ChronoVault

ChronoVault is an Android memory capsule app: create memories now, unlock later by time/location, explore on map, share with people, and chat around memories.

This README is a current-state inventory: what exists, where it is implemented, what is partially done, and what is not wired yet.

## Quick Flow

Splash -> Onboarding -> Auth -> Main Shell

Main shell tabs:
- Home
- Map
- Capsules
- Chat
- Profile

Notifications open from the Home header icon.

## Stack and Architecture

- Language: Kotlin
- UI: XML + Material components
- Architecture: MVVM + Repository + ServiceLocator
- Local data: Room (`ChronoVaultDatabase`)
- Remote data: Firebase Auth + Firestore
- Background: WorkManager
- Map: OSMDroid
- Networking: Retrofit (quote API)

## Mobile App Development Lab Mapping (ChronoVault)

This section maps your lab experiments to what is already implemented in this project.
All snippets are adapted from current code paths in ChronoVault.

### Experiment 1 (CO1)
Install Android Studio and create first app ("Hello, Android!").

How it is demonstrated here:
- The entry flow is production-grade instead of a single hello screen.
- App starts at splash, then routes to onboarding/auth/main based on app state.

Implementation in ChronoVault:

```kotlin
// SplashActivity.kt
private fun navigateNext() {
  val prefs = ServiceLocator.providePreferencesManager(this)
  val auth = ServiceLocator.provideAuthRepository(this)

  val intent = when {
    prefs.isFirstLaunch() -> Intent(this, OnboardingActivity::class.java)
    auth.isUserLoggedIn() -> Intent(this, MainActivity::class.java)
    else -> Intent(this, AuthActivity::class.java)
  }
  startActivity(intent)
  finish()
}
```

How this satisfies the practical:
- Demonstrates successful Android Studio project setup, app launch, and first UI flow.
- Replaces "Hello World" with a real startup navigation baseline.

### Experiment 2 (CO2)
Design a responsive UI using LinearLayout and ConstraintLayout.

How it is demonstrated here:
- ChronoVault uses ConstraintLayout as root and LinearLayout blocks for sections/cards.
- ScrollView wraps long forms and dashboard content for smaller devices.

Implementation pattern used in layouts:

```xml
<!-- fragment_home.xml -->
<androidx.constraintlayout.widget.ConstraintLayout ...>
  <ScrollView
    android:layout_width="0dp"
    android:layout_height="0dp"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintBottom_toBottomOf="parent">

    <LinearLayout
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:orientation="vertical">
      <!-- Sections: header, quote card, stats, recent list -->
    </LinearLayout>
  </ScrollView>
</androidx.constraintlayout.widget.ConstraintLayout>
```

Why this is responsive:
- Constraint-based root adapts to different screen sizes.
- Vertical section stacking avoids overlap on compact displays.
- Material components provide consistent spacing and touch targets.

### Experiment 3 (CO2)
Create multiple screens using Intents and Fragments.

How it is demonstrated here:
- Activity-level navigation: Splash -> Onboarding/Auth/Main using intents.
- In-app screen navigation: fragment-based navigation with NavHost + BottomNavigation.

Implementation snippets:

```kotlin
// Activity-based navigation
val intent = Intent(this, MainActivity::class.java)
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
startActivity(intent)
finish()
```

```xml
<!-- activity_main.xml -->
<androidx.fragment.app.FragmentContainerView
  android:id="@+id/nav_host_fragment_activity_main"
  android:name="androidx.navigation.fragment.NavHostFragment"
  app:navGraph="@navigation/mobile_navigation" />
```

```kotlin
// MainActivity.kt
val navHostFragment = supportFragmentManager
  .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
val navController = navHostFragment.navController
binding.navView.setupWithNavController(navController)
```

How this satisfies the practical:
- Demonstrates both explicit intent navigation and fragment transaction/navigation graph flow.

### Experiment 4 (CO2)
Build event-driven interactions with buttons and gestures.

How it is demonstrated here:
- Buttons drive actions (create capsule, refresh quote, navigation, filters).
- Touch and long-press gestures are used for feedback and contextual actions.

Implementation snippets:

```kotlin
// HomeFragment.kt - button event
binding.btnCreateCapsule.setOnClickListener {
  startActivity(Intent(requireContext(), CreateCapsuleActivity::class.java))
}
```

```kotlin
// HomeFragment.kt - touch gesture feedback
private fun applyPressFeedback(view: View) {
  view.setOnTouchListener { v, event ->
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90L).start()
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(90L).start()
    }
    false
  }
}
```

```kotlin
// ChatMessagesAdapter.kt - long press gesture
binding.root.setOnLongClickListener {
  if (isSender) {
    onMessageLongPress(message)
    true
  } else false
}
```

### Experiment 5 (CO3)
Save user data locally using SharedPreferences.

How it is demonstrated here:
- ChronoVault uses EncryptedSharedPreferences through PreferencesManager.
- Stores user session, settings, theme, and cached quote metadata.

Implementation snippet:

```kotlin
// PreferencesManager.kt
private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
  context,
  "chronovault_preferences",
  masterKey,
  EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
  EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

fun setUserId(userId: String) {
  sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
}

fun isLoggedIn(): Boolean {
  return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
}
```

Why this is strong for lab reporting:
- Shows local persistence and secure storage (beyond plain SharedPreferences).

### Experiment 6 (CO3)
Fetch and display data from a REST API.

How it is demonstrated here:
- Home page quote card is fed from ZenQuotes using Retrofit.
- ViewModel updates UI state with loading, success, and fallback error handling.

Implementation snippets:

```kotlin
// RetrofitClient.kt
private val retrofit: Retrofit = Retrofit.Builder()
  .baseUrl("https://zenquotes.io/api/")
  .client(okHttpClient)
  .addConverterFactory(GsonConverterFactory.create())
  .build()

val quoteApi: QuoteApiService by lazy {
  retrofit.create(QuoteApiService::class.java)
}
```

```kotlin
// QuoteApi.kt
interface QuoteApiService {
  @GET("random")
  suspend fun getRandomQuote(): List<QuoteResponse>
}
```

```kotlin
// HomeViewModel.kt
fun loadQuote() {
  viewModelScope.launch {
    _isQuoteRefreshing.value = true
    try {
      val result = quoteRepository.fetchQuote()
      result.onSuccess { _quote.value = it }
    } finally {
      _isQuoteRefreshing.value = false
    }
  }
}
```

Flow summary:
- UI button/initial load -> ViewModel -> QuoteRepository -> Retrofit API -> LiveData update -> Home quote card.

### Experiment 7 (CO3)
Database-backed app using Room (SQLite abstraction).

How it is demonstrated here:
- Core app data (capsules/comments/notifications/friends) is persisted in Room.
- Uses DAO queries + Flow streams + migration support.

Implementation snippets:

```kotlin
// ChronoVaultDatabase.kt
@Database(
  entities = [CapsuleEntity::class, CommentEntity::class, NotificationEntity::class, FriendEntity::class],
  version = 7,
  exportSchema = false
)
abstract class ChronoVaultDatabase : RoomDatabase() {
  abstract fun capsuleDao(): CapsuleDao
}
```

```kotlin
// CapsuleDao.kt
@Query("SELECT * FROM capsules WHERE ownerId = :ownerId ORDER BY createdAt DESC")
fun getUserCapsules(ownerId: String): Flow<List<CapsuleEntity>>

@Query("UPDATE capsules SET isUnlocked = 1 WHERE id = :capsuleId")
suspend fun unlockCapsule(capsuleId: String)
```

```kotlin
// ServiceLocator.kt
Room.databaseBuilder(
  context.applicationContext,
  ChronoVaultDatabase::class.java,
  ChronoVaultDatabase.DATABASE_NAME
)
  .addMigrations(
    ChronoVaultDatabase.MIGRATION_3_4,
    ChronoVaultDatabase.MIGRATION_4_5,
    ChronoVaultDatabase.MIGRATION_5_6,
    ChronoVaultDatabase.MIGRATION_6_7
  )
  .build()
```

### Experiment 8 (CO3, CO5)
Integrate authentication with Firebase Authentication.

How it is demonstrated here:
- Signup/login/reset password implemented via FirebaseAuth.
- Validation and error mapping done in ViewModel.
- Session persisted locally via PreferencesManager.

Implementation snippets:

```kotlin
// FirebaseAuthService (FirebaseServices.kt)
suspend fun registerUser(email: String, password: String, name: String): Result<String> {
  val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
  val userId = authResult.user?.uid ?: return Result.failure(Exception("User ID not found"))
  FirebaseFirestore.getInstance().collection("users").document(userId).set(
    mapOf("email" to email, "name" to name)
  ).await()
  return Result.success(userId)
}
```

```kotlin
// AuthRepository.kt
fun loginUser(email: String, password: String): Flow<Result<String>> = flow {
  firebaseAuthService.loginUser(email.trim().lowercase(Locale.US), password)
    .onSuccess { userId ->
      preferencesManager.setUserId(userId)
      preferencesManager.setLoggedIn(true)
      emit(Result.success(userId))
    }
}
```

```kotlin
// SignupViewModel.kt
private val strongPasswordRegex =
  Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$")
```

## Assignments Mapping

### Assignment 1 (CO4)
Build an app integrating a sensor (GPS/accelerometer) and media handling.

How it is done in ChronoVault:

1. GPS integration:
- Foreground continuous tracking: ForegroundLocationService (FusedLocationProviderClient).
- One-shot location capture during capsule creation: CreateCapsuleActivity.
- Map visualization with OSMDroid markers and nearby unlock checks.

```kotlin
// CreateCapsuleActivity.kt
private val locationPermissionLauncher = registerForActivityResult(
  ActivityResultContracts.RequestPermission()
) { isGranted ->
  if (isGranted) captureCurrentLocation()
}
```

```kotlin
// ForegroundLocationService.kt
private val locationRequest: LocationRequest by lazy {
  LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 20_000L)
    .setMinUpdateIntervalMillis(15_000L)
    .setMinUpdateDistanceMeters(20f)
    .build()
}
```

2. Media handling (images):
- User selects image from device storage while creating a capsule.
- Image is converted and stored as base64 in capsule data model.

```kotlin
// CreateCapsuleActivity.kt
private val pickImageLauncher = registerForActivityResult(
  ActivityResultContracts.GetContent()
) { uri: Uri? ->
  uri?.let { viewModel.setImageFromUri(this, it) }
}

binding.ivCapsuleImage.setOnClickListener {
  pickImageLauncher.launch("image/*")
}
```

### Assignment 2 (CO4)
Implement notifications and background tasks.

How it is done in ChronoVault:

1. Periodic background tasks:
- WorkManager schedules periodic workers from application startup.
- Time worker checks timed unlocks.
- Location worker checks proximity unlocks.

```kotlin
// ChronoVaultApplication.kt
override fun onCreate() {
  super.onCreate()
  WorkScheduler.scheduleAllWorkers(this)
}
```

```kotlin
// WorkScheduler.kt
val timeBasedWork = PeriodicWorkRequestBuilder<TimeBasedUnlockWorker>(15, TimeUnit.MINUTES).build()
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
  TimeBasedUnlockWorker.WORK_NAME,
  ExistingPeriodicWorkPolicy.KEEP,
  timeBasedWork
)
```

2. Notifications:
- Local notifications for unlock and nearby events.
- Notification records persisted via NotificationRepository/Room.

```kotlin
// TimeBasedUnlockWorker.kt
NotificationHelper.sendCapsuleUnlockedNotification(
  applicationContext,
  capsule.title,
  "Your capsule \"${capsule.title}\" has been unlocked!"
)
```

## Mini Project Implementation

### Problem Statement (ChronoVault)
Design and develop an Android memory-capsule application that allows users to create personal memories (text/image/location), lock them by time or geographic proximity, and unlock them later. The app must support secure authentication, local and cloud persistence, map-based visualization, controlled sharing, and contextual chat/notifications around shared memories.

### Case Study: UI Design Layouts (CO1 to CO6)

UI design choices implemented:
- Hybrid responsive layout approach: ConstraintLayout roots + LinearLayout section grouping.
- Multi-screen architecture with clear role separation:
  - Splash/Onboarding/Auth for first-time and identity flow.
  - Main shell tabs for feature grouping (Home/Map/Capsules/Chat/Profile).
- Action visibility by state:
  - Locked capsule hides content until unlock conditions are met.
  - Nearby and notification indicators are contextual and dynamic.
- Interaction language:
  - Tap for primary actions, long-press for message actions, animated touch feedback for cards/buttons.

### Mini Project Report Structure (Suggested)

Use this app as your report baseline:

1. Introduction and objective.
2. Problem statement and scope.
3. Tools and technologies (Kotlin, Firebase, Room, WorkManager, OSMDroid).
4. Architecture (MVVM + Repository + ServiceLocator).
5. Experiment-wise implementation mapping (use sections above).
6. Screens and UI case study (include key XML and screenshots).
7. Database and API integration details.
8. Authentication and security considerations.
9. Background tasks and notification strategy.
10. Testing, limitations, and future improvements.

Lab-ready note:
- This project already covers the full experiment list with production-level extensions.
- For practical submission, you can demonstrate each experiment using the mapped files and snippets above.

Code map:
- `app/src/main/java/com/example/chronovault/data/local` - Room DB, entities, DAOs
- `app/src/main/java/com/example/chronovault/data/repository` - data orchestration
- `app/src/main/java/com/example/chronovault/data/remote/firebase` - Firebase services
- `app/src/main/java/com/example/chronovault/ui` - screens, adapters, viewmodels
- `app/src/main/java/com/example/chronovault/workers` - periodic unlock checks
- `app/src/main/java/com/example/chronovault/services` - foreground location service
- `app/src/main/java/com/example/chronovault/utils` - helpers (theme, image, location, notifications)

## Feature Status by Page

### Home (`ui/home`)
Implemented:
- Dynamic greeting + subtitle
- Quote card with loading/fallback
- Stats cards and quick actions
- Nearby memory card behavior
- Recent memories list
- Notifications entry icon + unread indicator

Partial:
- Some polish animations vary by device/theme state

### Capsules List (`ui/capsules/CapsulesFragment.kt`, `CapsulesViewModel.kt`)
Implemented:
- Filters: `ALL`, `LOCKED`, `UNLOCKED`, `SHARED`, `PERSONAL`, `PUBLIC`
- Capsule cards with countdown/status
- Lock-aware click behavior
- Map clue handoff for locked location capsules

Partial:
- `PUBLIC` currently mapped via existing `canBeShared` data path (not a dedicated `isPublic` field migration yet)

### Capsule Details (`ui/capsules/CapsuleDetailsActivity.kt`, `CapsuleDetailsViewModel.kt`)
Implemented:
- Lock gate for message/image visibility
- Countdown and location lock messaging
- Share/unshare support
- Comments display/add/delete
- Owner controls (delete, make private)

Implemented recently:
- Share now opens bottom sheet (not manual ID dialog)
- Multi-select sharing to selected users

Partial:
- Shared-with list still displays generic identifier strings (UID display names not fully hydrated)

### Map (`ui/map`)
Implemented:
- Capsule markers from Room data
- Personal/World mode toggle
- Marker grouping/selection
- Discovery overlay flow
- Clue area circle for locked location capsules

Performance/stability:
- Foreground location cadence tuned to balanced power + distance filter
- Foreground location source disabled while map is active to avoid duplicate sources

Partial:
- Public discovery feed section UI (list-style panel) not added yet; map markers are present

### Chat List + Chat (`ui/chat`)
Implemented:
- Chat list from Firestore chats (`participants` contains current UID)
- Last message + timestamp
- Unread badge behavior
- Search chats (local realtime filtering)
- Open chat + send text/capsule messages
- Pagination for older messages
- Notification deep-link to chat

Implemented recently:
- New Chat FAB on chat list
- New chat bottom sheet with Firestore user search
- Deterministic chat creation before opening chat

Partial:
- Chat title shows UID by default; richer profile-name hydration is limited

### Profile (`ui/profile`)
Implemented:
- Profile header, edit name, avatar update
- User ID copy
- Friend requests (send/accept/reject)
- Friend list and accepted-friends sync
- Notification toggles
- Theme mode + color scheme controls
- Logout + delete account

Implemented recently:
- "My Capsule Comments" entry and dedicated screen

### My Capsule Comments (`ui/profile/MyCapsuleCommentsFragment.kt`)
Implemented:
- Owner-only comment feed using Room join query
- Shows capsule title, comment text, author, timestamp

## Sharing Model (Current)

Current effective behavior:
- Capsule owner can share with selected user IDs
- Shared IDs stored in capsule `sharedWith` list
- Shared capsule sync: Firestore -> Room one-shot + realtime listener
- Shared tab uses `isSharedWithMe` local flag populated by sync

Important:
- Sharing is now UID-based in app-side share flows
- Email-based sharing UI has been removed from active share entry points

Not fully migrated yet:
- Dedicated schema fields like `isPublic` and `isLockedForSharedUsers` are not rolled out as a formal DB migration at this time

## Comments Model (Current)

Implemented:
- Comments on shared capsules are allowed through details screen gating logic
- Owner can view all comments across owned capsules in My Capsule Comments

Storage:
- Local Room `comments` table via `CommentEntity`

## Auth and Validation

Implemented:
- Login/signup with Firebase Auth
- Strong signup password validation
- Friendly auth error mapping
- Forgot password flow from login

## Firestore Rules (Current Project Direction)

App currently expects UID-based identity checks.

Friends rule currently accepted by app flow:

```text
match /friends/{friendId} {
  allow create: if request.auth != null
                && request.resource.data.users is list
                && request.auth.uid in request.resource.data.users;

  allow read: if request.auth != null;

  allow delete: if request.auth != null
                && request.auth.uid in resource.data.users;
}
```

## Background Work and Location

Workers:
- `TimeBasedUnlockWorker` periodic unlock checks
- `LocationBasedUnlockWorker` periodic nearby unlock checks

Foreground service:
- `ForegroundLocationService` requests balanced-power updates
- Target cadence around 20s with 50m movement gate
- Guard prevents duplicate update registration

## What Is Fully Implemented vs Partial vs Not Wired

Fully implemented:
- Core capsule lifecycle (create/list/open/delete)
- Time/location unlock behavior and lock messaging
- Shared sync to Room and shared tab data path
- Friends requests and accepted friendship creation
- Chat persistence using deterministic chat IDs and Firestore
- New chat search + share bottom sheet UX

Partially implemented:
- Public/world model is represented via existing fields, not a dedicated finalized schema migration
- User display info in chat/share lists can still show IDs when profile data is missing
- Some UI micro-animations are present but not uniformly standardized

Not wired yet (known backlog):
- Full dedicated public capsule field rollout (`isPublic`) with migration/backfill strategy
- Strong server-driven display-name caching for all social lists
- Rich "Discover Memories" public feed panel separate from map markers

## Important Files (Entry Points)

- Main shell/navigation: `app/src/main/java/com/example/chronovault/MainActivity.kt`
- Auth container: `app/src/main/java/com/example/chronovault/ui/auth/AuthActivity.kt`
- Home: `app/src/main/java/com/example/chronovault/ui/home/HomeFragment.kt`
- Capsules: `app/src/main/java/com/example/chronovault/ui/capsules/CapsulesFragment.kt`
- Capsule details: `app/src/main/java/com/example/chronovault/ui/capsules/CapsuleDetailsActivity.kt`
- Map: `app/src/main/java/com/example/chronovault/ui/map/MapFragment.kt`
- Chat list: `app/src/main/java/com/example/chronovault/ui/chat/ChatListFragment.kt`
- Chat thread: `app/src/main/java/com/example/chronovault/ui/chat/ChatFragment.kt`
- Profile: `app/src/main/java/com/example/chronovault/ui/profile/ProfileFragment.kt`

## Build

```powershell
Set-Location "D:\Where Winds Meet\ChronoVault"
.\gradlew.bat assembleDebug
```

## Notes

- `strings.xml` is actively updated and contains page labels, CTA text, sharing/chat/search strings, and status labels.
- If you update Firestore rules, publish rules before testing social flows.
