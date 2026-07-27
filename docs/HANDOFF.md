# OrderEcho — Handoff

## Current goal

Implement the approved Today-first recording-list redesign, then validate it on the Huawei BAC-AL00 physical device.

## Completed: Today-first recording-list Task 1 expansion state

- `RecordingListViewModel` now keeps UI-only expanded-date state. Refreshing or clearing search expands today only; a non-empty matching search expands every matching date; individual dates can be toggled independently.
- The ViewModel now exposes visible month/date groups carrying each date's expansion state while retaining `RecordingGrouper`'s descending order.
- The existing activity converts these visible groups back to the adapter's current input only as a temporary compatibility boundary. The follow-up adapter/UI task must render the expansion state and route date-header taps to `toggleDate`.
- Added ViewModel coverage for the default, toggle, and search expansion rules using today, yesterday, and an older recording.
- `:app:testDebugUnitTest --tests com.luhaoyang.orderecho.ui.RecordingListViewModelTest`, the full `:app:testDebugUnitTest`, and `git diff --check` passed on 2026-07-27 with Android Studio JBR 17.

## Approved recording-list redesign

- User selected the Today-first layout from the visual comparison on 2026-07-27.
- Today starts expanded; yesterday and earlier dates start collapsed; date headers show their recording counts.
- Recording rows will prioritize phone number, time/duration, and playback. Deletion moves to a low-emphasis overflow action.
- Full design: `docs/superpowers/specs/2026-07-27-recording-list-redesign-design.md`.
- Implementation plan: `docs/superpowers/plans/2026-07-27-today-first-recording-list.md`.

## Completed: settings screen replaces recording content

- The Settings tab now clears direct recording-list views before synchronously replacing the content container with `SettingsFragment`.
- This fixes the observed screen overlay where recording controls and settings radio buttons appeared on top of one another.
- Added an instrumentation regression test asserting that Settings is visible while the recording list no longer exists in the content container.

## Settings overlay verification

- `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest` passed on 2026-07-27.
- The navigation instrumentation test requires a connected Android device and has not run in this environment.

## Completed: Huawei continuous-timestamp filename support

- Recording filenames in the form `number_yyyymmddhhmmss.amr` now parse the phone number and timestamp correctly.
- The existing `number_yyyymmdd_hhmmss.amr` format remains supported.
- A regression test covers a phone number containing spaces and a continuous timestamp.

## Outstanding device observation

- The installed device reports overlapping text in Settings, but the current layout already has a vertical `RadioGroup`. A screenshot is required to identify whether the problem is an old APK, device font/rendering behavior, or another view.

## Completed: metadata-free recording scans

- Recording discovery, cleanup, statistics, and playback-file revalidation no longer open media metadata for every AMR file.
- A recording's initial duration is intentionally unknown; Android `MediaPlayer` supplies it when that recording is actually played.
- `RecordingRepository.durationFor()` is reserved for a future background-only, single-recording duration enrichment flow. It must not be used by a scan, cleanup, or UI-thread refresh.
- A regression test proves directory scanning does not invoke the duration reader.

## Metadata-free scan verification

- The regression test failed against the previous implementation and passed after the fix.
- `:app:testDebugUnitTest :app:assembleDebug` and `git diff --check` passed on 2026-07-27.

## Completed: final-review Critical/Important fixes

- Filename timestamps now use strict calendar/time resolution. Impossible values such as `20260230` fall back to the file's last-modified time instead of being silently normalized.
- Recording scans isolate canonical-path and metadata failures per entry. Valid recordings remain available, skipped-entry counts are shown safely, and individual deletion converts file exceptions into a normal failure result.
- Cleanup continues after failed deletions, persists deleted/failed counts with its completion time, shows partial failures in Settings, and returns the counts from background work.
- A no-match search keeps the search field visible and offers `清除搜索`, restoring the unfiltered list without recreating the activity.
- Tapping a paused recording resumes its retained position instead of restarting. Recording rows now include explicit Stop and a safely derived total duration, with `时长未知` as the fallback.

## Final-review verification

- `:app:testDebugUnitTest :app:assembleDebug` passed on 2026-07-26 using Android Studio's bundled JDK 17.
- Targeted regression coverage passes for strict timestamp parsing, scan/delete exception isolation, cleanup continuation/result persistence, search clearing, pause/resume/stop behavior, and duration fallback.
- `git diff --check` passed and the app manifest still contains no `android.permission.INTERNET`.
- Physical-device playback and duration extraction remain part of BAC-AL00 acceptance because no Android device is connected to this environment.

## Final-review files changed

