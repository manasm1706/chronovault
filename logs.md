# ChronoVault

Preserve moments in time capsules that unlock by time or location, then rediscover them later.

## Current Project State (March 28, 2026)

This README is organized by page, then by task, then by the functions/classes that implement each task.

Status legend:
- `WORKING`: Implemented and wired in current navigation flow.
- `PARTIAL`: Implemented in UI/code, but still limited or not fully production-complete.

## App Flow

`SplashActivity` -> `OnboardingActivity` -> `AuthActivity` (`LoginFragment` / `SignupFragment`) -> `MainActivity` (Bottom Nav shell)

`MainActivity.onCreate()` also performs an auth gate (`AuthRepository.isUserLoggedIn()`) and force-redirects to `AuthActivity` when the session is missing.

Bottom navigation pages:
- `HomeFragment`
- `MapFragment`
- `CapsulesFragment`
- `ChatListFragment`
- `ProfileFragment`

Notifications are now accessed from the Home header action (`iv_notifications`) instead of a bottom-nav tab.

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

## Page: Main Shell (`MainActivity`)

### Task: Enforce auth gate before showing app shell
- Status: `WORKING`
- Functions/classes:
  - `MainActivity.onCreate()` checks `ServiceLocator.provideAuthRepository(this).isUserLoggedIn()`.
  - Unauthenticated users are redirected with `Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK` to `AuthActivity`.

### Task: Wire bottom navigation and add selection feedback
- Status: `WORKING`
- Functions/classes:
  - `BottomNavigationView.setupWithNavController(navController)` binds top-level tabs to a single `NavController`.
  - `MainActivity.animateBottomNavSelection()` applies selected/reselected scale feedback.
  - `MainActivity.navigateToTopLevelDestination()` uses `popUpTo(startDestination) + launchSingleTop + restoreState` to avoid tab stack buildup.
  - Home tab selection explicitly routes back to `R.id.navigation_home` (`popBackStack(..., false)` fallback to navigate).

### Task: Keep Home tab deterministic from any screen
- Status: `WORKING`
- Functions/classes:
  - `MainActivity` uses one `NavHostFragment` / one `NavController` for all bottom-nav behavior.
  - `setOnItemReselectedListener` and `setOnItemSelectedListener` both enforce Home return logic.
  - `onBackPressedDispatcher` callback pops back stack normally, but exits app when current destination is Home.
  - Debug destination logs are emitted through `Log.d("NAV", destination.id.toString())`.

### Task: Route chat notification deep-links safely
- Status: `WORKING`
- Functions/classes:
  - `MainActivity.handleChatNotificationNavigation()` reads `NotificationHelper.EXTRA_NAV_CHAT_ID` and `NotificationHelper.EXTRA_NAV_CHAT_USER_ID`.
  - Handles both cold start (`onCreate`) and already-running task (`onNewIntent`).
  - Navigates to `R.id.chatFragment` with `ChatFragment.ARG_CHAT_ID` and `ChatFragment.ARG_OTHER_USER_ID`, then clears consumed extras.

### Task: Request location permission and bootstrap background tracking
- Status: `WORKING`
- Functions/classes:
  - `MainActivity.ensureLocationPermissionFlow()` handles first ask, rationale, and permanent-deny-to-settings branches.
  - `showLocationRationaleDialog()` and `showLocationPermanentlyDeniedDialog()` provide user guidance.
  - `startLocationTrackingIfPermitted()` starts `ForegroundLocationService` via `ContextCompat.startForegroundService(...)`.

### Task: Provide app-level logout handoff target
- Status: `WORKING`
- Functions/classes:
  - `MainActivity.logout()` is called by profile flow and clears session using `AuthRepository.logoutUser()`.
  - After logout, app task is reset and routed to `AuthActivity`.

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
  - `HomeFragment.startNearbyCountdown()` shows per-second countdown with shared formatting rules.

