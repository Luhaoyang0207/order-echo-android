# OrderEcho — Handoff

## Current goal

D10 adds a user-authorized accessibility-overlay path for locked first calls. The design specification is ready for user review; no D10 application code has been added yet.

## 2026-09-22 — D10 accessibility-overlay design

- Huawei D9 physical testing and ADB prove the app posts its high-priority silent notification, but EMUI suppresses it behind the native locked incoming-call UI.
- The approved design uses an explicitly user-enabled accessibility service to own a small, noninteractive `TYPE_ACCESSIBILITY_OVERLAY`; it does not observe screen content, handle events, or control calls.
- The full design, privacy boundary, fallback behavior and BAC-AL00 acceptance steps are in `docs/superpowers/specs/2026-09-22-accessibility-lock-screen-overlay-design.md`.
- Await the user's review of that design before writing the implementation plan or application code.

## 2026-09-21 — D9 activity-free locked heads-up notification

### Evidence and change

- ADB on the connected Huawei BAC-AL00 / Android 8 recorded `moveTaskToBack: com.android.incallui`, followed by `InCallActivity windowsGone` and destruction of its display surface whenever `LockedFirstCallActivity` launched. This explains every D7/D8 outcome: window flags cannot prevent an Activity task transition.
- D9 removes `LockedFirstCallActivity` from the manifest and deletes its implementation/layout. The locked path posts only the existing high-importance `首次来电识别（锁屏提示）` channel with title `第一次来电`, no caller data, no sound/vibration, no full-screen intent and no content intent.
- The existing cancellation on answer, hang-up, service destruction and 12-second timeout remains. The unlocked noninteractive overlay remains unchanged.
- The Android test now verifies the locked notification has neither a full-screen nor content `PendingIntent`, and that cleanup removes it.

### Verification

- `:app:testDebugUnitTest`, Debug/Release APK builds, Android-test APK build and `lintDebug` passed.
- Physical D9 visual acceptance is pending user installation: confirm the Huawei call Activity stays visible and directly usable, then observe whether EMUI renders the silent high-priority banner.
- Delivery APK: `dist/OrderEcho-first-call-D9.apk`; SHA256 `660C915B90C373A0E820DD0B3F0254D9EC8801129195CF3C16DD948EADBD04AA`.

### Next

Cover-install D9. With the dedicated lock-screen channel still high priority and silent, lock the Huawei phone and call from a number whose incoming history was deleted. Confirm no `点击返回来电` screen ever appears, the native answer/reject controls work, and record whether the top banner is visible.

## 2026-09-21 — D8 non-blocking locked hint

### Evidence and change

- The Huawei D7 photos prove the high-priority channel now displays `第一次来电`, but the launched Activity puts the call UI behind a `点击返回来电` screen. The original focused/touchable Activity therefore blocks the native call experience.
- `LockedFirstCallActivity` is now a transparent, small top-centred Activity with `FLAG_NOT_FOCUSABLE`, `FLAG_NOT_TOUCHABLE` and `FLAG_NOT_TOUCH_MODAL`. Android sends its key and pointer input to the Huawei Phone window below, while the label remains visible.
- The lock-screen-only full-screen notification route, 12-second cleanup, no-number rule, silence/vibration settings and unlocked overlay path are unchanged.
- The new regression test was observed failing before these flags existed, then passing after the change. Existing visual/lifecycle tests now inspect the non-focusable Activity's own layout and lifecycle rather than asking UIAutomator to discover a window intentionally excluded from input focus.

### Verification

- `:app:testDebugUnitTest`, Debug/Release APK builds, Android-test APK build and `lintDebug` passed.
- On the local Android 35 emulator, the complete `FirstCallLockedHintTest` class passed: six executed tests and two expected API-26/keyguard skips. It verifies the label layout, lifecycle cleanup, dedicated channel, settings intent, and focus/touch pass-through flags.
- Delivery APK: `app/build/outputs/apk/debug/OrderEcho-first-call-D8.apk`; SHA256 `81A0E27813AA415E4ACB225CA5A098B0746991B22EA0BA72B727E82B69FDBB2A`.

### Next