- Domain and cleanup: `RecordingRepository.kt`, `RecordingFile.kt`, `AppSettings.kt`, `RetentionCleaner.kt`, `CleanupWorker.kt`
- Playback and UI: `PlaybackController.kt`, `PlaybackState.kt`, `RecordingDurationReader.kt`, `RecordingListViewModel.kt`, `RecordingListAdapter.kt`, `MainActivity.kt`, `SettingsFragment.kt`, recording layouts and Chinese strings
- Regression tests: `RecordingRepositoryTest.kt`, `AppSettingsTest.kt`, `RetentionCleanerTest.kt`, `PlaybackStateTest.kt`, `RecordingListViewModelTest.kt`

## Completed: Task 6 settings, confirmations, and release checks

- Added a bottom navigation bar between `录音` and `设置`.
- Added a Settings screen with exactly five persisted retention choices (7, 30, 60, 90, and 180 days), current recording count, occupied space, oldest recording date or `无录音`, and last cleanup time or `尚未清理`.
- Added a confirmation before manual cleanup that states the selected retention period and before individual deletion that states the phone number or `未知号码`.
- Manual cleanup refreshes Settings status after success; cleanup and deletion failures use non-sensitive Chinese error UI.
- Added the requested instrumentation boundary test: it verifies the five visible choices and that cleanup does not affect a file outside the configured Callrecord directory.
- Updated README with JDK 17 build and debug-APK installation instructions.

## Task 6 verification

- `:app:testDebugUnitTest :app:assembleDebug` passed on 2026-07-26 using Android Studio's bundled JDK 17.
- `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.luhaoyang.orderecho.cleanup.CleanupBoundaryTest` compiled and packaged the requested test, but could not execute because ADB reported `No connected devices!`.
- `rg -n "android.permission.INTERNET" app/src/main/AndroidManifest.xml` returned no matches.

## Remaining physical-device acceptance

- Install the debug APK on the BAC-AL00 and test storage permission grant, refusal, and recovery.
- Confirm real Huawei AMR discovery, virtual month/date grouping, two-file playback switching, every retention boundary, deletion confirmation/failure behavior, and cleanup after relaunch/reboot.
- Confirm that non-AMR files and files outside Callrecord remain unchanged.

## Completed: Task 5 grouped recording list and permission flow

- Added Android 8 storage-permission request and a recovery screen that opens this app's system-settings page after a refusal.
- Added the Chinese recording screen with phone-number search, virtual month/date headers, safe unknown-number fallback, and distinct empty, missing-directory, and read-error states.
- Added recording-row controls for play/pause, duration/progress, size, and deletion. Playback and deletion remain delegated to the previously established safe controller and repository.
- App startup cleanup now runs after storage permission has been granted, before showing the refreshed list.
- Added the requested Espresso permission-recovery test. Its APK compiled, but there was no connected Android device to execute it (`NO_DEVICE`).

## Task 5 verification

- `:app:testDebugUnitTest :app:assembleDebug` passed on 2026-07-26.
- The targeted connected test could not run because ADB reported `No connected devices!`; run it on the BAC-AL00 before release.

## Task 5 review fixes

- Returning from the app-details settings page now rechecks storage permission and refreshes the recording list. The retry action requests permission again when it remains denied.
- Playback publishes the current player position every 500 ms while playing, and the recording row refreshes its progress and elapsed-time display.
- The permission test revokes both storage permissions before launch and uses UiAutomator to deny the Android permission prompt before asserting the recovery UI.
- The permission test waits up to five seconds for Android 8's package-installer deny-button resource ID, then tries the newer permission-controller ID and a short localized fallback. It asserts that an action was found before clicking it.
- A search with no matching phone number now has a distinct Chinese message instead of looking like an empty recording directory.

## Completed: Task 4 single-file AMR playback

- Added a platform-independent playback state reducer with Idle, Playing, Paused, and Chinese error states.
- Added `PlaybackController`, which accepts a recording only when the repository currently validates and lists it.
- Playback is revalidated immediately before `MediaPlayer.setDataSource`, which opens only the returned canonical recording path.
- Starting playback releases the prior `MediaPlayer`; completion, errors, stop, and release all free player resources. Idle pause, stop, and release calls are safe.
- Added state-transition coverage for replacing an active file, pause state retention, and stopping to Idle.

## Task 4 verification

- `:app:testDebugUnitTest :app:assembleDebug` passed on 2026-07-26.

## Completed: Task 3 settings, grouping, and retention scheduling

