# OrderEcho — Handoff

## Current goal

Investigate missing first-caller hint on Huawei BAC-AL00. The user confirmed three permissions, EMUI launch management and removal of the test number history, but sees no hint even after opening the app before calling. USB is unavailable. Diagnostic APK D1 is ready for the user to install and return the in-app report; the actual Huawei root cause is still unknown. Recording filesystem follow-up remains separate.

## 2026-09-18 — D1 physical-device evidence

- User confirms the ordinary-screen test overlay is visible on Huawei.
- The next photo shows D1 with only the cleared-diagnostics marker (20:39:49), with no RINGING, OFFHOOK or IDLE entries visible. This narrows investigation to event receipt or stale report display, not yet to a proven OEM cause.
- Important: D1 builds the report text once when the dialog opens; it does not refresh while open or when returning from the dialer. Asked whether the user reopened it after the call. First close with OK and reopen View diagnostics without clearing or making another call. This answer is still pending.
- Verified source manifest declares the correct PHONE_STATE action and exported receiver. Android documentation still lists PHONE_STATE among Android 8 implicit-broadcast exceptions. No code change or root-cause claim based solely on this photo.
- Next: inspect the freshly reopened report. If still only CLEARED, investigate event delivery with a bounded explicit listener probe; if it contains steps, follow the first failed gate. Avoid replacing the receiver based on a potentially stale dialog.
## 2026-09-18 — No-USB first-call diagnostics D1

- Added debug-only Settings buttons: test a distinctly labelled overlay, view a scrollable diagnostic report, and clear only diagnostic records. Test window disappears on Settings onStop or its existing 12-second timeout.
- Added local bounded 32-event trace at receiver, permission/number/session gates, service startup, live-state checks, history results and overlay acceptance/failure/removal/timeout. Fixed enum event names and timestamps only; no caller numbers, raw Intents, query rows or exception messages. Async writes are best effort. Release builds neither collect nor expose diagnostics.
- Identification rules and all telephony/recording permissions remain unchanged. Window creation success explicitly does not claim visual visibility on EMUI. This is evidence gathering, not a claimed Huawei fix.
- Files: new `calls/CallDiagnostics.kt` and Android `CallDiagnosticsTest.kt`; receiver/service/overlay manager; SettingsFragment, settings layout/strings and FirstCallSettingsTest; DECISIONS, FIRST_CALL_TESTING and this HANDOFF.
- Validation: 61 unit tests passed; app/test APK builds and lint passed (zero errors, 20 existing warnings). Six API26 device tests passed: diagnostic privacy/bounding/clear, permission-gate trace, Settings report/overlay lifecycle and existing overlay tests. A separate reviewer found no blockers; addressed their missing overlay-timeout event.
- Testing found and fixed Context-created preference access in diagnostics. System overlay intentionally excludes accessibility, so its UI lifecycle test inspects actual type-2038 windows rather than requiring accessibility text.
- Manual AOSP API26 GSM call produced RINGING → service → query FIRST → overlay accepted → 12-second timeout → IDLE. Force-stop/reopen retained and displayed the report; visually checked its screenshot. This verifies ordinary persisted evidence, not durability of the last async write during an abrupt kill.
- Evidence in ignored `app/build`: `diagnostics-build.log`, `diagnostics-device-tests.log`, `diagnostics-report.png`; initial missing-class red build in `diagnostics-red.log`.
- Delivery APK: `app/build/outputs/apk/debug/OrderEcho-first-call-diagnostics-D1.apk` (same bytes as app-debug.apk). SHA256: `E0598A318541F073CEDA73B2315EF106A3A4BCFBED54633BB9C38BFFD2C3A67F`.
- Next: user covers existing install with D1, checks test overlay visibility, clears diagnostic events, makes a test call, then returns the report screenshot. Do not repeat generic permission instructions or guess a dual-SIM/OEM fix without evidence. Existing untracked dist APKs remain untouched.
## 2026-09-18 — Publication branch

- The user selected `feat/first-incoming-call` for uploading the completed feature to this project's existing `origin`.
- Verified `origin` is `https://github.com/Luhaoyang0207/order-echo-android.git`: its main branch exactly matches this feature's existing base commit `6ff5162e7c81be3358a8db43d46ffadbc7c7fa6c`.
- Renamed the local branch to `feat/first-incoming-call`. Feature commits are `6abc367`, `e84ed90`, `9a19b1d`; this checkpoint changes only this handoff document.
- Existing untracked `dist/` APKs are excluded. No code changes or test reruns were needed; next task remains Huawei physical-device acceptance.
## 2026-09-17 — Completed first incoming caller overlay

