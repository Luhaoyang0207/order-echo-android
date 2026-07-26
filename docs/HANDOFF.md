# OrderEcho — Handoff

## Current goal

Implement the first-release offline Android recording manager task by task.

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

Execute Task 3 from `docs/superpowers/plans/2026-07-26-orderecho-first-release.md`.
