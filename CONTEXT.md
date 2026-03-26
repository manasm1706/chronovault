# ChronoVault Context Log

## Current Snapshot (March 26, 2026)

### Reported Issues (Do Not Remove Without Explicit User Request)

1. Unlocked capsule details crash (reported by user on March 26, 2026)
- Scope: Opening an unlocked memory from capsule list/details entry flow.
- Current code-level mitigation:
  - `CapsulesFragment.navigateToDetails()` now uses a direct `Intent` launch path for `CapsuleDetailsActivity`.
  - `CapsuleDetailsActivity` validates and resolves `capsule_id` defensively before loading data.
  - `CapsuleDetailsActivity.handleActionState()` no longer resets state when already `Idle` (prevents LiveData callback loop).
  - Repeated observer registration removed from `CapsuleDetailsActivity.observeViewModel()` and `CapsulesFragment.observeViewModel()`.
- Status: `PENDING USER VERIFICATION`

### Active Behavior Rules
- Location-based memories unlock at 50m.
- Memories with no unlock method are created already unlocked.
- Creating a memory with no unlock method prompts user confirmation first.
- Map supports `Personal` and `World` modes; World mode shows eligible shared/shareable memories within 10km.