### Changes and decisions

- Added manifest PHONE_STATE receiver, short non-exported foreground service, cancellable worker Call Log query, Norway full-number identity and in-memory session deduplication.
- Added application-context, non-touchable/non-focusable `第一次来电` overlay. Screenshot review moved it below the dialer's number while keeping it in the upper screen. OFFHOOK/IDLE, 12-second display timeout, 15-second service deadline and service destruction remove it.
- Settings exposes Phone, Call Log and Overlay status/authorization/recovery. Storage denial no longer prevents Settings access. Unrelated permission callbacks preserve Settings; restoring storage there initializes the existing recording dependencies.
- No numbers are persisted; no database, dependency, INTERNET permission or call control was added. Existing WorkManager transitively contributes WAKE_LOCK/ACCESS_NETWORK_STATE as before; there is still no INTERNET permission in the merged manifest.
- Independent review found a queued-service race. Replaced unconditional stopSelf with stopSelfResult(latestStartId); stale starts preserve current work. An API26 real-service regression passes with the fix and fails when temporarily mutated back to stopSelf. The mutation was restored and the correct APK rebuilt. Follow-up review found no remaining important issue in these paths.
- Additional declared permissions: READ_PHONE_STATE, READ_CALL_LOG, SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE. Existing storage/boot permissions remain. SDKs stay 26/28/34; only lint's Google Play ExpiredTargetSdkVersion check is excluded for this internal APK.

### Files

- Production: `calls/{CallNumber,AndroidCallNumber,CallHistory,CallHistoryChecker,IncomingCallSession,IncomingCallReceiver,IncomingCallService,FirstCallOverlayManager,FirstCallPermissions}.kt`.
- Integration: `app/src/main/AndroidManifest.xml`, `ui/MainActivity.kt`, `ui/SettingsFragment.kt`, `res/layout/fragment_settings.xml`, `res/values/strings.xml`, `app/build.gradle.kts`.
- Tests: three unit classes in `src/test/.../calls/`; three Android classes in `src/androidTest/.../calls/`; `FirstCallSettingsTest.kt`; expanded `MainActivityPermissionTest.kt`.
- Memory: AGENTS, README, ARCHITECTURE, DECISIONS, this handoff, FIRST_CALL_TESTING, and the 2026-09-17 spec/plan. Detailed per-file responsibilities are in FIRST_CALL_TESTING.

### Verification

- Final `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug` passed with JBR 17. Unit tests: 61, zero failures/errors. Lint: zero errors; 20 existing warnings in unchanged recording UI/dependency recommendations.
- API26 instrumentation: 13 ordinary tests + 1 permission-denial/recovery test + 1 opt-in queued-service race test, all passed. Overlay tests had real SYSTEM_ALERT_WINDOW approval, not skipped.
- Permission test runs separately after adb revokes storage, because revoking a granted runtime permission inside a running instrumentation process can kill that process. The service race test runs separately during an emulator-generated call with `-e testEmulatedCall true`; never opt it in on the restaurant phone.
- API37's four cursor/normalization tests passed, but full UI tests were incompatible with the existing Espresso version (InputManager.getInstance removed). A dedicated API26 AOSP image was downloaded from Google, SHA1-checked, and used instead without changing app dependencies.
- Actual API26 GSM emulation from the background with screen asleep showed the hint over the system incoming call screen. Answering removed it (no type-2038 window); calling again found prior incoming history and showed no overlay.
- Evidence in ignored `app/build/`: `first-call-final-build.log`, `api26-main-tests.log`, `api26-permission-tests.log`, `api26-service-tests.log`, `service-race-red-test.log`, `api26-first-call.png`. Emulator image/AVD also stay only under app/build; existing user AVD data and dist APKs were not changed.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (about 3.8 MB), SHA256 `4BB6C3C6062A94B405BF8303B98F7B76E471457E58051E89F24163C69891BA42`.

### Remaining limits / next task

