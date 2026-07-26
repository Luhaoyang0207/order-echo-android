# OrderEcho — Handoff

## Current goal

Implement the first-release offline Android recording manager task by task.

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

Execute Task 6 from `docs/superpowers/plans/2026-07-26-orderecho-first-release.md`.
