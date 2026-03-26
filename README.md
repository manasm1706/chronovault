# ChronoVault

Preserve moments in time capsules that unlock by time or location, then rediscover them later.

## Current Project State (March 26, 2026)

This README is organized by page, then by task, then by the functions/classes that implement each task.

Status legend:
- `WORKING`: Implemented and wired in current navigation flow.
- `PARTIAL`: Implemented in UI/code, but still limited or not fully production-complete.

## App Flow

`SplashActivity` -> `OnboardingActivity` -> `AuthActivity` (`LoginFragment` / `SignupFragment`) -> `MainActivity` (Bottom Nav)

Bottom navigation pages:
- `HomeFragment`
- `CapsulesFragment`
- `MapFragment`
- `NotificationsFragment`
- `ProfileFragment`

---

## Page: Splash

### Task: Route user to the correct entry point
- Status: `WORKING`
- Functions/classes:
  - `SplashActivity.navigateNext()`
  - Checks first-launch via `PreferencesManager.isFirstLaunch()`
  - Checks auth via `AuthRepository.isUserLoggedIn()`
  - Routes to onboarding, auth, or main app accordingly.

## Page: Onboarding

### Task: Show first-time introduction slides
- Status: `WORKING`
- Functions/classes:
  - `OnboardingActivity.onCreate()` sets ViewPager pages and dots.
  - `OnboardingActivity.updateButtons()` toggles Next/Get Started and Skip visibility.
  - `OnboardingActivity.finishOnboarding()` sets first-launch false and opens auth.

## Page: Authentication

### Task: Login with validation
- Status: `WORKING`
- Functions/classes:
  - `LoginFragment.setupUI()` and `LoginFragment.handleLoginState()`
  - `LoginViewModel.login()` with email/password validation and repository auth call.

### Task: Signup with validation
- Status: `WORKING`
- Functions/classes:
  - `SignupFragment.setupUI()` and `SignupFragment.handleSignupState()`
  - `SignupViewModel.signup()` with name/email/password checks and registration flow.

### Task: Enter main app after auth
- Status: `WORKING`
- Functions/classes:
  - `AuthActivity.navigateToMainApp()`
  - `AuthActivity.onCreate()` auto-skips auth if already logged in.

---

## Page: Home (Dashboard)

### Task: Dynamic greeting + subtitle
- Status: `WORKING`
- Functions/classes:
  - `HomeViewModel.getGreeting()` for morning/afternoon/evening.
  - `HomeViewModel.greetingSubtitle` and `HomeFragment.observeViewModel()` binding.

### Task: Quote card with refresh
- Status: `WORKING`
- Functions/classes:
  - `HomeViewModel.refreshQuote()` calls quote API and updates `dailyQuote`.
  - `HomeFragment.setupUI()` -> `btnRefreshQuote` triggers refresh.

### Task: Summary cards (Total/Locked/Unlocked/Shared) with click actions
- Status: `WORKING`
- Functions/classes:
  - `HomeFragment.setupUI()` card click listeners.
  - `HomeFragment.navigateToCapsules()` sends filter request via fragment result and selects capsules tab.
  - `HomeViewModel` exposes counts through LiveData.

### Task: Recent memories list
- Status: `WORKING`
- Functions/classes:
  - `HomeViewModel.recentCapsules` (top 3 sorted by create date).
  - `RecentCapsulesAdapter` (lightweight delegate click only).
  - `HomeFragment.handleMemoryClick()` routes unlocked memories to center tab details flow.

### Task: Nearby memory card
- Status: `WORKING`
- Functions/classes:
  - `HomeViewModel.updateNearbyCapsule()` computes nearest capsule within 50m.
  - `HomeFragment.shouldOpenNearbyDetails()` picks button behavior:
    - unlocked/effectively unlocked/no lock conditions -> view details flow
    - locked -> go to map focus
  - `HomeFragment.startNearbyCountdown()` shows per-second countdown for nearby time-locked capsule.

### Task: Empty state
- Status: `WORKING`
- Functions/classes:
  - `HomeFragment.observeViewModel()` toggles empty state and hides stats/recent when no capsules.