### Task: Notifications shortcut + unread badge on Home header
- Status: `WORKING`
- Functions/classes:
  - `fragment_home.xml` includes `container_notifications`, `iv_notifications`, and `view_notification_dot`.
  - `HomeFragment.setupUI()` routes icon tap to `R.id.navigation_notifications`.
  - `HomeViewModel.unreadCount` is backed by `NotificationRepository.getUnreadCount()`.
  - `HomeFragment.observeViewModel()` toggles unread green dot visibility.

### Task: Quote/map loading + micro button feedback
- Status: `WORKING`
- Functions/classes:
  - `HomeViewModel.isQuoteRefreshing` + `HomeFragment` refresh button state provide quote loading feedback.
  - `MapFragment` now binds `MapViewModel.loadingState` to `progress_map` visibility.
  - Press-scale feedback is applied on key home/profile action buttons.

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

### Task: Time-lock card timer UI (large reverse countdown)
- Status: `WORKING`
- Functions/classes:
  - `item_capsule.xml` adds a large, high-contrast countdown text block (`tv_countdown`).
  - `CapsulesAdapter.startCountdown()` updates every second.
  - `CountdownFormatter.formatRemainingDuration()` is reused in `CapsulesAdapter` and details/home countdown paths.

### Task: Location-lock map clue from capsule card
- Status: `WORKING`
- Functions/classes:
  - `item_capsule.xml` adds separate map clue icon (`iv_map_clue`) beside status chip.
  - `CapsulesAdapter` shows clue icon only for location-locked capsules.
  - `CapsulesFragment.onMapClueClick()` switches to map tab and sends clue payload via fragment result.
  - `MapFragment` receives clue payload and renders a 1km clue circle without exposing exact point.

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
  - Details countdown now uses shared duration rules via `CountdownFormatter`.

### Task: Unlocked state content
- Status: `WORKING`
- Functions/classes:
  - `CapsuleDetailsActivity.showUnlockedState()` shows message/image/location/sharing/comments.

### Task: Locked -> unlocked reveal animation
- Status: `WORKING`
- Functions/classes:
  - `CapsuleDetailsActivity.runUnlockTransitionAnimationOnce()` animates scale/fade for memory content.
  - `CapsuleDetailsActivity.vibrateUnlockFeedback()` adds short haptic pulse.
  - Transition play state is preserved with `KEY_UNLOCK_TRANSITION_PLAYED` to avoid re-trigger on rotation.

### Task: Sharing + comments
- Status: `WORKING`
- Functions/classes:
  - `CapsuleDetailsViewModel.shareCapsule()`, `unshareCapsule()`, `makeCapsulePrivate()`
  - `CapsuleDetailsViewModel.addComment()` and `deleteComment()`

### Task: Comments polish
- Status: `WORKING`
- Functions/classes:
  - Empty state copy standardized to `No comments yet`.
  - `CommentDao.getCommentsForCapsule()` already serves latest-first order (`ORDER BY createdAt DESC`).
  - Empty input is blocked at both UI and ViewModel validation points.
  - Comment timestamp remains visible via `CommentsAdapter.tvCommentTime`.

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

### Task: Map utility controls (center + options menu)
- Status: `WORKING`
- Functions/classes:
  - `fragment_map.xml` adds `btn_center_map` and `btn_map_options` floating controls.
  - `MapFragment.setupMapControls()` centers map on user location and handles unavailable-location fallback.
  - `MapFragment.showMapOptionsMenu()` uses `map_overlay_menu.xml` for quick toggles.
  - `MapViewModel.overlayOptions` stores toggle state for clue circles, nearby waves, my-location layer, and discovery overlay.

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
  - Marker states are now visually separated for locked/nearby/unlocked/shared.

### Task: Nearby marker animation + click feedback
- Status: `WORKING`
- Functions/classes:
  - `MapFragment.startNearbyPulse()` applies gentle alpha pulse for nearby markers only.
  - `MapFragment.animateMarkerBounce()` adds marker click bounce feedback.
  - Pulse visibility can be toggled at runtime via map options (`showNearbyWaves`).