Cover-install `OrderEcho-first-call-D8.apk`. With the phone locked, call from a number whose incoming history was deleted. Confirm `第一次来电` stays at the top while Huawei's number, answer, reject/hang-up, SMS and reminder controls remain visible and directly usable, without tapping `点击返回来电`.

## 2026-09-21 — D7 direct locked-hint channel settings

### Evidence and change

- Huawei's Notification Manager contains three categories all named `首次来电识别`: the short-lived lookup service, the obsolete D4 lock hint, and the D5/D6 locked full-screen hint. EMUI does not expose their internal IDs, so the user cannot reliably select the right category.
- The current locked hint channel now has the distinct visible name `首次来电识别（锁屏提示）`. Android permits an app to rename an existing channel without changing its ID or resetting its user-controlled behavior.
- Settings now has `打开锁屏提示通知设置`. It creates/renames the current channel then opens Android's channel-specific settings intent for `first-call-locked-full-screen-hint`; a device that does not support that intent falls back to the app's settings page.
- No caller data, recording behavior, call handling, sound or vibration changed. The new test confirms both the distinct channel name and that the Settings intent contains only this channel's ID.

### Verification

- Unit tests, Debug/Release APK builds, Android-test APK build and `lintDebug` passed.
- The local device suite passed five executed locked-hint tests, including dedicated channel naming and Settings intent targeting. Two API-26/keyguard-specific tests were conditionally skipped on Android 35.
- Delivery APK: `app/build/outputs/apk/debug/OrderEcho-first-call-D7.apk`; SHA256 `59AA3C31D1C4B6D844433F178AD634E4919D9B9E4FC60A7B7515557ECF85701C`.

### Next

Cover-install `OrderEcho-first-call-D7.apk`, open Settings and tap `打开锁屏提示通知设置`. Confirm the system title is `首次来电识别（锁屏提示）`, then look for high-priority/banner/lock-screen settings while keeping sound and vibration off. Test a new number locked and send the resulting diagnostic page.
## 2026-09-21 — D6 full-screen route diagnostics

### Evidence and change

- The D5 Huawei report proves the service receives `RINGING`, classifies the caller as `HISTORY_FIRST`, posts the locked hint and reaches its 12-second cleanup. The lock-screen photo still shows only Huawei's incoming-call UI.
- D6 Huawei evidence now identifies the blocking boundary: `LOCKED_HINT_CHANNEL_NOT_HIGH` was recorded, while neither `LOCKED_ACTIVITY_CREATED` nor `LOCKED_ACTIVITY_STARTED` was recorded. Huawei therefore did not dispatch the full-screen Activity because the hint channel is not high priority.
- D5 could not distinguish whether Huawei withheld the Activity or covered it with the call UI. D6 resolves that ambiguity: the Activity was not dispatched because the channel is not high priority.
- D6 adds fixed, number-free diagnostics only. It records whether app notifications are enabled, whether the Android 8 notification channel remains high priority, and `LockedFirstCallActivity` creation, start and stop lifecycle events. No display policy, permission, caller data, sound, vibration or call behavior changes.
- A new Android instrumentation test first failed before the lifecycle events existed, then passed after their implementation. It verifies that a visibly launched Activity records its create/start events.

### Verification

- The new lifecycle diagnostic test failed before the Activity lifecycle records existed, then passed after the implementation.
- :app:testDebugUnitTest, debug/release APK builds, Android-test APK build and lintDebug all passed.
- On the available local Android 35 emulator, the locked-hint test suite passed with three executed tests; the two tests requiring API-26 full-screen/keyguard behavior were explicitly skipped. The D5 API-26 evidence remains the direct verification of the Android 8 notification contract.
- Delivery APK: `app/build/outputs/apk/debug/OrderEcho-first-call-D6.apk`; SHA256 `39D5B6C44A5B2D328DAE210C4C9598EFBEF132A60EE2D86CA34E92D4810394DF`.

### Next

Cover-install `OrderEcho-first-call-D6.apk`, clear diagnostics, delete all history for one test number and test once while locked. Send the resulting diagnostic screenshot. The D6 acceptance section in `FIRST_CALL_TESTING.md` explains the exact interpretation.
## 2026-09-21 — D5 full-screen top caller hint