### Task: Create CTA
- Status: `WORKING`
- Functions/classes:
  - `HomeFragment.setupUI()` -> opens `CreateCapsuleActivity`.

---

## Page: Capsules (Center Tab)

### Task: Filter memory list by status
- Status: `WORKING`
- Functions/classes:
  - `CapsulesFragment.applyFilter()` and chip handlers.
  - `CapsulesViewModel.setFilter()` and `loadCapsules()`.

### Task: Open memory details safely
- Status: `WORKING`
- Functions/classes:
  - `CapsulesAdapter` delegates click only.
  - `CapsulesFragment.onCapsuleClick()` is single click gate.
  - `CapsulesFragment.navigateToDetails()` uses Nav action `action_capsulesFragment_to_capsuleDetailsActivity` with `capsule_id`.

### Task: Locked memory feedback
- Status: `WORKING`
- Functions/classes:
  - `CapsulesViewModel.canOpenCapsule()` and `getLockedMessage()`.
  - `CapsulesFragment.showLockedMessage()` shows one lock dialog path.

### Task: Create memory from capsules page
- Status: `WORKING`
- Functions/classes:
  - `CapsulesFragment` FAB -> `CreateCapsuleActivity`.

---

## Page: Create Capsule

### Task: Capture location + set lock conditions + save capsule
- Status: `WORKING`
- Functions/classes:
  - `CreateCapsuleActivity.requestLocationAndCapture()` permission flow.
  - `CreateCapsuleActivity.captureCurrentLocation()` captures fresh/last known location.
  - `CapsulesViewModel.createCapsule()` validates fields and inserts into Room/repository.
  - If no unlock method is selected, `CreateCapsuleActivity.showNoUnlockConfirmationDialog()` asks whether to continue; continuing creates an already-unlocked capsule.

### Task: Image attach and compression
- Status: `WORKING`
- Functions/classes:
  - `CreateCapsuleActivity` image picker.
  - `CapsulesViewModel.setImageFromUri()` with compression via `ImageConverter`.

---

## Page: Capsule Details

### Task: Enforce lock gate before showing full content
- Status: `WORKING`
- Functions/classes:
  - `CapsuleDetailsActivity.applyLockGate()`
  - Priority order implemented:
    - time lock first
    - location lock second
    - otherwise unlocked

### Task: Locked state UI
- Status: `WORKING`
- Functions/classes:
  - `CapsuleDetailsActivity.showTimeLockedState()` + `startCountdown()`
  - `CapsuleDetailsActivity.showLocationLockedState()` with distance text.
  - Details countdown currently displays days/hours/minutes.

### Task: Unlocked state content
- Status: `WORKING`
- Functions/classes:
  - `CapsuleDetailsActivity.showUnlockedState()` shows message/image/location/sharing/comments.

### Task: Sharing + comments
- Status: `WORKING`
- Functions/classes:
  - `CapsuleDetailsViewModel.shareCapsule()`, `unshareCapsule()`, `makeCapsulePrivate()`
  - `CapsuleDetailsViewModel.addComment()` and `deleteComment()`

---

## Page: Map

### Task: Show all capsules on map (locked + unlocked)
- Status: `WORKING`
- Functions/classes:
  - `MapViewModel.allCapsules` observed by `MapFragment.observeViewModel()`.
  - `MapFragment.updateMapMarkers()` clears overlays and redraws markers from Room data.

### Task: Map mode switch (Personal / World)
- Status: `WORKING`
- Functions/classes:
  - `MapViewModel.MapMode` and `MapViewModel.setMapMode()` control the active data slice.
  - Personal mode shows current user capsules.
  - World mode shows non-owned shared/shareable capsules within 10km of current location.
  - `fragment_map.xml` contains top toggle card with `Personal` and `World` options.

### Task: Correct marker coordinates and focus
- Status: `WORKING`
- Functions/classes:
  - `GeoPoint(capsule.latitude, capsule.longitude)` used.
  - `MapFragment.centerOnPoint()` sets zoom/center.
  - `MapFragment.ARG_FOCUS_CAPSULE_ID` supports deep-link focus from nearby CTA.