### Task: Notification-driven map focus
- Status: `WORKING`
- Functions/classes:
  - `MapFragment` listens for `MAP_FOCUS_REQUEST` and focuses by `KEY_FOCUS_CAPSULE_ID`.
  - Nearby notifications can deep-link to map markers via fragment result API.

### Task: Clue-area rendering for locked location memories
- Status: `WORKING`
- Functions/classes:
  - `MapFragment.renderClueOverlay()` draws a 1km circle.
  - `MapFragment.buildClueArea()` applies a small deterministic offset so exact lock coordinates are not directly disclosed.
  - Clue circle visibility can be toggled at runtime (`showClueCircles`).

### Task: Distant clue text
- Status: `WORKING`
- Functions/classes:
  - `LocationHelper.getLocalityHint()` reverse-geocodes capsule coordinates.
  - Locked location/shared flows show `Somewhere in <City>` and fall back to `Unknown location`.
  - Applied in `CapsulesFragment.showLockedMessage()` and `CapsulePreviewBottomSheet.getLockedMessage()`.

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

### Task: Discovery moment modal
- Status: `WORKING`
- Functions/classes:
  - `MapViewModel.discoveryEvent` emits first-time discovery when user enters 50m.
  - `fragment_map.xml` includes full-screen discovery overlay with `Open Memory` CTA.
  - `MapFragment.showDiscoveryOverlay()` handles fade/scale animation and details routing.

---

## Page: Notifications

### Task: Access notifications screen from Home header (not bottom nav)
- Status: `WORKING`
- Functions/classes:
  - `MainActivity` bottom nav excludes notifications destination.
  - `HomeFragment` notification icon routes to `NotificationsFragment`.
  - Existing `NotificationsFragment` logic and data flow remain unchanged.

### Task: Show in-app notification list (read/delete/clear)
- Status: `WORKING`
- Functions/classes:
  - `NotificationsFragment` list + clear actions.
  - `NotificationsViewModel.markAsRead()`, `deleteNotification()`, `clearAllNotifications()`.

### Task: Personal vs World notification filter
- Status: `WORKING`
- Functions/classes:
  - `fragment_notifications.xml` adds `chipPersonal` and `chipWorld` with default Personal selection.
  - `NotificationsViewModel.selectedCategory` + `setCategory()` drive filtering.
  - `NotificationsViewModel.emptyStateMessage` provides category-aware empty state text.

### Task: Real notification system (Room-backed)
- Status: `WORKING`
- Functions/classes:
  - `NotificationEntity` persists `id/title/message/timestamp/isRead/type/typeCategory/capsuleId`.
  - `NotificationDao` + `NotificationRepository` provide observe/read/delete/clear APIs.
  - `NotificationsViewModel` now observes Room notifications via LiveData stream.
  - `TimeBasedUnlockWorker`, `LocationBasedUnlockWorker`, and `ChronoVaultMessagingService` create real events.
  - Room migrations (`MIGRATION_3_4`, `MIGRATION_4_5`) ensure notifications table exists on upgraded installs.

### Task: Notification tap actions
- Status: `WORKING`
- Functions/classes:
  - Nearby (`NEARBY`) notifications navigate to map and focus capsule marker.
  - Unlock/share notifications route into capsule details flow via Capsules tab.

### Task: Chat message notifications
- Status: `WORKING`
- Functions/classes:
  - Incoming chat events create Room notifications via `NotificationRepository.createChatMessageNotification()`.
  - Local push uses `NotificationHelper.sendChatMessageNotification()`.
  - Chat notifications are categorized as `WORLD` and rendered in notifications feed.
  - Notifications are suppressed for the currently active chat via `ChatSessionManager.activeChatId`.

### Task: Notification deep-link to chat
- Status: `WORKING`
- Functions/classes:
  - Tapping chat notifications in-app routes to `ChatFragment` with safe args (`chatId`, `otherUserId`).
  - System chat notifications include deep-link extras handled by `MainActivity.handleChatNotificationNavigation()`.