### Evidence and change

- D4 diagnostics on the Huawei device showed a locked fresh call reaches `RINGING`, finds no prior inbound call, and requests the system hint, but Huawei still makes the silent notification invisible.
- With the user's explicit approval, the locked path now posts a silent Android 8 full-screen notification that launches `LockedFirstCallActivity`. It is a small top-centred focused/touchable dialog with only `第一次来电`; it contains no number or other customer information and has no sound or vibration.
- The Activity is only selected while Keyguard is locked. Unlocked calls still use the working non-touchable overlay. The notification and Activity close on answer, hang-up, service shutdown or the 12-second deadline. The small window avoids the lower Huawei native call controls.
- Files: new `calls/LockedFirstCallActivity.kt` and its layout; changed `FirstCallLockedHint`, `FirstCallPresentation`, `IncomingCallService`, manifest/theme, locked-hint and presentation tests, README, ARCHITECTURE, DECISIONS, FIRST_CALL_TESTING and this HANDOFF.

### Verification

- Full Gradle verification passed: unit tests, Debug/Release APK builds, Android-test APK build and `lintDebug`.
- On an Android 8/API-26 emulator, the locked-hint instrumentation suite passed: the Activity visibly rendered only the top-quarter label, the notification contained a full-screen intent, and `hide()` removed the visible Activity. The test that requires actual Keyguard was skipped because this isolated emulator lacks a functioning Gatekeeper/secure-lock service.
- The local emulator cannot present a real GSM incoming-call UI or perform native accept/hang-up actions. Huawei physical-device acceptance remains required for the exact locked-call visual layer and native controls.
- Delivery APK: `app/build/outputs/apk/debug/OrderEcho-first-call-D5.apk`; SHA256 `11619125C3AEAE0770A108681CEBCEAC9A00ABAA71E468D93C5C02EE414645BD`.

### Next

Cover-install `OrderEcho-first-call-D5.apk`, keep monitoring and notifications enabled, delete all history for a test number, lock the phone and call it. Confirm the small top label is visible and Huawei's lower accept/hang-up buttons work and close it. Send a locked-call photo plus the D5 diagnostic page if it does not.
## 2026-09-21 — D4 lock-screen caller hint

### Evidence and change

- Huawei user testing isolates the failure: a fresh number displays `第一次来电` unlocked, but not locked. Earlier real-customer diagnostics reached `HISTORY_FIRST` and `OVERLAY_ADDED`, so classification and service startup are working.
- An API-26 `FLAG_SHOW_WHEN_LOCKED` overlay probe was still visually hidden behind Keyguard. A Toast was hidden too. Both probes were removed instead of shipped.
- Added `FirstCallLockedHint` and an explicit presentation choice after the existing FIRST/permission/live-RINGING checks. A locked call posts one silent, high-priority Android notification titled `第一次来电`; it has no number, no content text, no intent and no call controls. That branch does not create an overlay. The existing non-touchable overlay remains the unlocked presentation.
- The notification is cancelled on answer, hang-up, service shutdown or a 12-second display timeout. Debug diagnostics record fixed events only and never store a caller number.
- Files: new `calls/FirstCallLockedHint.kt`, `calls/FirstCallPresentation.kt`, `FirstCallLockedHintTest.kt` and `FirstCallPresentationTest.kt`; changed `IncomingCallService.kt`, `CallDiagnostics.kt`, `IncomingCallServiceTest.kt`, diagnostic label, README, ARCHITECTURE, DECISIONS, FIRST_CALL_TESTING and this HANDOFF.

### Verification

- Full final verification passed: 61 unit tests, Debug/Android-test/Release APK builds, and debug lint (zero errors; existing deprecation warnings only).
- API-26 emulator: all three `FirstCallSettingsTest` methods passed; `CallMonitoringTest`, `CallDiagnosticsTest` and `FirstCallOverlayManagerTest` passed (6 tests). `FirstCallLockedHintTest` passed with `isStatusBarKeyguard=true`. An isolated emulated GSM call passed `IncomingCallServiceTest`, including the regression that the displayed locked-screen hint disappears when the service finishes.
- Delivery APK: `app/build/outputs/apk/debug/OrderEcho-first-call-D4.apk`; SHA256 `96459709790964601BC88FFBBEEF7AAC7727CA27B3CEA0D44E01B1DC3EA9D2EC`.

