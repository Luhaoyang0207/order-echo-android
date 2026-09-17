# First incoming call implementation plan

**Goal:** Show `第一次来电` during ringing only when local system Call Log has no earlier inbound call from that number.
**Architecture:** Manifest receiver, in-memory session, short foreground service, background read-only history checker and application-context overlay.
**Tech stack:** Existing Kotlin/XML/AndroidX; API 26 minimum, target 28, compile 34; no new dependency.
**Spec:** `docs/superpowers/specs/2026-09-17-first-incoming-call-design.md`.
**Execution:** Inline in the authorized implementation session, with focused commits.

## Constraints

Keep recording/retention/search behavior, remain offline, persist no numbers, use only system Call Log as history. Exclude current records with a fixed first-ring cutoff minus 5000 ms. Missing data/permission/provider error cannot assert FIRST. Default app remains Huawei Phone.

## 1. Caller identity and session rules

Files: `app/src/main/java/com/luhaoyang/orderecho/calls/{CallNumber,CallHistory,IncomingCallSession}.kt`; matching unit tests under `app/src/test/java/com/luhaoyang/orderecho/calls/`.

- [x] Write tests with literal fixtures for Norway formats, foreign-prefix collision, unavailable IDs, incoming types, cutoff, outgoing-only history, early scan termination, duplicate ringing, unknown-then-number, stale results and reset.
- [x] Run `:app:testDebugUnitTest --tests 'com.luhaoyang.orderecho.calls.*'` and observe missing-feature failures.
- [x] Implement pure number canonicalization (`CallNumber.canonical`), lazy history matching (`CallHistory.hasPreviousIncomingCall`) and main-thread session (`ringing`, `offhook`, `idle`, `acceptResult`, `dismiss`).
- [x] Rerun tests and commit this working slice with handoff notes.

## 2. Android lifecycle, query and permission integration

Files: new `calls/{AndroidCallNumber,CallHistoryChecker,IncomingCallReceiver,IncomingCallService,FirstCallOverlayManager,FirstCallPermissions}.kt`; `AndroidManifest.xml`; `ui/{MainActivity,SettingsFragment}.kt`; `res/layout/fragment_settings.xml`; `res/values/strings.xml`; Android test `calls/CallHistoryCheckerTest.kt`.

- [x] Write Android tests using MatrixCursor and an injected resolver query for exact SQL boundary, close/early exit, query failure, and platform normalization. Build test APK to expose absent integration.
- [x] Implement a receiver capturing `System.currentTimeMillis()` before work, validating action and runtime/overlay access. Process state synchronously in a process-local session; send only the first valid request to the foreground service.
- [x] Service immediately posts the required generic notification, runs a single executor query with CancellationSignal, and rechecks token, permission and live RINGING before displaying. OFFHOOK/IDLE invalidates requests immediately; service destruction/timeout cancels work and removes the overlay.
- [x] Query `DATE < ? AND TYPE IN (1,3,5,6)` using original cutoff, minimal projection, newest first; close cursor on every path. Fail closed on null/error/cancelled provider.
- [x] Overlay uses one application-context TextView, wrap-content, top-centre, 24sp, no touch/focus, twelve-second timeout, idempotent removal.
- [x] Add only phone-state, call-log, overlay, foreground-service permissions. Keep storage callback conditional to its request code; allow Settings even without storage; guard cleanup/statistics when recording model is absent.
- [x] Settings exposes separate runtime and overlay grant paths, current status, denial recovery, and concise Huawei setup guidance.
- [x] Run unit tests, app/test APK assembly and lint; fix issues introduced by this feature; commit.

## 3. Delivery and acceptance

Files: `AGENTS.md`, `README.md`, `docs/{ARCHITECTURE,DECISIONS,HANDOFF,FIRST_CALL_TESTING}.md`, this plan.

- [x] Update previous no-call-state scope to permit read-only first-caller identification while preserving all offline/recording safety rules.
- [x] Document three user permissions, transient notification, EMUI startup/background/battery settings, first launch, force-stop, reboot/unlock, timestamp uncertainty and system-history deletion consequences.
- [x] Record the twelve physical scenarios plus locked-screen, fast redial, delayed result and denied permission checks.
- [x] Run final `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`, `git diff --check`, inspect merged permissions and APK, and report ADB device availability.
- [x] Commit verified changes; deliver APK path and explicit remaining physical-device acceptance.

Verification completed on 2026-09-17: 61 unit tests and 15 API26 instrumentation tests passed; debug APK/test APK/lint passed. Physical Huawei acceptance remains the next task. See HANDOFF and FIRST_CALL_TESTING for evidence and constraints.
