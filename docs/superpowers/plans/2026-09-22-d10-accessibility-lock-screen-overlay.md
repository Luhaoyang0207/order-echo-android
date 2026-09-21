# D10 Accessibility Lock-Screen Overlay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On Huawei BAC-AL00 (Android 8 / API 26), show a compact noninteractive `第一次来电` label for a locked first call without replacing, hiding, or handling Huawei's incoming-call UI.

**Architecture:** Keep the existing PHONE_STATE and read-only Call Log classifier. A manually enabled, display-only `AccessibilityService` owns a `TYPE_ACCESSIBILITY_OVERLAY` only for a locked first-call session. Presentation remains centralized: unlocked calls use the existing application overlay; locked calls use the accessibility overlay only while its service is connected; otherwise they retain D9's silent notification fallback. `IncomingCallService` owns all timeout and terminal cleanup.

**Tech Stack:** Kotlin, Android framework APIs, Android 8/API 26 minimum, AndroidX test, Espresso/UIAutomator, Gradle.

**Spec:** `docs/superpowers/specs/2026-09-22-accessibility-lock-screen-overlay-design.md`

## Global Constraints

- Do not add `INTERNET`, call recording/control, contacts, analytics, caller-number persistence, screen-content retrieval, screenshot, key filtering, or gesture capabilities.
- The service is enabled only by the user in Android Settings; the app never enables it automatically.
- `onAccessibilityEvent` remains empty. Service metadata must not request window content, events, gestures, key filtering, or screenshots.
- The locked label is fixed `第一次来电`, silent, non-vibrating, and uses no Activity or full-screen intent.
- Keep the unlocked `TYPE_APPLICATION_OVERLAY` unchanged.
- Answer, hang-up, 12-second hint expiry, 15-second service safety deadline, service destruction, and accessibility-service disconnect are idempotent cleanup paths.

## Review Focus

- `TYPE_ACCESSIBILITY_OVERLAY` is created only by the connected service and is nonfocusable, nontouchable, and top-centred.
- An unavailable/disconnected service chooses D9 fallback and never starts an Activity.
- Settings reports a live connected service rather than auto-enabling or inferring access.
- Tests inspect the actual window flags/type and lifecycle; the Huawei test verifies the native call screen stays drawn and usable.

---

## File Map

- Create `app/src/main/java/com/luhaoyang/orderecho/calls/FirstCallAccessibilityService.kt`: protected display-only service and its process-local overlay owner.
- Create `app/src/main/res/xml/first_call_accessibility_service.xml`: minimal service metadata.
- Modify `app/src/main/AndroidManifest.xml`: register this non-exported service only.
- Modify `calls/FirstCallPresentation.kt`: add deterministic three-way routing.
- Modify `calls/IncomingCallService.kt`: show/clean up accessibility overlay while retaining D9 fallback and unlocked behavior.
- Modify `calls/CallDiagnostics.kt`: fixed, number-free lifecycle evidence.
- Modify `res/layout/fragment_settings.xml`, `res/values/strings.xml`, and `ui/SettingsFragment.kt`: status and system Accessibility Settings entry.
- Modify `test/.../FirstCallPresentationTest.kt`; add focused Android tests under `androidTest/.../calls` and `androidTest/.../ui`.
- Modify `docs/FIRST_CALL_TESTING.md`, `docs/HANDOFF.md`, and `docs/DECISIONS.md`: manual setup, verification, result, and delivery state.

## Implementation Steps

### 1. Lock the routing contract with a failing unit test