### Next

Cover-install D4, retain existing permissions and the enabled monitoring switch, then test a fresh number while the phone is locked. Confirm App notifications are allowed if the system hint is absent; send the D4 diagnostic report if needed.

## Current goal

D3 runtime-reception fix is ready for Huawei installation and physical acceptance. D2 photos prove runtime PHONE_STATE and default/SIM1 callbacks deliver ringing with a number while the original manifest receiver remains silent. Production now offers opt-in foreground runtime monitoring; emulator end-to-end verification passed with the original receiver disabled. Actual Huawei first-caller overlay visibility/reliability remains to be confirmed. Recording filesystem follow-up stays separate.

## 2026-09-19 — D3 runtime reception fix

### Evidence and change

- User's D2 photos cover startup at 15:39:27, SIM1/default/runtime RINGING and number-presence at 15:39:44–45, IDLE at 15:40:07 and timeout/stop at 15:40:27. Original manifest receiver entries are absent. Repair the entry path proven to work; vendor-specific cause of missing manifest dispatch remains unknown.
- Added CallMonitoring user intent (only an enabled Boolean, default false) and non-exported foreground CallMonitoringService with one runtime PHONE_STATE receiver. Forward into existing IncomingCallReceiver/session/worker/overlay path. Both original and runtime reception, and lookup completion, respect Off.
- Settings has Enable/Disable and an explanation of the persistent generic notification. Notification has Stop. Disable resets session, cancels work and stops monitoring. Preference observation keeps visible Settings controls current when stopped externally.
- START_STICKY, BOOT_COMPLETED/MY_PACKAGE_REPLACED recovery and Activity resume attempt restoration only for enabled, permission-ready state. Existing recording cleanup boot receiver is unchanged. No new permission/dependency/network/number persistence; D2 remains a separate debug-only diagnostic tool.
- Files: new calls/CallMonitoring.kt, calls/CallMonitoringService.kt, Android CallMonitoringTest; receiver/service gating, CallDiagnostics, MainActivity/SettingsFragment, manifest/layout/strings; adjusted CallDiagnosticsTest/IncomingCallServiceTest, new FirstCallSettingsTest regression; README, ARCHITECTURE, DECISIONS, FIRST_CALL_TESTING, original design revision and this HANDOFF.

### Verification

- Unit tests: 61, zero failures/errors. Debug, Android-test and Release APK builds passed; debug lint zero errors and 20 existing warnings.
- API26 instrumentation: 7 relevant tests passed (monitor enable/duplicate/disable/recovery, diagnostic privacy/bounding, Settings). Existing queued-service race test separately passed during an isolated emulated call. New monitor initially failed compilation as absent; notification-stop UI regression failed on stale Close label before the observer fix, then passed.
- Independent review found the stale Settings button after notification Stop. Fixed with lifecycle-bound preference observation; reviewer confirmed no remaining blocker.
- Fault injection on owned API26 emulator: disabled IncomingCallReceiver component using app's own UID. D2 had zero overlay windows. Installed D3, explicitly enabled, returned Home/screen asleep, generated a fresh GSM call: runtime RINGING → HISTORY_FIRST → one overlay, visually confirmed `第一次来电` over the dialer. Same number later produced HISTORY_PREVIOUS with zero overlay windows.
- Rebooted the emulator with monitoring enabled: before opening the app, the monitoring service was present and foreground with enabled preference true. Restored the original receiver component, turned feature off in Settings and generated another fresh call: zero overlay windows and no monitor/lookup service. Test emulator stopped after checks.
- Evidence in ignored app/build: monitor-red.log, monitor-ui-red-test.log, monitor-final-build.log, monitor-final-tests.log, monitor-service-race-test.log, monitor-disabled-entry-before/after.log, monitor-incoming.png, monitor-runtime-events.xml, monitor-after-reboot.log, monitor-disabled-services.log.
- Delivery APK: `app/build/outputs/apk/debug/OrderEcho-first-call-D3.apk`; SHA256 `4D5D6608886CF55EF2BABC4CF83E01A2D487636E7E7C27E72A6CC5A2B38738C0`. Existing untracked dist APKs remain untouched.