No physical Huawei is attached. Complete all 17 manual scenarios, especially EMUI auto/background launch, reboot after unlock, locked dialer, simultaneous SIM/call waiting and fast redial. Android exposes no exact shared call ID/ring-start timestamp: the five-second cutoff cannot guarantee unbounded OEM delays or incorrect clocks. Deleted or not-yet-written system history can change classification. These limits are explicit in the acceptance document.

## 2026-09-17 — First-call rules checkpoint

- Inspected Kotlin/XML app, SDK 26/28/34 configuration, Activity, Settings, permissions, boot receiver, cleanup and recording code.
- Added caller identity, lazy incoming-history filtering and call-session rules in `calls/`, with unit tests. Design and plan are under `docs/superpowers/`.
- Baseline tests/build passed. New tests first failed on absent classes, then passed with `:app:testDebugUnitTest :app:assembleDebug` (JBR 17).
- No number persistence or new dependency. Five-second first-ring cutoff excludes contemporaneous rows; public APIs cannot prove absence of unbounded OEM delay.
- Next: receiver/service, Android query, overlay and Settings permissions.
- Existing untracked `dist/` APKs belong to the starting workspace and remain untouched.

## Completed: bilingual README

- `README.md` now presents the app's purpose, capabilities, limits, privacy boundary, build/install commands, test safety, and known asynchronous filesystem follow-up in matched Simplified Chinese and English sections.

## Completed: normalized-search final-review fix wave

- All instrumentation classes that launch `MainActivity` now use a cache-only `MainActivityTestEnvironment` rule. It restores the complete `order_echo_settings` snapshot, the previous static recording-directory override, and fixture files after every test.
- Returning from Settings now clears the retained search query to match the newly blank input; unit and Espresso regressions cover the behavior.
- A null recording-directory enumeration now throws a read failure and reaches `RecordingListState.Error`, while empty directories and per-file skip/count behavior remain distinct.
- Focused RED/GREEN evidence and the full verification contract are recorded in `.superpowers/sdd/2026-07-28-normalized-phone-search/final-fix-report.md`.
- Implementation commit: `fdb9221`.

## Unresolved architectural follow-up

- Startup cleanup, refresh, Settings statistics, manual cleanup, and individual deletion still scan/delete synchronously on the UI thread.
- Do not wrap only one call path in an ad-hoc thread. The minimum safe follow-up is a lifecycle-owned serialized filesystem executor, main-thread result delivery, stale-result suppression/cancellation, asynchronous Settings callbacks, and deterministic executor-based tests.

## Completed: explicit normalized phone search controls

- Digit-only queries now use Task 1's normalized phone matching from both the keyboard action and a dedicated `搜索` button; separator-only input resets to the unfiltered list.
- `刷新` now sits to the right of the `通话录音 · X 条` header and remains a full directory refresh without clearing the input; `搜索` sits to the right of the phone-number input and does not trigger a scan.
- The new cache-fixture Espresso regression test compiles with the app and proves a digits-only query finds a number formatted with spaces. `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest` and `git diff --check` passed on 2026-07-28.
- Run the instrumentation test and visually verify the two 48dp control rows, input retention on Refresh, and search behavior on the BAC-AL00; no Android device is attached in this environment.

## Completed: Today-first recording-list Task 2 UI

- The recording screen now shows a compact `通话录音 · N 条` heading, with today's date expanded and older dates rendered as 48dp expandable headers carrying their recording counts.
- Each date header has a visual expand/collapse indicator and routes only that date's tap to `RecordingListViewModel.toggleDate`; collapsed groups contribute no recording rows.
- Recording rows are rounded white cards with the number, time/duration, and a primary play/pause action. Progress, elapsed time, and Stop remain hidden until that exact row is active; the low-emphasis `更多` action preserves the existing deletion confirmation flow.
- Added an Espresso regression test using only an app-cache fixture directory. It verifies a yesterday header reveals and then hides its recording, without reading the real Huawei Callrecord directory.
- The test-only directory override changes no production default: ordinary runs still use `/storage/emulated/0/Sounds/Callrecord/`. No scan now reads media metadata.

## Task 2 verification

- The new test was first compiled red against the absent test-directory seam (`recordingDirectoryForTesting` unresolved), then compiled successfully after the minimal UI/integration implementation.
- `:app:assembleDebugAndroidTest` passed on 2026-07-27 with Android Studio JBR 17.
- Run `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest` and execute the new test on a connected Android device before release; no device is connected in this environment.

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
