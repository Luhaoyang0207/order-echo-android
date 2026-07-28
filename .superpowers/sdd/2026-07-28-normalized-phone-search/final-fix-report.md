# Final Fix Report

## Status

The single narrow final-fix wave is complete for findings 1–3. The UI-thread filesystem finding remains an unresolved architectural follow-up; no partial asynchronous refactor was attempted.

## Implementation commit

- `fdb9221` — `fix: resolve normalized search final review regressions`

## Findings resolved

### 1. Instrumentation safety

- Added `MainActivityTestEnvironment`, a JUnit `ExternalResource` used by every instrumentation class that launches `MainActivity`.
- Each test receives a unique canonical `Callrecord` fixture below the target application's cache directory. The rule rejects the production path and installs `MainActivity.recordingDirectoryForTesting` before the test launches the activity.
- The rule snapshots the complete private `order_echo_settings` preference file and restores it synchronously with `commit()` after each test. It also restores the previous static directory override and deletes the fixture in nested `finally` blocks, including when the test fails.
- `CleanupBoundaryTest` now uses the cache fixture and its retention change to 7 days is restored after the test.
- `MainActivityNavigationTest` and `MainActivityPermissionTest` use the same rule, so all five current `ActivityScenario.launch(MainActivity::class.java)` call sites are cache-isolated.

### 2. Search navigation

- Chosen behavior: clear the query whenever a newly inflated recording screen is entered, matching the newly blank search input.
- `MainActivity.showRecordingList()` now calls `RecordingListViewModel.enterRecordingScreen()` before cleanup/refresh.
- Unit coverage proves screen entry restores all recordings after a retained search.
- Espresso coverage searches for one number, opens Settings, returns to Recordings, verifies the field is blank, and verifies the formerly filtered recording is visible.

### 3. Null directory enumeration

- `RecordingRepository.scan()` now throws `IOException` when its child enumeration returns `null`; an actual empty array still produces an empty successful scan.
- Per-file canonical/metadata failures remain isolated and counted in `failedCount`.
- `RecordingListViewModel.refresh()` already catches scan exceptions and now reaches its existing `RecordingListState.Error` path for a null enumeration.
- Unit coverage proves both the repository boundary and ViewModel propagation.

## TDD evidence

1. Instrumentation safety RED:
   - Command: `:app:assembleDebugAndroidTest`
   - Expected failure: `MainActivityTestEnvironment` was unresolved in all three activity-test classes.
   - GREEN: the same task passed after adding the cache/preference rule.
2. Search recreation RED:
   - Command: `:app:testDebugUnitTest --tests com.luhaoyang.orderecho.ui.RecordingListViewModelTest`
   - Expected failure: `enterRecordingScreen` was unresolved.
   - GREEN: the focused ViewModel suite passed after adding the screen-entry reset and wiring it into `MainActivity`.
3. Scan enumeration RED:
   - Command: `:app:testDebugUnitTest --tests com.luhaoyang.orderecho.data.RecordingRepositoryTest --tests com.luhaoyang.orderecho.ui.RecordingListViewModelTest`
   - Expected result: 24 tests ran and the two new regressions failed because null enumeration returned an empty scan/state.
   - GREEN: the same 24 focused tests passed after the repository change.

## Final verification

- `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --rerun-tasks`
  - Result: `BUILD SUCCESSFUL`
  - 68 Gradle tasks executed.
  - 44 unit tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check`
  - Result: passed.
- Manifest `android.permission.INTERNET` search
  - Result: absent.
- Instrumentation APK compilation passed. Instrumentation behavior was not executed because no Android device is connected in this environment.

## Unresolved finding: UI-thread filesystem work

This finding is not safely fixed in this wave. The existing synchronous call paths are:

- Startup and recording-tab entry: `MainActivity.onCreate()` / recordings-tab click → `showRecordingList()` → `RecordingListViewModel.runCleanup()` → `RetentionCleaner.clean()` → `RecordingRepository.scan()` plus canonical revalidation/deletion for every expired file; `runCleanup()` then calls `refresh()`, causing another full scan.
- Resume and Refresh: `MainActivity.onResume()` and the Refresh button → `RecordingListViewModel.refresh()` → `RecordingRepository.scan()`.
- Settings statistics: `SettingsFragment.onViewCreated()` / `onResume()` → `refreshStatus()` → `SettingsHost.recordingStatistics()` → `RecordingListViewModel.recordingStatistics()` → `RecordingRepository.list()` / `scan()`.
- Manual retention cleanup: Settings confirmation → `SettingsHost.runCleanupFromSettings()` → `RecordingListViewModel.runCleanup()` → cleanup/delete loop followed by refresh/scan.
- Individual deletion also performs canonical validation, deletion, and a full refresh synchronously from the confirmation callback.

A safe follow-up requires one lifecycle-owned, serialized background filesystem executor; explicit loading/content/error results posted to the main thread; stale-result suppression or cancellation across navigation and destruction; asynchronous Settings host callbacks; and serialization between cleanup, scans, statistics, and individual deletion. Meaningful automated coverage must use a controllable executor to prove filesystem operations do not run on the main thread, destructive work is serialized, stale completions are ignored, and failures render safely. Implementing only an ad-hoc thread around one call path would leave the other paths unsafe and introduce lifecycle races, so no partial change was made.