### Task: Marker color rules and precedence
- Status: `WORKING`
- Functions/classes:
  - `MapFragment.getMarkerIcon()` tints one marker drawable by state.
  - Current precedence prioritizes user-owned markers before shared markers.

### Task: Multiple memories at one coordinate
- Status: `WORKING`
- Functions/classes:
  - `MapFragment.handleMarkerSelection()` opens chooser dialog.
  - Selected item opens details screen.

### Task: Marker preview bottom sheet
- Status: `WORKING`
- Functions/classes:
  - `CapsulePreviewBottomSheet` loads capsule asynchronously (no `runBlocking`).
  - Uses lifecycle-aware coroutine; cancellation handled safely.

---

## Page: Notifications

### Task: Show in-app notification list (read/delete/clear)
- Status: `WORKING`
- Functions/classes:
  - `NotificationsFragment` list + clear actions.
  - `NotificationsViewModel.markAsRead()`, `deleteNotification()`, `clearAllNotifications()`.

### Task: Pull real notification feed from backend
- Status: `PARTIAL`
- Functions/classes:
  - `NotificationsViewModel.loadNotifications()` currently uses mock list in code.
  - Read state writes to preferences, but source data is not yet backend-driven.

---

## Page: Profile

### Task: View/update profile and avatar
- Status: `WORKING`
- Functions/classes:
  - `ProfileViewModel.loadUserProfile()`, `updateName()`, `updateAvatar()`.
  - `ProfileFragment` binds profile state and image picker.

### Task: Logout and delete account
- Status: `WORKING`
- Functions/classes:
  - `ProfileViewModel.logout()`, `deleteAccount()`.
  - `ProfileFragment.handleAccountState()` delegates app exit to `MainActivity.logout()`.

---

## Cross-Cutting Tasks

### Task: Bottom nav feedback + selection animation
- Status: `WORKING`
- Functions/classes:
  - `MainActivity.animateBottomNavSelection()`.
  - `setupWithNavController` + item selected/reselected listeners.

### Task: Location permission flow
- Status: `WORKING`
- Functions/classes:
  - `MainActivity.ensureLocationPermissionFlow()` (first ask, rationale, settings for permanent deny).
  - Page-level handling in `MapFragment` and `CreateCapsuleActivity`.

### Task: Time unlock persistence after unlock date passes
- Status: `WORKING`
- Functions/classes:
  - `persistExpiredTimeUnlocks()` is used in `HomeViewModel`, `CapsulesViewModel`, `MapViewModel`, `CapsuleDetailsViewModel`.
  - Ensures expired time-locked memories stay unlocked.

### Task: Location unlock radius
- Status: `WORKING`
- Functions/classes:
  - Location unlock gate uses 50m in `CapsuleDetailsActivity`.
  - Background unlock uses 50m in `LocationBasedUnlockWorker`.
  - Nearby logic uses 50m in `HomeViewModel` and `MapViewModel`.

### Task: Background unlock workers
- Status: `WORKING`
- Functions/classes:
  - `TimeBasedUnlockWorker`
  - `LocationBasedUnlockWorker`
  - `WorkScheduler`

---

## Partially Done / Open Items

- `PARTIAL`: Notifications backend integration is still mocked in `NotificationsViewModel.loadNotifications()`.
- `PARTIAL`: Some UI text in `CapsulePreviewBottomSheet.kt` still uses hardcoded strings and should be moved to string resources.
- `PARTIAL`: Memory details open through `CapsuleDetailsActivity` destination (not yet converted to a dedicated detail Fragment page).
- `PARTIAL`: User-reported issue (Mar 26, 2026): app crashes when opening unlocked capsule details in some flows. Mitigations now include direct `Intent` launch path in `CapsulesFragment.navigateToDetails()`, safer capsule ID extraction in `CapsuleDetailsActivity`, removal of repeated observer registration, and prevention of `ActionState.Idle` reset loops; pending user verification on device.

---

## Key Docs

- `SETUP.md`
- `ARCHITECTURE.md`
- `FEATURES.md`
- `API_REFERENCE.md`
- `DEPLOYMENT.md`

---

Last updated: March 26, 2026