- Added `AppSettings`, backed by the private `order_echo_settings` preferences file. It defaults to 30 days, persists only the five approved retention values, and records the latest cleanup completion time.
- Added in-memory `RecordingGrouper` month/date sections. Both month and date are sorted newest first; the underlying Huawei recording files remain untouched.
- Added `RetentionCleaner`, which applies the existing natural-day retention rule and calls the repository for every deletion. It counts failures and continues if a deletion fails or throws.
- Added a unique once-daily WorkManager job plus a boot-completed receiver that only restores that schedule. The worker uses the fixed `Sounds/Callrecord` directory and no network capability.
- App startup now requests the same unique daily cleanup schedule, so a fresh install does not need to reboot before WorkManager work is established.
- Added unit coverage for the settings default, persistence, invalid selection rejection, and descending virtual grouping.

## Task 3 verification

- `:app:testDebugUnitTest :app:assembleDebug` passed on 2026-07-26.

## Completed: Task 2 safe recording discovery and retention domain

- Added `RecordingFile`, a local model for a validated recording and its display metadata.
- Added `RetentionPolicy` with the fixed 7, 30, 60, 90, and 180 natural-day options. Its cutoff includes today and the previous `days - 1` dates.
- Added `RecordingRepository`, which lists only canonical, direct-child AMR files in the supplied Callrecord directory. It parses Huawei names shaped as `<number>_<yyyyMMdd>_<HHmmss>.amr` and falls back to the file modification time for malformed names.
- Individual deletion revalidates the canonical direct-child path, regular-file state, and AMR extension immediately before deletion. It refuses directories and anything outside the configured directory.
- Added unit coverage for retention boundaries and allowed values, filename parsing, malformed-name fallback, non-AMR/nested exclusion, outside-path deletion, and directory deletion refusal.

## Completed: Task 1 Android shell

- Created the single-module Kotlin/XML Android app with application ID `com.luhaoyang.orderecho`.
- Set `minSdk` to 26 and `targetSdk` to 28.
- Added only storage and boot-completed permissions; the manifest has no `INTERNET` permission.
- Added the Simplified Chinese launcher activity title `餐厅通话录音` and a configuration test for the API 26 minimum.
- Verified `:app:testDebugUnitTest :app:assembleDebug`; the debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Confirmed environment

- Restaurant device: Huawei BAC-AL00 (Huawei nova 2 Plus).
- Android version: Android 8.0 (API 26).
- Root access: not used.
- Huawei built-in automatic call recording is already working.
- Recording directory: `/storage/emulated/0/Sounds/Callrecord/`.
- File format: `.amr`.
- Known file-name shape: `phoneNumber_datetime.amr`.

## First-release scope

- Scan the Huawei Callrecord directory for `.amr` files.
- Display recordings in a Chinese UI grouped virtually by **month, then date**.
- Show telephone number, recording date/time, duration, and file size where available.
- Play AMR files inside OrderEcho.
- Allow a user to delete an individual recording after confirmation.
- Allow a user to choose a retention period: **7, 30, 60, 90, or 180 days**. The default is **30 days**.
- Cleanup runs when the app starts, through a best-effort daily background job, and on an explicit user action.
- Show storage/cleanup status in Settings.

## Retention rule

The selected number means whole **natural calendar days**, including today. For a 30-day setting on 2026-07-26, retain 2026-06-27 through 2026-07-26 and delete files dated 2026-06-26 or earlier.

## Important decisions

- Do not move or rename original Huawei files. Grouping is visual only.
- Do not implement phone recording or interfere with phone calls.
- The app remains fully offline and must not request the `INTERNET` permission.
- A background job may be delayed by Android 8 battery management, so app-start cleanup is a required fallback.

## Files added in this task

- `AGENTS.md`
- `README.md`
- `docs/HANDOFF.md`
- `docs/ARCHITECTURE.md`
- `docs/DECISIONS.md`
- `docs/superpowers/specs/2026-07-26-orderecho-design.md`
- `docs/superpowers/plans/2026-07-26-orderecho-first-release.md`
- `app/src/main/java/com/luhaoyang/orderecho/model/RecordingFile.kt`
- `app/src/main/java/com/luhaoyang/orderecho/data/RetentionPolicy.kt`
- `app/src/main/java/com/luhaoyang/orderecho/data/RecordingRepository.kt`
- `app/src/test/java/com/luhaoyang/orderecho/data/RetentionPolicyTest.kt`
- `app/src/test/java/com/luhaoyang/orderecho/data/RecordingRepositoryTest.kt`

## Blockers

None for design. The actual directory path and AMR playback must be revalidated on the physical restaurant device during implementation.

## Next recommended task

Install the debug APK on the Huawei BAC-AL00 and complete the physical-device acceptance checklist above.