### Next

User covers the old APK, grants existing permissions and taps Enable call identification (default Off for D1/D2 upgrades). Confirm persistent notification, delete the test caller's system incoming history and test normal incoming call; do not use the 60-second probe as the production toggle. Return D3 report if no hint. Huawei foreground-service survival, lockscreen/dialer visibility, reboot and two-SIM behavior still require physical acceptance; do not claim the target phone is fixed solely from emulator success.
## 2026-09-19 — D2 bounded reception probe ready

- Confirmed user evidence: D1 test overlay is visible; after a real call, closing and reopening the report still shows only CLEARED. No successful entry into the manifest receiver was recorded. This does not prove an OEM-specific root cause.
- Added explicit Settings action to run a debug-only, non-exported foreground probe for approximately 60 seconds. It observes runtime PHONE_STATE broadcasts, the default PhoneStateListener, and up to two active SIM subscriptions. Number presence is recorded only as a fixed present/missing event, never the number or subscription identifiers.
- Probe never calls IncomingCalls or queries Call Log and has no boot/restart behavior. Repeated starts do not extend its deadline. Unregisters all listeners on stop, with an explicit notification stop action. Release manifest/source set excludes the probe.
- Diagnostic dialog now refreshes when preferences change and releases observation on dismissal/view destruction. Report still retains at most 32 timestamped enum events. Actual production caller classification remains unchanged.
- Files: `src/debug/AndroidManifest.xml`, `src/debug/java/.../calls/CallReceptionProbeService.kt`; CallDiagnostics, SettingsFragment, settings layout/strings; new CallReceptionProbeTest and updated FirstCallSettingsTest; DECISIONS, FIRST_CALL_TESTING and this HANDOFF.
- Review: independent reviewer confirmed fixes for stale test evidence, registration-request wording and observer disposal; no remaining blockers. Listener registration return is not treated as proof of callback delivery.
- Verification: fresh unit/debug/test/release builds and debug lint passed. Unit tests 61 passed; lint zero errors/20 existing warnings after moving report formatting into a string resource. API26 instrumentation: 5 tests passed, including the real 60-second timeout/duplicate-start test, diagnostic privacy/bounding and live report refresh. The final string-resource-only formatting change was compiled/linted and exercised in the manual UI run.
- Manual API26 GSM call: runtime receiver, default listener and SIM1 listener all reported RINGING with only number-presence flags. Original receiver/query/overlay also operated. Returned report displayed the new events without reopening. Tapped notification Stop before the deadline: PROBE_STOPPED recorded and neither probe nor identification service remained running.
- Evidence in ignored `app/build`: `probe-final-build.log`, `probe-final-device-tests.log`, `probe-real-call-events.xml`, `probe-report.png`, `probe-report-bottom.png`. The test emulator was shut down after verification.
- Delivery: `app/build/outputs/apk/debug/OrderEcho-first-call-diagnostics-D2.apk`; SHA256 `C4A8362FA7DD4A42C14EC6A707934F5E55CF0D0F5F9E4582E73A663E653E5790`. Existing untracked dist APKs are untouched.
- Next: user covers existing installation with D2, taps Start 60-second listening test, calls within that window, then sends the report (scroll for all events). No need to delete call history for this reception-only probe. If the original receiver also starts working, isolate foreground process survival before attributing success to a different receiver API. Actual Huawei repair remains pending that evidence.
## 2026-09-18 — D1 physical-device evidence

- User confirms the ordinary-screen test overlay is visible on Huawei.
- The next photo shows D1 with only the cleared-diagnostics marker (20:39:49), with no RINGING, OFFHOOK or IDLE entries visible. This narrows investigation to event receipt or stale report display, not yet to a proven OEM cause.
- Important: D1 builds the report text once when the dialog opens; it does not refresh while open or when returning from the dialer. Asked whether the user reopened it after the call. First close with OK and reopen View diagnostics without clearing or making another call. On 2026-09-19 the user confirmed the freshly reopened dialog still only shows CLEARED.
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