- [ ] Replace the locked-only expectation in `FirstCallPresentationTest` with three cases: unlocked + service available => `OVERLAY`; locked + service available => `ACCESSIBILITY_OVERLAY`; locked + unavailable => `HEADS_UP_NOTIFICATION`.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest`; observe failure because the new route and availability argument do not exist.
- [ ] Add `ACCESSIBILITY_OVERLAY` to `FirstCallPresentation`; change its API to `forKeyguard(isLocked: Boolean, isAccessibilityOverlayAvailable: Boolean)`. Routing order is exactly: unlocked overlay, then locked accessibility overlay if available, then notification fallback.
- [ ] Update compilation call sites, rerun the unit test, and commit `test: define D10 first-call presentation routing`.

### 2. Add a deliberately limited accessibility service

- [ ] Create `res/xml/first_call_accessibility_service.xml` with `android:canRetrieveWindowContent="false"`; omit event types and capabilities requesting gesture, key, screenshot, or window-content access. Do not declare `canPerformGestures`, `canRequestFilterKeyEvents`, or `canTakeScreenshot`.
- [ ] Register `.calls.FirstCallAccessibilityService` with `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`, `android:exported="false"`, the `android.accessibilityservice.AccessibilityService` intent filter, and metadata XML. Add no runtime permission.
- [ ] First add Android metadata tests asserting the service is non-exported, protected by `BIND_ACCESSIBILITY_SERVICE`, and references the metadata; run them to fail before production code.
- [ ] Implement `FirstCallAccessibilityService : AccessibilityService`: `onAccessibilityEvent` is empty and `onInterrupt` does nothing. `onServiceConnected` installs a process-local owner and records a fixed event. `onDestroy` disconnects it, removes a live view, and records a fixed event.
- [ ] Implement `internal object FirstCallAccessibilityOverlay` with only `isAvailable()`, `show()`, and `hide()`. It holds the connected service window manager and never accepts a number, event, node, intent, or Call Log value.
- [ ] `show()` builds one fixed-label `TextView`, `WRAP_CONTENT`, top-centred, with compact dark rounded styling. It uses `TYPE_ACCESSIBILITY_OVERLAY`, translucent format, and exactly `FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL or FLAG_NOT_TOUCHABLE`. It has no local timer; incoming service owns lifecycle.
- [ ] Make show/hide idempotent. On `RuntimeException`, remove any partially attached view, retain no reference, and return false. Never play sound/vibration, launch an Activity, or create a notification.
- [ ] Add instrumentation coverage for text, type, three flags, repeat show/hide, failure cleanup, and service disconnect. Window inspection is limited to test/emulator contexts; never auto-enable the service on the restaurant phone.
- [ ] Run focused Android tests and `:app:assembleDebugAndroidTest`; commit `feat: add display-only accessibility overlay service`.

### 3. Use the service only for locked first calls, preserve D9 fallback

- [ ] Add fixed number-free diagnostics: accessibility service connected/disconnected; accessibility overlay accepted/failed/removed; D9 fallback used.
- [ ] In `IncomingCallService`, call `FirstCallPresentation.forKeyguard(FirstCallLockedHint.isLocked(this), FirstCallAccessibilityOverlay.isAvailable())` after the existing FIRST/RINGING/permission checks.
- [ ] Preserve the unlocked `showOverlay()` code. For `ACCESSIBILITY_OVERLAY`, call `FirstCallAccessibilityOverlay.show()`, record success, and schedule the existing 12-second locked timeout.
- [ ] On accessibility add failure, record failure and post `FirstCallLockedHint` only as D9 fallback; use the same timeout. If fallback posting fails, finish normally. For the existing notification route, retain D9 unchanged except record the fallback event.
- [ ] Update `clearWork()`, `finishCall()`, and `onDestroy()` to remove both `FirstCallAccessibilityOverlay` and `FirstCallLockedHint` safely, without changing stale-start protection or manipulating Huawei's dialer.
- [ ] Add tests proving availability selects accessibility, unavailability/failure selects the D9 helper, and answer/end cleanup removes whichever locked presentation was active. Retain D9 tests that reject both full-screen and content intents.
- [ ] Run `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`; commit `feat: use accessibility overlay for locked first calls`.

### 4. Expose manual setup, never auto-enable

- [ ] Add a status text and `打开无障碍设置` button to `fragment_settings.xml` near the current lock-screen notification setting. Status says only `锁屏顶部提示：已开启／未开启`.
- [ ] Add matching resources explaining staff must manually enable the display-only service; include no customer data.
- [ ] In `SettingsFragment`, refresh status in `onViewCreated` and `onResume` using `FirstCallAccessibilityOverlay.isAvailable()`. Button opens `Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)` without package URI; failures use the existing generic settings toast.
- [ ] Add an Espresso/UI test for the status/button and `Settings.ACTION_ACCESSIBILITY_SETTINGS`; make it independent of actual system service state.
- [ ] Change diagnostic copy from D7 to D10 and keep the warning that local events cannot prove visibility.
- [ ] Run targeted settings tests and lint; commit `feat: expose D10 accessibility setup status`.

### 5. Verify on device and package

- [ ] Run:
  ```powershell
  $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
  .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
  ```
- [ ] Cover-install the debug APK on BAC-AL00. The user manually enables only OrderEcho's lock-screen display service in Accessibility Settings; verify it claims no content reading, gestures, or key control.
- [ ] Delete incoming history for a non-production test number, lock phone, call once. Verify compact fixed label, no number/sound/vibration, no `点击返回来电`, and directly usable Huawei answer/reject/SMS/reminder controls.
- [ ] While ringing, capture only safe ADB evidence: `dumpsys window windows` should show both visible/drawn Huawei `InCallActivity` and OrderEcho accessibility-overlay window; `dumpsys activity activities` must show no OrderEcho Activity launched. Use notification dump only to confirm D9 fallback was not needed. Do not export Call Log, recordings, or numbers.
- [ ] Answer, hang up, wait for timeout, then disable service and repeat. Confirm no residual window; when disabled, only D9 notification may post.
- [ ] Update handoff/decision/test docs with command output, manual step, device result, and OEM limitation if present. Copy verified artifact to `dist/OrderEcho-first-call-D10.apk`, calculate SHA-256, record it, commit logical checkpoints, and push `feat/first-incoming-call`.

## Acceptance Criteria

- API 26 locked first calls use accessibility overlay only when its manually enabled service is actually connected and never start an OrderEcho Activity.
- Label is compact, top-centred, fixed-only, silent, noninteractive, and removed on every terminal path.
- Huawei incoming-call UI remains visible, drawn, and directly usable in real-device evidence.
- Unlocked calls stay on existing overlay; unavailable accessibility remains D9 silent notification fallback.
- Documentation enables another agent to reproduce setup and interpret evidence without this chat.