---

## Page: Chat

### Task: Real-time chat list and chat screen
- Status: `WORKING`
- Functions/classes:
  - `ChatListFragment` + `ChatListViewModel` observe chat summaries via Firestore snapshot listeners.
  - `ChatFragment` + `ChatViewModel` observe latest messages in real time.
  - Chat IDs are deterministic (`sortedUserA_sortedUserB`) through `FirebaseChatService.buildChatId()`.

### Task: Message model + pagination
- Status: `WORKING`
- Functions/classes:
  - Firestore structure uses `chats/{chatId}` + `messages` subcollection.
  - `ChatViewModel.loadMore()` loads older pages (last 20 + incremental).
  - Message list merges with de-duplication by `messageId` and preserves older pages during realtime updates.

### Task: Share capsule via chat
- Status: `WORKING`
- Functions/classes:
  - `ChatViewModel.sendCapsule()` sends `CAPSULE` type messages with `capsuleId`.
  - `ChatMessagesAdapter` renders capsule card with `View` action.
  - `View` opens `CapsuleDetailsActivity` and existing unlock rules remain enforced.

### Task: Friend-only chat safety
- Status: `WORKING`
- Functions/classes:
  - `ChatRepository.sendTextMessage()` and `sendCapsuleMessage()` verify friendship via `/friends/{sortedUserA_userB}` before sending.
  - Prevents first-message chat creation between non-friends.

### Task: Chat UI polish
- Status: `WORKING`
- Functions/classes:
  - Sender and receiver bubbles are visually distinct.
  - Capsule messages are card-style with button action.
  - Empty state text: `Start a conversation`.
  - Timestamp remains visible below each message bubble.

---

## Page: Profile

### Task: View/update profile and avatar
- Status: `WORKING`
- Functions/classes:
  - `ProfileViewModel.loadUserProfile()`, `updateName()`, `updateAvatar()`.
  - `ProfileFragment` binds profile state and image picker.

### Task: Structured settings layout (header/account/user id/friends/requests/appearance/notifications/danger)
- Status: `WORKING`
- Functions/classes:
  - `fragment_profile.xml` is organized into card-style sections with updated spacing and hierarchy.
  - Keeps existing friend and friend-request adapters/actions while separating concerns per section.

### Task: Appearance customization (theme mode + color scheme)
- Status: `WORKING`
- Functions/classes:
  - `ProfileFragment.bindSettingsControls()` initializes chips/switches from persisted preferences.
  - `ProfileViewModel` exposes appearance getters/setters backed by `PreferencesManager`.
  - `ThemeManager.applyTheme()` applies mode (`SYSTEM/LIGHT/DARK`) and scheme (`GREEN/BLUE/OCHRE/GRAY`).
  - Profile appearance changes trigger activity recreate for immediate UI update.

### Task: Theme propagation across app surfaces
- Status: `WORKING`
- Functions/classes:
  - `MainActivity`, `SplashActivity`, `AuthActivity`, `OnboardingActivity`, `CreateCapsuleActivity`, and `CapsuleDetailsActivity` apply saved appearance on startup.
  - Theme-aware colors now drive nav item tint, home/splash gradients, unread dot, notification accent color, and key badge drawables.
  - Chat message bubble/text colors resolve from theme attrs for sender/receiver contrast per active scheme.

### Task: Logout and delete account
- Status: `WORKING`
- Functions/classes:
  - `ProfileViewModel.logout()`, `deleteAccount()`.
  - `ProfileFragment.handleAccountState()` delegates app exit to `MainActivity.logout()`.

### Task: Social foundation (user ID + basic friends)
- Status: `WORKING`
- Functions/classes:
  - Profile displays Firebase user ID with copy action (`tv_user_id_value`, `btn_copy_user_id`).
  - `FriendEntity`, `FriendDao`, `FriendRepository`, and `FirebaseFriendService` create the base friend model/request flow.
  - `ProfileViewModel.sendFriendRequest()` + `acceptFriend()` and `FriendsAdapter` provide basic request/list interactions.

