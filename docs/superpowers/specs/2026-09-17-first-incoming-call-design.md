# First incoming call identification

Date: 2026-09-17. Scope authorized by the user's detailed implementation request.

## Project inspection

Single `app` module; Kotlin 1.9.22/XML/AndroidX, AGP 8.2.2, Java 17,
minSdk 26, targetSdk 28, compileSdk 34. No custom Application or native bridge.
MainActivity owns storage permission and recording UI; SettingsFragment is the
permission entry point. Existing BootCompletedReceiver only schedules cleanup.
The recording search helper does substring matching and must not identify callers.

## Design

Manifest PHONE_STATE receiver records the first RINGING timestamp immediately.
A process-local session accepts an initially absent number followed by a numbered
broadcast, submits at most one lookup, and invalidates pending results on OFFHOOK
or IDLE. IDLE permits a new session. No phone number is persisted anywhere.

A short-lived, non-exported foreground service owns the background query and
application-context overlay. It posts the Android-required low-importance generic
notification immediately, checks session identity and live RINGING state before
showing, and stops after completion/hide or a bounded safety timeout. A manifest
receiver works without an Activity or a boot-started perpetual service. Huawei
startup/background management still requires physical-device configuration.

Read only CallLog.Calls. Query incoming, missed, rejected and blocked types with
DATE strictly less than the original RINGING receipt time minus 5000 ms. Project
NUMBER, DATE, TYPE; scan lazily, newest first, stopping at the first number match.
Null cursors, cancellation, provider errors and denied permissions mean UNKNOWN,
never FIRST. Cursor closure and query cancellation belong to the query lifecycle.

Use PhoneNumberUtils normalization/E164 (Norway) with an explicit Norwegian
eight-digit equivalence rule and exact canonical equality, not suffix/substring
matching. Reject blank/hidden/sentinel/non-dialable caller IDs.

One top-centred wrap-content overlay says only `第一次来电`, 24sp. Use
TYPE_APPLICATION_OVERLAY with NOT_FOCUSABLE, NOT_TOUCH_MODAL and NOT_TOUCHABLE.
Remove on OFFHOOK/IDLE, service destruction, permission failure or 12 seconds.
Never let late asynchronous results recreate a removed overlay.

Settings shows the three permission states and simple authorization controls.
Phone/Call Log runtime permissions are separate from storage; overlay opens the
package-specific system screen with an app-details fallback. Settings remains
accessible even when storage is denied.

## Constraints and limits

No networking, database, contacts, dialer replacement, call control, new library,
recording-file change, or unrelated filesystem refactor. Additional permissions:
READ_PHONE_STATE, READ_CALL_LOG, SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE (API 28).
Existing RECEIVE_BOOT_COMPLETED remains only for cleanup.

CallLog and PHONE_STATE expose no shared public per-call ID or authoritative
ring-start timestamp. The five-second margin follows the user's requested rule;
it excludes contemporaneous rows under normal delivery, but cannot mathematically
cover unbounded OEM broadcast delays or incorrect clocks. A previous call within
five seconds may be excluded. Document these limits rather than promise certainty.
Call waiting/multiple simultaneous SIM calls are not individually identifiable
with this device-wide state; require device testing, never control the calls.

After a first install or force stop Android requires launching the app once.
After ordinary reboot, receipt can resume without reopening once unlocked, subject
to EMUI settings. Locked pre-unlock direct boot is outside this implementation.

## Verification

Unit tests: Norway formats, distinct country codes/numbers, unknown callers;
incoming/missed/rejected/blocked versus outgoing; cutoff/current row exclusion;
duplicate/blank-then-number RINGING; late results after OFFHOOK/IDLE/timeout;
new session after IDLE; early exit with thousands of rows; query failure UNKNOWN.
Android tests exercise PhoneNumberUtils and real cursor/query integration using
isolated fake provider data (never modify the real device Call Log).
Build unit tests, app APK, test APK and lint. Device checklist covers all twelve
user scenarios, lock screen, EMUI background/reboot, denial/revocation and existing
recording functionality. No connected Huawei means no claim of physical acceptance.

## Evidence-based revision, 2026-09-19

D2 on Huawei delivered runtime PHONE_STATE and default/SIM1 callbacks with numbers
while the original manifest receiver remained silent. Replace the manifest-only
lifetime assumption with opt-in foreground runtime reception, a persistent generic
notification and explicit Off. Preserve the lookup/session/overlay rules. Attempt
restoration of the enabled preference after boot/unlock, APK update and Activity
resume; subject to platform/EMUI restrictions. See DECISIONS for full rationale.