### Task: Friend request inbox
- Status: `WORKING`
- Functions/classes:
  - Profile page now shows `Friend Requests` list with Accept/Reject actions.
  - `FriendRequestsAdapter` binds request user IDs with action buttons.
  - `ProfileViewModel` observes realtime incoming requests via `FriendRepository.observeIncomingRequests()`.
  - Firestore requests use rules-aligned fields: `senderId`, `receiverId`, `status`.
  - Accept updates request status to `ACCEPTED` and creates deterministic `/friends/{userA_userB}` with `users` list (no duplicate friendship docs).
  - Reject updates request status to `REJECTED` and removes the item from inbox stream.

### Task: Firestore security alignment (friends + requests + chat)
- Status: `WORKING`
- Functions/classes:
  - Friend request payload uses rules-aligned fields: `senderId`, `receiverId`, `status`.
  - Friendship docs are created in `/friends/{sortedUserA_sortedUserB}` with `users` list.
  - Chat code expects `/chats/{chatId}` with `participants` and `messages` subcollection.

---

## Cross-Cutting Tasks

### Task: Bottom nav feedback + selection animation
- Status: `WORKING`
- Functions/classes:
  - `MainActivity.animateBottomNavSelection()`.
  - `setupWithNavController` + item selected/reselected listeners + `navigateToTopLevelDestination(...)` keep tabs stable without duplicate stacks.

### Task: Shared countdown formatting rules across surfaces
- Status: `WORKING`
- Functions/classes:
  - `CountdownFormatter.formatRemainingDuration()` is shared by Home nearby timer, Capsules list timer, and Capsule Details timer.
  - Current thresholds:
    - `> 30 days`: years/months/days
    - `7-30 days`: days only
    - `< 7 days and >= 24h`: days + hours
    - `< 24h`: hours + minutes + seconds

### Task: Location permission flow
- Status: `WORKING`
- Functions/classes:
  - `MainActivity.ensureLocationPermissionFlow()` (first ask, rationale, settings for permanent deny).
  - `MainActivity.startLocationTrackingIfPermitted()` starts `ForegroundLocationService` only when permission is granted.
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

### Task: Nearby event anti-spam (once per session)
- Status: `WORKING`
- Functions/classes:
  - `HomeViewModel` keeps in-memory session set and emits one nearby event per capsule per app session.
  - Nearby events are persisted through `NotificationRepository.createNearbyNotification()`.

### Task: Shared capsule discovery access
- Status: `WORKING`
- Functions/classes:
  - `CapsuleDao.markCapsuleDiscovered()` + `CapsuleRepository.markCapsuleDiscovered()`.
  - `MapViewModel.checkNearbyCapules()` marks shared capsules discovered within 50m.
  - `CapsulesViewModel.canOpenCapsule()` respects `isDiscovered` for shared memory access.

### Task: World/shared comments gating
- Status: `WORKING`
- Functions/classes:
  - `CapsuleDetailsViewModel.canComment` allows comments only for shared + discovered memories.
  - `CapsuleDetailsViewModel.addComment()` validates and blocks comments until discovery.
  - `CapsuleDetailsActivity` ties comment input visibility to `canComment`.

### Task: Local data preservation + cloud restore fallback
- Status: `WORKING`
- Functions/classes:
  - `ServiceLocator.provideDatabase()` now uses explicit migrations and no destructive fallback.
  - `CapsuleRepository.restoreUserCapsulesFromCloudIfLocalEmpty()` restores local capsules from Firestore when local store is empty.
  - One-time restore is triggered in `HomeViewModel` and `CapsulesViewModel`.

---

## Partially Done / Known Errors

- `PARTIAL`: Some UI text in `CapsulePreviewBottomSheet.kt` still uses hardcoded strings and should be moved to string resources.
- `PARTIAL`: Memory details open through `CapsuleDetailsActivity` destination (not yet converted to a dedicated detail Fragment page).
- `PARTIAL`: User-reported issue (still under verification): app can crash when opening certain unlocked capsules from the Capsules tab on some devices/data states.
- `PARTIAL`: If capsules were never synced to Firestore before a historical local wipe, those local-only capsules cannot be auto-recovered.
- `PARTIAL`: Ensure deployed Firestore rules include `/chats/{chatId}` + `/messages/{messageId}` participant checks before production chat rollout.

---

## What Changed Recently and Why It Helped

### Capsules Page
- Changed from small/inconsistent lock hinting to a very large reverse countdown for time-locked capsules.
- Improvement: unlock timing is now instantly readable while scrolling, which makes lock state feel more alive.

- Changed from map icon embedded in lock badge to a separate clue icon beside lock state.
- Improvement: lock state text stays clear, and clue action is explicit/intentional.

### Map Page
- Changed from direct lock-point reveal behavior to a 1km clue-circle experience for location-locked clues.
- Improvement: user gets guidance without exposing exact hidden memory coordinates.

- Added quick map controls (center-to-me button + options popup for clue circles, nearby waves, my-location, and discovery overlay).
- Improvement: map interaction is now faster and user-controlled without changing core discovery logic.

- Added reverse-geocoded city clue text (`Somewhere in <City>`) for distant locked/shared hints.
- Improvement: exploration feels more contextual without leaking exact coordinates.

- Added a discovery moment modal (fade-in overlay + scale-in card) when entering unlock radius for new memories.
- Improvement: unlock/discovery now feels intentional and memorable.

### Capsule Details Page
- Added one-time unlock transition animation and vibration feedback when locked memory becomes accessible.
- Improvement: locked-to-unlocked flow feels smooth instead of abrupt.

### Profile/Social Foundation
- Added user ID display/copy and basic friend request/list foundation.
- Improvement: starts social layer without disrupting existing architecture.

- Reworked Profile into structured settings cards and added Appearance controls (theme mode + color scheme presets).
- Improvement: settings are cleaner, easier to scan, and personalization is now first-class.

- Applied saved theme mode/scheme across splash/auth/main/capsule flows and key drawable/UI tints.
- Improvement: color-theme changes now feel global and consistent instead of partial.

### Main Navigation Shell
- Fixed tab navigation consistency with explicit Home-return handling and top-level nav options (`launchSingleTop`, `restoreState`, `popUpTo`).
- Improvement: tapping Home reliably returns to dashboard and avoids stuck/duplicate tab states.

- Added friend request inbox (accept/reject) with Firestore status updates.
- Improvement: social loop is now interactive and demo-ready.

### Chat Layer
- Added real-time friend chat (chat list + chat detail + pagination + capsule share messages).
- Improvement: social interaction is now live and contextual to memory sharing.

- Added chat-to-notification integration with WORLD category events.
- Improvement: users get timely message awareness without opening chat screen.

- Added active-chat suppression and deep-link routing to chat from notifications.
- Improvement: avoids notification spam and improves message-to-chat navigation flow.

### Notifications Page
- Changed from Firestore-derived/mock view logic to a Room-backed event feed with real producers.
- Improvement: notification feed is persistent, reactive, and supports robust read/delete/clear behavior.

- Added Personal/World filter chips and category-specific empty states.
- Improvement: users can separate personal memory lifecycle events from world/shared events quickly.

- Fixed a runtime inflate crash in `fragment_notifications.xml` by adding explicit chip width/height attributes inside `ChipGroup`.
- Improvement: notifications tab now opens reliably without `InflateException` on startup/navigation.

---

## Key Docs

- `SETUP.md`
- `ARCHITECTURE.md`
- `FEATURES.md`
- `API_REFERENCE.md`
- `DEPLOYMENT.md`

---

Last updated: March 28, 2026
